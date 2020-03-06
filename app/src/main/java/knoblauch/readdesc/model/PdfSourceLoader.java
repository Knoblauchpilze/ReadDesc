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
     * @param progress - the desired progress to load in priority. This allows
     *                   to orient the parsing operations to quickly reach this
     *                   point as it's of interest for the user.
     */
    PdfSourceLoader(Context context, float progress) { super(context, progress); }

    @Override
    ArrayList<String> loadFromSource(InputStream stream, float progress) throws IOException {
        // Create the reader from the stream related to this element.
        PdfReader reader = new PdfReader(stream);

        // Perform the extraction of the text contained in this `PDF` document
        // through a text extraction strategy.
        PdfTextExtractor extractor = new PdfTextExtractor();
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);

        ArrayList<String> out = new ArrayList<>();

        try {
            for (int i = 1 ; i <= reader.getNumberOfPages() && !isCancelled(); ++i) {
                // Parse the paragraphs for this page.
                Log.i("main", "Processing page " + i + "/" + reader.getNumberOfPages() + " of PDF doc");
                extractor = parser.processContent(i, extractor);

                // Register each one of them in the internal array and update
                // the word count along the way.
                ArrayList<String> words = extractor.getWords();
                out.addAll(words);

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
