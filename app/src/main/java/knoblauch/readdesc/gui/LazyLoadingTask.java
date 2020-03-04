package knoblauch.readdesc.gui;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class LazyLoadingTask extends AsyncTask<String, Float, Boolean> {

    /**
     * Internal interface used to now how we can notify results to the
     * caller object for this task. This object describes useful cases
     * to handle the success or the failure of the lazy load operation.
     */
    interface LazyLoadingListener {

        /**
         * Called by the lazy loading task whenever the loading of the
         * image resource succeeded.
         * @param uri - the `uri` of the image that was loaded.
         */
        void onLazyLoadSuccess(Uri uri);

        /**
         * Called by the lazy loading task whenever the loading of the
         * image resource failed. We provide the `uri` that was meant
         * to be loaded so as to give more context to the failure.
         * @param uri - the `uri` of the image that was loaded.
         */
        void onLazyLoadFailure(Uri uri);
    }

    /**
     * The calling activity of this task. Allows to keep track of the
     * publisher and notify the results.
     */
    private WeakReference<LazyLoadingListener> m_caller;

    /**
     * The context to use to provide information to the task to resolve
     * references to the resources (such as image paths, etc.).
     */
    private WeakReference<Context> m_context;

    /**
     * The `uri` which should be loaded by this task. Note that this
     * attribute is set to `null` until we actually receive the signal
     * to start the loading operation.
     * It is used as a mean to give relevant information to the caller
     * in case of failure or success.
     */
    private Uri m_uri;

    /**
     * The object into which the result of the execution of this loading
     * task should be placed. Its value is originally set to display some
     * waiting data and populated when the execution of the loading task
     * has succeeded.
     */
    private WeakReference<ImageView> m_placeHolder;

    /**
     * The result of the loading operation. This is initially set to `null`
     * and populated with the result of the asynchronous loading operation.
     */
    private Bitmap m_bitmap;

    /**
     * Creates a task to perform the load of said resource in a dedicated
     * thread. The `caller` allows to reference the activity that called
     * this object and to publish the results.
     * @param caller - the caller to which the loading of the image res
     *                 is to be notified when finished.
     * @param placeholder - the place holder that should receive the loaded
     *                      image when the task is done.
     * @param context - the context to use to resolve resources paths in
     *                  the loading process.
     */
    LazyLoadingTask(LazyLoadingListener caller, ImageView placeholder, Context context) {
        link(caller);

        // Assign the context.
        m_context = new WeakReference<>(context);

        // Indicate that no bitmap has been loaded yet.
        m_placeHolder = new WeakReference<>(placeholder);
        m_bitmap = null;
        m_uri = null;
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
    private void link(LazyLoadingListener caller) {
        m_caller = new WeakReference<>(caller);
    }

    /**
     * Used to retrieve the `uri` currently being loaded by this task. Note
     * that this method will return `null` until the `doInBackground` method
     * has effectively been called.
     * @return - the `uri` being loaded by this task as a `String` object.
     */
    String getUri() { return m_uri.toString(); }

    @Override
    protected Boolean doInBackground(String... strings) {
        // We want to perform the download of the image described by the input
        // `url` and return it when it's finished.

        // Retrieve the `url` to load. This is indicated by the first parameter
        // of this method.
        if (strings.length == 0 || strings[0] == null) {
            return false;
        }

        m_uri = Uri.parse(strings[0]);

        // Attempt to retrieve a content resolver for this task. We have to use
        // the provided context to do so.
        if (m_caller == null || m_context == null) {
            return false;
        }

        ContentResolver res = m_context.get().getContentResolver();

        // Load the bitmap.
        try {
            InputStream inStream = res.openInputStream(m_uri);
            m_bitmap = BitmapFactory.decodeStream(inStream);
        }
        catch (FileNotFoundException e) {
            // We failed to load the bitmap, this is an issue.
            return false;
        }

        // We successfully loaded the image resource.
        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        // We want to associate the loaded bitmap to the caller of this activity
        // if it is valid. In any other case we want to call the correct handler
        // so that the activity can handle correctly the failure.

        // Handle cancellation requests and cases where there's no caller to send
        // the bitmap to.
        if (isCancelled() || m_caller == null) {
            return;
        }

        // Depending on the result of the execution we will call the dedicated
        // handler for this task.
        if (!success || m_bitmap == null) {
            m_caller.get().onLazyLoadFailure(m_uri);
        }
        else {
            m_caller.get().onLazyLoadSuccess(m_uri);
        }

        // Finally assign the image to the place holder that should receive it.
        if (m_placeHolder != null) {
            ImageView imageView = m_placeHolder.get();
            if (imageView != null) {
                imageView.setImageBitmap(m_bitmap);
            }
        }
    }
}
