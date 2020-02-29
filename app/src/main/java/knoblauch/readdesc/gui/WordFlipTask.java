package knoblauch.readdesc.gui;

import android.os.Handler;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
     * Protect this object from concurrent accesses to the internal variable
     * indicating whether we should stop on the next paragraph encountered
     * by the parser.
     */
    private Lock m_locker;

    /**
     * Internal variable allowing to determine whether we should stop on the
     * next paragraph we encounter with the parser. This is set to `true` in
     * case we are processing word and to `false` whenever the task is moved
     * to a running state again.
     * Indeed without this value we would get stuck forever on any paragraph
     * as we would have no mean to move through it.
     */
    private boolean m_stopOnNextParagraph;

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

        m_locker = new ReentrantLock();
        m_stopOnNextParagraph = false;
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
        Log.i("word", "Parser is at " + m_parser.getCompletion() + ", st: " + m_parser.isAtStart() + ", prg: " + m_parser.isAtParagraph() + ", en: " + m_parser.isAtEnd());

        // Update the progression.
        m_progression.setProgress(m_parser.getCompletionAsPercentage());

        // Update the next word: this should be done no matter whether
        // we reached a paragraph or not as we want to stop *after* the
        // current word anyways.
        m_parser.advance();
        m_text.setText(m_parser.getCurrentWord());

        // Check whether we reached a paragraph.
        if (m_parser.isAtParagraph() || m_parser.isAtEnd()) {
            // Check whether we should stop on this paragraph.
            m_locker.lock();
            if (m_stopOnNextParagraph) {
                // We need to stop on this paragraph (so presumably we
                // already parsed a whole paragraph).
                m_locker.unlock();

                // Notify the listener if any.
                if (m_listener != null) {
                    m_listener.onParagraphReached();
                }

                return;
            }

            // We won't stop on this paragraph but we should indicate
            // that we *will* stop on the next one.
            m_stopOnNextParagraph = true;
            m_locker.unlock();
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
        // Make this parser ignore the next paragraph: indeed as we might
        // have stopped the task precisely because of such a paragraph we
        // don't want to get stuck in a loop.
        m_locker.lock();
        m_stopOnNextParagraph = false;
        m_locker.unlock();

        // Schedule this task.
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
