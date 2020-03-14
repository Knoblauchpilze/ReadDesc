package knoblauch.readdesc.model;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;

class HtmlSourceLoader extends ReadLoader {

    /**
     * A string representing the path to the data source for this loader. This is
     * interesting as the `HTML` parser we're using needs to have access to the
     * base path to reach the `HTML` document in order to be able to resolve links
     * and resources in the document.
     */
    private String m_base;

    /**
     * Create a new `HTML` source loader from the specified arguments. Will call
     * the base class constructor and forward the arguments.
     * The `HTML` parser we're using requires to have knowledge of the base `uri`
     * of the data source so that it can make sense of the relative links and the
     * resources defined in the source. The user should thus provide a string to
     * describe the location of the data source.
     * @param context - the context to use to resolve links and resources.
     * @param progress - the desired progress to load in priority. This allows
     *                   to orient the parsing operations to quickly reach this
     *                   point as it's of interest for the user.
     * @param base - a string representing the path to the data source, used to
     *               resolve relative links and resources.
     */
    HtmlSourceLoader(Context context, float progress, String base) {
        super(context, progress);

        // Save the base path to the data source for further usage.
        m_base = base;
    }

    /**
     * Used to copy the input loader and create a new object from it. We need to
     * copy fields which make sense and create new one as needed.
     * @param other - the other elements to copy.
     */
    HtmlSourceLoader(HtmlSourceLoader other) {
        // Call base handler.
        super(other);

        // Copy the path to the data source.
        m_base = other.m_base;

    }

    @Override
    void loadFromSource(InputStream stream, float progress) throws IOException {
        // Try to create the document from the input stream.
        Document doc = Jsoup.parse(stream, null, m_base);

        Log.i("main", "Successfully parsed document");

        // Retrieve the body of the document: if we can't this is an issue.
        Element body = doc.body();
        if (body == null) {
            throw new IOException("Cannot retrieve body for html page");
        }

        String str = body.text();
        Log.i("main", "Doc str is \"" + str.substring(0, 150) + "...\" (size: " + str.length() + ")");

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
    String getPreviousWord() { return ""; }

    @Override
    String getNextWord() { return ""; }

    @Override
    Pair<Boolean, Boolean> handleMotion(Action action, int param) {
        return new Pair<>(false, false);
    }
}
