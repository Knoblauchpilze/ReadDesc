package knoblauch.readdesc.model;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

class PdfSourceLoader extends ReadLoader {

    /**
     * Convenience class allowing to keep track of all the relevant information
     * needed to find the words associated to a specified page in the general
     * `m_words` map.
     */
    private class PageInfo {

        /**
         * The index of the first word of the page in the general `m_words` map.
         * This index should be valid relatively to the bounds of the map and is
         * not checked in any way after being initialized.
         */
        int m_startIndex;

        /**
         * Similar to the `m_startIndex` but holds the index of the last word of
         * the page in the `m_words` map. This word *is* part of the page (so the
         * next one is not).
         */
        int m_endIndex;

        /**
         * Used as a convenience method to retrieve the number of words defined
         * for this page. This is just a basic subtraction between the starting
         * index and end index but it is convenient to have this as a method
         * rather than computing it everywhere.
         * @return - the number of words defined for this page.
         */
        int getWordsCount() {
            return m_endIndex - m_startIndex;
        }

        /**
         * Used to compute the general index of a word described by its index by
         * assuming it is part of the page. We basically just offset it with the
         * `m_startIndex` and make no controls to check whether it is valid based
         * on the number of words contained in this page.
         * @param index - the local index to be converted into general index.
         * @return - the general index assuming the input `index` refers to a word
         *           in this page.
         */
        int computeWordIndex(int index) {
            return m_startIndex + index;
        }
    }

    /**
     * Defines how many pages surrounding the desired progress location should be
     * loaded upon each parsing operation. The larger this value the more buffer
     * is created to provide a lag-free reading experience to the user but also
     * the longer it will take to load this data in the first place. Note that it
     * will be applied on the direction that _makes sense_: for example if the
     * source already has the page `2-3` available and the user is requesting
     * page `4` we will load this many pages *after* page `4` (so pages `5`, `6`
     * and so on).
     * This value is used internally in the `loadFromSource` method.
     */
    private static final int LOAD_SURROUNDING_PAGES_COUNT = 4;

    /**
     * An information read from the data source itself when being parsed to
     * give an indication of the total number of pages available in the `PDF`
     * document. It helps determining the status of the parser (typically in
     * the `isAtEnd` method).
     */
    private int m_pagesCount;

    /**
     * Contains all the information about the pages loaded so far from the
     * source. This table is what allows to interpret the data contained in
     * the general `m_words` array: it describes the position of each page
     * in this global array and also allows to quickly determine whether the
     * data for a specific page is already available.
     */
    private HashMap<Integer, PageInfo> m_pages;

    /**
     * The list of words currently registered and loaded from the data source.
     * It contains all the words in an unorganized fashion. The goal is just
     * to have all the words readily available for further exploration and is
     * used as a quick and easy way to handle dynamic loading of additional
     * pages even in an unordered manner.
     * This global array should be used in coordination with the `m_pagesInfo`
     * element which keep track of the starting and end element of each page
     * loaded so far.
     */
    private ArrayList<String> m_words;

    /**
     * Defines the index of the page currently being viewed by the user. It
     * is guaranteed to correspond to a page existing in the `m_pagesInfo`
     * map unless it is negative. Trying to access to a page that has not
     * yet been loaded will trigger the loading process again.
     */
    private int m_pageID;

    /**
     * Defines the index of the word currently visualized inside the page.
     * This value is incremented each time the user calls the `perform`
     * method and might lead to a change in the `m_pageID` if we reach the
     * end of the page.
     * Note that this word index is relative to the number of words existing
     * in the page: it is not a global index that can be used directly to
     * access data from the `m_words` map.
     */
    private int m_wordID;

    /**
     * Create a new `PDF` source loader from the specified arguments. Will call
     * the base class constructor and forward the arguments. Note that we don't
     * specify the source as it will be provided as part of the execution state
     * for this loader.
     * @param context - the context to use to resolve links and resources.
     * @param progress - the desired progress to load in priority. This allows
     *                   to orient the parsing operations to quickly reach this
     *                   point as it's of interest for the user.
     */
    PdfSourceLoader(Context context, float progress) {
        // Call base handler.
        super(context, progress);

        // Initialize the data with no words so far (and no pages neither).
        m_pagesCount = 0;
        m_pages  = new HashMap<>();
        m_words = new ArrayList<>();

        // At first we don't have any valid position information.
        m_pageID = -1;
        m_wordID = -1;
    }

    /**
     * Used to retrieve the current page information. Note that this method
     * assumes that the locker is already acquired so it is not thread-safe.
     * It may return `null` in case no pages are defined yet for this source
     * or if the page count references an invalid page.
     * One should always check the the current page is valid through the
     * `isValidPage` method before using this method.
     * @return - the page info as the struct defined in the `m_pagesInfo` map.
     */
    private PageInfo getCurrentPageInfo() {
        return m_pages.get(m_pageID);
    }

    /**
     * Defines whether the current page index is valid considering the loaded page
     * information so far. Note that this method does not try to acquire the lock
     * on this object.
     * @return - `true` if the current page index is valid considering the rest of
     *           the information available in the loader and `false` otherwise.
     */
    private boolean isValidPage() {
        return !m_pages.isEmpty() && m_pages.containsKey(m_pageID);
    }

    /**
     * Defines whether the current word index is valid considering the associated
     * page information. We first check that the page itself is valid and then use
     * this data to check whether the word is valid.
     * Note that just like the `isValidPage` this method does not try to access
     * the locker on this object.
     * @return - `true` if one can safely access the current selected word.
     */
    private boolean isValidWord() {
        // The word can't be valid if the page is not.
        if (!isValidPage()) {
            return false;
        }

        // Retrieve the current page's information.
        PageInfo pi = getCurrentPageInfo();
        int gID = pi.computeWordIndex(m_wordID);

        // Check both that the `m_wordID` is valid and that the general index
        // computed from this index is also valid.
        return m_wordID >= 0 && m_wordID < pi.getWordsCount() && gID >= 0 && gID < m_words.size();
    }

    /**
     * Creates the relevant data for a new page with the specified index and words.
     * This method will try to acquire the locker on this object before proceeding
     * to the registration of the data. In case a page with a similar `id` already
     * exists nothing happens.
     * @param id - the index of the page to create.
     * @param words - the words associated to this page.
     */
    private void handlePageCreation(int id, ArrayList<String> words) {
        // Acquire the lock to protect from concurrent accesses.
        m_locker.lock();

        try {
            // Check whether this page already exists.
            if (m_pages.containsKey(id)) {
                // Don't add the page again.
                return;
            }

            // Create the key describing the page.
            PageInfo pi = new PageInfo();
            pi.m_startIndex = m_words.size();
            pi.m_endIndex = pi.m_startIndex + words.size();

            // Register both the key and the words.
            m_pages.put(id, pi);
            m_words.addAll(words);
        }
        finally {
            m_locker.unlock();
        }
    }

    @Override
    boolean isInvalid() {
        return m_pages.isEmpty() || m_words.isEmpty() || !isValidPage() || !isValidWord();
    }

    @Override
    boolean isAtStart() {
        m_locker.lock();
        boolean atStart = (isValidPage() && m_pageID == 0 && m_wordID == 0);
        m_locker.unlock();

        return atStart;
    }

    @Override
    boolean isAtEnd() {
        m_locker.lock();
        // Assume we're not at the end of the parser.
        boolean atEnd = false;

        // In case the current page is valid we can refine our judgment.
        if (isValidPage()) {
            PageInfo pi = getCurrentPageInfo();

            atEnd = (m_pageID == m_pagesCount - 1 && m_wordID == pi.getWordsCount() - 1);
        }

        m_locker.unlock();

        return atEnd;
    }

    @Override
    float getCompletion() {
        m_locker.lock();
        // Assume `0` completion.
        float progress = 0.0f;

        // Update in case this source is in a valid state.
        if (!isInvalid()) {
            PageInfo pi = getCurrentPageInfo();

            // The progress is the concatenation of the percentage of the page
            // in the global read and the percentage of the word inside this page.
            // We account for empty pages in the division (so as not to divide by
            // `0`).
            float pProgress = 1.0f * m_wordID / Math.max(1, pi.getWordsCount());
            float pagePercentage = 1.0f / m_pagesCount;
            progress = 1.0f * m_pageID / m_pagesCount + pProgress * pagePercentage;
        }

        m_locker.unlock();

        return progress;
    }

    @Override
    String getCurrentWord() {
        m_locker.lock();

        // In case the parser is not a valid state, do nothing. This include cases
        // where the page is not valid or the word within the page is not valid.
        // Long story short after this test we can access the page and the word's
        // information safely.
        if (isInvalid()) {
            m_locker.unlock();
            return "";
        }

        // Retrieve the word at the index specified both by the active page and
        // the active word within this page.
        PageInfo pi = getCurrentPageInfo();
        int gID = pi.computeWordIndex(m_wordID);
        String word = m_words.get(gID);

        m_locker.unlock();

        return word;
    }

    @Override
    Pair<Boolean, Boolean> handleMotion(Action action, int param) {
        // We want to first determine whether the parser is in a valid state or
        // not: if this not the case there's no need to try to handle the motion.
        if (isInvalid()) {
            return new Pair<>(false, false);
        }

        // Save the indices to see whether we could apply at least part of the
        // motion.
        int sPageID = m_pageID;
        int sWordID = m_wordID;

        // Perform the action.
        switch (action) {
            case Rewind:
                m_pageID = 0;
                m_wordID = 0;
                break;
            case NextWord:
                ++m_wordID;
                break;
            case PreviousStep:
                m_wordID -= param;
                break;
            case NextStep:
                m_wordID += param;
                break;
        }

        // Consolidate the word's index in regard to the bounds available in the
        // current page. We might have to trigger a new loading operation in case
        // we travel to a page that is not yet loaded.
        boolean needsLoading = false;
        PageInfo pi = getCurrentPageInfo();

        if (pi == null) {
            needsLoading = true;
        }
        else if (m_wordID < 0) {
            // We need to move to the previous page until we obtain a word index
            // that is positive. Note that in case we're already at page `0` it
            // is not needed and we will say that moving back to the beginning
            // is enough.
            if (m_pageID == 0) {
                m_wordID = 0;
                // Note that we are certain that there's no loading operation
                // needed as otherwise we couldn't have pass the `pi == null`
                // text.
            }
            else {
                while (m_wordID < 0 && !needsLoading) {
                    --m_pageID;

                    // Check whether this page is already loaded.
                    if (!isValidPage()) {
                        // TODO: This leaves the parser in a potentially inconsistent state.
                        needsLoading = true;
                    }
                    else {
                        // Otherwise, offset the current word index with the number
                        // of words contained in the new current page.
                        pi = getCurrentPageInfo();
                        m_wordID += pi.getWordsCount();
                    }
                }
            }
        }
        else if (m_wordID >= pi.getWordsCount()) {
            // We need to move to the next page until we obtain a word index that
            // is within the bounds of the page.
            // Note that in case we're already at the last page it is not needed
            // and we will say that moving back to the beginning is enough.
            if (m_pageID == m_pagesCount - 1) {
                m_wordID = pi.getWordsCount() - 1;
                // Note that we are certain that there's no loading operation
                // needed as otherwise we couldn't have pass the `pi == null`
                // text.
            }
            else {
                while (m_wordID >= pi.getWordsCount() && !needsLoading) {
                    ++m_pageID;

                    m_wordID -= pi.getWordsCount();

                    // Check whether this page is already loaded.
                    if (!isValidPage()) {
                        // TODO: This leaves the parser in a potentially inconsistent state.
                        needsLoading = true;
                    } else {
                        // Otherwise, offset the current word index with the number
                        // of words contained in the new current page.
                        pi = getCurrentPageInfo();
                    }
                }
            }
        }

        // The return status indicates whether we could move from the position
        // indicated by the virtual cursor a bit and also whether a loading
        // operation is required.
        return new Pair<>(sPageID != m_pageID || sWordID != m_wordID, needsLoading);
    }

    @Override
    void loadFromSource(InputStream stream, float progress) throws IOException {
        // Create the reader from the stream related to this element.
        PdfReader reader = new PdfReader(stream);

        // Perform the extraction of the text contained in this `PDF` document
        // through a text extraction strategy.
        PdfTextExtractor extractor = new PdfTextExtractor();
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);

        // Update the number of pages as a first step.
        m_pagesCount = reader.getNumberOfPages();

        // If there are no valid pages, stop there.
        if (m_pagesCount == 0) {
            throw new IOException("Could not parse content of PDF source not containing any page");
        }

        try {
            // We want to load the area around the `progress` value provided as input.
            // Depending on the number of pages available in the source this will be
            // translated as loaded only specific pages from the read.
            // At first we will try to distribute the surrounding pages to load in a
            // manner that is suited for the canonical experience of reading. Suppose
            // the progress indicates that the user is reading page `10` we will lay
            // `25%` of the surrounding area on the pages before and `75%` ahead as
            // the user is most likely to read forward rather than go back.
            // To give a numerical example if the `LOAD_SURROUNDING_PAGES_COUNT = 4`
            // we will load the page preceding the current page and the `3` after it.
            int pageToLoad = Math.round((int)Math.floor((double)(progress * reader.getNumberOfPages())));
            int minPage = Math.round((int)Math.floor((double)(pageToLoad - 0.25f * LOAD_SURROUNDING_PAGES_COUNT)));
            int maxPage = Math.round((int)(Math.floor((double)pageToLoad + 0.75f * LOAD_SURROUNDING_PAGES_COUNT)));

            // Clamp the pages based on the actual structure of the `PDF` document.
            minPage = Math.min(reader.getNumberOfPages() - 1, Math.max(0, minPage));
            maxPage = Math.min(reader.getNumberOfPages() - 1, Math.max(0, maxPage));

            int count = Math.max(1, maxPage - minPage);

            for (int id = minPage ; id <= maxPage && !isCancelled(); ++id) {
                // Determine whether we need to load this page at all: indeed we might
                // already have loaded it before as the loading process can overlap due
                // to the surrounding area settings.
                m_locker.lock();
                if (m_pages.containsKey(id)) {
                    // The page already exists, don't load it again.
                    Log.i("main", "Prevented loading of page " + id + " already existing");
                    m_locker.unlock();
                    continue;
                }
                m_locker.unlock();

                // Parse the paragraphs for this page. Note that as `iText` counts page
                // starting at `1` we need to account for this this.
                extractor = parser.processContent(id + 1, extractor);

                // Retrieve the words parsed for this page.
                ArrayList<String> words = extractor.getWords();

                // Handle the creation of the page in the internal data.
                handlePageCreation(id, words);

                // Clear the extractor to be ready for the next page.
                extractor.clear();

                // Notify progression.
                publishProgress(1.0f * id / count);
            }

            // Once we're done loading the relevant pages we can set the desired progress
            // as the current one.
            m_pageID = pageToLoad;

            // To compute the word in the page we need to first interpret the part of the
            // progress that indicates the page and the rest will indicate which word in
            // the page should be set as current.
            PageInfo pi = getCurrentPageInfo();
            float pagePercentage = 1.0f / m_pagesCount;
            float wProgress = (progress - 1.0f * m_pageID / m_pagesCount) / pagePercentage;
            m_wordID = Math.round(wProgress * pi.getWordsCount());

            Log.i("main", "Settings start: " + m_pageID + "/" + m_pagesCount + " at word " + m_wordID + "/" + pi.getWordsCount() + " (page: " + pagePercentage + ", word: " + wProgress + ", progress: " + progress + ")");
        }
        catch (Exception e) {
            // We encountered an error while parsing the `PDF` document, consider
            // the input source as invalid.
            throw new IOException("Cannot parse content of PDF source in parser");
        }
    }
}
