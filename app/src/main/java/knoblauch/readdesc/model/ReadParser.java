package knoblauch.readdesc.model;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ReadParser implements ReadLoader.DataLoadingListener {

    /**
     * Describe a convenience interface allowing for anyone to listen to the
     * notification fired whenever the parser finishes to load its data from
     * its source.
     */
    public interface ParsingDoneListener {

        /**
         * Triggered whenever the parsing of the data associated to the read
         * has started.
         */
        void onParsingStarted();

        /**
         * Triggered whenever the parsing of the data associated to the read
         * has progressed to the specified value.
         * @param progress - the current progression of the loading operation.
         */
        void onParsingProgress(float progress);

        /**
         * Triggered whenever the parsing of the data associated to the read
         * has succeeded.
         */
        void onParsingFinished();

        /**
         * Triggered whenever the parsing of the data associated to the read
         * has failed.
         */
        void onParsingFailed();
    }

    /**
     * Used to define the number of words advanced at once upon processing
     * a `Next/PreviousStep` action.
     * TODO: This should be replaced by a configurable property.
     */
    private static final int STEP_WORDS_COUNT = 10;

    /**
     * The read description associated to this parser. It describes the data
     * and where to find it, along with the progression that has been reached
     * on this read.
     * The progress will be matched as soon as the data has been fetched. It
     * is also used as a way to identify the read so that we can save back on
     * the disk the progression reached in this reading session.
     */
    private ReadIntent m_desc;

    /**
     * Describes the actual element able to fetch the data from the
     * source of the read and to populate the paragraphs (and thus
     * the content) of this reader.
     * The source is expected to fill the corresponding interface
     * and knows what each operation mean in terms of its type. It
     * notifies this reader whenever the loading is done through the
     * interface method which allows to not block the process for
     * too long if the source is either long to respond or heavy to
     * load.
     */
    private ReadLoader m_source;

    /**
     * The list of listeners that should be notified whenever the data
     * loaded from the source is successfully received. It will also be
     * used if the data fails to be loaded.
     */
    private ArrayList<ParsingDoneListener> m_listeners;

    /**
     * Create a new parser from the specified read. The parser will detect the
     * type of the read and instantiate a valid data source to fetch and load
     * it.
     * @param read - the read containing information about the data source to
     *               use to get the data.
     * @param context - the context to use to resolve links and resources so
     *                  that we can access to the read's content for example.
     * @param desiredProgress - a value in the range `[0; 1]` which represents
     *                          the desired progress to be reached by this
     *                          parser. This will be interpreted by the parser
     *                          to load directly the relevant data.
     */
    public ReadParser(ReadIntent read, Context context, float desiredProgress) {
        // Assign the name of the read.
        m_desc = read;

        // Create the listeners' list.
        m_listeners = new ArrayList<>();

        // Create the source with a valid desired progress value. In case this
        // value is negative (indicating that no specific desired progress is
        // needed) we should rely on the value provided by the `read` itself.
        float consolidated = (desiredProgress < 0.0f ? m_desc.getCompletion() : desiredProgress);
        float progress = Math.min(1.0f, Math.max(0.0f, consolidated));
        createSource(context, progress);
    }

    /**
     * Used to register a new parsing done listener to the internal list
     * handled by this object. Note that in case the listener is `null`
     * we won't add id.
     * @param listener - the listener to register.
     */
    public void addOnParsingDoneListener(ParsingDoneListener listener) {
        if (listener != null) {
            m_listeners.add(listener);
        }
    }

    /**
     * Used to perform the creation of the source's data for this parser
     * and schedule an execution of the loading process so that it can be
     * used as soon as possible to populate other views.
     * The source should be initialized with the specified progress which
     * means that we should try to load preferentially the content close
     * to the progression (and not always start from the beginning for
     * example).
     * A context is provided in order to perform the resolution of links
     * and resources in the parsing process.
     * @param context - the object to use to perform link resolution and
     *                  resource fetching during the parsing process.
     * @param desiredProgress - an indication of the progress reached by
     *                          the user: should be used to load first
     *                          the data close to this percentage.
     */
    private void createSource(Context context, float desiredProgress) {
        // Create the data source based on the type of the read associated
        // to this parser.
        switch (m_desc.getType()) {
            case WebPage:
                m_source = new HtmlSourceLoader(context, desiredProgress);
            case EBook:
                m_source = new EBookSourceLoader(context, desiredProgress);
            case File:
                m_source = new PdfSourceLoader(context, desiredProgress);
                break;
        }

        // Register as a listener of the notifications produced by the
        // source so that we can forward the information to external
        // elements.
        m_source.addOnDataLoadingListener(this);

        // Start the loading of the data.
        m_source.execute(m_desc.getDataUri());
    }

    /**
     * Retrieves the name of the read associated to this parser.
     * @return - the name of the read linked to this parser.
     */
    public String getName() { return m_desc.getName(); }

    /**
     * Used by external objects to cancel the loading operation that might
     * be pending for this parser. Note that in case nothing is loading it
     * does not change anything.
     */
    public void cancel() {
        // Stop any operation running on the `source` of this parser.
        m_source.cancel(true);
    }

    @Override
    public void onDataLoadingStarted() {
        // Forward the signal to this object's listeners.
        for (ParsingDoneListener listener : m_listeners) {
            listener.onParsingStarted();
        }
    }

    @Override
    public void onDataLoadingProgress(float progress) {
        // Forward the signal to this object's listeners.
        for (ParsingDoneListener listener : m_listeners) {
            listener.onParsingProgress(progress);
        }
    }

    @Override
    public void onDataLoadingSuccess() {
        // Forward the signal to this object's listeners.
        for (ParsingDoneListener listener : m_listeners) {
            listener.onParsingFinished();
        }
    }

    @Override
    public void onDataLoadingFailure() {
        // Forward the signal to this object's listeners.
        for (ParsingDoneListener listener : m_listeners) {
            listener.onParsingFailed();
        }
    }

    /**
     * Used to perform a save operation on the progression reached by this parser
     * on the attached read. We will use the specified context in order to get the
     * file linked to this file and thus save the progression.
     * @param context - the context to use to perform the saving operation.
     */
    public boolean saveProgression(Context context) {
        // Retrieve the local storage path.
        File appDir = context.getFilesDir();

        // Retrieve the list of the files registered in the directory: this will
        // correspond to the existing reads.
        File[] reads = appDir.listFiles();

        // Check whether this directory exists: if this is not the case we don't
        // need to bother with loading reads from it.
        if (reads == null) {
            return false;
        }

        // Try to find the file describing the current read.
        String saveFile = ReadDesc.generateReadSaveName(context, m_desc.getName());

        int id = 0;
        while (id < reads.length && !reads[id].getName().equals(saveFile)) {
            ++id;
        }

        // Check whether we could find the save for this read.
        if (id >= reads.length) {
            return false;
        }

        // Save the progression to this file: we need to first open it, then
        // update the progression key and finally serialize back the data. In
        // order to successfully parse the file we need to first retrieve a
        // stream reader that will be used to get the content.
        File in = reads[id];

        FileInputStream stream;
        try {
            stream = context.openFileInput(in.getName());
        }
        catch (FileNotFoundException e) {
            // Failed to load the file, we can't save the progression.
            return false;
        }

        // Open the file and prepare to parse it. We will extract its content and then
        // try to instantiate a valid `ReadDesc` object from it.
        ReadDesc read = ReadDesc.fromInputStream(context, stream);

        // Check for failure to parse the file.
        if (read == null) {
            // Failure to load from the file, return early.
            return false;
        }

        // Update the progression for this read.
        read.setProgression(getCompletion());

        // Also bump the last access date to right now.
        read.touch();

        // Save the read back to the disk.
        try {
            FileWriter writer = new FileWriter(in);
            read.save(context, writer);
        }
        catch (IOException e) {
            // Failure to perform the save on disk.
            return false;
        }

        // We successfully saved the progression for the underlying read.
        return true;
    }

    /**
     * Return `true` if the parser is ready, which means that the data from the
     * source has been retrieved and is accessible for fetching.
     * @return - `true` if the data is ready and `false` otherwise.
     */
    public boolean isReady() {
        return !m_source.isEmpty();
    }

    /**
     * Similar to the `isAtEnd` method but allows to determine whether the
     * parser has reached the beginning of the data stream. This is typically
     * the case when the user has never opened a read so far.
     * @return - `true` if the parser has reached the beginning of the data
     *           stream associated to it.
     */
    public boolean isAtStart() {
        return m_source.isAtStart();
    }

    /**
     * Returns `true` if this parser has reached the end of the data stream
     * describing the read. This can be used to detect whenever some actions
     * that require some data to be left to be triggered have to be disabled.
     * @return - `true` if this parser has reached the end of the data stream
     *           and `false` otherwise.
     */
    public boolean isAtEnd() {
        return m_source.isAtEnd();
    }

    /**
     * Should return `true` in case this parser is right at a paragraph which
     * indicate that the reading session should be paused in case the user is
     * wanting to stop reading and start again later.
     * TODO: We should instead provide some sort of counting mechanism where
     * we only stop when a fixed amount of words have been read.
     * @return - always `false` for now.
     */
    public boolean isAtParagraph() {
        return false;
    }

    /**
     * Returns the current completion reached by this parser. This value is
     * initialized upon building the object and then updated each one a new
     * word is requested.
     * @return - the current completion reached by this parser.
     */
    public float getCompletion() {
        return m_source.getCompletion();
    }

    /**
     * Used for external elements to retrieve the current word pointed at by
     * this parser. This is somewhat similar to the `getNextWord` method but
     * it only gets the current word and does not advance on the data used
     * by this parser.
     * Calling this method repeatedly will not cause the parser to reach the
     * end of the data stream.
     * @return - a string representing the current word.
     */
    public String getCurrentWord() {
        return m_source.getCurrentWord();
    }

    /**
     * Used to perform a rewind of all the data read so far by the parser.
     * This is useful to get back to the beginning of a read.
     */
    public void rewind() {
        m_source.perform(ReadLoader.Action.Rewind, 0);
    }

    /**
     * Used to perform a motion to the indicate to the parser that it should
     * be moved to the previous step. The step is defined as a fixed amount
     * of words that need to be skipped.
     * Note that in case it's not possible to move that far backwards we do
     * try to move as far as possible.
     */
    public void moveToPrevious() {
        m_source.perform(ReadLoader.Action.PreviousStep, STEP_WORDS_COUNT);
    }

    /**
     * Similar method to `moveToPrevious` but used in case the parser should
     * move to the next word by skipping an entire step. Just like for the
     * `moveToPrevious` method we try to move as far as possible even though
     * it might not be possible to move all the way through.
     */
    public void moveToNext() {
        m_source.perform(ReadLoader.Action.NextStep, STEP_WORDS_COUNT);
    }

    /**
     * Used by external elements to make the parser advance to the next word.
     * Repeatedly calling this method will eventually make the parser reach
     * the end of the stream.
     * Note that in case the parser is already at the end of the data source
     * nothing happens.
     */
    public void advance() {
        m_source.perform(ReadLoader.Action.NextWord, 0);
    }
}
