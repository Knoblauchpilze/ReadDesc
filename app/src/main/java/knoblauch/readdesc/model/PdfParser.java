package knoblauch.readdesc.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class PdfParser extends ReadParser {

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
     * Create a new `PDF` parser from the specified name and source. The source
     * will be assigned internally and used whenever some new data is required
     * to advance the parser.
     * @param name - the name of the read.
     * @param source - an `uri` describing the data source for this parser. In
     *                 case this is not a valid `PDF` file an error is raised.
     * @param context - the context to use to perform link resolution and access
     *                  to resources in general.
     */
    PdfParser(String name, Uri source, Context context) throws IOException {
        // Call the base construction method.
        super(name, context);

        // Create the list of paragraphs.
        m_paragraphs = new ArrayList<>();
        m_paragraphIndex = 0;
        m_wordIndex = 0;

        m_totalWordCount = 0;
        m_globalWordIndex = 0;

        // Assign the source for this read.
        // TODO: Actually this is the specific part of the parser. Maybe we could make this
        // abstract and set all the other methods to be implemented in the base class which
        // would be more interesting for factorization.
        // We only have to find a way to correctly handle lazy loading of data.
        setData(source);
    }

    /**
     * Used internally to setup the provided `source` as the main baking data
     * for this reader. Some checks are performed to make sure that the input
     * `uri` refers to a valid `PDF` document.
     * @param source - an `uri` describing a valid `PDF` document.
     */
    private void setData(Uri source) throws IOException {
        // Try to instantiate a valid `PDF` reader from this source.
        if (source == null || source.getPath() == null) {
            throw new IOException("Cannot set invalid PDF source in parser for \"" + getName() + "\"");
        }

        // We need to create a file as the source of the `PDF` reader. Note
        // that as we store the content's `uri` we need to resolve the link
        // in order to access to the file.
        ContentResolver res = m_context.getContentResolver();
        InputStream inStream = res.openInputStream(source);
        if (inStream == null) {
            throw new IOException("Cannot load PDF content \"" + source.toString() + "\" in parser for \"" + getName() + "\"");
        }

        PdfReader reader = new PdfReader(inStream);

        // Perform the extraction of the text contained in this `PDF` document
        // through a text extraction strategy.
        PdfTextExtractor extractor = new PdfTextExtractor();
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);

        try {
            for (int i = 1 ; i <= reader.getNumberOfPages() ; ++i) {
                // Parse the paragraphs for this page.
                extractor = parser.processContent(i, extractor);

                // Register each one of them in the internal array and update
                // the word count along the way.
                ArrayList<Paragraph> paragraphs = extractor.getParagraphs();

                for (Paragraph p : paragraphs) {
                    // Sanitize the paragraph.
                    p.sanitize();

                    // Discard empty paragraph.
                    if (p.isEmpty()) {
                        continue;
                    }

                    // Check whether we can group it with another existing one.
                    if (p.canBeGrouped() && !m_paragraphs.isEmpty()) {
                        Paragraph last = m_paragraphs.get(m_paragraphs.size() - 1);
                        m_totalWordCount -= last.size();
                        last.merge(p);
                        m_totalWordCount += last.size();

                        continue;
                    }

                    // Register this paragraph as it cannot be grouped.
                    m_totalWordCount += p.size();
                    m_paragraphs.add(p);
                }

                // Clear the extractor to be ready for the next page.
                extractor.clear();
            }
        }
        catch (Exception e) {
            // We encountered an error while parsing the `PDF` document, consider
            // the input source as invalid.
            throw new IOException("Cannot parse content of PDF source in parser for \"" + getName() + "\"");
        }
    }

    /**
     * Determine whether this parser is valid or not based on whether it
     * has at least some paragraphs.
     * @return - `true` if the parser is valid or `false` otherwise.
     */
    private boolean isValid() {
        return !m_paragraphs.isEmpty() && m_totalWordCount > 0;
    }

    @Override
    public boolean isAtEnd() {
        // The parser reached the end of the read in case the current paragraph
        // index is larger (or equal) to the size of the paragraphs list.
        m_locker.lock();
        boolean atEnd = isAtEndPrivate();
        m_locker.unlock();

        return atEnd;
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

    @Override
    public boolean isAtStart() {
        // The parser is at the beginning of the read if at least one paragraph
        // is available and we didn't even read a single word.
        m_locker.lock();
        boolean atStart = isAtStartPrivate();
        m_locker.unlock();

        return atStart;
    }

    /**
     * Fills a similar role to `isAtEndPrivate` for the `isAtEnd` method: as
     * it does not acquire the internal lock we can use it in internal calls.
     * @return - `true` if the parser is at the beginning of the data stream.
     */
    private boolean isAtStartPrivate() {
        return isValid() && m_paragraphIndex == 0 && m_wordIndex == 0;
    }

    @Override
    public boolean isAtParagraph() {
        // We are right at a paragraph if the current word index is `0`, no matter
        // the current paragraph index.
        m_locker.lock();
        boolean atParagraph = (m_wordIndex == 0);
        m_locker.unlock();

        return atParagraph;
    }

    @Override
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

    @Override
    float advanceTo(float progress) {
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

            // TODO: We should also load this asynchronously and protect the object with
            // some sort of a locker.

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

    @Override
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

    @Override
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

    @Override
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

    @Override
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
}
