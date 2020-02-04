package knoblauch.readdesc.gui;

public interface ReadItemClickListener {

    /**
     * @brief - Called whenever a view belonging to a read item view is clicked. The
     *          input view is the one being clicked.
     * @param resource - the identifier of the resource that has been clicked on this
     *                   read item. Takes a value of one of the identifier of an elem
     *                   described in the layout for the read item.
     * @param id - the index of the read item that has been clicked. This index can
     *             be used in the read adapter to fetch the corresponding read. It is
     *             linked to the `resource` provided as first argument of this slot.
     */
    void onReadItemViewClick(int resource, int id, String name);
}
