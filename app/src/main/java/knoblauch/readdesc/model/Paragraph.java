package knoblauch.readdesc.model;

import java.util.ArrayList;

class Paragraph {

    /**
     * Holds the words defining this paragraph. Note that the words are stored
     * in the order they are supposed to be displayed. This list might be empty
     * if the paragraph does not contain any element.
     */
    private ArrayList<String> m_words;

    /**
     * Create a new paragraph with no words associated to it.
     */
    Paragraph() {
        m_words = new ArrayList<>();
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
}
