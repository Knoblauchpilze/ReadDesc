package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import knoblauch.readdesc.R;
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

        RelativeLayout centering = findViewById(R.id.read_controls_centering_layout);
        LinearLayout controls = findViewById(R.id.read_controls_layout);

        // We need to update the properties of the main element to match the
        // colors defined in the preferences.
        ReadPref pref = new ReadPref(this);

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
}
