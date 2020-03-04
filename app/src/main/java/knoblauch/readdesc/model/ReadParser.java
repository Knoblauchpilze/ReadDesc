package knoblauch.readdesc.model;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReadParser implements ReadLoader.ReadLoaderListener {

    /**
     * Describe a convenience interface allowing for anyone to listen to the
     * notification fired whenever the parser finishes to load its data from
     * its source.
     */
    public interface ParsingDoneListener {

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
     * Defines the maximum progression possible for a parser.
     */
    private static final float MAX_PROGRESSION = 1.0f;

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
     * Holds the current progression of this parser. Upon building the object
     * it is updated with the completion reached so far by previous openings
     * of the read and is updated while the next word is retrieved.
     */
    private float m_completion;

    /**
     * A mutex allowing to protect concurrent access to the data in this parser
     * so that we can safely handle modifications of the internal state such as
     * a request on a new word or a jump to a later paragraph.
     */
    private Lock m_locker;

    /**
     * Holds the list of paragraphs that have been parsed from the reader's
     * source. This is a full list of the content that should be navigated
     * and displayed by this reader.
     * Note that each paragraph is composed of words and that the controls
     * should aim at providing level of control intra-paragraph and not only
     * at a macro (i.e. paragraph) level.
     */
    private ArrayList<Paragraph> m_paragraphs;

    /**
     * Holds the current index of the virtual cursor in the total list of
     * paragraphs registered in this object. This value is set to a value
     * larger than the size of the `m_paragraphs` size in case we reached
     * the end of the last paragraph.
     */
    private int m_paragraphIndex;

    /**
     * Holds the index of the current word in the active paragraph. This
     * value is never greater than the size of the active paragraph and
     * is reset to `0` in case the next paragraph should be processed.
     */
    private int m_wordIndex;

    /**
     * Holds the total number of words contained in all the paragraphs
     * encountered by this parser. This is used in order to speed up
     * some computations where we need to quickly access the position
     * of a word within the total document.
     */
    private int m_totalWordCount;

    /**
     * Holds the position of the `m_wordIndex` in terms of global word
     * index. This is useful to easily compute a progress in addition
     * to the `m_totalWordCount`.
     */
    private int m_globalWordIndex;

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
     * The listener that should be notified whenever the data loaded
     * from the source is successfully received. It will also be used
     * if the data fails to be loaded.
     */
    private ParsingDoneListener m_listener;

    /**
     * Create a new parser from the specified read. The parser will detect the
     * type of the read and instantiate a valid data source to fetch and load
     * it.
     * @param read - the read containing information about the data source to
     *               use to get the data.
     * @param context - the context to use to resolve links and resources so
     *                  that we can access to the read's content for example.
     * @param listener - a listener that should be notified when the data has
     *                   been loaded for this parser.
     */
    public ReadParser(ReadIntent read, Context context, ParsingDoneListener listener) {
        // Assign the name of the read.
        m_desc = read;

        // Create the multi-threading protection.
        m_locker = new ReentrantLock();

        // Create the list of paragraphs and the rest of the properties.
        m_paragraphs = new ArrayList<>();
        m_paragraphIndex = 0;
        m_wordIndex = 0;

        m_totalWordCount = 0;
        m_globalWordIndex = 0;

        // Create the data source based on the type of the read associated
        // to this parser.
        switch (m_desc.getType()) {
            case WebPage:
                m_source = new HtmlSourceLoader(context, this);
            case EBook:
                m_source = new EBookSourceLoader(context, this);
            case File:
                m_source = new PdfSourceLoader(context, this);
                break;
        }

        // Register the listener so that we can notify it.
        m_listener = listener;

        // Start the loading of the data.
        m_source.execute(m_desc.getDataUri());
    }

    /**
     * Returns the current completion reached by this parser. This value is
     * initialized upon building the object and then updated each one a new
     * word is requested.
     * @return - the current completion reached by this parser.
     */
    public float getCompletion() {
        // Acquire the lock on this parser.
        m_locker.lock();
        float cp = m_completion;
        m_locker.unlock();

        return cp;
    }

    /**
     * Convenience wrapper around the `getCompletion` method which allows to
     * retrieve the progression as a percentage value. Uses said method as
     * a way to compute this percentage (so it requires the lock on this item
     * to be acquired).
     * @return - an integer representing the percentage of completion reached
     *           so far by this reader.
     */
    public int getCompletionAsPercentage() {
        return Math.round(100.0f * getCompletion());
    }

    /**
     * Retrieves the name of the read associated to this parser.
     * @return - the name of the read linked to this parser.
     */
    public String getName() { return m_desc.getName(); }

    /**
     * Very similar to the `setProgressPrivate` method but attempts to get
     * the lock on this object first.
     * @param progress - the progress to assign to this parser.
     */
    public void setProgress(float progress) {
        m_locker.lock();

        try {
            setProgressPrivate(progress);
        }
        finally {
            m_locker.unlock();
        }
    }

    /**
     * Used to update the internal values so that this parser reaches the
     * desired progression. This usually means moving the virtual cursor
     * on the attached data to reach at least this progression value.
     * This value is clamped to the range `[0; 1]` if it is not already
     * in this interval.
     * Note that in this virtual base we will only update the internal
     * completion value if the interface method returns a valid result.
     * @param progress - the progression to set for this parser.
     */
    private void setProgressPrivate(float progress) {
        // Use the abstract handler and update the internal completion value
        // if it succeeds.
        m_completion = advanceTo(Math.min(1.0f, Math.max(0.0f, progress)));
    }

    /**
     * Returns `true` if this parser has reached the end of the data stream
     * describing the read. This can be used to detect whenever some actions
     * that require some data to be left to be triggered have to be disabled.
     * @return - `true` if this parser has reached the end of the data stream
     *           and `false` otherwise.
     */
    public boolean isAtEnd() {
        // The parser reached the end of the read in case the current paragraph
        // index is larger (or equal) to the size of the paragraphs list.
        m_locker.lock();
        boolean atEnd = isAtEndPrivate();
        m_locker.unlock();

        return atEnd;
    }

    /**
     * Similar to the `isAtEnd` method but allows to determine whether the
     * parser has reached the beginning of the data stream. This is typically
     * the case when the user has never opened a read so far.
     * @return - `true` if the parser has reached the beginning of the data
     *           stream associated to it.
     */
    public boolean isAtStart() {
        // The parser is at the beginning of the read if at least one paragraph
        // is available and we didn't even read a single word.
        m_locker.lock();
        boolean atStart = isAtStartPrivate();
        m_locker.unlock();

        return atStart;
    }

    /**
     * Similar to the `isAtEnd` but allows to determine whether this parser
     * has reached an inner paragraph of some sort in the reading data. It
     * is usually used to give a break to the user and prompt him whether
     * the read should be stopped or continued.
     * @return - `true` if the reader reached the end of the paragraph and
     *           `false` otherwise.
     */
    public boolean isAtParagraph() {
        // We are right at a paragraph if the current word index is `0`, no matter
        // the current paragraph index.
        m_locker.lock();
        boolean atParagraph = (m_wordIndex == 0);
        m_locker.unlock();

        return atParagraph;
    }

    /**
     * Used by external elements to make this parser advance to the next word.
     * This will usually move to the next element of the data stream based on
     * the actual type of the content to fetch.
     */
    public void advance() {
        // Discard cases where the parser is not valid or is already at the end.
        m_locker.lock();
        if (!isValid() || isAtEndPrivate()) {
            m_locker.unlock();
            return;
        }

        try {
            // Move to the next word. This might include moving to the next paragraph
            // if we reach the last word.
            ++m_wordIndex;
            ++m_globalWordIndex;

            // Check whether we need to move to the next paragraph.
            if (m_wordIndex >= m_paragraphs.get(m_paragraphIndex).size()) {
                m_wordIndex = 0;
                ++m_paragraphIndex;
            }

            // Update completion.
            m_completion = 1.0f * m_globalWordIndex / m_totalWordCount;
        }
        finally {
            m_locker.unlock();
        }
    }

    /**
     * Used internally to advance to a certain progression. Depending on the
     * actual data source baking this parser it might means fetching content
     * from the disk or querying another source, etc. It is up to the concrete
     * parser to determine what is needed.
     * @param progress - the progress that should be reached by this parser.
     *                   Note that this value is guaranteed to be in the range
     *                   `[0; 1]`.
     * @return - the actual progression reached by this parser. In case the
     *           specified progression cannot be exactly reached the closest
     *           approximation is returned.
     */
    private float advanceTo(float progress) {
        // We can't do anything if the parser is not valid.
        m_locker.lock();
        if (!isValid()) {
            m_locker.unlock();
            return MAX_PROGRESSION;
        }

        float completion;

        try {
            // We need to traverse the list of paragraphs until we reach the desired
            // progression or at least the closest approximation possible.
            m_paragraphIndex = 0;
            m_wordIndex = 0;

            m_globalWordIndex = 0;

            m_completion = 0.0f;

            // Avoid unnecessary processing in case the progress should be set to `0`.
            if (progress == 0.0f) {
                return 0.0f;
            }

            float last = 0.0f;
            while (m_completion < progress) {
                // Check whether we can move one paragraph ahead.
                float nextP = 1.0f * (m_globalWordIndex + m_paragraphs.get(m_paragraphIndex).size()) / m_totalWordCount;
                if (nextP < progress) {
                    m_globalWordIndex += m_paragraphs.get(m_paragraphIndex).size();
                    m_completion = 1.0f * m_globalWordIndex / m_totalWordCount;
                    ++m_paragraphIndex;

                    // Save the last progress in case we need to rewind one word because
                    // it was closer than the current one.
                    last = nextP;

                    continue;
                }

                // Advancing from a whole paragraph is not possible, check how many words
                // we can advance at most within this paragraph.
                float remaining = progress - 1.0f * m_globalWordIndex / m_totalWordCount;
                float pProgress = 1.0f * m_paragraphs.get(m_paragraphIndex).size() / m_totalWordCount;
                int expected = (int)Math.round(Math.floor((double)remaining* m_paragraphs.get(m_paragraphIndex).size() / pProgress)) + 1;

                // Handle the additional `1` at the end: indeed imagine the following case
                // where we have 2 paragraphs, each one composed of `1` word. We want to
                // reach `53%` completion.
                // We will first skip the first paragraph (as it only brings us to `50%`).
                // Then we will compute the remaining progression within the second one and
                // find `3%` which is not a complete word so we will add `1` to be sure so
                // we will end up past the last word of the paragraph.
                if (expected >= m_paragraphs.get(m_paragraphIndex).size()) {
                    m_globalWordIndex += m_paragraphs.get(m_paragraphIndex).size();
                    ++m_paragraphIndex;
                    expected = 0;
                }

                m_wordIndex = expected;
                m_globalWordIndex += expected;

                m_completion = 1.0f * m_globalWordIndex / m_totalWordCount;
                last = 1.0f * (m_globalWordIndex - 1) / m_totalWordCount;
            }

            // Determine whether the previous word was closer to the desired progress.
            if (Math.abs(progress - last) <= Math.abs(progress - m_completion)) {
                if (m_wordIndex == 0) {
                    moveToPrevious();
                } else {
                    --m_wordIndex;
                    --m_globalWordIndex;
                }
            }
        }
        finally {
            completion = 1.0f * m_globalWordIndex / m_totalWordCount;

            m_locker.unlock();
        }

        // Return the progression that was reached.
        return completion;
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
        // In case the parser is not a valid state, do nothing (i.e.
        // return an empty string).
        m_locker.lock();
        if (!isValid() || isAtEndPrivate()) {
            m_locker.unlock();
            return "";
        }

        // Retrieve the current word of the paragraph we're at.
        return m_paragraphs.get(m_paragraphIndex).getWord(m_wordIndex);
    }

    /**
     * Used to indicate to the parser that it should be moved to the previous
     * paragraph of the read. This method is usually triggered by the user and
     * requests to go fetch the previous paragraph's data from the source of
     * the read.
     * Note that in case the previous paragraph does not exist (typically when
     * the user did not get past the first) nothing happens.
     */
    public void moveToPrevious() {
        // When moving to the previous paragraph we also reset the word
        // index so as to start from the beginning of the paragraph.
        // If the action is not possible (i.e. if we're already in the
        // first paragraph) we just reset the word index. Note that we
        // handle the case where the user is in the middle of a paragraph
        // by first rewinding to the first word of the current paragraph
        // and if the user is right at the beginning to the previous one.
        // And if none of that is possible we don't do anything (because
        // we are most likely already at the beginning).
        m_locker.lock();
        if (!isValid() || isAtStartPrivate()) {
            m_locker.unlock();
            return;
        }

        // Handle the global word index by counting how many words we
        // are rewinding.
        boolean updated = false;
        if (m_wordIndex != 0) {
            m_globalWordIndex -= m_wordIndex;
            updated = true;
        }

        // In any case we will reset the word index to `0`.
        m_wordIndex = 0;

        // We want to move to the previous paragraph only if we were at
        // the beginning of a paragraph: otherwise we will just reset the
        // position in the current paragraph.
        if (!updated) {
            m_paragraphIndex = Math.max(0, m_paragraphIndex - 1);
            m_globalWordIndex -= m_paragraphs.get(m_paragraphIndex).size();
        }

        // Update the completion.
        m_completion = 1.0f * m_globalWordIndex / m_totalWordCount;

        m_locker.unlock();
    }

    /**
     * Similar method to `moveToPrevious` but used in case the parser should
     * move to the next paragraph. Just like for the `moveToPrevious` method
     * nothing happens in case the reader already reached the last paragraph
     * available in the read.
     */
    public void moveToNext() {
        // Similar to the `moveToPrevious` but to the next paragraph.
        // Note that we also reset the word index to start at the
        // beginning of the paragraph.
        // In case we're already in the last paragraph we will reach
        // the end of the read.
        m_locker.lock();
        if (!isValid() || isAtEndPrivate()) {
            m_locker.unlock();
            return;
        }

        // Handle the global word index by counting how many words we
        // are skipping.
        m_globalWordIndex += (m_paragraphs.get(m_paragraphIndex).size() - m_wordIndex);

        // Reset as we are at the beginning of the next paragraph.
        m_wordIndex = 0;

        // Update the paragraph index.
        m_paragraphIndex = Math.min(m_paragraphs.size(), m_paragraphIndex + 1);

        // Update the completion.
        m_completion = 1.0f * m_globalWordIndex / m_totalWordCount;

        m_locker.unlock();
    }

    /**
     * Used to perform a rewind of all the data read so far by the parser. This
     * is useful to get back to the beginning of a read.
     */
    public void rewind() {
        // Discard invalid cases.
        m_locker.lock();
        if (!isValid()) {
            m_locker.unlock();
            return;
        }

        // Reset the parser to the first paragraph.
        m_wordIndex = 0;
        m_paragraphIndex = 0;

        // Also update the word index.
        m_globalWordIndex = 0;

        // Update the completion.
        m_completion = 1.0f * m_globalWordIndex / m_totalWordCount;

        m_locker.unlock();
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
     * source has been retrieved and is accessible in the `m_paragraphs` item
     * in this object.
     * @return - `true` if the data is ready and `false` otherwise.
     */
    public boolean isReady() {
        m_locker.lock();
        boolean ready = !m_paragraphs.isEmpty();
        m_locker.unlock();

        return ready;
    }

    @Override
    public void onDataLoaded(ArrayList<Paragraph> paragraphs) {
        // Acquire the lock on this object.
        m_locker.lock();

        try {
            // Copy data to the local attribute.
            m_paragraphs = paragraphs;

            // Count the total number of words available.
            m_totalWordCount = 0;
            for (Paragraph p : m_paragraphs) {
                m_totalWordCount += p.size();
            }

            // Once the data has been loaded we can setup the progress to match
            // the value that was reached in a previous read session.
            setProgressPrivate(m_desc.getCompletion());
        }
        finally {
            m_locker.unlock();
        }

        // Notify the listener that the data has been successfully loaded.
        m_listener.onParsingFinished();
    }

    @Override
    public void onFailureToLoadData() {
        // Notify the listener so that it can take corresponding measures.
        m_listener.onParsingFailed();
    }

    /**
     * Determine whether this parser is valid or not based on whether it
     * has at least some paragraphs.
     * @return - `true` if the parser is valid or `false` otherwise.
     */
    private boolean isValid() {
        return !m_paragraphs.isEmpty() && m_totalWordCount > 0;
    }

    /**
     * Similar to the `isAtEnd` method but does not try to acquire the lock
     * on this object which allows to use it in internal methods.
     * @return - `true` if the parser is at the end of the data stream and
     *           `false` otherwise.
     */
    private boolean isAtEndPrivate() {
        return m_paragraphIndex >= m_paragraphs.size();
    }

    /**
     * Fills a similar role to `isAtEndPrivate` for the `isAtEnd` method: as
     * it does not acquire the internal lock we can use it in internal calls.
     * @return - `true` if the parser is at the beginning of the data stream.
     */
    private boolean isAtStartPrivate() {
        return isValid() && m_paragraphIndex == 0 && m_wordIndex == 0;
    }
}
