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
    private static final Pattern SPACE_PATTERN = Pattern.compile("^\\s+$");

    /**
     * The list of paragraphs parsed by this text extractor. Note that
     * it is only containing the words and no additional information
     * such as links, images, etc.
     */
    private ArrayList<Paragraph> m_paragraphs;

    PdfTextExtractor() {
        m_paragraphs = new ArrayList<>();
    }

    /**
     * Return the list of paragraphs extracted by this object so far.
     * @return - a list of the paragraphs already extracted by this
     *           object.
     */
    ArrayList<Paragraph> getParagraphs() {
        return m_paragraphs;
    }

    /**
     * Remove any existing paragraph from this object, so that it can
     * be used again as if it has just been constructed.
     */
    void clear() {
        m_paragraphs.clear();
    }

    @Override
    public void beginTextBlock() {
        // Each time we encounter a new text block we want to create a
        // new paragraph. This will help cut the document into smaller
        // pieces that can be easier to read.
        m_paragraphs.add(new Paragraph());
    }

    @Override
    public void endTextBlock() {
        // No op: nothing to be done we will create a new paragraph in
        // the next `beginTextBlock` operation if needed.
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

        // Make sure that we have at least one paragraph.
        if (m_paragraphs.isEmpty()) {
            beginTextBlock();
        }

        // TODO: Refine the model to extract better the paragraphs. We could
        // also group the punctuation.

        for (String word : words) {
            // Keep only valid words.
            Matcher matcher = SPACE_PATTERN.matcher(word);
            if (!matcher.matches()) {
                m_paragraphs.get(m_paragraphs.size() - 1).addWord(word);
            }
        }
    }
}
