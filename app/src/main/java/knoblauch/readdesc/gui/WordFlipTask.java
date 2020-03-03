package knoblauch.readdesc.gui;

import android.os.Handler;
import android.widget.SeekBar;
import android.widget.TextView;

import knoblauch.readdesc.model.ReadParser;

public class WordFlipTask implements Runnable {

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
     * Holds a reference to the text view that is bounded to this word flip
     * task. It is used in order to update the relevant display with the
     * next word provided by the reader.
     */
    private TextView m_text;

    /**
     * The reader used to fetch the next word to display in the text view.
     * This element is used every time a new word is needed.
     */
    private ReadParser m_parser;

    /**
     * Used internally to notify the progression reached by this task. It
     * will be updated each time a new word is handled so that the state
     * of the bar reflects the internal progression of the read.
     */
    private SeekBar m_progression;

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
     * Contains the potential listener of this task which should be notified
     * whenever a new paragraph is reached by this task.
     */
    private ParagraphListener m_listener;

    /**
     * Create a new word flip task with the specified parameters.
     * @param flip - the word flip interval in milliseconds. This task
     *               will be scheduled using this interval and change
     *               the word displayed in the `text` view on this basis.
     * @param text - the text view to update with relevant words.
     * @param parser - the parser allowing to fetch the newt words to be
     *                 displayed in the text view.
     * @param handler - the handler to use to re-schedule this task regularly.
     * @param progress - an object which should be notified of any progress
     *                   in the read. This brings the current completion value
     *                   to the user in a visual way.
     * @param listener - the listener (potentially empty) that should be used
     *                   in case a new paragraph is reached by this task.
     */
    public WordFlipTask(int flip, TextView text, ReadParser parser, Handler handler, SeekBar progress, ParagraphListener listener) {
        m_text = text;
        m_flipInterval = flip;
        m_parser = parser;
        m_progression = progress;
        m_handler = handler;
        m_listener = listener;
    }

    @Override
    public void run() {
        // Update the word displayed in the main text view with the next
        // one from the parser. We want to detect special cases where the
        // read is finished or when we reached a paragraph of some sort
        // in the input data.
        // This will allow to stop the reading process and allow the user
        // to see whether the reading should pursue or stop. Also we need
        // to reflect the current progression of the parser to the right
        // object registered in the `m_progression` attribute: this will
        // bring this information as a visual representation to the user.

        // Update the progression.
        m_progression.setProgress(m_parser.getCompletionAsPercentage());

        // Update the next word: this should be done no matter whether
        // we reached a paragraph or not as we want to stop *after* the
        // current word anyways.
        m_parser.advance();
        m_text.setText(m_parser.getCurrentWord());

        // Check whether we reached a paragraph.
        if (m_parser.isAtParagraph() || m_parser.isAtEnd()) {
            // We reached a paragraph, notify listeners.
            if (m_listener != null) {
                m_listener.onParagraphReached();
            }

            return;
        }

        // Schedule a new repaint within the required time interval from
        // the preferences.
        m_handler.postDelayed(this, m_flipInterval);
    }

    /**
     * Used to start the scheduling of this task so that we start to flip
     * the word displayed in the text view attached to this object every
     * requested interval of time.
     */
    public void start() {
        // Schedule this task.
        // TODO: Should be linked to the fact that the parser has finished to load the data.
        m_handler.post(this);
    }

    /**
     * Used to stop the task performing the word flip. Any pending events
     * requesting to flip the currently displayed word will be trashed and
     * the task will be stopped.
     */
    public void stop() {
        // Remove any callback associated with this task.
        m_handler.removeCallbacks(this);
    }

}
