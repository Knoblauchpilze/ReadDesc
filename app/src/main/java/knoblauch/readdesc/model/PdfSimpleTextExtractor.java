package knoblauch.readdesc.model;

import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.LineSegment;
import com.itextpdf.text.pdf.parser.RenderListener;

import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfSimpleTextExtractor implements RenderListener {

    /**
     * Convenience class allowing to keep track of the last position of the
     * text parsed by this extractor. It is used to add some implied spaces
     * and information from the `PDF` data.
     */
    private class LastCoordinates {

        /**
         * The position of the last starting position for the text.
         */
        Vector start;

        /**
         * The position of the last ending position for the text.
         */
        Vector end;
    }

    /**
     * A common pattern describing a string only composed of space characters.
     * Used to only accept valid words as part of this paragraph.
     */
    private static final Pattern SPACE_ONLY_PATTERN = Pattern.compile("^\\s*$");

    /**
     * A common regular expression allowing to split a string on successive space
     * characters.
     */
    private static final String SPACE_PATTERN = "\\s+";

    /**
     * A common pattern describing a string starting with some space characters.
     */
    private static final Pattern SPACE_BEFORE_PATTERN = Pattern.compile("^\\s+");

    /**
     * Similar to `SPACE_BEFORE_PATTERN` but for strings ending with some space
     * characters.
     */
    private static final Pattern SPACE_AFTER_PATTERN = Pattern.compile("\\s+$");

    /**
     * Convenience value used when determining whether some spacing between two
     * words is enough to consider that they form two distinct words. This value
     * is used as a mitigation factor in order to detect spacing when a block of
     * text is not easily separable in words in a `PDF` document.
     */
    private static final float SPACING_TO_FONT_FACTOR = 5.2f;

    /**
     * Define the list of punctuations symbols that will be collapsed during
     * the sanitize operation taking place when extracting the words from the
     * `PDF` document.
     */
    private static final String PUNCTUATION = ",?;.:!()°\"'";

    /**
     * Define the list of currencies that will be collapsed during a sanitize
     * operation to be linked to their associated numerical value.
     */
    private static final String CURRENCIES = "€$£";

    /**
     * Define the list of separators that are used as a way to mark the new
     * line and other separations in the `PDF` document but that we don't want
     * to actually interpret.
     * A word composed of only this character will be discarded.
     */
    private static final String SEPARATOR = "*";

    /**
     * The list of words parsed by this text extractor. Note that it is only
     * containing the words and no additional information such as links, or
     * images, etc.
     * Words are filtered upon being added to the extractor so querying the
     * registered words through the `getWords` method is guaranteed to only
     * return valid words.
     */
    private ArrayList<String> m_words;

    /**
     * Information allowing to keep track of the last parsed information in
     * the `PDF` document. This value is initialized with a `null` value and
     * is then populated while processing the `PDF` data.
     */
    private LastCoordinates m_last;

    /**
     * The current word we're building from the data of the `PDF` document.
     * The structure might slice a single word into several elements which
     * need to be gathered together afterwards. This buffer allows to sort
     * of accumulate this data before flushing it to a real word in the
     * `m_words` array.
     */
    private String m_currentWord;

    /**
     * Creates a new extractor with no words registered.
     */
    PdfSimpleTextExtractor() {
        m_words = new ArrayList<>();

        m_last = null;
        m_currentWord = "";
    }

    /**
     * Return the list of words extracted by this object so far. Note that
     * this list is filtered and does not contain any empty words. We also
     * pass a currencies and punctuation filter which prevent words that
     * are composed only of currencies or punctuations to be added.
     * @return - a list of the words extracted by this object so far.
     */
    ArrayList<String> getWords() {
        return m_words;
    }

    /**
     * Use to register the `m_currentWord` being built as an actual word
     * in the `m_words` area. Note that we perform some checks in here to
     * clean the output by removing empty words, concatenating words only
     * composed of currencies or punctuations to the previous ones, etc.
     */
    private void closeWord() {
        // In order to be ready to return from this method at any moment
        // we will first copy the `m_currentWord` to a local attribute
        // and work on this for the rest of the method: this allows to
        // clean the `m_currentWord` right away.
        String word = m_currentWord;
        m_currentWord = "";

        // In case the word does not exist or is empty, we don't do anything.
        if (word == null || word.isEmpty()) {
            return;
        }

        // Trash words composed only of spaces.
        Matcher matcher = SPACE_ONLY_PATTERN.matcher(word);
        if (matcher.matches()) {
            return;
        }

        // Trash words composed only of separators.
        if (word.length() == 1 && SEPARATOR.contains(word)) {
            return;
        }

        // Check whether this word is composed only of a single punctuation
        // or currency character: if this is the case we will concatenate
        // it to the previous word (if any).
        if (!m_words.isEmpty() && word.length() == 1 && (PUNCTUATION.contains(word) || CURRENCIES.contains(word))) {
            String c = m_words.get(m_words.size() - 1).concat(word);
            m_words.set(m_words.size() - 1, c);

            return;
        }

        // The word is valid, register it.
        m_words.add(word);
    }

    /**
     * Used to perform the analysis of the input string and to append the
     * needed bits to the `m_currentWord` and closing it if needed until
     * there's no more characters in the input string. We will also perform
     * some cleaning and sanitizing on the input string so as not to fill
     * the `m_words` array with junk.
     * @param text - the text to analyze and append to the `m_currentWord`
     *               if needed.
     */
    private void appendToCurrent(String text) {
        // Check whether the text is valid: if this is not the case we will
        // return immediately.
        if (text == null || text.isEmpty()) {
            return;
        }

        // In case the input word starts with some space characters we will
        // terminate the current word and start a new one. We will also keep
        // track of whether this word ends with some space characters to be
        // able to correctly terminate the last word.
        boolean spaceBefore = SPACE_BEFORE_PATTERN.matcher(text).matches();
        boolean spaceAfter = SPACE_AFTER_PATTERN.matcher(text).matches();

        // Trim the spaces from the input text now that we retrieved and
        // saved the relevant information.
        text = text.trim();

        // Close the current word if the string starts with some spaces.
        if (spaceBefore) {
            closeWord();
        }

        // Split the string on `spaces` so as to register each word.
        String[] words = text.split(SPACE_PATTERN);

        // In case no words are found, do nothing.
        if (words.length > 0) {
            // Traverse the input list and handle the words.
            for (int id = 0 ; id < words.length ; ++id) {
                // Append the current word to the existing word being built:
                // this will guarantee that the first word gets appended to
                // any existing data and that the subsequent ones can be
                // processed with the checks being performed in `closeWord`.
                m_currentWord = m_currentWord.concat(words[id]);

                // Close this word as we're splitting on spaces. This is
                // except for the last one as we might not have a space
                // to separate from other words.
                if (id < words.length - 1) {
                    closeWord();
                }
            }
        }

        // Close the running word if the initial string finished with some
        // space characters.
        if (spaceAfter) {
            closeWord();
        }
    }

    @Override
    public void beginTextBlock() {
        // No op: nothing to be done here.
    }

    @Override
    public void endTextBlock() {
        // No op: nothing to be done here.
    }

    @Override
    public void renderImage(ImageRenderInfo renderInfo) {
        // No op: we don't handle images as we cannot easily transform
        // them into words.
    }

    @Override
    public void renderText(TextRenderInfo renderInfo) {
        // Retrieve parsing information from the input argument.
        LineSegment segment = renderInfo.getBaseline();
        Vector start = segment.getStartPoint();
        Vector end = segment.getEndPoint();

        // In case we're not at our first text processing we can try
        // to determine whether we underwent a `hard return` (i.e. a
        // new line) since the last parsing. This will indicate that
        // the current word should be terminated and a new one started.
        boolean firstRender = (m_last == null);
        boolean hardReturn = false;

//        Log.i("main", "Handling text \"" + renderInfo.getText() + "\"");

        if (!firstRender) {
            Vector x1 = m_last.start;
            Vector x2 = m_last.end;

            // See http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
            float dist = (x2.subtract(x1)).cross((x1.subtract(start))).lengthSquared() / x2.subtract(x1).lengthSquared();

            // We should probably base this on the current font metrics,
            // but `1 pt` seems to be sufficient for the time being
            float sameLineThreshold = 1.0f;
            if (dist > sameLineThreshold) {
                hardReturn = true;
            }

            // Note: Technically, we should check both the start and end positions,
            // in case the angle of the text changed without any displacement but this
            // sort of thing probably doesn't happen much in reality, so we'll leave
            // it alone for now
        }

        if (hardReturn) {
            // In case we detected a hard we need to close the current word we're
            // building.
            closeWord();
        }
        else if (!firstRender) {
            // Check whether we need to insert a space before this chunk of text based
            // on the value of the last word.
            float spacing = m_last.end.subtract(start).length();
            if (spacing > renderInfo.getSingleSpaceWidth() / SPACING_TO_FONT_FACTOR) {
                closeWord();
            }
        }

        // We now need to register the text to the current word.
        appendToCurrent(renderInfo.getText());

        if (firstRender) {
            m_last = new LastCoordinates();
        }
        m_last.start = start;
        m_last.end = end;
    }
}
