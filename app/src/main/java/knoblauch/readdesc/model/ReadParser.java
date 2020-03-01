package knoblauch.readdesc.model;

import android.content.Context;
import android.net.Uri;
import android.text.Html;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ReadParser {

    /**
     * The name of the underlying read backing the data for this parser. It is
     * used as a way to identify the read associated to this object so that the
     * result of the progression can be saved back to the disk.
     */
    private String m_name;

    /**
     * Holds the current progression of this parser. Upon building the object
     * it is updated with the completion reached so far by previous openings
     * of the read and is updated while the next word is retrieved.
     */
    float m_completion;

    /**
     * A mutex allowing to protect concurrent access to the data in this parser
     * so that we can safely handle modifications of the internal state such as
     * a request on a new word or a jump to a later paragraph.
     */
    Lock m_locker;

    /**
     * Instantiate a suitable parser for the input `desc`. Depending on the type
     * of the read a specific parser is instantiated so that we can successfully
     * fetch and display data from the source of the read.
     * @param desc - the read description from which a parser should be built.
     * @return - a valid parser for this read or `null` if the type of the read
     *           does not correspond to a known parser.
     */
    public static ReadParser fromRead(ReadIntent desc) {
        // Detect the type of the read and instantiate the correct parser.
        ReadParser reader = null;

        switch (desc.getType()) {
            case WebPage:
                reader = new HtmlReader(desc.getName(), Uri.parse(desc.getDataUri()));
                break;
            case EBook:
                reader = new EbookReader(desc.getName(), Uri.parse(desc.getDataUri()));
                break;
            case File:
                reader = new PdfReader(desc.getName(), Uri.parse(desc.getDataUri()));
                break;
        }

        // Assign the completion specified by the intent.
        reader.setProgress(desc.getCompletion());

        // Return the built-in parser.
        return reader;
    }

    /**
     * Create a new parser from the specified name. Note that such a parser does
     * not actually have any data source associated to it and its progression is
     * set to `0`.
     * @param name - the name of the read.
     */
    ReadParser(String name) {
        // Assign the name of the read.
        m_name = name;

        // Create the multi-threading protection.
        m_locker = new ReentrantLock();
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
    public String getName() { return m_name; }

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
    public void setProgress(float progress) {
        // Lock this object.
        m_locker.lock();

        // Use the abstract handler and update the internal completion value
        // if it succeeds.
        try {
            if (advanceTo(Math.min(1.0f, Math.max(0.0f, progress)))) {
                m_completion = progress;
            }
        }
        finally {
            m_locker.unlock();
        }
    }

    /**
     * Returns `true` if this parser has reached the end of the data stream
     * describing the read. This can be used to detect whenever some actions
     * that require some data to be left to be triggered have to be disabled.
     * @return - `true` if this parser has reached the end of the data stream
     *           and `false` otherwise.
     */
    public abstract boolean isAtEnd();

    /**
     * Similar to the `isAtEnd` method but allows to determine whether the
     * parser has reached the beginning of the data stream. This is typically
     * the case when the user has never opened a read so far.
     * @return - `true` if the parser has reached the beginning of the data
     *           stream associated to it.
     */
    public abstract boolean isAtStart();

    /**
     * Similar to the `isAtEnd` but allows to determine whether this parser
     * has reached an inner paragraph of some sort in the reading data. It
     * is usually used to give a break to the user and prompt him whether
     * the read should be stopped or continued.
     * @return - `true` if the reader reached the end of the paragraph and
     *           `false` otherwise.
     */
    public abstract boolean isAtParagraph();

    /**
     * Used by external elements to make this parser advance to the next word.
     * This will usually move to the next element of the data stream based on
     * the actual type of the content to fetch.
     */
    public abstract void advance();

    /**
     * Used internally to advance to a certain progression. Depending on the
     * actual data source baking this parser it might means fetching content
     * from the disk or querying another source, etc. It is up to the concrete
     * parser to determine what is needed.
     * @param progress - the progress that should be reached by this parser.
     *                   Note that this value is guaranteed to be in the range
     *                   `[0; 1]`.
     * @return - `true` if the parser could reach the specified progress value
     *           and `false` otherwise.
     */
    abstract boolean advanceTo(float progress);

    /**
     * Used for external elements to retrieve the current word pointed at by
     * this parser. This is somewhat similar to the `getNextWord` method but
     * it only gets the current word and does not advance on the data used
     * by this parser.
     * Calling this method repeatedly will not cause the parser to reach the
     * end of the data stream.
     * @return - a string representing the current word.
     */
    public abstract String getCurrentWord();

    /**
     * Used to indicate to the parser that it should be moved to the previous
     * paragraph of the read. This method is usually triggered by the user and
     * requests to go fetch the previous paragraph's data from the source of
     * the read.
     * Note that in case the previous paragraph does not exist (typically when
     * the user did not get past the first) nothing happens.
     */
    public abstract void moveToPrevious();

    /**
     * Similar method to `moveToPrevious` but used in case the parser should
     * move to the next paragraph. Just like for the `moveToPrevious` method
     * nothing happens in case the reader already reached the last paragraph
     * available in the read.
     */
    public abstract void moveToNext();

    /**
     * Used to perform a rewind of all the data read so far by the parser. This
     * is useful to get back to the beginning of a read.
     */
    public abstract void rewind();

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
        String saveFile = ReadDesc.generateReadSaveName(context, m_name);

        int id = 0;
        while (id < reads.length && !reads[id].getName().equals(saveFile)) {
            id++;
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
}
