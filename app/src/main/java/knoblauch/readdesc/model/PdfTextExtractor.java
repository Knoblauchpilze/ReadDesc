package knoblauch.readdesc.model;

import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfTextExtractor implements RenderListener {

    /**
     * A common pattern describing a string only composed of space characters.
     * Used to only accept valid words as part of this paragraph.
     */
    private static final Pattern SPACE_PATTERN = Pattern.compile("^\\s*$");

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
     * The list of words parsed by this text extractor. Note that it is only
     * containing the words and no additional information such as links, or
     * images, etc.
     * Words are filtered upon being added to the extractor so querying the
     * registered words through the `getWords` method is guaranteed to only
     * return valid words.
     */
    private ArrayList<String> m_words;

    /**
     * Creates a new extractor with no words registered.
     */
    PdfTextExtractor() {
        m_words = new ArrayList<>();
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
     * Remove any existing word from this object, so that it can be used
     * again as if it had just been constructed.
     */
    void clear() {
        m_words.clear();
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
        // We only care about the text that needs to be rendered: we will
        // create as many words as needed and register each one of them in
        // the current paragraph we're building.
        String[] words = renderInfo.getText().split("\\s+");

        for (String word : words) {
            // Discard empty words and words composed only of spaces.
            Matcher matcher = SPACE_PATTERN.matcher(word);
            if (matcher.matches()) {
                continue;
            }

            // Also check whether this word is a single punctuation or
            // currency character: in this case we will try to perform
            // some cleaning by associating it with the last word we
            // parsed. Indeed it's most likely that it is linked to the
            // existing context and it will be easier to read.
            // Of course this only applies in case we have at least one
            // word already defined.
            if (!m_words.isEmpty() && word.length() == 1 && (PUNCTUATION.contains(word) || CURRENCIES.contains(word))) {
                String c = m_words.get(m_words.size() - 1).concat(word);
                m_words.set(m_words.size() - 1, c);
                continue;
            }

            // Register this word normally.
            m_words.add(word);
        }
    }
}
