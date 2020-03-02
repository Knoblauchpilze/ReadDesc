package knoblauch.readdesc.model;

import android.content.Context;
import android.net.Uri;

public class EBookParser extends ReadParser {

    /**
     * Describe the source of the data that can be accessed to this parser. It
     * is retrieved from the `ReadDesc` from which this parser is instantiated
     * and is used as the primary data source of the content.
     * We assume in this parser that the source describes a valid `EBook` item.
     */
    private Uri m_source;

    private int m_count;

    /**
     * Create a new `E-book` parser from the specified name and source. The source
     * will be assigned internally and used whenever some new data is required
     * to advance the parser.
     * @param name - the name of the read.
     * @param source - an `uri` describing the data source for this parser. In
     *                 case this is not a valid `E-book` document an error is
     *                 raised.
     * @param context - the context to use to perform link resolution and access
     *                  to resources in general.
     */
    EBookParser(String name, Uri source, Context context) {
        // Call the base construction method.
        super(name, context);

        // Assign the source for this read.
        setData(source);
    }

    /**
     * Used internally to setup the provided `source` as the main baking data
     * for this reader. Some checks are performed to make sure that the input
     * `uri` refers to a valid `E-book` document.
     * @param source - an `uri` describing a valid `E-book` document.
     */
    private void setData(Uri source) {
        m_source = source;

        // TODO: Remove this temporary hack for the completion.
        m_count = Math.round(m_completion * 100.0f);
    }

    @Override
    public boolean isAtEnd() {
        // TODO: Handle the case where the parser is at the end of the data.
        return m_count >= 100;
    }

    @Override
    public boolean isAtStart() {
        // TODO: Handle the correct state of the parser.
        return m_count == 0;
    }

    @Override
    public boolean isAtParagraph() {
        m_locker.lock();
        boolean par = (m_count % 10 == 0);
        m_locker.unlock();
        // TODO: Should determine precisely when we reach a paragraph.
        return par;
    }

    @Override
    public void advance() {
        // Acquire the lock on this parser.
        m_locker.lock();
        // TODO: Handle properly the advance in the read.

        ++m_count;
        m_completion = 1.0f * m_count / 100.0f;

        m_locker.unlock();
    }

    @Override
    float advanceTo(float progress) {
        // TODO: Handle properly the advance to a certain position in the read.
        m_count = Math.round(100.0f * progress);
        return progress;
    }

    @Override
    public String getCurrentWord() {
        // Acquire the lock on this parser.
        m_locker.lock();
        // TODO: Handle properly the retrieval of the current word.

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

    @Override
    public void moveToPrevious() {
        // TODO: Handle move to the previous paragraph.
        m_locker.lock();
        m_count = Math.max(m_count - 10, 0);
        m_completion = 1.0f * m_count / 100.0f;
        m_locker.unlock();
    }

    @Override
    public void moveToNext() {
        // TODO: Handle move to the next paragraph.
        m_locker.lock();
        m_count = Math.min(m_count + 10, 100);
        m_completion = 1.0f * m_count / 100.0f;
        m_locker.unlock();
    }

    @Override
    public void rewind() {
        // TODO: Handle properly the rewind of the parser.
        m_locker.lock();
        m_count = 0;
        m_completion = 1.0f * m_count / 100.0f;
        m_locker.unlock();
    }
}
