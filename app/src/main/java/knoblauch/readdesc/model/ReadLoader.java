package knoblauch.readdesc.model;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;

abstract class ReadLoader {

    /**
     * Describe the interface that external objects can implement
     * to receive notifications whenever some data has been loaded
     * by this source.
     */
    interface ReadLoaderListener {

        /**
         * Interface method allowing to be notified of a data load
         * operation by this source. The loaded data is provided as
         * an argument of the method so that the listener can react
         * accordingly.
         */
        void onDataLoaded(ArrayList<Paragraph> paragraphs);

        /**
         * Interface method allowing to register to handle cases
         * where the loader did not succeed in loading the data
         * from the source.
         */
        void onFailureToLoadData();
    }


    /**
     * The listener that should receive notifications whenever this
     * source loads some data.
     */
    private ReadLoaderListener m_listener;

    /**
     * Internal object used to perform resolution of resources and potential
     * links in the data to read. This is mostly used as a way to determine
     * the correct way to access to the data source of this reader.
     */
    private Context m_context;

    /**
     * The actual path to the source of the data. This can describe a variety
     * of elements based on whether the loader actually loads from a `PDF`, or
     * a website, etc.
     */
    private Uri m_uri;

    /**
     * Create a new read loader with the specified listener.
     * @param context - the context to use to resolve resources and elements
     *                  to load by this source.
     * @param uri - the link to the actual data source for this loader. This
     *              defines where the data should be fetched.
     * @param listener - the listener which should be notified upon
     *                   successfully loading some data.
     */
    ReadLoader(Context context, String uri, ReadLoaderListener listener) {
        // Copy input arguments to local attributes.
        m_listener = listener;
        m_context = context;
        m_uri = Uri.parse(uri);
    }

    /**
     * Used to start the loading from the data. This will typically create
     * a thread that will be charged of loading the data from the source
     * and notify the listener when it is done.
     */
    void start() {
        // TODO: Start the process in a dedicated thread.

        // Load from the source.
        boolean success = true;
        ArrayList<Paragraph> paragraphs = new ArrayList<>();

        try {
            paragraphs = loadFromSource(m_uri, m_context);
        }
        catch (IOException e) {
            // In case we couldn't load the data from the source, notify
            // the listener anyway.
            success = false;
        }

        // Notify listeners if any.
        if (m_listener != null) {
            if (success) {
                m_listener.onDataLoaded(paragraphs);
            }
            else {
                m_listener.onFailureToLoadData();
            }
        }
    }

    /**
     * Interface method that should be implemented by inheriting classes
     * and which handle the actual fetch of the data from the source and
     * populate the internal values.
     * The goal of this method is to retrieve a list of paragraphs which
     * compose the read itself.
     * @param source - the `uri` of the data to load.
     * @param context - an object to be used to resolve links and resources
     *                  that may be needed to load the data.
     * @return - the list of paragraphs that were fetched from the source
     *           data.
     */
    abstract ArrayList<Paragraph> loadFromSource(Uri source, Context context) throws IOException;
}
