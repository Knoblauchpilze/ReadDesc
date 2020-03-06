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
     * @param progress - the desired progress to load in priority. This allows
     *                   to orient the parsing operations to quickly reach this
     *                   point as it's of interest for the user.
     */
    EBookSourceLoader(Context context, float progress) { super(context, progress); }

    @Override
    ArrayList<String> loadFromSource(InputStream stream, float progress) throws IOException {
        // TODO: Should handle e-book parsing.
        return new ArrayList<>();
    }
}
