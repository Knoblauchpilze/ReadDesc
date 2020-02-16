package knoblauch.readdesc.gui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

public class UriUtils {

    /**
     * Convenience wrapper around the method `condenseUri` method which
     * takes a real `uri` object in argument.
     * @param uri - the `uri` tu condense as a string object.
     * @param context - the context to use to resolve information about
     *                  the `uri`.
     * @return - the string representing the condensed `uri`.
     */
    public static String condenseUri(String uri, Context context) {
        return condenseUri(Uri.parse(uri), context);
    }

    /**
     * Used to condense the input `uri` into a valid string that is better suited for
     * display. This will usually keep ony the useful part of the `uri`, for example
     * in the case of a file it might strip everything but the file's name.
     * @param uri - the `uri` to condense.
     * @param context - the context to use to resolve information about the `uri` as
     *                  input. This is the link between the abstract logical info of
     *                  the `uri` and its content.
     * @return - a string representing the useful content of the `uri` (at least from
     *           a user standpoint) or the `uri` itself if no meaningful info can be
     *           retrieved from it.
     */
    public static String condenseUri(Uri uri, Context context) {
        // In case the input `uri` is not valid, return early.
        if (uri == null) {
            return null;
        }

        String name = null;

        // Try to extract the name assuming the scheme corresponds to a file.
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("content")) {
            // This link explains how android handles the content and provide some
            // useful resource to use it:
            // https://developer.android.com/guide/topics/providers/document-provider.html
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }

        // In case we couldn't find the name (either because the scheme did not
        // correspond to a file or because of a failure), try to resort on the
        // last '/' character existing in the string.
        if (name == null && uri.getPath() != null) {
            name = uri.getPath();

            // Keep only the part after the '/' character.
            int cut = name.lastIndexOf('/');
            if (cut != -1) {
                name = name.substring(cut + 1);
            }
        }

        // We either retrieved a valid result or failed to extract one.
        return name;
    }
}
