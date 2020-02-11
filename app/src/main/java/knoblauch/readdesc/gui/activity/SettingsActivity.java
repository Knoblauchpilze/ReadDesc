package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import knoblauch.readdesc.R;
import knoblauch.readdesc.gui.NotifierDialog;
import knoblauch.readdesc.gui.ResetPreferencesDialog;
import knoblauch.readdesc.model.ReadPref;

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

    /**
     * Holds the current set of preferences defined in the settings view. This
     * attribute is populated from the local data upon creating the view and
     * is then saved when needed.
     */
    private ReadPref m_prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Create using the parent handler.
        super.onCreate(savedInstanceState);

        // Assign the main content.
        setContentView(R.layout.activity_settings);

        // Retrieve layout attributes to local variables.
        loadViews();

        // Load preferences from disk.
        loadPreferences();

        // TODO: We should connect the read storage location.

        // Register this view as a listener of relevant properties.
        m_wordFlipValue.setOnSeekBarChangeListener(this);

        m_bgColor.red.setOnSeekBarChangeListener(this);
        m_bgColor.green.setOnSeekBarChangeListener(this);
        m_bgColor.blue.setOnSeekBarChangeListener(this);

        m_textColor.red.setOnSeekBarChangeListener(this);
        m_textColor.green.setOnSeekBarChangeListener(this);
        m_textColor.blue.setOnSeekBarChangeListener(this);
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
        // Reset the preferences.
        m_prefs.resetToDefault();

        // Reload the graphical elements to match the internal preferences values.
        loadPreferencesToGraphics();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // The user refused to reset the settings to default. Nothing more to do.
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // We only want to react to changed started by the user.
        if (!fromUser) {
            return;
        }

        // Determine whether the progress notification comes from the word flip
        // progress bar (and updated the value if needed, both in the preferences
        // and in the display text).
        if (seekBar == m_wordFlipValue) {
            // Update prefs object.
            m_prefs.setWordFlipInterval(progress);

            // Update the display text.
            String wordFlip = String.format(getResources().getString(R.string.settings_word_flip_text), m_prefs.getWordFlipInterval());
            m_wordFlipText.setText(wordFlip);

            return;
        }

        // Handle a modification of the background color while in reading mode.
        // Any change in the progress bar should update the preview button bg
        // color (along with the preferences' value).
        if (seekBar == m_bgColor.red || seekBar == m_bgColor.green || seekBar == m_bgColor.blue) {
            // Retrieve the new background color.
            int r = m_bgColor.red.getProgress();
            int g = m_bgColor.green.getProgress();
            int b = m_bgColor.blue.getProgress();

            // Convert it to a valid color.
            int c = Color.argb(255, r, g, b);

            // Update preferences' object.
            m_prefs.setBackgroundColor(c);

            // Update the preview button's background color.
            m_bgColor.preview.setBackgroundColor(m_prefs.getBackgroundColor());

            return;
        }

        // Handle a modification of the text color while in reading mode. This
        // is very similar to the background color.
        if (seekBar == m_textColor.red || seekBar == m_textColor.green || seekBar == m_textColor.blue) {
            int r = m_textColor.red.getProgress();
            int g = m_textColor.green.getProgress();
            int b = m_textColor.blue.getProgress();

            // Convert it to a valid color.
            int c = Color.argb(255, r, g, b);

            // Update preferences' object.
            m_prefs.setTextColor(c);

            // Update the preview button's background color.
            m_textColor.preview.setBackgroundColor(m_prefs.getTextColor());

            // No need to return but don't forget to return in case some other
            // preferences are added later on.
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
        m_prefs.save();
    }

    /**
     * Used internally to get the views used by this activity to local attributes so
     * that it is faster to access properties. This method should be called upon the
     * building of the activity so that everything is readily available to process a
     * user's request.
     */
    private void loadViews() {
        Log.i("main", "Loading views");
        // Retrieve the word flip interval.
        m_wordFlipText = findViewById(R.id.settings_word_flip_value);
        m_wordFlipValue = findViewById(R.id.settings_word_flip_seek_bar);

        // Retrieve the read storage location.
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
    }

    /**
     * Used to perform the loading of the preferences into the internal attribute and
     * then populate the display elements with the corresponding data. This method is
     * typically used whenever the activity is started to get consistent display with
     * the saved values.
     * In case no preferences are registered for a certain value the default value is
     * used and displayed. It will be saved on the next call to `save` on the internal
     * `m_prefs` object.
     */
    private void loadPreferences() {
        // Create the preferences' object and load it.
        m_prefs = new ReadPref(this);

        // Update internal attributes with the values retrieved from the local data.
        loadPreferencesToGraphics();
    }

    /**
     * Used to populate the graphical elements with the data contained in the internal
     * `m_prefs` object. A typical usage of this method is either when the prefs have
     * just been loaded from disk or when the user chose to reset to the default values.
     */
    private void loadPreferencesToGraphics() {
        // Word flip interval.
        int wordFlipInterval = m_prefs.getWordFlipInterval();
        String wordFlipText = String.format(getResources().getString(R.string.settings_word_flip_text), wordFlipInterval);
        m_wordFlipText.setText(wordFlipText);
        m_wordFlipValue.setProgress(wordFlipInterval);

        // Read storage location.
        m_readStorageLocation.setText(m_prefs.getReadStorageLocation());

        // Background color while in reading mode.
        int bgColor = m_prefs.getBackgroundColor();
        m_bgColor.red.setProgress(Color.red(bgColor));
        m_bgColor.green.setProgress(Color.green(bgColor));
        m_bgColor.blue.setProgress(Color.blue(bgColor));
        m_bgColor.preview.setBackgroundColor(bgColor);

        // Text color while in reading mode.
        int textColor = m_prefs.getTextColor();
        m_textColor.red.setProgress(Color.red(textColor));
        m_textColor.green.setProgress(Color.green(textColor));
        m_textColor.blue.setProgress(Color.blue(textColor));
        m_textColor.preview.setBackgroundColor(textColor);
    }

}
