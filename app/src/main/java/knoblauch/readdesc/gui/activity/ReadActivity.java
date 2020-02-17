package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import knoblauch.readdesc.R;
import knoblauch.readdesc.model.ReadIntent;
import knoblauch.readdesc.model.ReadParser;
import knoblauch.readdesc.model.ReadPref;

public class ReadActivity extends AppCompatActivity {

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

        // We need to update the properties of the main element to match the colors
        // defined in the preferences.
        setupFromPreferences();

        // We now need to retrieve the intent that started this activity so that we
        // can instantiate the related parser.
        instantiateParser();
    }

    /**
     * Used internally to assign the colors to each menu element so that it
     * matches the expected aspect defined in the preferences.
     */
    private void setupFromPreferences() {
        // Retrieve the preferences object.
        ReadPref pref = new ReadPref(this);

        // Retrieve auxiliary elements.
        RelativeLayout centering = findViewById(R.id.read_controls_centering_layout);
        LinearLayout controls = findViewById(R.id.read_controls_layout);

        // Assign values to the graphic elements of this activity.
        int bg = pref.getBackgroundColor();

        m_text.setBackgroundColor(bg);
        m_text.setTextColor(pref.getTextColor());

        centering.setBackgroundColor(bg);
        controls.setBackgroundColor(bg);

        m_controls.reset.setBackgroundColor(bg);
        m_controls.prev.setBackgroundColor(bg);
        m_controls.pause.setBackgroundColor(bg);
        m_controls.play.setBackgroundColor(bg);
        m_controls.next.setBackgroundColor(bg);

        m_controls.completion.setBackgroundColor(bg);
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
        Log.i("main", "Finishing");
        finish();
    }
}
