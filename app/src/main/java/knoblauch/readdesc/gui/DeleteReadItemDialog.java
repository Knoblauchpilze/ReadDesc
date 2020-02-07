package knoblauch.readdesc.gui;
import android.content.Context;

import knoblauch.readdesc.R;
import knoblauch.readdesc.model.ReadDesc;

public class DeleteReadItemDialog extends NotifierDialog {

    /**
     * The read description attached to this dialog.
     */
    private ReadDesc m_read;

    /**
     * Creates a fragment dialog with the specified read attached to it.
     * @param read - the read attached to this dialog.
     * @param context - the context into which this dialog is created.
     * @param listener - the potential listener to notify of options chosen in
     *                   this dialog.
     */
    public DeleteReadItemDialog(ReadDesc read, Context context, NotifierDialog.NoticeDialogListener listener) {
        // Call base handler to create the internal field.
        super(context, listener);

        // Register the read.
        m_read = read;
    }

    /**
     * Reimplementation of the base interface method to provide the text associated with
     * this dialog.
     * @return - the message displayed on this dialog.
     */
    String getMessage() {
        return String.format(getResources().getString(R.string.delete_read_confirmation_text), m_read.getName());
    }

    /**
     * Reimplementation of the base interface method to provide the affirmative answer
     * for this dialog.
     * @return - the resource of the affirmative text.
     */
    int getYesResource() {
        return R.string.delete_read_confirmation_yes;
    }

    /**
     * Reimplementation of the base interface method to provide the negative answer
     * text for this dialog.
     * @return - the resource of the negative text.
     */
    int getNoResource() {
        return R.string.delete_read_confirmation_no;
    }

}
