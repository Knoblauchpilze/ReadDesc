package knoblauch.readdesc.model;

import android.content.Context;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;

class HtmlSourceLoader extends ReadLoader {

    /**
     * Create a new `HTML` source loader from the specified arguments. Will call
     * the base class constructor and forward the arguments.
     * @param context - the context to use to resolve links and resources.
     * @param progress - the desired progress to load in priority. This allows
     *                   to orient the parsing operations to quickly reach this
     *                   point as it's of interest for the user.
     */
    HtmlSourceLoader(Context context, float progress) {
        super(context, progress);
    }

    /**
     * Used to copy the input loader and create a new object from it. We need to
     * copy fields which make sense and create new one as needed.
     * @param other - the other elements to copy.
     */
    HtmlSourceLoader(HtmlSourceLoader other) {
        // Call base handler.
        super(other);

    }

    @Override
    void loadFromSource(InputStream stream, float progress) throws IOException {
        // TODO: Should handle HTML parsing.
        throw new IOException("Could not load HTML from source, not implemented");
    }

    @Override
    boolean isInvalid() {
        return true;
    }

    @Override
    boolean isAtStart() {
        return true;
    }

    @Override
    boolean isAtEnd() {
        return false;
    }

    @Override
    float getCompletion() {
        return 0.0f;
    }

    @Override
    String getCurrentWord() {
        return "";
    }

    @Override
    Pair<Boolean, Boolean> handleMotion(Action action, int param) {
        return new Pair<>(false, false);
    }
}
