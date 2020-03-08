package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import knoblauch.readdesc.R;
import knoblauch.readdesc.gui.ReadingControls;
import knoblauch.readdesc.gui.ReadingTextHandler;
import knoblauch.readdesc.model.ReadIntent;
import knoblauch.readdesc.model.ReadParser;
import knoblauch.readdesc.model.ReadPref;

public class ReadActivity extends AppCompatActivity implements ReadingControls.ControlsListener, ReadingTextHandler.ParagraphListener, ReadParser.ParsingDoneListener {

    /**
     * Holds the buttons allowing to control the reading process. This element
     * is initialized upon creating the view so that we can refer to it later.
     */
    private ReadingControls m_controls;

    /**
     * Holds the text items used to either display the text to be read by the
     * user or the waiting progress bar in case the read parser does not yet
     * have the available data.
     */
    private ReadingTextHandler m_textHandler;

    /**
     * Internal attribute populated from the `ReadDesc` that is currently being
     * read by this activity. We instantiate it so that we can fetch words from
     * the source and display them sequentially in the main text view.
     * We usually don't really need to know anything about this parser regarding
     * its actual content and how it fetches the data, we rely on the common
     * interface for such an object.
     */
    private ReadParser m_parser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Create the layout and inflate it. We also need to restore saved parameters
        // using the base handler.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        // Try to instantiate a parser and return in case of failure: we will pass
        // the saved instance to the instantiation method to retrieve any existing
        // progress.
        if (!instantiateParser(savedInstanceState)) {
            return;
        }

        // Retrieve the buttons from the layout that should be used to control
        // the reading process.
        createControls(savedInstanceState);

        // We need to update the properties of the main element to match the colors
        // defined in the preferences.
        setupFromPreferences();

        // Update the title for this activity: we should display the name of the read.
        String title = getResources().getString(R.string.activity_read_title);
        setTitle(String.format(title, m_parser.getName()));

        // Start the loading of the data from the source.
        m_parser.load();
    }


    /**
     * Used to retrieve the controls buttons used to interpret user's requests
     * and extract a valid object from it. We will also register this activity
     * as a listener of the controls to be notified of the user's desires.
     * The input bundle state allows to keep the progression that was actually
     * reached by the waiter in case it was performing a load operation.
     * @param savedInstanceState - the saved instance state of a previous use
     *                             of this activity.
     *
     */
    private void createControls(Bundle savedInstanceState) {
        // Retrieve buttons from the layout.
        ImageButton reset = findViewById(R.id.read_restart_read_id);
        ImageButton prev = findViewById(R.id.read_previous_chapter_id);
        ImageButton pause = findViewById(R.id.read_pause_id);
        ImageButton play = findViewById(R.id.read_resume_id);
        ImageButton next = findViewById(R.id.read_next_chapter_id);

        // Create the controls object.
        m_controls = new ReadingControls(m_parser, reset, prev, pause, play, next);

        // Register this view as a listener of the controls.
        m_controls.addOnControlsListener(this);

        // Retrieve text items.
        TextView text = findViewById(R.id.read_current_word);
        ProgressBar waiter = findViewById(R.id.read_progress_bar);
        m_textHandler = new ReadingTextHandler(text, waiter, m_parser, new Handler());

        // Register this view as a listener of the paragraphs.
        m_textHandler.addOnParagraphListener(this);

        // Register the text handler as a listener of the parsing.
        m_parser.addOnParsingDoneListener(m_textHandler);
    }

    /**
     * Used internally to terminate the activity with a failure status. We will also
     * save the `uuid` of the read that was attempted to be opened during the reading
     * session so that one can provide a relevant error message in the parent activity.
     */
    private void prepareForTermination() {
        // We want to terminate the activity with the actual completion percentage of
        // the read that was reached by the user. To do so we will once again use the
        // intent mechanism.
        Resources res = getResources();
        String keySuccess = res.getString(R.string.activity_read_key_out);

        // Create and post the result as an intent.
        Intent ret = new Intent();
        // For now we only call this method when some failure occur so we always set
        // the return value to `false`: this might change if needed.
        ret.putExtra(keySuccess, false);
        setResult(RESULT_OK, ret);

        // Terminate the activity if needed.
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // We need to save the current completion reached by the parser. Indeed even though we
        // will successfully save the read to the file when the activity is destroyed (through
        // the `onPause` method mechanism), we still need to make sure that the controls will
        // be populated with the correct value.
        // This is done here by saving the progression reached. We will not save anything else
        // (for example the controls state) because we are okay with their initial state if the
        // activity needs to be recreated. It is actually even a desired side effect because we
        // want the user to have time to resume the reading again.
        //
        // More information can be found here:
        // https://developer.android.com/guide/components/activities/activity-lifecycle
        // https://developer.android.com/guide/components/activities/activity-lifecycle#saras

        String progress = getResources().getString(R.string.activity_read_key_bundle_progress);
        outState.putFloat(progress, m_parser.getCompletion());

        // Use the base handler.
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // This method is called after the `onCreate` method and is supposed to restore
        // the `UI` parameters and some custom saved states from previous executions of
        // the activity.
        // Notably it should restore the progression reached by the waiter element of
        // the read view to its previous value. Most of the time this value will be set
        // to `100%` because it's quite rare that the user orients its device while the
        // data is being loaded.
        // More importantly, as we don't quite save the loading progress during changes
        // in the orientation we always restart from scratch this process. As such it's
        // an issue if this method restores an invalid progression to the waiter and we
        // want to prevent that.
        // So here we will forcibly reset the progression of the waiter to `0` no matter
        // the saved value.

        // Save the state from the provided instance.
        super.onRestoreInstanceState(savedInstanceState);

        // Also reset the progression *after* the reset so that we are guaranteed to
        // see the correct progress (i.e. `0`) displayed.
        ProgressBar waiter = findViewById(R.id.read_progress_bar);
        waiter.setProgress(0);
        waiter.setIndeterminate(true);
    }

    /**
     * Used internally to assign the colors to each menu element so that it
     * matches the expected aspect defined in the preferences.
     */
    private void setupFromPreferences() {
        // Retrieve the preferences object.
        ReadPref prefs = new ReadPref(this);

        // First apply preferences to the known elements.
        m_controls.updateFromPrefs(prefs);
        m_textHandler.updateFromPrefs(prefs);
        m_parser.updateFromPrefs(prefs);

        // We also need to apply the preferences to the auxiliary elements
        // such as the layouts.
        int bg = prefs.getBackgroundColor();

        LinearLayout general = findViewById(R.id.read_general_layout);
        RelativeLayout centering = findViewById(R.id.read_controls_centering_layout);
        LinearLayout controls = findViewById(R.id.read_controls_layout);

        general.setBackgroundColor(bg);
        centering.setBackgroundColor(bg);
        controls.setBackgroundColor(bg);
    }

    /**
     * Used upon building this activity to fetch the intent that started it. Normally
     * we should get an extra field describing the `Read` that should be displayed by
     * this activity. If this is not the case we will terminate the activity and get
     * back to previous view as we don't know what to display in here.
     * Note also that as this activity might be subject to orientation changes we can
     * have to re-instantiate it from an existing reading session. In this case the
     * progress for the read have theoretically be saved through the `onPause` method
     * but in order to avoid a reload of this information from the disk we actually
     * also save it in the bundle that is passed to the next instance of the activity.
     * This value is then passed to this method as the `savedInstanceState` argument.
     * In case no previous instance could be found a `null`value is passed which tells
     * that we need to fetch the progression from the read itself.
     * @param savedInstanceState - the progress saved from a previous execution of the
     *                             activity.
     * @return - `true` if the parser could correctly be instantiated and `false` if
     *           this is not the case.
     */
    private boolean instantiateParser(Bundle savedInstanceState) {
        // Retrieve the intent that started this activity.
        Intent will = getIntent();

        // The `Read` to display should be registered under the following key.
        String key = getResources().getString(R.string.activity_read_key_in);

        // Retrieve the intent that started this activity so that we can instantiate
        // the related parser. Note that we will analyze the input bundle to determine
        // whether we have a chance to retrieve the progression reached by an earlier
        // version of this activity.
        // This typically happen when the orientation is changed: we successfully
        // save the progression for this read to the disk but we need to also save
        // and transmit it to this activity (so as not to be forced to read again
        // from the disk).
        // In case we can't find anything we will use a default negative value so
        // that the `instantiateParser` method knows that we should retrieve the
        // progression from the described read.
        float savedProgress = -1.0f;
        if (savedInstanceState != null) {
            String prgKey = getResources().getString(R.string.activity_read_key_bundle_progress);
            savedProgress = savedInstanceState.getFloat(prgKey, -1.0f);
        }

        // Retrieve the description of the read to display. Android is already able
        // to return `null` in case the parcelable cannot be found.
        ReadIntent read = will.getParcelableExtra(key);
        if (read == null) {
            // Terminate the activity if the creation of the `ReadIntent` failed.
            prepareForTermination();

            return false;
        }
        else {
            try {
                m_parser = new ReadParser(read, this, savedProgress);
            }
            catch (Exception e) {
                // We failed to load the parser from the read's description. There's
                // no point in continuing further, get back to the recent reads view.
                prepareForTermination();

                return false;
            }

            // Register this activity as a listener of the parser.
            m_parser.addOnParsingDoneListener(this);
        }

        return true;
    }

    /**
     * Used internally to format the progression message to display in a nice
     * human readable way the current state of the progression for this read.
     * @return - a string indicating the progression for this read in a human
     *           readable way.
     */
    private String formatProgressionDisplay() {
        // Retrieve the progression message.
        Resources res = getResources();
        String msg = res.getString(R.string.activity_recent_reads_save_progress_message);

        // Convert the progression into a nice integer (rather than a float value).
        int prg = Math.round(100.0f * m_parser.getCompletion());

        // Format it so as to display relevant information.
        return String.format(msg, prg, m_parser.getName());
    }

    @Override
    public void onActionRequested(ReadingControls.Action action) {
        // Detect which type of action has been requested and update the parser
        // from it. This will only be done in case the parser is ready.
        if (!m_parser.isReady()) {
            return;
        }

        // Update the parser from the input action.
        switch (action) {
            case Rewind:
                m_parser.rewind();
                break;
            case PreviousParagraph:
                m_parser.moveToPrevious();
                break;
            case NextParagraph:
                m_parser.moveToNext();
                break;
            default:
                // Nothing to do, it will be handled afterwards.
                break;
        }

        // Now update the word flip task.
        if (action == ReadingControls.Action.Play) {
            m_textHandler.start();
        }
        if (action == ReadingControls.Action.Pause) {
            m_textHandler.stop();
        }
    }

    @Override
    public void onParsingStarted() {
        // Nothing to be done here.
    }

    @Override
    public void onParsingProgress(float progress) {
        // Nothing to be done here.
    }

    @Override
    public void onParsingFinished() {
        // Nothing to be done here.
    }

    @Override
    public void onParsingFailed() {
        // The parsing of the read's source failed: we can't do anything more, we
        // just have to return to the recent reads activity. We should also enable
        // the controls but as we will be terminated soon there's really no point
        // in doing that.
        prepareForTermination();
    }

    @Override
    public void onResume() {
        // Perform needed de/activation of buttons so that we have a consistent state
        // in the controls panel.
        m_controls.setState(ReadingControls.State.Stopped);

        // Use the base handler.
        super.onResume();
    }

    @Override
    public void onPause() {
        // Remove any callback for the word flip task.
        m_textHandler.stop();

        // We want to save the progression we reached for this read to the dedicated file.
        boolean success = m_parser.saveProgression(this);

        if (!success) {
            Resources res = getResources();
            String msg = String.format(res.getString(R.string.activity_recent_reads_save_progress_failure), m_parser.getName());
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }

        // Notify the progression reached on this read.
        Toast.makeText(this, formatProgressionDisplay(), Toast.LENGTH_SHORT).show();

        // Use the base handler.
        super.onPause();
    }

    @Override
    public void onStop() {
        // In case we're stopping the activity we want to stop any loading process of the
        // associated parser: indeed as we won't be reusing the activity it does not make
        // sense to continue loading meaningless data.
        m_parser.cancel();

        // Call the base handler.
        super.onStop();
    }

    @Override
    public void onParagraphReached() {
        // We want to stop the scheduling of the word flip task.
        m_textHandler.stop();

        // Also we need to update the controls with the current state of the reader.
        m_controls.setState(ReadingControls.State.Stopped);
    }

}
