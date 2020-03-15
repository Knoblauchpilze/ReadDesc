package knoblauch.readdesc.model;

import android.content.Context;
import android.util.Pair;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class HtmlSourceLoader extends ReadLoader {

    /**
     * A common regular expression allowing to split a string on successive space
     * characters.
     */
    private static final String SPACE_PATTERN = "\\s+";

    /**
     * Define the list of separators that are used as a way to mark the new line
     * and other separations in the `HTML` document but that we don't want to use
     * while in reading mode.
     * A word composed of only this character will be discarded.
     */
    private static final String SEPARATOR = "*";

    /**
     * A string representing the path to the data source for this loader. This is
     * interesting as the `HTML` parser we're using needs to have access to the
     * base path to reach the `HTML` document in order to be able to resolve links
     * and resources in the document.
     */
    private String m_base;

    /**
     * Contains the data loaded from the `HTML` document associated to this loader.
     * It contains the entirety of the source and can be navigated with auxiliary
     * structures defined in this object.
     * The input string is divided into single words based on space characters and
     * also grouped semantically so as to make the reading process easier (like in
     * case of a single punctuation character, we will try to group it with the
     * previous word, etc.
     */
    private ArrayList<String> m_words;

    /**
     * Defines the word index currently pointed at by this parser. This is linked
     * to the total number of words defined in the `m_words` list. It indicates the
     * progress by simple ratio.
     */
    private int m_wordID;

    /**
     * Contains the list of the position of titles in the document and in the `m_words`
     * index. The list is sequentially sorted (meaning that iterating from `0` to the
     * size of the array will yield increasing values) and represent the list of titles
     * and their associated position. This allows to move to the next title when asked.
     */
    private ArrayList<Integer> m_titlesID;

    /**
     * Holds the current index of the last title we passed. This means that the current
     * word index is *at least* greater than the index of this title. This value is used
     * as a base reference point when searching to move to the next/previous section to
     * avoid searching the entire `m_titlesID` array.
     */
    private int m_currentTitleID;

    /**
     * Create a new `HTML` source loader from the specified arguments. Will call
     * the base class constructor and forward the arguments.
     * The `HTML` parser we're using requires to have knowledge of the base `uri`
     * of the data source so that it can make sense of the relative links and the
     * resources defined in the source. The user should thus provide a string to
     * describe the location of the data source.
     * @param context - the context to use to resolve links and resources.
     * @param progress - the desired progress to load in priority. This allows
     *                   to orient the parsing operations to quickly reach this
     *                   point as it's of interest for the user.
     * @param base - a string representing the path to the data source, used to
     *               resolve relative links and resources.
     */
    HtmlSourceLoader(Context context, float progress, String base) {
        super(context, progress);

        // Save the base path to the data source for further usage.
        m_base = base;

        // Define words cursor variables.
        m_words = new ArrayList<>();
        m_wordID = -1;

        m_titlesID = new ArrayList<>();
        m_currentTitleID = -1;
    }

    /**
     * Used to copy the input loader and create a new object from it. We need to
     * copy fields which make sense and create new one as needed.
     * @param other - the other elements to copy.
     */
    HtmlSourceLoader(HtmlSourceLoader other) {
        // Call base handler.
        super(other);

        // Copy the path to the data source. Also copy them_text loaded data string
        // and cursors.
        m_base = other.m_base;

        m_words = other.m_words;
        m_wordID = other.m_wordID;

        m_titlesID = other.m_titlesID;
        m_currentTitleID = other.m_currentTitleID;
    }

    /**
     * Used internally to handle the creation of the list of words from the input
     * text string. This string is usually retrieved from the actual data source
     * and is analyzed to extract words from it. Each individual word is then set
     * as an entry in the `m_words` list for further usage.
     * Note that the virtual cursors defined by this object are not modified by
     * this method and that if any data is already registered in this object it
     * is not modified (basically we append the input content to the existing one).
     * @param text - the complete list of words to append to the `m_words` list.
     */
    private void handlePageLoading(String text) {
        // Discard obviously trivial cases.
        if (text == null || text.isEmpty()) {
            return;
        }

        // Trim the space characters from the input string.
        text = text.trim();

        // Split the input text based on space characters.
        String[] words = text.split(SPACE_PATTERN);

        // Interpret each word.
        for (String word : words) {
            // In case the word is empty or invalid, do not register it.
            if (word == null || word.isEmpty()) {
                continue;
            }

            // Trash words composed only of separators.
            if (word.length() == 1 && SEPARATOR.contains(word)) {
                continue;
            }

            // Add this word to the internal list.
            m_words.add(word);
        }
    }

    /**
     * Used to generate a list of checkpoints referencing the position of the first word of a
     * `HTML` title within the general `m_words` list. This allows to provide more intuitive
     * navigation when the user requests to move to the next/previous section.
     * The goal is to find all the elements having the `header` tag in the input document so
     * as to provide the most detailed navigation.
     * @param body - the body of the document associated to this source.
     */
    private void generateTitleCheckpoints(Element body) {
        // TODO: Handle title creation and fetching.
        // TODO: We should generate a first title at 0 all the time.
    }

    /**
     * Used to determine whether this parser contains some data. Note that the
     * locker is not acquired by this method so it can be used internally.
     * @return - `true` if this object defines some words and `false` otherwise.
     */
    private boolean hasWords() {
        return m_words != null && !m_words.isEmpty();
    }

    /**
     * Used internally to determine whether the word index is valid compared to
     * the data defined in the `m_words` list is consistent.
     * Note that we don't try to acquire the locker on this object so it is safe
     * to use it internally.
     * @return - `true` if the word index is consistent with the `m_words` data.
     */
    private boolean isValidWord() {
        return hasWords() && m_wordID >= 0 && m_wordID < m_words.size();
    }

    /**
     * Used to determine whether the `HTML` source of this document defines some
     * titles. This can be used before trying to access the `m_currentTitleID`.
     * @return - `true` if this document defines at least one title.
     */
    private boolean hasTitles() {
        return m_titlesID != null && !m_titlesID.isEmpty();
    }

    /**
     * Used internally to determine whether the current title is consistent in
     * regard to the titles parsed from the `HTML` source. This method does not
     * try to acquire the internal lock on this object so it is safe to use it
     * internally.
     * @return - `true` if it is safe to access the current title.
     */
    private boolean isValidTitle() {
        return hasTitles() && m_currentTitleID >= 0 && m_currentTitleID < m_titlesID.size();
    }

    @Override
    boolean isInvalid() {
        return !isValidWord();
    }

    @Override
    boolean isAtStart() {
        m_locker.lock();
        boolean atStart = (isValidWord() && m_wordID == 0);
        m_locker.unlock();

        return atStart;
    }

    @Override
    boolean isAtEnd() {
        m_locker.lock();
        boolean atEnd = (isValidWord() && m_wordID == m_words.size() - 1);
        m_locker.unlock();

        return atEnd;
    }

    @Override
    float getCompletion() {
        m_locker.lock();
        // Assume `0` completion.
        float progress;

        // Update in case this source is in a valid state. If the source is invalid
        // we have to make sure that it's not currently loading some data. If this
        // is the case we can still attempt to define a valid progression.
        if (isInvalid()) {
            progress = m_progress;
        }
        else {
            // Some data is available in the parser: compare the current word index
            // to the total words count.
            progress = 1.0f * m_wordID / m_words.size();
        }

        m_locker.unlock();

        // Return the built-in progression either through direct computation from
        // the construction of the object or by checking the index of the current
        // word compared to the total available list.
        return progress;
    }

    @Override
    String getCurrentWord() {
        m_locker.lock();

        // In case the parser is not a valid state, do nothing. This guarantees
        // that we can safely access to the `m_words` and `m_wordID` pointed to
        // in this internal object.
        if (isInvalid()) {
            m_locker.unlock();
            return "";
        }

        String word = m_words.get(m_wordID);
        m_locker.unlock();

        return word;
    }

    @Override
    String getPreviousWord() {
        m_locker.lock();

        // In case the parser is not a valid state, do nothing. This guarantees
        // that we can safely access to the `m_words` and `m_wordID` pointed to
        // in this internal object.
        // In case the parser is currently pointing to the first word we can't
        // retrieve the previous word so return an empty string
        if (isInvalid() || m_wordID == 0) {
            m_locker.unlock();
            return "";
        }

        String word = m_words.get(m_wordID - 1);
        m_locker.unlock();

        return word;
    }

    @Override
    String getNextWord() {
        m_locker.lock();

        // In case the parser is not a valid state, do nothing. This guarantees
        // that we can safely access to the `m_words` and `m_wordID` pointed to
        // in this internal object.
        // In case the parser is currently pointing to the last word we can't
        // retrieve the next one so return an empty string
        if (isInvalid() || m_wordID == m_words.size() - 1) {
            m_locker.unlock();
            return "";
        }

        String word = m_words.get(m_wordID + 1);
        m_locker.unlock();

        return word;
    }

    @Override
    Pair<Boolean, Boolean> handleMotion(Action action, int param) {
        // In case the parser is not in a valid state we consider that we didn't
        // achieve the motion nor that we require some loading.
        if (isInvalid()) {
            return new Pair<>(false, false);
        }

        // We now know that the parser is valid we can try to handle the motion.
        // Save the indices to see whether we could apply at least part of the
        // motion.
        int sWordID = m_wordID;

        // Perform the action.
        switch (action) {
            case Rewind:
                // Move to the first word.
                m_wordID = 0;

                // Try to update the title's index. We will assume that the first
                // title is the one to select: indeed even if this title is located
                // after the current word's index we can't do better than that in
                // terms of index.
                if (hasTitles()) {
                    m_currentTitleID = 0;
                }
                break;
            case NextWord:
                // Move to the next word.
                ++m_wordID;

                // Try to update the title's index: to do so we need to fetch the
                // position of the next title and see whether it is located before
                // the current word's index.
                if (isValidTitle()) {
                    // Start with the current title index.
                    int nexTitleWordID = m_titlesID.get(m_currentTitleID);

                    // Try to fetch the next one.
                    if (m_currentTitleID < m_titlesID.size() - 1) {
                        nexTitleWordID = m_titlesID.get(m_currentTitleID + 1);
                    }

                    // In case the next title is located before the current word's
                    // position, move to the next one.
                    if (m_wordID >= nexTitleWordID) {
                        ++m_currentTitleID;
                    }
                }
                break;
            case PreviousStep:
                // In case we want to reach the previous title we need to compute
                // the different between the position of the current word and the
                // position of the previous title: if we're not right at the start
                // of the current section we will move to the beginning of it. In
                // case we're already at the start we will move to the start of
                // the previous section.
                // In case no titles are defined we won't move.
                if (isValidTitle()) {
                    int titleWordID = m_titlesID.get(m_currentTitleID);
                    if (m_wordID > titleWordID) {
                        m_wordID = titleWordID;
                    }
                    else if (m_currentTitleID > 0) {
                        --m_currentTitleID;
                        m_wordID = m_titlesID.get(m_currentTitleID);
                    }
                }
                break;
            case NextStep:
                // We will follow a similar process to the `previous step` case but
                // trying to move forward in the read. Note that it is simpler in
                // this case as no matter the position of the word in the current
                // section we will always move to the next one (as long as a valid
                // title can be found in the read).
                if (isValidTitle()) {
                    // Try to find the next position to move to. We will start by
                    // moving to the current title's position (which should always
                    // fail as it is meant to represent the *last title reached*.
                    int titleWordID = m_titlesID.get(m_currentTitleID);

                    // Try to move to the next title: this might fail if we reached
                    // the last title.
                    if (m_currentTitleID < m_titlesID.size() - 1) {
                        ++m_currentTitleID;
                        titleWordID = m_titlesID.get(m_currentTitleID);
                    }

                    // In case the value is larger than the current word index we
                    // can move to this position. Otherwise we will move to the end
                    // of the read as there's no title beyond the last one we reached.
                    if (m_wordID < titleWordID) {
                        m_wordID = titleWordID;
                    }
                    else {
                        m_wordID = m_words.size() - 1;
                    }
                }
                break;
        }

        // The return status indicates whether we could move from the position
        // indicated by the virtual cursor a bit. In the case of a `HTML` parser
        // we never have to load some more data as everything has been loaded
        // right away.
        return new Pair<>(sWordID != m_wordID, false);
    }

    @Override
    void loadFromSource(InputStream stream, float progress) throws IOException {
        // Try to create the document from the input stream. This might fail in case
        // the parser is not able to correctly analyze the `HTML` page.
        Document doc;
        try {
            doc = Jsoup.parse(stream, null, m_base);
        }
        catch (Exception e) {
            // This indicates a failure of the parsing process: consider that we can
            // not make sense of the data source.
            throw new IOException("Could not parse HTML source: \"" + e.toString() + "\"");
        }

        // Retrieve the body of the document: if we can't this is an issue. Once this
        // is done we will retrieve the text from the body.
        Element body = doc.body();
        if (body == null) {
            throw new IOException("Cannot retrieve body from html page");
        }
        String text = body.text();

        // Once we have a valid text we can analyze it and group it in order to
        // build the `m_words` list of words.
        handlePageLoading(text);

        // We will now interpret the list of titles that can be found from the input document.
        // This will allow for more convenient navigation in the document when handling a next
        // or previous section motion request.
        generateTitleCheckpoints(body);

        // Now we need to interpret the input parsing progress: we know that the desired
        // progression is defined by the `progress` value in input. We will consider that
        // it represents some fraction of the total number of words. Note that some control
        // is performed to make sure that the computed progress is actually consistent with
        // the data contained in the `m_words` data.
        float cProgress = Math.min(1.0f, Math.max(0.0f, progress));
        m_wordID = Math.round((int)Math.floor(cProgress * m_words.size()));

        // We should also find the corresponding title index from the compute word position.
        if (hasTitles()) {
            // Traverse the list of titles until we reach one that is after the current word
            // index.
            int titleWordID = 0;
            int titleID = 0;

            while (titleWordID < m_wordID && titleID < m_titlesID.size()) {
                titleWordID = m_titlesID.get(titleID);
                ++titleID;
            }

            // Register the found title.
            m_currentTitleID = Math.min(m_titlesID.size(), titleID);
        }
    }
}
