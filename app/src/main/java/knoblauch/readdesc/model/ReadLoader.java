package knoblauch.readdesc.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class ReadLoader extends AsyncTask<String, Float, Boolean> {

    /**
     * Describe the interface that external objects can implement
     * to receive notifications whenever some data has been loaded
     * by this source.
     */
    interface DataLoadingListener {

        /**
         * Interface method allowing to be notified when a data loading
         * operation has just started. This method is notified *before*
         * any loading process is started and allows listeners to get
         * ready to receive data.
         */
        void onDataLoadingStarted();

        /**
         * Interface method which allows to notify progression of the
         * loading operation. Some callers might want to display some
         * sort of progress bar along the way.
         * @param progress - the current progress of the loading.
         */
        void onDataLoadingProgress(float progress);

        /**
         * Interface method allowing to be notified of a data load
         * operation by this source. The data can freely be accessed
         * from this parser.
         */
        void onDataLoadingSuccess();

        /**
         * Interface method allowing to notify a failure to load the
         * data registered in the source. This should usually trigger
         * some sort of error handling mechanism.
         */
        void onDataLoadingFailure();
    }

    /**
     * A convenience enumeration allowing to describe the possible
     * actions on the virtual cursor handled by this loader. This
     * mostly represents human readable motions that can be applied
     * to the data source (rewind, etc.).
     */
    public enum Action {
        Rewind,
        NextWord,
        PreviousStep,
        NextStep
    }

    /**
     * The context to use to provide information to the task to resolve
     * references to the resources (such as image paths, etc.).
     */
    private WeakReference<Context> m_context;

    /**
     * The list of elements that should be notified whenever the data
     * parsed is updated.
     */
    private ArrayList<DataLoadingListener> m_listeners;

    /**
     * Mutex allowing to protect concurrent accesses to the data stored
     * by this loader. This is especially useful to prevent accessing
     * some information that is being loaded by the background process
     * trusted with the fetching of the data.
     */
    private Lock m_locker;

    /**
     * The list of words representing this read. This collection is not
     * organized in any way because it requires too much care according
     * to the great variety of documents that could be parsed by this
     * elements.
     * Instead we focus in providing some sort of global collection of
     * data that can be traversed in several ways by the user.
     */
    private ArrayList<String> m_words;

    /**
     * The index of the word currently pointed at by the virtual cursor
     * used to get the words. This is the real indication of how far the
     * user has come within the source.
     */
    private int m_index;

    /**
     * An indication in the range `[0; 1]` which tells the portion of
     * the source that should be loaded in priority. This value can be
     * set to `0` in which case it is the default behavior of loading
     * the beginning of the read first.
     * This information is transmitted to the `loadFromSource` method
     * so that inheriting classes can work with it.
     */
    private float m_progress;

    /**
     * Create a new read loader with the specified context. This object
     * will be used to perform the resolution of links and get resources
     * during the parsing process.
     * The user should provide a `progress` value which indicates the
     * location that should be loaded preferentially from the source. It
     * allows to converge more quickly to the interesting data in case
     * the user already read part of the source.
     * In case the user never opened the source a value of `0` tells
     * that the data should be loaded from the beginning.
     * @param context - the context to use to resolve links and resources
     *                  needed for the loading operation.
     * @param progress - a percentage representing the region from the
     *                   source that should be loaded in priority.
     */
    ReadLoader(Context context, float progress) {
        // Copy input arguments to local attributes.
        m_context = new WeakReference<>(context);

        // Create listeners' list.
        m_listeners = new ArrayList<>();

        // Also assume no words are available for this loader.
        m_locker = new ReentrantLock();
        m_words = new ArrayList<>();
        m_index = -1;

        // And define the desired progression.
        m_progress = progress;
    }

    /**
     * Register the input listener in the internal list if it is valid.
     * This object will then be notified of all available signals from
     * this object.
     * Note that the listener is not added in case it is `null`.
     * @param listener - the listener to register.
     */
    void addOnDataLoadingListener(DataLoadingListener listener) {
        if (listener != null) {
            m_listeners.add(listener);
        }
    }

    @Override
    protected void onPreExecute() {
        // We're starting a new parsing process, notify listeners with
        // the corresponding signal.
        for (DataLoadingListener listener : m_listeners) {
            listener.onDataLoadingStarted();
        }
    }

    @Override
    protected Boolean doInBackground(String... uris) {
        // This method is scheduled in a dedicated thread and should perform
        // the loading of the data provided as input. The input arguments is
        // supposed to be referencing the `uri` of the data to load.
        // We will also use the internal `progression` in order to load first
        // the relevant data as requested by the caller: this will help find
        // and load quickly the data around the location at which the user is
        // reading and let other parts to be loaded afterwards.
        if (uris.length == 0 || uris[0] == null || m_context == null) {
            // Prevent cases where the `uri` is not given for some reasons or
            // invalid internal attributes.
            return false;
        }

        // Retrieve the `uri` of the source to parse.
        Uri uri = Uri.parse(uris[0]);

        // Perform the loading of this `uri`.
        ContentResolver res = m_context.get().getContentResolver();
        try {
            InputStream inStream = res.openInputStream(uri);
            ArrayList<String> words = loadFromSource(inStream, m_progress);

            handleDataLoaded(words);
        }
        catch (IOException e) {
            // We failed to load the source, this is an issue.
            return false;
        }

        // We successfully loaded the data from the source.
        return true;
    }

    /**
     * Used internally to handle the loading of some words. The input array
     * will be interpreted and appended to the already existing data so that
     * it can be accessed from the rest of the application.
     * Note that this method tries to acquire the lock on this object so as
     * to be protected from concurrent accesses.
     * @param words - the words that have just been loaded.
     */
    private void handleDataLoaded(ArrayList<String> words) {
        // Acquire the lock.
        m_locker.lock();

        // Copy the data and update the progress.
        try {
            m_words = words;

            // Update the current word's index.
            m_index = Math.round(m_progress * m_words.size());
        }
        finally {
            m_locker.unlock();
        }
    }

    @Override
    protected void onProgressUpdate (Float... results) {
        // We want to update the progress on the calling activity if possible.
        if (results == null || results.length == 0) {
            return;
        }

        // Retrieve the progression by assuming it's the first argument.
        float progress = results[0];

        // Notify listeners.
        for (DataLoadingListener listener : m_listeners) {
            listener.onDataLoadingProgress(progress);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        // The data has been successfully loaded or this task has been cancelled.
        // Depending on the case we will notify differently the listeners.
        if (isCancelled()) {
            // Do not notify in case we've been cancelled.
            return;
        }

        // Notify listeners.
        for (DataLoadingListener listener : m_listeners) {
            if (success) {
                listener.onDataLoadingSuccess();
            }
            else {
                listener.onDataLoadingFailure();
            }
        }
    }

    /**
     * Used as an internal method to determine whether the parser is in
     * a valid state. This helps not trying to apply processes that will
     * most likely fail.
     * Note that this method does not attempt to acquire the lock on the
     * data which means it is safe to call from an internal handler.
     * @return - `true` if the internal state indicate a valid parser.
     */
    private boolean isValid() {
        return !m_words.isEmpty() && m_index >= 0 && m_index < m_words.size();
    }

    /**
     * Used to determine whether some data is already accessible within
     * this parser. This usually indicates whether a loading operation
     * has already succeeded or not.
     * @return - `true` if some data can be fetched from this loader.
     */
    boolean isEmpty() {
        m_locker.lock();
        boolean empty = !isValid();
        m_locker.unlock();

        return empty;
    }

    /**
     * Allows to determine whether this loader is currently positioned at
     * the start of the data described by the source or not. This helps
     * with managing the controls used to move throughout the read.
     * @return - `true` if the virtual cursor is located at the beginning
     *           of the read and `false` otherwise.
     */
    boolean isAtStart() {
        m_locker.lock();
        boolean atStart = (m_index == 0);
        m_locker.unlock();

        return atStart;
    }

    /**
     * Similar to the `isAtStart` method but allows to determiner whether
     * the parser is currently at the end of the data stream defined by
     * the source.
     * @return - `true` if the virtual cursor is at the end of the data
     *           stream.
     */
    boolean isAtEnd() {
        m_locker.lock();
        boolean atEnd = (m_index == m_words.size() - 1);
        m_locker.unlock();

        return atEnd;
    }

    /**
     * Used to retrieve the completion reached by this loader so far. This
     * is computed from the current word index compared to the total words
     * count available in the reader.
     * Note that the output value is guaranteed to be in the range `[0; 1]`
     * and that in case no data is available yet the returned value is `0`.
     * @return - a value indicating how far in the read the current word
     *           is.
     */
    float getCompletion() {
        m_locker.lock();
        float progress = (!isValid() ? 0.0f : 1.0f * m_index / m_words.size());
        m_locker.unlock();

        return progress;
    }

    /**
     * Used for external elements to retrieve the current word pointed at
     * by this parser. The virtual cursor is left unchanged by this call
     * which means that calling this method repeatedly will not cause the
     * parser to reach the end of the data stream.
     * In case no data is yet available for this parser the empty string
     * is returned.
     * @return - a string representing the current word.
     */
    String getCurrentWord() {
        m_locker.lock();

        // In case the parser is not a valid state, do nothing.
        if (!isValid()) {
            m_locker.unlock();
            return "";
        }

        // Otherwise return the current words.
        String word = m_words.get(m_index);
        m_locker.unlock();

        return word;
    }

    /**
     * Used by external elements to modify the internal state of the parser
     * and perform the desired action. This typically allows to modify the
     * position of the virtual cursor maintained by this loader to somewhere
     * else in the source's data.
     * In case the action cannot be performed (or partially performed) it is
     * performed as much as possible.
     * @param action - the action which should be performed. This action may
     *                 require a parameter in which case the `param` value
     *                 is used.
     * @param param - a value that may be relevant depending on the value of
     *                the `action`. Unused otherwise.
     */
    void perform(Action action, int param) {
        // Acquire the lock on this object.
        m_locker.lock();

        if (!isValid()) {
            // Do not perform the action.
            return;
        }

        // TODO: We should introduce a mechanism to detect whether the source
        // can already provide the requested data or start a loading operation
        // if needed. This also goes for the other motion methods.

        boolean changed;

        try {
            // Depending on the action we want to perform different changes
            // to the internal virtual cursor.
            int index = m_index;

            switch (action) {
                case Rewind:
                    m_index = 0;
                    break;
                case NextWord:
                    m_index = Math.min(m_index + 1, m_words.size() - 1);
                    break;
                case PreviousStep:
                    m_index = Math.max(m_index - param, 0);
                    break;
                case NextStep:
                    m_index = Math.min(m_index + param, m_words.size() - 1);
                    break;
            }

            changed = (index != m_index);

        }
        finally {
            m_locker.unlock();
        }

        // In case we actually changed the word pointed at by this loader
        // we can fire a new signal to listeners.
        if (changed) {
            for (DataLoadingListener listener : m_listeners) {
                listener.onDataLoadingSuccess();
            }
        }
    }

    /**
     * Interface method that should be implemented by inheriting classes
     * and which handle the actual fetch of the data from the source and
     * populate the internal values.
     * The goal of this method is to retrieve a list of words describing
     * the read itself. We provide the `progress` indication as a mean
     * for inheriting classes to know which part of the source should be
     * loaded in priority.
     * @param stream - the input `stream` to use to fetch data from the
     *                 source.
     * @param progress - the location that should be loaded in priority
     *                   as specified by the caller: this usually refers
     *                   to the current position of the user's in the
     *                   source.
     * @return - the list of words that were fetched from the source
     *           data.
     */
    abstract ArrayList<String> loadFromSource(InputStream stream, float progress) throws IOException;
}
