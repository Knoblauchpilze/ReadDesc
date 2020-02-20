package knoblauch.readdesc.gui;

import android.os.Handler;
import android.widget.TextView;

import knoblauch.readdesc.model.ReadParser;

public class WordFlipTask implements Runnable {

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
     * Create a new word flip task with the specified parameters.
     * @param flip - the word flip interval in milliseconds. This task
     *               will be scheduled using this interval and change
     *               the word displayed in the `text` view on this basis.
     * @param text - the text view to update with relevant words.
     * @param parser - the parser allowing to fetch the newt words to be
     *                 displayed in the text view.
     * @param handler - the handler to use to re-schedule this task regularly.
     */
    public WordFlipTask(int flip, TextView text, ReadParser parser, Handler handler) {
        m_text = text;
        m_flipInterval = flip;
        m_parser = parser;
        m_handler = handler;
    }

    @Override
    public void run() {
        // Update the word displayed in the main text view with the next
        // one from the parser.
        m_text.setText(m_parser.getNextWord());

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
