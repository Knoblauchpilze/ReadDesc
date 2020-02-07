package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import knoblauch.readdesc.R;

public class SettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    /**
     * Describe a useful collection of seek bars representing the possible channels
     * associated to a color. It also contains information about a button that is
     * used to display a preview of the color defined by the seek bars.
     */
    private class ColorSeekBars {
        SeekBar red;
        SeekBar green;
        SeekBar blue;

        Button preview;
    }

    /**
     * Used to save internally the components displaying a preview of the color
     * that will be associated to the background when in reading view. The data
     * is previewed using a button which background color is set to match the
     * information existing in the seek bars.
     */
    private ColorSeekBars m_bgColor;

    /**
     * Very similar to the `m_bgColor` but handles the color of the text in
     * reading mode.
     */
    private ColorSeekBars m_textColor;

    /**
     * Holds the text view displaying the current word flip interval expressed
     * in terms of units defined in the layout itself. Used to update the value
     * whenever the user changes the seek bar allowing to control this setting.
     */
    private TextView m_wordFlipText;

    /**
     * Holds the seek bar allowing to control the word flip interval. Used to
     * easily detect changes from this element.
     */
    private SeekBar m_wordFlipValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Create using the parent handler.
        super.onCreate(savedInstanceState);

        // Assign the main content.
        setContentView(R.layout.activity_settings);

        // Initialize the word flip interval.
        m_wordFlipText = findViewById(R.id.settings_word_flip_value);
        m_wordFlipValue = findViewById(R.id.settings_word_flip_seek_bar);

        int defWordFlip = getResources().getInteger(R.integer.settings_default_world_flip);
        m_wordFlipValue.setProgress(defWordFlip);
        m_wordFlipValue.setOnSeekBarChangeListener(this);

        m_wordFlipText.setText("" + defWordFlip + getResources().getString(R.string.settings_word_flip_unit));

        // Save relevant views in order to be able to change the suited properties.
        // Also we need to connect signals from various sliders.
        m_bgColor = new ColorSeekBars();
        m_textColor = new ColorSeekBars();

        m_bgColor.red = findViewById(R.id.settings_red_bg_seek_bar);
        m_bgColor.green = findViewById(R.id.settings_green_bg_seek_bar);
        m_bgColor.blue = findViewById(R.id.settings_blue_bg_seek_bar);
        m_bgColor.preview = findViewById(R.id.settings_bg_color_preview);

        m_textColor.red = findViewById(R.id.settings_red_text_seek_bar);
        m_textColor.green = findViewById(R.id.settings_green_text_seek_bar);
        m_textColor.blue = findViewById(R.id.settings_blue_text_seek_bar);
        m_textColor.preview = findViewById(R.id.settings_text_color_preview);

        // Connect signals from the seek bars.
        m_bgColor.red.setOnSeekBarChangeListener(this);
        m_bgColor.green.setOnSeekBarChangeListener(this);
        m_bgColor.blue.setOnSeekBarChangeListener(this);

        m_textColor.red.setOnSeekBarChangeListener(this);
        m_textColor.green.setOnSeekBarChangeListener(this);
        m_textColor.blue.setOnSeekBarChangeListener(this);

        // Initialize colors to their default values.
        int bg = ContextCompat.getColor(this, R.color.settings_default_bg_color);
        int text = ContextCompat.getColor(this, R.color.settings_default_text_color);

        m_bgColor.red.setProgress(Color.red(bg));
        m_bgColor.green.setProgress(Color.green(bg));
        m_bgColor.blue.setProgress(Color.blue(bg));

        m_textColor.red.setProgress(Color.red(text));
        m_textColor.green.setProgress(Color.green(text));
        m_textColor.blue.setProgress(Color.blue(text));

        m_bgColor.preview.setBackgroundColor(bg);
        m_textColor.preview.setBackgroundColor(text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Call base handler.
        super.onCreateOptionsMenu(menu);

        // Inflate the options menu as described in the corresponding resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_actions_menu, menu);

        // The menu was created.
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // We only want to react to changed started by the user.
        if (fromUser) {
            // Determine which switch bar has generated the event. When this is done we
            // can use it to update the background color from the preview button or to
            // change the text displaying the word flip interval.
            if (seekBar == m_wordFlipValue) {
                m_wordFlipText.setText("" + progress + getResources().getString(R.string.settings_word_flip_unit));

                return;
            }

            boolean valid = false;
            Button prev = null;
            int r = 0, g = 0, b = 0;

            // Depending on the seek bars concerned we will assign the preview and the
            // color from the relevant data.
            if (seekBar == m_bgColor.red ||
                seekBar == m_bgColor.green ||
                seekBar == m_bgColor.blue)
            {
                valid = true;
                r = m_bgColor.red.getProgress();
                g = m_bgColor.green.getProgress();
                b = m_bgColor.blue.getProgress();

                prev = m_bgColor.preview;
            }
            else if (seekBar == m_textColor.red ||
                    seekBar == m_textColor.green ||
                    seekBar == m_textColor.blue)
            {
                valid = true;
                r = m_textColor.red.getProgress();
                g = m_textColor.green.getProgress();
                b = m_textColor.blue.getProgress();

                prev = m_textColor.preview;
            }

            if (valid) {
                int c = Color.argb(255, r, g, b);
                prev.setBackgroundColor(c);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing.
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Do nothing.
    }
}
