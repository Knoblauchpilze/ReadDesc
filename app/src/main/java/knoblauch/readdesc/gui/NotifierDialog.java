package knoblauch.readdesc.gui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public abstract class NotifierDialog extends DialogFragment {

    /**
     * The context executing this dialog. Usually the parent
     * activity creating the dialog.
     */
    private Context m_context;

    /**
     * The activity that creates an instance of this dialog fragment
     * must implement this interface in order to receive these event
     * callbacks. Each method passes the DialogFragment in case the
     * host needs to query it.
     */
    public interface NoticeDialogListener {

        /***
         * Fired in case the positive option has been chosen on the dialog.
         * @param dialog - the dialog that emitted the positive option.
         */
        void onDialogPositiveClick(DialogFragment dialog);

        /**
         * Fired in case the negative option has been chosen.
         * @param dialog - the dialog that emitted this option.
         */
        void onDialogNegativeClick(DialogFragment dialog);
    }

    /**
     * Internal variable to notify of options chosen in this dialog.
     */
    private NoticeDialogListener m_listener;

    /**
     * Creates a fragment dialog with the specified read attached to it.
     * @param context - the context into which this dialog is created.
     * @param listener - the potential listener to notify of options chosen in
     *                   this dialog.
     */
    NotifierDialog(Context context, NoticeDialogListener listener) {
        m_context = context;
        m_listener = listener;
    }

    /**
     * Interface method that should be implemented in inheriting classes to
     * define the precise message to display for this dialog.
     * @return - the string representing the message for this dialog.
     */
    abstract String getMessage();

    /**
     * Interface method to retrieve the resource describing the string for
     * the `yes` option of this dialog.
     * @return - the resource representing the acceptation string.
     */
    abstract int getYesResource();

    /**
     * Interface method to retrieve the resource describing the string for
     * the `no` option of this dialog. Very similar to `getYesResource` in
     * its behavior.
     * @return - the string to use for the negative option of this dialog.
     */
    abstract int getNoResource();

    @Override
    public @NonNull
    Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(m_context);

        // Build the dialog.
        builder.setMessage(getMessage())
                .setPositiveButton(
                        getYesResource(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (m_listener != null) {
                                    m_listener.onDialogPositiveClick(NotifierDialog.this);
                                }
                            }
                        })
                .setNegativeButton(
                        getNoResource(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (m_listener != null) {
                                    m_listener.onDialogNegativeClick(NotifierDialog.this);
                                }
                            }
                        });

        return builder.create();
    }

}
