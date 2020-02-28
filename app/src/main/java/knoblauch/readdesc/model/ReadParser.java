package knoblauch.readdesc.model;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReadParser {

    /**
     * The name of the underlying read backing the data for this parser. It is
     * used as a way to identify the read associated to this object so that the
     * result of the progression can be saved back to the disk.
     */
    private String m_name;

    /**
     * Describe the source of the data that can be accessed to this parser. It
     * is retrieved from the `ReadDesc` from which this parser is instantiated
     * and is used as the primary data source of the content.
     */
    private Uri m_source;

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

    private int m_count;

    /**
     * Instantiate a suitable parser for the input `desc`. Depending on the type
     * of the read a specific parser is instantiated so that we can successfully
     * fetch and display data from the source of the read.
     * @param desc - the read description from which a parser should be built.
     * @return - a valid parser for this read or `null` if the type of the read
     *           does not correspond to a known parser.
     */
    public static ReadParser fromRead(ReadIntent desc) {
        return new ReadParser(desc.getName(), Uri.parse(desc.getDataUri()), desc.getCompletion());
    }

    /**
     * Create a new parser from the specified content `uri` and name.
     * @param name - the name of the read.
     * @param uri - the `uri` of the primary data source for this parser.
     * @param completion - the completion to associated to this parser.
     */
    private ReadParser(String name, Uri uri, float completion) {
        m_name = name;
        m_source = uri;
        m_completion = completion;

        // TODO: Remove this temporary hack for the completion.
        m_count = Math.round(m_completion * 100.0f);

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
     * Returns `true` if this parser has reached the end of the data stream
     * describing the read. This can be used to detect whenever some actions
     * that require some data to be left to be triggered have to be disabled.
     * @return - `true` if this parser has reached the end of the data stream
     *           and `false` otherwise.
     */
    public boolean isAtEnd() {
        // TODO: Handle the case where the parser is at the end of the data.
        return m_count >= 100;
    }

    /**
     * Similar to the `isAtEnd` method but allows to determine whether the
     * parser has reached the beginning of the data stream. This is typically
     * the case when the user has never opened a read so far.
     * @return - `true` if the parser has reached the beginning of the data
     *           stream associated to it.
     */
    public boolean isAtStart() {
        // TODO: Handle the correct state of the parser.
        return m_count == 0;
    }

    /**
     * Similar to the `isAtEnd` but allows to determine whether this parser
     * has reached an inner paragpraph of some sort in the reading data. It
     * is usually used to give a break to the user and prompt him whether
     * the read should be stopped or continued.
     * @return - `true` if the reader reached the end of the paragraph and
     *           `false` otherwise.
     */
    public boolean isAtParagraph() {
        m_locker.lock();
        boolean par = (m_count % 10 == 0);
        m_locker.unlock();
        // TODO: Should handle this.
        return par;
    }

    /**
     * Retrieve the next word available in this parser. This accounts for the
     * already decoded content and will move the virtual cursor of this parser
     * by one word.
     * In case the parser reached the end of the read an empty string is set
     * as the return value.
     * @return - a string representing the next word for this parser.
     */
    public String getNextWord() {
        // Acquire the lock on this parser.
        m_locker.lock();

        String str;
        try {
            // Retrieve the next word.
             str = "" + m_count;
            m_count++;

            m_completion = 1.0f * m_count / 100.0f;
        }
        finally {
            // Safe to unlock the mutex.
            m_locker.unlock();
        }

        // Return the created string.
        return str;
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
        // Acquire the lock on this parser.
        m_locker.lock();

        String str;
        try {
            // Convert the current word.
            str = "" + m_count;
        }
        finally {
            // Safe to unlock the mutex protecting this object.
            m_locker.unlock();
        }

        // The current word is given by `str`.
        return str;
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
        // TODO: Handle the previous paragraph.
        m_locker.lock();
        m_count = Math.max(m_count - 10, 0);
        m_completion = 1.0f * m_count / 100.0f;
        m_locker.unlock();
    }

    /**
     * Similar method to `moveToPrevious` but used in case the parser should
     * move to the next paragraph. Just like for the `moveToPrevious` method
     * nothing happens in case the reader already reached the last paragraph
     * available in the read.
     */
    public void moveToNext() {
        // TODO: Handle properly the next paragraph.
        m_locker.lock();
        m_count = Math.min(m_count + 10, 100);
        m_completion = 1.0f * m_count / 100.0f;
        m_locker.unlock();
    }

    /**
     * Used to perform a rewind of all the data read so far by the parser. This
     * is useful to get back to the beginning of a read.
     */
    public void rewind() {
        // TODO: Handle properly the rewind of the parser.
        m_locker.lock();
        m_count = 0;
        m_completion = 1.0f * m_count / 100.0f;
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
        String saveFile = ReadDesc.generateReadSaveName(context, m_name);

        int id = 0;
        while (id < reads.length && !reads[id].getName().equals(saveFile)) {
            id++;
        }

        // Check whether we could find the save for this read.
        if (id >= reads.length) {
            return false;
        }

        Log.i("main", "Saving progress " + m_completion + " for \"" + m_name + "\" (id: " + id + " to file \"" + reads[id].getName() + "\")");

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
