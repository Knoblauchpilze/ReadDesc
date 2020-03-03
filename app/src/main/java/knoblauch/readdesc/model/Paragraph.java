package knoblauch.readdesc.model;

import android.util.Log;

import java.util.ArrayList;

class Paragraph {

    /**
     * Define the list of punctuations symbols that will be collapsed during
     * a `sanitize` operation of a paragraph.
     */
    private static final String PUNCTUATION = ",?;.:!()°\"'";

    /**
     * Define the list of currencies that will be collapsed during a sanitize
     * operation to be linked to their associated numerical value.
     */
    private static final String CURRENCIES = "€$£";

    /**
     * Holds the words defining this paragraph. Note that the words are stored
     * in the order they are supposed to be displayed. This list might be empty
     * if the paragraph does not contain any element.
     */
    private ArrayList<String> m_words;

    /**
     * Holds `true` whenever this paragraph is composed only of items that can
     * be grouped together with words (such as punctuation symbols, etc.). This
     * will help collapse entire paragraph together and thus provide a better
     * reading experience.
     */
    private boolean m_needsGrouping;

    /**
     * Create a new paragraph with no words associated to it.
     */
    Paragraph() {
        m_words = new ArrayList<>();
        m_needsGrouping = true;
    }

    /**
     * Used to determine whether this paragraph is empty or not.
     * @return - `true` if the paragraph does not contain any word and `false`
     *           otherwise.
     */
    boolean isEmpty() {
        return m_words.isEmpty();
    }

    /**
     * Used to determine whether it makes sense to collapse this
     * paragraph into a single one with its predecessor. This is
     * usually the case when a paragraph contains a single symbol
     * of punctuation for example.
     * @return - `true` if it makes sense to group this paragraph
     *           with another one.
     */
    boolean canBeGrouped() { return m_needsGrouping; }

    /**
     * Return the number of words registered in this paragraph.
     * @return - the number of words in this paragraph.
     */
    int size() {
        return m_words.size();
    }

    /**
     * Used to insert a new word to this paragraph at the end of the existing
     * list of words.
     * Note that we do not consider that adding an empty string to be valid
     * as word and we will thus prevent their registration in the paragraph.
     * @param word - the new word to append to this paragraph.
     */
    void addWord(String word) {
        if (word != null && !word.isEmpty()) {
            m_needsGrouping = (m_needsGrouping && word.length() == 1 && (PUNCTUATION.contains(word) || CURRENCIES.contains(word)));
            m_words.add(word);
        }
    }

    /**
     * Return the word at the specified index. In case the provided index is
     * either negative or larger than the number of words in this paragraph
     * we return an empty string.
     * @param index - the index of the word to retrieve.
     * @return - the word at the specified index.
     */
    String getWord(int index) {
        if (isEmpty() || index < 0 || index >= size()) {
            return "";
        }

        return m_words.get(index);
    }

    /**
     * Used as a way to compact a bit the paragraph by grouping punctuation
     * symbols with the previous word so as to make the reading more easy
     * and convenient.
     */
    void sanitize() {
        // If the paragraph is empty there's nothing to sanitize.
        if (isEmpty()) {
            return;
        }

        // We will try to build a new collection of words here.
        ArrayList<String> words = new ArrayList<>();

        // Traverse the list of words and collapse the punctuations.
        int id = 0;
        while (id < m_words.size()) {
            String w = m_words.get(id);

            // Discard empty words.
            if (w.isEmpty()) {
                ++id;
                continue;
            }

            // Check whether this word is a punctuation symbol or a
            // currency.
            if (w.length() == 1 && (PUNCTUATION.contains(w) || CURRENCIES.contains(w))) {
                // Try to register this word at the end of the
                // last existing one. If no such word exist we
                // will register it anyway.
                appendToPrevious(words, w);
                ++id;
                continue;
            }

            // This word cannot be compacted in any way we can add
            // it to the final list.
            words.add(w);
            ++id;
        }

        // Assign the list of words as the internal attribute.
        m_words = words;
    }

    /**
     * Attempt to merge the `other` paragraph with this one. This usually
     * indicates that the `other` paragraph only contains punctuation item
     * or currencies that can be concatenated with the last word of this
     * one.
     * @param other - the other paragraph to merge.
     */
    void merge(Paragraph other) {
        // Check that the other paragraph can effectively be merged.
        if (!other.canBeGrouped()) {
            return;
        }

        // In case this paragraph is empty, copy the list of words.
        if (isEmpty()) {
            m_words = other.m_words;
            m_needsGrouping = other.m_needsGrouping;

            return;
        }

        // Traverse the list of words of the `other` paragraph and
        // merge them with the latest element of this paragraph.
        String last = m_words.get(m_words.size() - 1);

        for (String w : other.m_words) {
            last = last.concat(w);
        }

        m_words.set(m_words.size() - 1, last);
    }

    /**
     * Used to perform the concatenation of the input `word` with the last
     * word defined in the array list if any. If no such word exist we add
     * the word to the list.
     * Note that we assume that the `word `is not empty.
     * @param words - the list of words to which the `word` should be added.
     * @param word - the word to append to the last element of `words`.
     */
    private void appendToPrevious(ArrayList<String> words, String word) {
        // Check whether a last word exists.
        if (words.isEmpty()) {
            words.add(word);
            return;
        }

        // Concatenate both words.
        String prev = words.get(words.size() - 1).concat(word);

        // Reset the existing last word.
        words.set(words.size() - 1, prev);
    }
}
