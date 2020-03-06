package knoblauch.readdesc.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.core.content.ContextCompat;

import knoblauch.readdesc.R;

import static android.content.Context.MODE_PRIVATE;

public class ReadPref {

    /**
     * The context providing access to the default value of the preferences and also to the
     * framework allowing to save them to the disk. This context is usually dependent on
     * the activity creating this object.
     */
    private Context m_context;

    /**
     * Describe the background color to use in the reading view. This color is set through
     * the preferences and is meant to be not super aggressive so that the user can focus
     * on the reading itself.
     * Note that he color is not stored as a real color but as an `int-color` which is a
     * translation of a color into a single integer value.
     */
    private int m_bgColor;

    /**
     * The color of the text. Should ideally contrast well with the background color for
     * the text to be easily separated from the background.
     * This is stored in the same way as the `m_bgColor`, i.e. through an `int-color`.
     */
    private int m_textColor;

    /**
     * The interval in milliseconds that a word is displayed on the screen. After a duration
     * equal to this value has elapsed the next word is displayed.
     */
    private int m_wordFlipInterval;

    /**
     * The size (expressed in word(s)) of a single `step` motion when pressing the
     * corresponding control button while in reading mode. This applies both for
     * forward and backwards motion.
     */
    private int m_wordStep;

    /**
     * Create a default read preference object with no associated preferences. The context is
     * used to retrieve the properties saved in the application and if none are defined, we
     * use the default values.
     * @param context - the context from which preferences should be created.
     */
    public ReadPref(Context context) {
        // Assign the internal context.
        m_context = context;

        // Perform the loading of the preferences.
        load();
    }

    /**
     * Retrieve the background color to use when in reading mode.
     * @return - the background color to use.
     */
    public int getBackgroundColor() {
        return m_bgColor;
    }

    /**
     * Assign a new background color to use when in reading mode.
     * @param bg - the new color to use.
     */
    public void setBackgroundColor(int bg) {
        m_bgColor = bg;
    }

    /**
     * Retrieve the text color to use when in reading mode.
     * @return - the text color to use.
     */
    public int getTextColor() {
        return m_textColor;
    }

    /**
     * Assign a new text color to use when in reading mode.
     * @param text - the new color to use.
     */
    public void setTextColor(int text) {
        m_textColor = text;
    }

    /**
     * Retrieve the duration in milliseconds that a single word is displayed on screen
     * before the next one is displayed.
     * @return - the current word flip interval in milliseconds.
     */
    public int getWordFlipInterval() {
        return m_wordFlipInterval;
    }

    /**
     * Assign a new word flip interval representing the duration a single word is displayed
     * on screen before a new one is displayed.
     * @param wordFlip - the duration in milliseconds of the word flip.
     */
    public void setWordFlipInterval(int wordFlip) {
        m_wordFlipInterval = wordFlip;
    }

    /**
     * Retrieve the size in word(s) of a single `step` motion while in reading mode.
     * @return - the size in word(s) of a `step`.
     */
    public int getWordStep() { return m_wordStep; }

    /**
     * Assign a new word step value describing a single motion while in reading mode.
     * @param wordStep - the new size in word(s) of a `step` motion.
     */
    public void setWordStep(int wordStep) { m_wordStep = wordStep; }

    /**
     * Used in order to load the preferences from the values saved on the disk. Uses the
     * internal context to retrieve the previously saved values or uses the default ones
     * if none have been saved already.
     */
    private void load() {
        // Nothing can be done if the context is not set.
        if (m_context == null) {
            return;
        }

        // Retrieve the preferences editor from the context along with the resources
        // object which will provide default value.
        Resources res = m_context.getResources();
        String key = res.getString(R.string.activity_settings_pref_name);
        SharedPreferences pref = m_context.getSharedPreferences(key, MODE_PRIVATE);

        // Restore the word flip interval or create it from default value if it does
        // not exist.
        m_wordFlipInterval = res.getInteger(R.integer.activity_settings_pref_word_flip_default);
        key = res.getString(R.string.activity_settings_pref_xml_key_word_flip);
        if (pref.contains(key)) {
            m_wordFlipInterval = pref.getInt(key, m_wordFlipInterval);
        }

        // Restore the word step or create it from default value if it does not exist.
        m_wordStep = res.getInteger(R.integer.activity_settings_pref_word_step_default);
        key = res.getString(R.string.activity_settings_pref_xml_key_word_step);
        if (pref.contains(key)) {
            m_wordStep = pref.getInt(key, m_wordStep);
        }

        // Restore background color while in reading mode.
        m_bgColor = ContextCompat.getColor(m_context, R.color.activity_settings_pref_color_bg_default);
        key = res.getString(R.string.activity_settings_pref_xml_key_color_bg);
        if (pref.contains(key)) {
            m_bgColor = pref.getInt(key, m_bgColor);
        }

        // Restore text color while in reading mode.
        m_textColor = ContextCompat.getColor(m_context, R.color.activity_settings_pref_color_text_default);
        key = res.getString(R.string.activity_settings_pref_xml_key_color_text);
        if (pref.contains(key)) {
            m_textColor = pref.getInt(key, m_textColor);
        }
    }

    /**
     * Used to perform a save operation of the values defined in this object to the disk.
     * The values can then be retrieved on a later session of the application.
     */
    public void save() {
        // Nothing can be done if the context is not set.
        if (m_context == null) {
            return;
        }

        // Retrieve the preferences editor from the context along with the resources
        // object which will provide default value.
        Resources res = m_context.getResources();
        String key = res.getString(R.string.activity_settings_pref_name);
        SharedPreferences pref = m_context.getSharedPreferences(key, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        // Save the word flip interval.
        key = res.getString(R.string.activity_settings_pref_xml_key_word_flip);
        editor.putInt(key, m_wordFlipInterval);

        // Save the word step.
        key = res.getString(R.string.activity_settings_pref_xml_key_word_step);
        editor.putInt(key, m_wordStep);

        // Save the background color while in reading mode.
        key = res.getString(R.string.activity_settings_pref_xml_key_color_bg);
        editor.putInt(key, m_bgColor);

        // Save the text color while in reading mode.
        key = res.getString(R.string.activity_settings_pref_xml_key_color_text);
        editor.putInt(key, m_textColor);

        // Apply the modifications.
        editor.apply();
    }

    /**
     * Used to perform a reset of the preferences to their default values as defined by
     * the internal context. Note that only the internal values are changed but nothing
     * is dumped to the disk just yet. One should call the `save` method for the reset
     * to be validated.
     */
    public void resetToDefault() {
        // Nothing can be done if the context is not set.
        if (m_context == null) {
            return;
        }

        // Retrieve the resources manager which will help retrieving the default values
        // for some properties.
        Resources res = m_context.getResources();

        // Restore each preference with its default value.
        m_wordFlipInterval = res.getInteger(R.integer.activity_settings_pref_word_flip_default);
        m_bgColor = ContextCompat.getColor(m_context, R.color.activity_settings_pref_color_bg_default);
        m_textColor = ContextCompat.getColor(m_context, R.color.activity_settings_pref_color_text_default);
    }
}
