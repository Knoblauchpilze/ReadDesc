package knoblauch.readdesc.gui;

import android.view.View;

public class ReadItemClickNotifier implements View.OnClickListener {

    /**
     * Holds the index of the read that is associated to this notifier.
     * It will be used whenever an event is detected on the read item
     * associated to this notifier.
     */
    private int m_resource;

    /**
     * An index of the view that is listened to by this element. It helps
     * to determine precisely which element has been clicked or triggered
     * in some way in the read item.
     */
    private int m_viewId;

    /**
     * The listener to notify of interaction on the view monitored by this
     * notifier.
     */
    private ReadItemClickListener m_listener;

    /**
     * Create a new read item notifier with the specified resource (i.e.
     * index of the view) and `id` (which represents the index of the read
     * item in the model.
     * @param resource - an index representing a specific element in the read item
     *                   view.
     * @param viewId - the index of the element in the model represented by the view.
     * @param listener - the listener which should be notified upon detecting events
     *                   happening in the view monitored by this element.
     */
    ReadItemClickNotifier(int resource, int viewId, ReadItemClickListener listener) {
        // Assign internal values.
        m_resource = resource;
        m_viewId = viewId;
        m_listener = listener;
    }

    /**
     * Used to update the view index assigned to this notifier. This can
     * be used in the case the view listened to by this notifier changes
     * and is meant to represent another element in the data model.
     * @param viewId - the new view index represented by this notifier.
     */
    void setViewId(int viewId) {
        m_viewId = viewId;
    }

    @Override
    public void onClick(View v) {
        // We need to fire the notification signal on the local listener.
        if (m_listener != null) {
            m_listener.onReadItemViewClick(m_resource, m_viewId);
        }
    }
}
