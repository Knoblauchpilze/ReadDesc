package knoblauch.readdesc.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

abstract class ReadLoader extends AsyncTask<String, Float, Boolean> {

    /**
     * Describe the interface that external objects can implement
     * to receive notifications whenever some data has been loaded
     * by this source.
     */
    interface ReadLoaderListener {

        /**
         * Interface method allowing to be notified when a data loading
         * operation has just started. This method is notified *before*
         * any loading process is started and allows listeners to get
         * ready to receive data.
         */
        void onLoadingStarted();

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

        /**
         * Interface method which allows to notify progression of the
         * loading operation. Some callers might want to display some
         * sort of progress bar along the way.
         * @param progress - the current progress of the loading.
         */
        void onLoadingProgress(float progress);
    }


    /**
     * The listener that should receive notifications whenever this
     * source loads some data.
     */
    private WeakReference<ReadLoaderListener> m_caller;

    /**
     * The context to use to provide information to the task to resolve
     * references to the resources (such as image paths, etc.).
     */
    private WeakReference<Context> m_context;

    /**
     * The list of paragraphs that should will be loaded by the fetching
     * process of the source's file. Should not be empty by the end of
     * the loading (otherwise it means that the source is empty and thus
     * we don't have anything to read).
     */
    private ArrayList<Paragraph> m_paragraphs;

    /**
     * Create a new read loader with the specified listener.
     * @param context - the context to use to resolve links and resources
     *                  needed for the loading operation.
     * @param caller - the listener which should be notified whenever the
     *                 loading operation succeeded or failed. It is also
     *                 the calling element for this loader.
     */
    ReadLoader(Context context, ReadLoaderListener caller) {
        // Copy input arguments to local attributes.
        link(caller);

        m_context = new WeakReference<>(context);

        m_paragraphs = null;
    }

    /**
     * Used both internally and by the output elements to link an activity
     * to this loading task. The specified activity will be the one notified
     * when the loading task is finished.
     * This method is called when building the object but can also be used
     * to handle some changes in the parent activity (for example a change
     * in orientation where the activity needs to be recreated).
     * @param caller - the new calling activity for this task.
     */
    private void link(ReadLoaderListener caller) {
        m_caller = new WeakReference<>(caller);
    }

    @Override
    protected void onPreExecute() {
        // Call the listener's corresponding interface method.
        if (m_caller.get() != null) {
            m_caller.get().onLoadingStarted();
        }
    }


    @Override
    protected Boolean doInBackground(String... uris) {
        // We want to perform the loading of the data described by the input `uri`
        // which should correspond to the source file of the read. This means that
        // all the paragraphs from the `PDF` will be retrieved.

        // Retrieve the `url` to load. This is indicated by the first parameter of
        // this method.
        if (uris.length == 0 || uris[0] == null) {
            return false;
        }

        Uri uri = Uri.parse(uris[0]);

        // Initialize the return value.
        m_paragraphs = null;

        // Attempt to retrieve a content resolver for this task. We have to use
        // the provided context to do so.
        if (m_caller == null || m_context == null) {
            return false;
        }

        ContentResolver res = m_context.get().getContentResolver();

        // Load the bitmap.
        try {
            InputStream inStream = res.openInputStream(uri);
            m_paragraphs = loadFromSource(inStream);
        }
        catch (IOException e) {
            // We failed to load the source, this is an issue.
            return false;
        }

        // We successfully loaded the data from the source.
        return true;
    }

    @Override
    protected void onProgressUpdate (Float... results) {
        // We want to update the progress on the calling activity if possible.
        if (results == null || results.length == 0) {
            return;
        }

        // Retrieve the progression by assuming it's the first argument.
        float progress = results[0];

        if (m_caller.get() != null) {
            m_caller.get().onLoadingProgress(progress);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        // We want to associate the loaded paragraphs with the caller of this
        // object if it is valid. In any other case we want to call the correct
        // handler so that the activity can handle correctly the failure.

        // Handle cancellation requests and cases where there's no caller to send
        // the bitmap to.
        if (isCancelled() || m_caller == null) {
            return;
        }

        // Depending on the result of the execution we will call the dedicated
        // handler for this task.
        if (!success || m_paragraphs == null) {
            m_caller.get().onFailureToLoadData();
        }
        else {
            m_caller.get().onDataLoaded(m_paragraphs);
        }
    }

    /**
     * Interface method that should be implemented by inheriting classes
     * and which handle the actual fetch of the data from the source and
     * populate the internal values.
     * The goal of this method is to retrieve a list of paragraphs which
     * compose the read itself.
     * @param stream - the input `stream` to use to fetch data from the
     *                 source.
     * @return - the list of paragraphs that were fetched from the source
     *           data.
     */
    abstract ArrayList<Paragraph> loadFromSource(InputStream stream) throws IOException;
}
