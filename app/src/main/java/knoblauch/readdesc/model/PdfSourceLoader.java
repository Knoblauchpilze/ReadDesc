package knoblauch.readdesc.model;

import android.content.Context;
import android.util.Log;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class PdfSourceLoader extends ReadLoader {

    /**
     * Create a new `PDF` source loader from the specified arguments. Will call
     * the base class constructor and forward the arguments. Note that we don't
     * specify the source as it will be provided as part of the execution state
     * for this loader.
     * @param context - the context to use to resolve links and resources.
     * @param listener - the object to notify whenever the data has successfully
     *                   been loaded or a failure has been detected.
     */
    PdfSourceLoader(Context context, ReadLoaderListener listener) {
        // Use the base handler.
        super(context, listener);
    }

    @Override
    ArrayList<Paragraph> loadFromSource(InputStream stream) throws IOException {
        // Create the reader from the stream related to this element.
        PdfReader reader = new PdfReader(stream);

        // Perform the extraction of the text contained in this `PDF` document
        // through a text extraction strategy.
        PdfTextExtractor extractor = new PdfTextExtractor();
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);

        ArrayList<Paragraph> out = new ArrayList<>();

        try {
            for (int i = 1 ; i <= reader.getNumberOfPages() ; ++i) {
                // Parse the paragraphs for this page.
                Log.i("main", "Processing page " + i + "/" + reader.getNumberOfPages() + " of PDF doc");
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

                // Notify progression.
                publishProgress(1.0f * i / reader.getNumberOfPages());
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
