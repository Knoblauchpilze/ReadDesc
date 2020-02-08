package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import knoblauch.readdesc.R;
import knoblauch.readdesc.gui.NotifierDialog;
import knoblauch.readdesc.gui.ResetPreferencesDialog;

public class SettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, NotifierDialog.NoticeDialogListener {

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

    /**
     * Holds the text view representing the string for the storage location of
     * distant read source. This will be used to fetch locally the remote reads
     * so that the user can read them even when not connected to the internet.
     */
    private TextView m_readStorageLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Create using the parent handler.
        super.onCreate(savedInstanceState);

        // Assign the main content.
        setContentView(R.layout.activity_settings);

        // Initialize the word flip interval.
        m_wordFlipText = findViewById(R.id.settings_word_flip_value);
        m_wordFlipValue = findViewById(R.id.settings_word_flip_seek_bar);

        m_wordFlipValue.setOnSeekBarChangeListener(this);

        // Initialize the read storage location.
        m_readStorageLocation = findViewById(R.id.settings_read_storage_location);

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

        // Load preferences (or use default values).
        loadPreferences();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Check which menu item has been selected and perform the appropriate action.
        switch (item.getItemId()) {
            case R.id.settings_menu_apply:
                // The activity is finished (we consider that the user is done with
                // this view when the apply button is selected). It will automatically
                // call the `onPause` method and thus trigger the save of the prefs
                // for the activity.
                finish();

                return true;
            case R.id.settings_menu_reset:
                // Create a dialog to ask the user for confirmation. Dialogs in android
                // are asynchronous so we should not expect to do anything here. Instead
                // we will wait for the specific slots to be called indicating that the
                // dialog was confirmed.
                DialogFragment dialog = new ResetPreferencesDialog(this, this);
                dialog.show(getSupportFragmentManager(), "ResetPrefDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // Reset word flip interval.
        int defWordFlip = getResources().getInteger(R.integer.settings_default_world_flip);
        m_wordFlipValue.setProgress(defWordFlip);

        String wordFlip = String.format(getResources().getString(R.string.settings_word_flip_text), defWordFlip);
        m_wordFlipText.setText(wordFlip);

        // Reset read storage location.
        m_readStorageLocation.setText(R.string.settings_default_read_storage_location);

        // Reset background and text colors.
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
    public void onDialogNegativeClick(DialogFragment dialog) {
        // The user refused to reset the settings to default. Nothing more to do.
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // We only want to react to changed started by the user.
        if (fromUser) {
            // Determine which switch bar has generated the event. When this is done we
            // can use it to update the background color from the preview button or to
            // change the text displaying the word flip interval.
            if (seekBar == m_wordFlipValue) {
                String wordFlip = String.format(getResources().getString(R.string.settings_word_flip_text), progress);
                m_wordFlipText.setText(wordFlip);

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

    @Override
    public void onPause() {
        // Call the base handler.
        super.onPause();

        // We need to save preferences as this activity is not the current one anymore.
        // In order not to lose data we save it beforehand.
        savePreferences();
    }

    /**
     * Used to perform the saving of the preferences for the application into the dedicated
     * location provided by the API. We will save only the relevant data so as to be able to
     * recreate the view upon the next loading.
     * This include the word flip interval, the storage location of the remote reads and the
     * colors to apply when in reading mode.
     */
    private void savePreferences() {
        // Retrieve the preferences editor.
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        Resources res = getResources();

        // Save each preferences key.
        editor.putInt(
                res.getString(R.string.settings_word_flip_pref_key),
                m_wordFlipValue.getProgress()
        );

        editor.putString(
                res.getString(R.string.settings_read_storage_location_pref_key),
                m_readStorageLocation.getText().toString()
        );

        ColorDrawable bgDraw = (ColorDrawable)m_bgColor.preview.getBackground();
        int bgColor = bgDraw.getColor();
        editor.putInt(
                res.getString(R.string.settings_read_mode_bg_color_pref_key),
                bgColor
        );

        ColorDrawable textDraw = (ColorDrawable)m_textColor.preview.getBackground();
        int textColor = textDraw.getColor();
        editor.putInt(
                res.getString(R.string.settings_read_mode_text_color_pref_key),
                textColor
        );

        // Actually save preferences.
        editor.apply();
    }

    /**
     * Used with a similar purpose to `savePreferences` but in the case preferences should be restored
     * from their storage location. This method assumes that the internal components have already be
     * loaded and point to valid locations and will try to assign the corresponding state to each one
     * of them based on what's read from the preferences.
     */
    private void loadPreferences() {
        // Retrieve the preferences editor.
        SharedPreferences pref = getPreferences(MODE_PRIVATE);

        Resources res = getResources();

        // Restore the word flip interval.
        int wordFlipInterval = pref.getInt(
                res.getString(R.string.settings_word_flip_pref_key),
                R.integer.settings_default_world_flip
        );

        String wordFlipText = String.format(getResources().getString(R.string.settings_word_flip_text), wordFlipInterval);
        m_wordFlipText.setText(wordFlipText);
        m_wordFlipValue.setProgress(wordFlipInterval);

        // Restore read storage location.
        String readStorage = pref.getString(
                res.getString(R.string.settings_read_storage_location_pref_key),
                res.getString(R.string.settings_default_read_storage_location)
        );
        m_readStorageLocation.setText(readStorage);

        // Restore background color while in reading mode.
        int defBgColor = res.getColor(R.color.settings_default_bg_color);
        int bgColor = pref.getInt(
                res.getString(R.string.settings_read_mode_bg_color_pref_key),
                defBgColor
        );
        m_bgColor.red.setProgress(Color.red(bgColor));
        m_bgColor.green.setProgress(Color.green(bgColor));
        m_bgColor.blue.setProgress(Color.blue(bgColor));
        m_bgColor.preview.setBackgroundColor(bgColor);

        // Restore text color while in reading mode.
        int defTextColor = res.getColor(R.color.settings_default_text_color);
        int textColor = pref.getInt(
                res.getString(R.string.settings_read_mode_text_color_pref_key),
                defTextColor
        );
        m_textColor.red.setProgress(Color.red(textColor));
        m_textColor.green.setProgress(Color.green(textColor));
        m_textColor.blue.setProgress(Color.blue(textColor));
        m_textColor.preview.setBackgroundColor(textColor);
    }
}
