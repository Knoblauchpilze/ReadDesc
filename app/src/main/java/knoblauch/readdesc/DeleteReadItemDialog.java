package knoblauch.readdesc;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DeleteReadItemDialog extends DialogFragment {

    /**
     * @brief - The read description attached to this dialog.
     */
    private ReadDesc m_read;

    /**
     * @brief - The context executing this dialog. Usually the parent
     *          activity creating the dialog.
     */
    private Context m_context;

    /**
     * @brief - The activity that creates an instance of this dialog fragment
     *           must implement this interface in order to receive these event
     *           allbacks. Each method passes the DialogFragment in case the
     *           host needs to query it.
     */
    public interface NoticeDialogListener {
        /***
         * @bief - Fired in case the positive option has been chosen on the dialog.
         * @param dialog - the dialog that emitted the positive option.
         */
        void onDialogPositiveClick(DialogFragment dialog);

        /**
         * @brief - Fired in case the negative option has been chosen.
         * @param dialog - the dialog that emitted this option.
         */
        void onDialogNegativeClick(DialogFragment dialog);
    }

    /**
     * @brief - Internal variable to notify of options chosen in this dialog.
     */
    private NoticeDialogListener m_listener;

    /**
     * @brief - Creates a fragment dialog with the specified read attached to it.
     * @param read - the read attached to this dialog.
     * @param context - the context into which this dialog is created.
     * @param listener - the potential listener to notify of options chosen in
     *                   this dialog.
     */
    public DeleteReadItemDialog(ReadDesc read, Context context, NoticeDialogListener listener) {
        m_read = read;
        m_context = context;
        m_listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(m_context);

        // Retrieve the read's name from the input identifier.
        Resources res = getResources();
        String text = String.format(res.getString(R.string.delete_read_confirmation_text), m_read.getName());

        // Build the dialog.
        builder.setMessage(text)
               .setPositiveButton(
                       R.string.delete_read_confirmation_yes,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               if (m_listener != null) {
                                   m_listener.onDialogPositiveClick(DeleteReadItemDialog.this);
                               }
                           }
                       })
               .setNegativeButton(
                       R.string.delete_read_confirmation_no,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               if (m_listener != null) {
                                   m_listener.onDialogNegativeClick(DeleteReadItemDialog.this);
                               }
                           }
                       });

        return builder.create();
    }
}
