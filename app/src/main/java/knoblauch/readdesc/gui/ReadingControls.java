package knoblauch.readdesc.gui;

import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;

import knoblauch.readdesc.model.ReadPref;

public class ReadingControls implements View.OnClickListener {

    /**
     * Convenience enumeration describing the possible actions to
     * perform while in reading mode.
     */
    public enum Action {
        Rewind,
        PreviousParagraph,
        Pause,
        Play,
        NextParagraph
    }

    /**
     * Convenience enumeration describing the possible position of
     * a parser which should be reflected in the availability of
     * the controls buttons.
     */
    public enum Position {
        AtStart,
        Running,
        AtEnd
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
     * The image button to move to the previous paragraph.
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
     * The image button to move to the next paragraph.
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
     * Creates a new reading controls class with the specified buttons
     * to use to perform and detect the operations requested by the user.
     * @param reset - the reset button.
     * @param prev - the move to previous paragraph button.
     * @param pause - the pause button.
     * @param play - the play button.
     * @param next - the move to next paragraph button.
     */
    public ReadingControls(ImageButton reset, ImageButton prev, ImageButton pause, ImageButton play, ImageButton next) {
        // Assign internal attributes.
        m_reset = reset;
        m_prev = prev;
        m_pause = pause;
        m_play = play;
        m_next = next;

        // Create the listeners' list.
        m_listeners = new ArrayList<>();

        // Register to the `onClick` single from these buttons: this will
        // allow to interpret the user's requests.
        m_reset.setOnClickListener(this);
        m_prev.setOnClickListener(this);
        m_pause.setOnClickListener(this);
        m_play.setOnClickListener(this);
        m_next.setOnClickListener(this);

        // Assume we're disabled by default.
        setActive(false);
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
    public void setActive(boolean active) {
        m_enabled = active;
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
            action = Action.PreviousParagraph;
        }
        else if (v == m_pause) {
            action = Action.Pause;
        }
        else if (v == m_play) {
            action = Action.Play;
        }
        else if (v == m_next) {
            action = Action.NextParagraph;
        }
        else {
            // Unknown view, do nothing.
            return;
        }

        // Notify listeners of the requested user's action.
        for (ControlsListener listener : m_listeners) {
            listener.onActionRequested(action);
        }
    }

    /**
     * Used to update the controls availability based on the position set
     * as input parameters. Basically it allows to always enable controls
     * that corresponds to the current state of the parser (typically we
     * don't want to enable the `rewind` button in case the parser is set
     * at the beginning of the read already).
     * @param position - the position of the parser which should be set
     *                   as a reflection of the controls available.
     */
    public void setPosition(Position position) {
        // Depending on the position we will de/activate some of the buttons.
        boolean canRewind = true;
        boolean canGoPrev = true;
        boolean canGoNext = true;

        switch (position) {
            case AtStart:
                canRewind = false;
                canGoPrev = false;
                break;
            case Running:
                break;
            case AtEnd:
                canGoNext = false;
                break;
        }

        m_reset.setEnabled(canRewind);
        m_prev.setEnabled(canGoPrev);
        m_next.setEnabled(canGoNext);

        // In case we're at the end of the read, deactivate the `play` button
        // as well.
        if (!canGoNext) {
            m_play.setEnabled(false);
        }
    }

    /**
     * Used to update the controls availability based on the state of the
     * reading mode. This mainly updates both the `play` and the `pause`
     * item of the controls.
     * @param state - the state to apply to the controls.
     */
    public void setState(State state) {
        // Update the availability of each state.
        switch (state) {
            case Stopped:
                m_pause.setEnabled(false);
                m_play.setEnabled(true);
                break;
            case Running:
                m_pause.setEnabled(true);
                m_play.setEnabled(false);
                break;
        }
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
