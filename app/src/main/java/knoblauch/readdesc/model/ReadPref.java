package knoblauch.readdesc.model;

import android.graphics.Color;

public class ReadPref {

    /**
     * Describe the background color to use in the reading view. This color is set through
     * the preferences and is meant to be not super aggressive so that the user can focus
     * on the reading itself.
     */
    Color m_bgColor;

    /**
     * The color of the text. Should ideally contrast well with the background color for
     * the text to be easily separated from the background.
     */
    Color m_textColor;

    /**
     * The interval in milliseconds that a word is displayed on the screen. After a duration
     * equal to this value has elapsed the next word is displayed.
     */
    int m_wordFlipInterval;

    /**
     * The location at which reads should be stored in the local disk. This allows to fetch
     * local contents and not need to retrieve them (and thus have a working internet conn)
     * each time the user wants to load them.
     */
    String m_readStorageLocation;

    /**
     * Create a default read preference object with no associated preferences. Each property
     * will not be accessible (or rather return `null`) until the corresponding `set` method
     * as been called for said property.
     */
    ReadPref() {}

    /**
     * Create a read preferences object with the specified values. Typically used to restore
     * this object from saved preferences.
     * @param bg - the color of the background when in reading mode.
     * @param text - the color of the text when in reading mode.
     * @param wordFlip - the interval in milliseconds between two words flip.
     * @param storageLoc - the local storage location for reads.
     */
    ReadPref(Color bg, Color text, int wordFlip, String storageLoc) {
        setBackgroundColor(bg);
        setTextColor(text);

        setWordFlipInterval(wordFlip);

        setReadStorageLocation(storageLoc);
    }

    /**
     * Retrieve the background color to use when in reading mode.
     * @return - the background color to use.
     */
    public Color getBackgroundColor() {
        return m_bgColor;
    }

    /**
     * Assign a new background color to use when in reading mode.
     * @param bg - the new color to use.
     */
    public void setBackgroundColor(Color bg) {
        m_bgColor = bg;
    }

    /**
     * Retrieve the text color to use when in reading mode.
     * @return - the text color to use.
     */
    public Color getTextColor() {
        return m_textColor;
    }

    /**
     * Assign a new text color to use when in reading mode.
     * @param text - the new color to use.
     */
    public void setTextColor(Color text) {
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
     * Retrieve the current read storage location. Used for remote read source so that
     * they can be retrieved locally and avoid fetching them from the net each time the
     * user wants to access them.
     * @return - the read storage location.
     */
    public String getReadStorageLocation() {
        return m_readStorageLocation;
    }

    /**
     * Assign a new location to store remote read source in local disk. This is used to
     * save the distant source so that we don't need an internet access each time the
     * user wants to access this distant location.
     * @param readStorage - the new local storage location.
     */
    public void setReadStorageLocation(String readStorage) {
        m_readStorageLocation = readStorage;
    }
}
