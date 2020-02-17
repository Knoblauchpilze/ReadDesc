package knoblauch.readdesc.model;

import android.net.Uri;

public class ReadParser {

    /**
     * Describe the source of the data that can be accessed to this parser. It
     * is retrieved from the `ReadDesc` from which this parser is instantiated
     * and is used as the primary data source of the content.
     */
    private Uri m_source;

    /**
     * Instantiate a suitable parser for the input `desc`. Depending on the type
     * of the read a specific parser is instantiated so that we can successfully
     * fetch and display data from the source of the read.
     * @param desc - the read description from which a parser should be built.
     * @return - a valid parser for this read or `null` if the type of the read
     *           does not correspond to a known parser.
     */
    public static ReadParser fromRead(ReadIntent desc) {
        return new ReadParser(Uri.parse(desc.getDataUri()));
    }

    /**
     * Create a new parser from the specified content `uri`.
     * @param uri - the `uri` of the primary data source for this parser.
     */
    private ReadParser(Uri uri) {
        m_source = uri;
    }

    public float getCompletion() {
        // TODO: Implement parser in general (not only this method). We should probably add the `uuid` of the read as well.
        return 0.0f;
    }
}
