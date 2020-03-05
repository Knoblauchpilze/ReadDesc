package knoblauch.readdesc.gui;

import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import knoblauch.readdesc.model.ReadParser;
import knoblauch.readdesc.model.ReadPref;

public class ReadingTextHandler implements ReadParser.ParsingDoneListener, Runnable {

    /**
     * Internal class allowing to detect whenever a new paragraph has been
     * reached by this task. This is useful for the caller of such a task
     * which might want to get notified whenever specific locations of the
     * read are reached.
     * The method of this interface is called whenever a new paragraph is
     * reached in the underlying parser of this task.
     */
    public interface ParagraphListener {

        /**
         * Called by this task whenever a new paragraph is reached by the
         * underlying task.
         */
        void onParagraphReached();
    }

    /**
     * Holds the main text view allowing to display the current content of
     * the view. Its content is updated each time a period of time defined
     * in the preferences has past to flip to the next word of the view.
     */
    private TextView m_text;

    /**
     * Used to hold the progress bar allowing to make the user wait for the
     * content of this read in case it takes some time to be loaded. We use
     * this as a replacement of the text view whenever needed.
     */
    private ProgressBar m_waiter;

    /**
     * The parser that is used to provide the text to be displayed in the
     * main view managed by this object. We use this object as a source of
     * data to handle the switch between the waiter and the text view.
     */
    private ReadParser m_parser;

    /**
     * A once-and-for-all value indicating how often this task should be
     * executed. The smaller this value the more often the word displayed
     * in the text view will change.
     */
    private int m_flipInterval;

    /**
     * The handler to use to schedule events and re-launching of this task
     * on a regular basis.
     */
    private Handler m_handler;

    /**
     * The list of elements which should be notified whenever a paragraph
     * is reached by this handler.
     */
    private ArrayList<ParagraphListener> m_listeners;

    /**
     * Creates a new text handler from the input text view and progress bar.
     * This element will automatically switch from one element to the other
     * in case it is needed.
     * @param text - the text view to display the text.
     * @param waiter - the progress bar to display when the text is not
     *                 available.
     * @param parser - the read parser that should be used to retrieve the
     *                  text to display in the main view.
     * @param handler - base object used to schedule this task within a set
     *                  time interval.
     */
    public ReadingTextHandler(TextView text, ProgressBar waiter, ReadParser parser, Handler handler) {
        m_text = text;
        m_waiter = waiter;
        m_parser = parser;
        m_handler = handler;

        // Assign an invalid value for the flip interval. We will wait for a
        // call to `updateFromPrefs` to get a correct value.
        m_flipInterval = -1;

        m_listeners = new ArrayList<>();
    }

    /**
     * Used to define a new background and text color for the elements used
     * in this text handler. This is typically useful whenever the style to
     * apply to the elements handled by this object.
     * @param prefs - the set of preferences to use to customize the items
     *                of these controls.
     */
    public void updateFromPrefs(ReadPref prefs) {
        // Apply preferences to the controls.
        m_text.setBackgroundColor(prefs.getBackgroundColor());
        m_text.setTextColor(prefs.getTextColor());

        m_waiter.setBackgroundColor(prefs.getBackgroundColor());

        // Update the word flip interval.
        m_flipInterval = prefs.getWordFlipInterval();
    }

    /**
     * Used to register the specified listener as a new paragraph listener
     * for this object. Note that it won't be added in case it is `null`.
     * @param listener - the listener to register in this object.
     */
    public void addOnParagraphListener(ParagraphListener listener) {
        if (listener != null) {
            m_listeners.add(listener);
        }
    }

    @Override
    public void onParsingStarted() {
        // We need to make the waiter to be visible and hide the text as it probably
        // does not display anything relevant.
        m_text.setVisibility(View.GONE);
        m_waiter.setVisibility(View.VISIBLE);
    }

    @Override
    public void onParsingFinished() {
        // Receiving this signal indicates that the parser is now ready to display the
        // text of the read. We can hide the progress bar and start display the actual
        // text.
        m_text.setVisibility(View.VISIBLE);
        m_waiter.setVisibility(View.GONE);

        // We can also update the text with the current word provided by the parser.
        m_text.setText(m_parser.getCurrentWord());
    }

    @Override
    public void onParsingFailed() {
        // Nothing to be done here.
    }

    @Override
    public void onLoadingProgress(float progress) {
        // We want to update the value of the progress bar to reach the input value.
        m_waiter.setProgress(Math.round(100.0f * progress));
    }

    @Override
    public void run() {
        // Update the word displayed in the main text view with the next
        // one from the parser. We want to detect special cases where the
        // read is finished or when we reached a paragraph of some sort
        // in the input data.
        // This will allow to stop the reading process and allow the user
        // to see whether the reading should pursue or stop.

        // Prevent cases where the parser is not ready.
        if (!m_parser.isReady()) {
            return;
        }

        // Update the next word: this should be done no matter whether
        // we reached a paragraph or not as we want to stop *after* the
        // current word anyways.
        m_parser.advance();
        m_text.setText(m_parser.getCurrentWord());

        // Check whether we reached a paragraph.
        if (m_parser.isAtParagraph() || m_parser.isAtEnd()) {
            // We reached a paragraph, notify listeners.
            for (ParagraphListener listener : m_listeners) {
                listener.onParagraphReached();
            }

            return;
        }

        // Reschedule this task as we didn't reach any paragraph.
        m_handler.postDelayed(this, m_flipInterval);
    }

    /**
     * Used to start the scheduling of this `task` using the provided internal
     * handler. The first execution of the task will be set within a `interval`
     * of time consistent with the value retrieved from the preferences.
     * Note that if the interval is not valid, the task is not started.
     */
    public void start() {
        if (m_flipInterval > 0) {
            m_handler.postDelayed(this, m_flipInterval);
        }
    }

    /**
     * Cancel any pending execution of this task from the internal handler.
     */
    public void stop() {
        m_handler.removeCallbacks(this);
    }

    /**
     * Used as a mean for external objects to force a refresh of the word
     * displayed by the text view controlled by this object. This typically
     * indicates that the parser has been updated by external means.
     */
    public void refresh() {
        // Impossible to refresh the word displayed if the parser is not ready.
        if (!m_parser.isReady()) {
            return;
        }

        // Otherwise we can update the word displayed in the main view.
        m_text.setText(m_parser.getCurrentWord());
    }
}