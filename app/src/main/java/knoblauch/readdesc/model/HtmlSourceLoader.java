package knoblauch.readdesc.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class HtmlSourceLoader extends ReadLoader {

    /**
     * Create a new `HTML` source loader from the specified arguments. Will call
     * the base class constructor and forward the arguments.
     * @param context - the context to use to resolve links and resources.
     * @param uri - the `uri` from which the data should be loaded.
     * @param listener - the object to notify whenever the data has successfully
     *                   been loaded or a failure has been detected.
     */
    HtmlSourceLoader(Context context, String uri, ReadLoaderListener listener) {
        super(context, uri, listener);
    }

    @Override
    ArrayList<Paragraph> loadFromSource(Uri source, Context context) throws IOException {
        // Try to instantiate a valid `HTML` reader from this source.
        if (source == null || source.getPath() == null) {
            throw new IOException("Cannot set invalid HTML source in parser");
        }

        // We need to create a file as the source of the `HTML` reader. Note
        // that as we store the content's `uri` we need to resolve the link
        // in order to access to the file.
        ContentResolver res = context.getContentResolver();
        InputStream inStream = res.openInputStream(source);
        if (inStream == null) {
            throw new IOException("Cannot load HTML content \"" + source.toString() + "\" in parser");
        }

        // TODO: Should handle HTML parsing.
        return new ArrayList<>();
    }
}
