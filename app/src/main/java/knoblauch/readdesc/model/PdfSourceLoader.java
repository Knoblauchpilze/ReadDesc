package knoblauch.readdesc.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class PdfSourceLoader extends ReadLoader {

    /**
     * Create a new `PDF` source loader from the specified arguments. Will call
     * the base class constructor and forward the arguments.
     * @param context - the context to use to resolve links and resources.
     * @param uri - the `uri` from which the data should be loaded.
     * @param listener - the object to notify whenever the data has successfully
     *                   been loaded or a failure has been detected.
     */
    PdfSourceLoader(Context context, String uri, ReadLoaderListener listener) {
        super(context, uri, listener);
    }

    @Override
    ArrayList<Paragraph> loadFromSource(Uri source, Context context) throws IOException {
        // Try to instantiate a valid `PDF` reader from this source.
        if (source == null || source.getPath() == null) {
            throw new IOException("Cannot set invalid PDF source in parser");
        }

        // We need to create a file as the source of the `PDF` reader. Note
        // that as we store the content's `uri` we need to resolve the link
        // in order to access to the file.
        ContentResolver res = context.getContentResolver();
        InputStream inStream = res.openInputStream(source);
        if (inStream == null) {
            throw new IOException("Cannot load PDF content \"" + source.toString() + "\" in parser");
        }

        PdfReader reader = new PdfReader(inStream);

        // Perform the extraction of the text contained in this `PDF` document
        // through a text extraction strategy.
        PdfTextExtractor extractor = new PdfTextExtractor();
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);

        ArrayList<Paragraph> out = new ArrayList<>();

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
                    if (p.canBeGrouped() && !out.isEmpty()) {
                        Paragraph last = out.get(out.size() - 1);
                        last.merge(p);

                        continue;
                    }

                    // Register this paragraph as it cannot be grouped.
                    out.add(p);
                }

                // Clear the extractor to be ready for the next page.
                extractor.clear();
            }
        }
        catch (Exception e) {
            // We encountered an error while parsing the `PDF` document, consider
            // the input source as invalid.
            throw new IOException("Cannot parse content of PDF source in parser");
        }

        return out;
    }
}
