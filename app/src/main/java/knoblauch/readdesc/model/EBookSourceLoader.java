package knoblauch.readdesc.model;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class EBookSourceLoader extends ReadLoader {

    /**
     * Create a new `e-book` source loader from the specified arguments. Will call
     * the base class constructor and forward the arguments.
     * @param context - the context to use to resolve links and resources.
     * @param listener - the object to notify whenever the data has successfully
     *                   been loaded or a failure has been detected.
     */
    EBookSourceLoader(Context context, ReadLoaderListener listener) { super(context, listener); }

    @Override
    ArrayList<Paragraph> loadFromSource(InputStream stream) throws IOException {
        // TODO: Should handle e-book parsing.
        return new ArrayList<>();
    }
}
