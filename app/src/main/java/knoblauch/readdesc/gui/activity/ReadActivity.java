package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import knoblauch.readdesc.R;
import knoblauch.readdesc.gui.WordFlipTask;
import knoblauch.readdesc.model.ReadIntent;
import knoblauch.readdesc.model.ReadParser;
import knoblauch.readdesc.model.ReadPref;

public class ReadActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * Convenience class which holds all the relevant buttons used to control
     * the reading process. This activity will listen to click on these so as
     * to stop, resume or move to a specific part of the read.
     */
    private class Controls {
        ImageButton reset;
        ImageButton prev;
        ImageButton pause;
        ImageButton play;
        ImageButton next;

        SeekBar completion;
    }

    /**
     * Holds the buttons allowing to control the reading process. This element
     * is initialized upon creating the view so that we can refer to it later.
     */
    private Controls m_controls;

    /**
     * Used to hold the main text view allowing to display the current content
     * of the view. Its content is updated each time a period of time defined
     * in the preferences has past to flip to the next word of the view.
     */
    private TextView m_text;

    /**
     * Internal attribute populated from the `ReadDesc` that is currently being
     * read by this activity. We instantiate it so that we can fetch words from
     * the source and display them sequentially in the main text view.
     * We usually don't really need to know anything about this parser regarding
     * its actual content and how it fetches the data, we rely on the common
     * interface for such an object.
     */
    private ReadParser m_parser;

    /**
     * The handler used by this parser to time the flipping of the word displayed
     * in the main text view. This will be used internally to keep track of the
     * time.
     */
    private WordFlipTask m_timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Create the layout and inflate it. We also need to restore saved parameters
        // using the base handler.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        // We know need to retrieve the views used by this activity.
        m_controls = new Controls();

        m_controls.reset = findViewById(R.id.read_restart_read_id);
        m_controls.prev = findViewById(R.id.read_previous_chapter_id);
        m_controls.pause = findViewById(R.id.read_pause_id);
        m_controls.play = findViewById(R.id.read_resume_id);
        m_controls.next = findViewById(R.id.read_next_chapter_id);

        m_controls.completion = findViewById(R.id.read_completion_percentage_id);

        m_text = findViewById(R.id.read_current_word);

        // We now need to retrieve the intent that started this activity so that we
        // can instantiate the related parser.
        instantiateParser();

        // We need to update the properties of the main element to match the colors
        // defined in the preferences.
        setupFromPreferences();

        // Connect signals to the local slot so that we can react to the user's will
        // regarding the controls.
        m_controls.reset.setOnClickListener(this);
        m_controls.prev.setOnClickListener(this);
        m_controls.pause.setOnClickListener(this);
        m_controls.play.setOnClickListener(this);
        m_controls.next.setOnClickListener(this);
    }

    /**
     * Used internally to assign the colors to each menu element so that it
     * matches the expected aspect defined in the preferences.
     */
    private void setupFromPreferences() {
        // Retrieve the preferences object.
        ReadPref prefs = new ReadPref(this);

        // Retrieve auxiliary elements.
        RelativeLayout centering = findViewById(R.id.read_controls_centering_layout);
        LinearLayout controls = findViewById(R.id.read_controls_layout);

        // Assign values to the graphic elements of this activity.
        int bg = prefs.getBackgroundColor();

        m_text.setBackgroundColor(bg);
        m_text.setTextColor(prefs.getTextColor());

        centering.setBackgroundColor(bg);
        controls.setBackgroundColor(bg);

        m_controls.reset.setBackgroundColor(bg);
        m_controls.prev.setBackgroundColor(bg);
        m_controls.pause.setBackgroundColor(bg);
        m_controls.play.setBackgroundColor(bg);
        m_controls.next.setBackgroundColor(bg);

        m_controls.completion.setBackgroundColor(bg);

        // Instantiate the timer objects used to perform the flipping of the word.
        instantiateTimer(prefs);
    }

    /**
     * Used upon building this activity to fetch the intent that started it. Normally
     * we should get an extra field describing the `Read` that should be displayed by
     * this activity.
     * If this is not the case we will terminate the activity and get back to previous
     * view as we don't know what to display in here.
     */
    private void instantiateParser() {
        // Retrieve the intent that started this activity.
        Intent will = getIntent();

        // The `Read` to display should be registered under the following key.
        String key = getResources().getString(R.string.start_reading_intent_desc);

        // Retrieve the description of the read to display. Android is already able
        // to return `null` in case the parcelable cannot be found.
        ReadIntent read = will.getParcelableExtra(key);
        if (read == null) {
            // Terminate the activity if the creation of the `ReadIntent` failed.
            prepareForTermination(false);
        }
        else {
            m_parser = ReadParser.fromRead(read);
        }

        // Update controls so that they match the state of the parser (i.e. reset
        // button might not always be allowed (in case we didn't read any word yet
        // and so on).
        // TODO: Actually handle the state of the controls based on the current state
        // of the parser.
        m_controls.reset.setEnabled(false);
        m_controls.prev.setEnabled(false);
        m_controls.pause.setEnabled(false);

        m_controls.play.setEnabled(true);
        m_controls.next.setEnabled(true);
    }

    /**
     * Used internally to terminate the activity with the success status. We will also
     * save the `uuid` of the read that was attempted to be opened during the reading
     * session so that one can provide a relevant error message in the parent activity.
     * Whether or not the reading was a success is set as defined by the input arg.
     * @param success - `true` if the termination should consider that the reading was
     *                  a success and `false` otherwise.
     */
    private void prepareForTermination(boolean success) {
        // We want to terminate the activity with the actual completion percentage of
        // the read that was reached by the user. To do so we will once again use the
        // intent mechanism.
        Resources res = getResources();
        String keySuccess = res.getString(R.string.read_mode_success_notification);
        String keyUuid = res.getString(R.string.read_mode_uuid_notification);

        // Create and post the result as an intent.
        Intent ret = new Intent();
        ret.putExtra(keySuccess, success);
        // TODO: Actually save the uuid of the read if possible.
        ret.putExtra(keyUuid, 1);
        setResult(RESULT_OK, ret);

        // Terminate the activity if needed.
        finish();
    }

    /**
     * Used to instantiate the needed objects to keep track of the time intervals to
     * flip the main text view display.
     * @param prefs - the preferences to use to retrieve the word flip interval.
     */
    private void instantiateTimer(ReadPref prefs) {
        // Retrieve the world flip interval from the preferences.
        int wordFlip = prefs.getWordFlipInterval();

        // Create the wrapper for the timing task.
        m_timer = new WordFlipTask(wordFlip, m_text, m_parser, new Handler());
    }

    @Override
    public void onClick(View v) {
        // We need to determine what to do based on the view producing the click.
        if (v == m_controls.reset) {
        }
        else if (v == m_controls.prev) {

        }
        else if (v == m_controls.next) {

        }
        else if (v == m_controls.play) {
            // Start the word flip task and update the state of the controls button.
            m_timer.start();

            toggleStartStop(false);
        }
        else if (v == m_controls.pause) {
            // Stop the word flip task if it was started.
            m_timer.stop();

            // Also reset the state of the buttons so that the user can start the
            // reading again.
            toggleStartStop(true);
        }
    }

    @Override
    public void onResume() {
        // Perform needed de/activation of buttons so that we have a consistent state
        // in the controls panel.
        toggleStartStop(true);

        // Use the base handler.
        super.onResume();
    }

    @Override
    public void onPause() {
        // Remove any callback for the word flip task.
        m_timer.stop();

        // We want to save the progression we reached for this read to the dedicated file.
        boolean success = m_parser.saveProgression(this);

        if (!success) {
            Resources res = getResources();
            String msg = String.format(res.getString(R.string.read_desc_failure_save_progress), m_parser.getName());
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }

        // Use the base handler.
        super.onPause();
    }

    /**
     * Used to reset the states of the play and pause buttons so that one of them
     * is enabled while the other one is disabled. The `play` button will be set
     * with the status described in input while the `pause` button will be assigned
     * a value of `!toggle`.
     * @param toggle - `true` if the `play` button should be set to active.
     */
    private void toggleStartStop(boolean toggle) {
        m_controls.play.setEnabled(toggle);
        m_controls.pause.setEnabled(!toggle);
    }

}
