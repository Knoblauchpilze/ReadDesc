package knoblauch.readdesc.gui;

import android.content.Context;

import knoblauch.readdesc.R;

public class ResetPreferencesDialog extends NotifierDialog {

    /**
     * Creates a fragment dialog allowing to reset the preferences to their default value
     * using a predefined message.
     * @param context - the context into which this dialog is created.
     * @param listener - the potential listener to notify of options chosen in
     *                   this dialog.
     */
    public ResetPreferencesDialog(Context context, NotifierDialog.NoticeDialogListener listener) {
        // Call base handler to create the internal field.
        super(context, listener);
    }

    /**
     * Reimplementation of the base interface method to provide the text associated with
     * this dialog.
     * @return - the message displayed on this dialog.
     */
    String getMessage() {
        return getResources().getString(R.string.activity_settings_pref_reset_text_prompt);
    }

    /**
     * Reimplementation of the base interface method to provide the affirmative answer
     * for this dialog.
     * @return - the resource of the affirmative text.
     */
    int getYesResource() {
        return R.string.activity_request_text_accept;
    }

    /**
     * Reimplementation of the base interface method to provide the negative answer
     * text for this dialog.
     * @return - the resource of the negative text.
     */
    int getNoResource() {
        return R.string.activity_request_text_cancel;
    }
}


