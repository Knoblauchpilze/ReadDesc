package knoblauch.readdesc.gui;

import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;

import knoblauch.readdesc.model.ReadParser;
import knoblauch.readdesc.model.ReadPref;

public class ReadingControls implements View.OnClickListener, ReadParser.ParsingDoneListener {

    /**
     * Convenience enumeration describing the possible actions to
     * perform while in reading mode.
     */
    public enum Action {
        Rewind,
        Previous,
        Pause,
        Play,
        Next
    }

    /**
     * Convenience enumeration describing the reading mode currently
     * applied to the parser. This helps defining the availability of
     * the controls based on whether the user is reading or not.
     */
    public enum State {
        Stopped,
        Running
    }

    /**
     * Convenience interface allowing to register some element that
     * needs to be notified whenever a user action is requested. It
     * is guaranteed to be notified whenever a valid user input is
     * received by this object.
     */
    public interface ControlsListener {

        /**
         * Called whenever a user action is requested.
         * @param action - the user's action to perform.
         */
        void onActionRequested(Action action);
    }

    /**
     * The image button allowing to reset the read to the beginning.
     */
    private ImageButton m_reset;

    /**
     * The image button to move to the previous section.
     */
    private ImageButton m_prev;

    /**
     * The image button to pause the reading mode.
     */
    private ImageButton m_pause;

    /**
     * The image button to start the reading mode.
     */
    private ImageButton m_play;

    /**
     * The image button to move to the next section.
     */
    private ImageButton m_next;

    /**
     * The list of listeners that should be notified upon a user's
     * request.
     */
    private ArrayList<ControlsListener> m_listeners;

    /**
     * Defines whether the controls associated to this object can
     * be used to generate user's request or if any click event is
     * to be ignored. This provide a high-level mean to control all
     * the buttons of the activity.
     */
    private boolean m_enabled;

    /**
     * The reader that is controlled by the elements in this UI. Note
     * that it is also used to handle the availability of buttons for
     * a given position of the reader in the virtual data stream. It
     * allows to disable the next button when the parser reaches the
     * end of the read, etc.
     */
    private ReadParser m_reader;

    /**
     * Defines the current state of the reading mode as activated from
     * the controls available in this object. This helps defining and
     * performing a reset of the controls in the state it was before a
     * loading operation (as we disable most of the controls during an
     * operation like this to prevent actions and weird behaviors).
     * This state is updated upon receiving data from the source and
     * when the user either press on the `play` or `pause` buttons.
     */
    private State m_state;

    /**
     * Creates a new reading controls class with the specified buttons
     * to use to perform and detect the operations requested by the user.
     * @param reader - the reader underlying the controls (i.e. the item
     *                 controlled by the buttons in this object).
     * @param reset - the reset button.
     * @param prev - the move to previous section button.
     * @param pause - the pause button.
     * @param play - the play button.
     * @param next - the move to next section button.
     */
    public ReadingControls(ReadParser reader, ImageButton reset, ImageButton prev, ImageButton pause, ImageButton play, ImageButton next) {
        // Assign internal attributes.
        m_reset = reset;
        m_prev = prev;
        m_pause = pause;
        m_play = play;
        m_next = next;

        // Create the listeners' list.
        m_listeners = new ArrayList<>();

        // Update the reader and register as a listener of the parsing's
        // related notifications.
        m_reader = reader;
        m_reader.addOnParsingDoneListener(this);

        // Register to the `onClick` single from these buttons: this will
        // allow to interpret the user's requests.
        m_reset.setOnClickListener(this);
        m_prev.setOnClickListener(this);
        m_pause.setOnClickListener(this);
        m_play.setOnClickListener(this);
        m_next.setOnClickListener(this);

        // Assume we're disabled by default and at the beginning of the read.
        setActive(false);
        setState(State.Stopped);
    }

    /**
     * Used to disable or enable the actions on the controls, typically
     * when the activity is performing some data loading and should not
     * react to the user's requests.
     * The click on buttons will not be transmitted until the next call
     * to the `setActive` method with a `true` value.
     * @param active - `true` if the command from the buttons should be
     *                 interpreted and `false` otherwise.
     */
    private void setActive(boolean active) {
        // Disable handling of click events.
        m_enabled = active;

        // Also handle controls activation: in case this component is
        // deactivated we will disable all controls while we need to
        // reactivate some if the state is set to `active`.
        if (!active) {
            m_reset.setEnabled(false);
            m_prev.setEnabled(false);
            m_pause.setEnabled(false);
            m_play.setEnabled(false);
            m_next.setEnabled(false);
        }
        else {
            m_play.setEnabled(m_state == State.Stopped);
            m_pause.setEnabled(m_state == State.Running);
        }
    }

    /**
     * Used to register the input listener as a new element to notify
     * in case of a user's action request. Note that if the input item
     * is `null` it won't be added to the internal list.
     * @param listener - the listener to register for the user's actions.
     */
    public void addOnControlsListener(ControlsListener listener) {
        if (listener != null) {
            m_listeners.add(listener);
        }
    }

    @Override
    public void onClick(View v) {
        // First determine whether we need to interpret the click. As we
        // only register for the controls button it is safe to assume that
        // in case we're disabled we won't interpret anything (no other
        // view can send such a signal).
        if (!m_enabled) {
            return;
        }

        // Detect which view was activated.
        Action action;
        if (v == m_reset) {
            action = Action.Rewind;
        }
        else if (v == m_prev) {
            action = Action.Previous;
        }
        else if (v == m_pause) {
            action = Action.Pause;
        }
        else if (v == m_play) {
            action = Action.Play;
        }
        else if (v == m_next) {
            action = Action.Next;
        }
        else {
            // Unknown view, do nothing.
            return;
        }

        // Update internal UI state from the requested action. We only
        // care about a request to start the reading or to pause it as
        // the other actions will be handled as we request the reader
        // to update its internal state and will thus produce a signal
        // received in `onParsingFinished`.
        switch (action) {
            case Pause:
                setState(State.Stopped);
                break;
            case Play:
                setState(State.Running);
                break;
            case Rewind:
            case Previous:
            case Next:
                break;
        }

        // Notify listeners of the requested user's action.
        for (ControlsListener listener : m_listeners) {
            listener.onActionRequested(action);
        }
    }

    @Override
    public void onParsingStarted() {
        // We want to deactivate the controls until we receive the notification
        // indicating that the parsing is finished.
        setActive(false);
    }

    @Override
    public void onParsingProgress(float progress) {
        // Nothing to be done here.
    }

    @Override
    public void onParsingFinished() {
        // The parsing just finished: we need to both update the controls allowing
        // to move throughout the read based on where we are now on the read and we
        // also need to re-allow the controls related to starting the reading mode
        // or pause it based on the current expected state of the parser.

        // First reset controls as active: this will also handle the `play` and the
        // `pause` based on the current `m_state` of the controls.
        setActive(true);

        // And update controls availability based on the state of the reader.
        updateControlsAvailability();
    }

    @Override
    public void onParsingFailed() {
        // Nothing to be done here.
    }

    /**
     * Used to update the controls availability based on the state of the
     * reading mode. This mainly updates both the `play` and the `pause`
     * item of the controls.
     * @param state - the state to apply to the controls.
     */
    public void setState(State state) {
        // Update the availability of each state.
        boolean controlsActive = false;

        switch (state) {
            case Stopped:
                m_pause.setEnabled(false);
                m_play.setEnabled(m_reader.isReady() && !m_reader.isAtEnd());
                controlsActive = true;
                break;
            case Running:
                m_pause.setEnabled(true);
                m_play.setEnabled(false);
                break;
        }

        m_reset.setEnabled(controlsActive);
        m_prev.setEnabled(controlsActive);
        m_next.setEnabled(controlsActive);

        // And update the controls availability based on the new state of the
        // reader if we didn't disabled them.
        if (controlsActive) {
            updateControlsAvailability();
        }

        m_state = state;
    }


    /**
     * Used to update the controls availability based on the current state
     * of the reader underlying this object. We will set the `enabled` for
     * each motion button (i.e. the move to next/previous and the rewind)
     * based on whether each of these operation actually makes sense given
     * the current position of the reader in the data stream.
     */
    private void updateControlsAvailability() {
        // Depending on the position we will de/activate some of the buttons.
        m_reset.setEnabled(m_reader.isReady() && !m_reader.isAtStart());
        m_prev.setEnabled(m_reader.isReady() && !m_reader.isAtStart());
        m_next.setEnabled(m_reader.isReady() && !m_reader.isAtEnd());
    }

    /**
     * Used to define a new background color for each control button handled
     * by this object. This is typically useful whenever the preferences are
     * fetched and should be applied to the controls.
     * @param prefs - the set of preferences to use to customize the buttons
     *                of these controls.
     */
    public void updateFromPrefs(ReadPref prefs) {
        // Retrieve the background color.
        int bg = prefs.getBackgroundColor();

        // Apply to the controls.
        m_reset.setBackgroundColor(bg);
        m_prev.setBackgroundColor(bg);
        m_pause.setBackgroundColor(bg);
        m_play.setBackgroundColor(bg);
        m_next.setBackgroundColor(bg);
    }
}
