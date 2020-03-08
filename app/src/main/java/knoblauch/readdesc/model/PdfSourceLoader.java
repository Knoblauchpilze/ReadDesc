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
     * Used to copy the input loader and create a new object from it. We need to
     * copy fields which make sense and create new one as needed.
     * @param other - the other elements to copy.
     */
    PdfSourceLoader(PdfSourceLoader other) {
        // Call base handler.
        super(other);

        // Copy internal fields.
        m_pagesCount = other.m_pagesCount;

        m_pages = other.m_pages;
        m_words = other.m_words;

        m_pageID = other.m_pageID;
        m_wordID = other.m_wordID;

    }

    /**
     * Used to determine whether this source already has some pages loaded or not.
     * Note that this method does acquire the lock on this object.
     * @return - `true` if some pages are already available and `false` otherwise.
     */
    private boolean hasPages() {
        m_locker.lock();
        boolean somePagesExist = !m_pages.isEmpty();
        m_locker.unlock();

        return somePagesExist;
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
                    // Account for the fact that we move to the next page.
                    m_wordID -= pi.getWordsCount();

                    // Actually move to the next page.
                    ++m_pageID;

                    // Check whether this page is already loaded.
                    if (!isValidPage()) {
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
        // This method can be called in two contexts: either when it's the first time
        // that we instantiate information for the parser or when we need to load some
        // additional data because we reached an area not yet loaded.
        // In the first case we will rely on the `progress` to determine the pages to
        // load and also in the end to update the `m_pageID` and `m_wordID` values
        // based on this progress.
        // On the other hand if we already have some data loaded and we need to load
        // more, we should have a somewhat valid `m_pageID` and `m_wordID` which we
        // will interpret to load the data until we are able to bring back these to
        // some consistent values.

        // In any case we need to create some parsing utilities.
        PdfReader reader = new PdfReader(stream);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);

        // Update the number of pages as a first step. Note that we don't verify that
        // the number of pages stays consistent between two load. This could be the
        // case if the file has been modified outside of the application but we will
        // not handle this case for now.
        m_pagesCount = reader.getNumberOfPages();

        // If there are no valid pages, stop there.
        if (m_pagesCount == 0) {
            throw new IOException("Could not parse content of PDF source not containing any page");
        }

        // Now check whether we are in the case where no data has ever been loaded
        // in the parser or if we are in the process of loading some more data as
        // we reached a non-loaded area.
        // We will protect the call to each method so as to be able to provide some
        // notification of any failure.
        try {
            if (hasPages()) {
                // At this point we should have an invalid word index compared to the
                // active page. This should be the case as calling this method should
                // be a direct consequence of performing a motion that could not be
                // achieved. We will make sure that it is the case first.
                // Once this is done we need to keep loading pages until we can bring
                // back the word index to consistent values. Note that the `m_pageID`
                // should already correspond to a non-loaded page so we can start from
                // there.
                if (isValidPage() && isValidWord()) {
                    // Nothing more to do.
                    return;
                }

                // Keep track of the initial progress to be made.
                int count = Math.abs(m_wordID);

                boolean valid = false;
                while (!valid) {
                    // Load the page corresponding to the `m_pageID` and continue until
                    // we can bring back a `m_wordID` consistent with the page's data.
                    loadPage(m_pageID, parser);

                    // Try to retrieve the information about this page to update the
                    // `m_wordID`.
                    PageInfo pi = getCurrentPageInfo();
                    if (pi == null) {
                        throw new IOException("Could not parse content of PDF source for page " + m_pageID + "/" + m_pagesCount);
                    }

                    // We have two main cases: we moved backwards to a page that is not
                    // already loaded or we moved forward to a page.
                    if (m_wordID < 0) {
                        // In the case of a backwards motion, we have for sure a negative
                        // value in the `m_wordID` so we obviously need to add the words
                        // count of the newly loaded page before checking the validity of
                        // this index.
                        m_wordID += pi.getWordsCount();

                        publishProgress(1.0f * (count + Math.min(m_wordID, 0)) / count);
                    }
                    else {
                        // In the case of a forward motion, we have for sure a positive
                        // value in the `m_wordID` which can either be larger than the
                        // current page words count (if we moved more than one page ahead)
                        // or valid in case we only moved to the next page. So we only
                        // need to update the `m_wordID` attribute in the first case.
                        if (m_wordID >= pi.getWordsCount()) {
                            m_wordID -= pi.getWordsCount();
                        }

                        publishProgress(1.0f * Math.max(count, count - m_wordID) / count);
                    }

                    // Check whether we reached a valid state.
                    valid = isValidWord();

                    // If this is not the case, load the next page.
                    if (!valid) {
                        if (m_wordID < 0) {
                            --m_pageID;
                        }
                        else {
                            ++m_pageID;
                        }
                    }
                }

                PageInfo pi = getCurrentPageInfo();
                Log.i("main", "Settings start: " + m_pageID + "/" + m_pagesCount + " at word " + m_wordID + "/" + pi.getWordsCount());
            }
            else {
                // We want to load the area around the `progress` value provided as
                // input. Depending on the number of pages defined in the source this
                // will be translated as loading only specific pages from the read.
                // At first we will try to distribute the surrounding pages to load
                // in a manner that is suited for the canonical experience of a read.
                // Suppose the progress indicates that the user is reading page `10`
                // we will lay `25%` of the surrounding area on the pages before and
                // `75%` ahead: indeed the user is most likely to read forward rather
                // than go back.
                int pageToLoad = Math.round((int)Math.floor((double)(progress * m_pagesCount)));

                int minPage = Math.round((int)Math.floor((double)(pageToLoad - 0.25f * LOAD_SURROUNDING_PAGES_COUNT)));
                int maxPage = Math.round((int)(Math.floor((double)pageToLoad + 0.75f * LOAD_SURROUNDING_PAGES_COUNT)));

                // Clamp the pages based on the actual structure of the `PDF` document.
                minPage = Math.min(reader.getNumberOfPages() - 1, Math.max(0, minPage));
                maxPage = Math.min(reader.getNumberOfPages() - 1, Math.max(0, maxPage));

                int count = Math.max(1, maxPage - minPage);

                for (int id = minPage ; id <= maxPage && !isCancelled(); ++id) {
                    loadPage(id, parser);

                    // Notify progression.
                    publishProgress(1.0f * (id - minPage) / count);
                }

                // Once all the pages have been loaded we need to update the `m_pageID`
                // and `m_wordID` to reflect the desired progress. To compute the word
                // index we first need to interpret the part of the progress indicating
                // the page and the part indicating the progress inside the page.
                m_pageID = pageToLoad;

                PageInfo pi = getCurrentPageInfo();
                float pagePercentage = 1.0f / m_pagesCount;
                float wProgress = (progress - 1.0f * m_pageID / m_pagesCount) / pagePercentage;
                m_wordID = Math.round(wProgress * pi.getWordsCount());

                Log.i("main", "Settings start: " + m_pageID + "/" + m_pagesCount + " at word " + m_wordID + "/" + pi.getWordsCount() + " (page: " + pagePercentage + ", word: " + wProgress + ", progress: " + progress + ")");
            }
        }
        catch (Exception e) {
            // We encountered an error while parsing the `PDF` document, consider
            // the input source as invalid.
            throw new IOException("Cannot parse content of PDF source in parser");
        }
    }

    /**
     * Used to perform the loading of the page defined by the input index
     * from the provided parser. Note that nothing happens in case no such
     * page can be found in the input parser nothing happens.
     * A filtering is also applied to determine whether the requested page
     * already exists in the internal table of pages (to prevent loading
     * several times the same page).
     * @param id - the index of the page to load. Starts at `0` and is
     *             converted internally to match the expected `iText`
     *             semantic (which starts at `1`).
     * @param pdf - a reader on the `PDF` document from which the page is
     *              to be extracted.
     */
    private void loadPage(int id, PdfReaderContentParser pdf) throws IOException {
        // Determine whether we need to load this page at all: indeed we might
        // already have loaded it before as the loading process can overlap due
        // to the surrounding area settings.

        m_locker.lock();
        if (m_pages.containsKey(id)) {
            // The page already exists, don't load it again.
            m_locker.unlock();

            return;
        }
        m_locker.unlock();

        // Parse the paragraphs for this page. Note that as `iText` counts page
        // starting at `1` we need to account for this this.
        PdfTextExtractor extractor = new PdfTextExtractor();
        extractor = pdf.processContent(id + 1, extractor);

        // Retrieve the words parsed for this page.
        ArrayList<String> words = extractor.getWords();

        // Handle the creation of the page in the internal data.
        handlePageCreation(id, words);
    }
}
