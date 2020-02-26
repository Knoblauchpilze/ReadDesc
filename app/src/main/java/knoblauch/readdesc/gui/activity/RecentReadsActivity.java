package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Stack;

import knoblauch.readdesc.R;
import knoblauch.readdesc.gui.DeleteReadItemDialog;
import knoblauch.readdesc.gui.NotifierDialog;
import knoblauch.readdesc.gui.ReadItemClickListener;
import knoblauch.readdesc.gui.ReadsAdapter;
import knoblauch.readdesc.gui.UriUtils;
import knoblauch.readdesc.model.ReadDesc;
import knoblauch.readdesc.model.ReadIntent;

public class RecentReadsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, ReadItemClickListener, NotifierDialog.NoticeDialogListener, FloatingActionButton.OnClickListener {

    /**
     * Convenience enumeration describing the possible actions to take in the
     * application. Not all actions might be interpreted by all the activities.
     */
    private enum AppAction {
        OpenRead,
        OpenSource,
        Delete
    }

    /**
     * The possible menu actions for this view. This corresponds to the possible
     * actions that are always available to the user no matter what the current
     * activity is.
     */
    private enum MenuAction {
        CreateRead,
        Settings
    }

    /**
     * The adapter containing all the reads to be displayed by this activity.
     */
    private ReadsAdapter m_reads;

    /**
     * The list of pending operations that are awaiting dialog confirmation. We
     * process them using a LIFO system because we assume that confirmations are
     * the result of some process: if a new process awaits a confirmation this
     * probably means that the operation to be processed was a consequence of an
     * existing operation (and should thus be processed first).
     */
    Stack<Pair<AppAction, ReadDesc>> m_pendingOps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_reads);

        // Create the adapter to display recent reads.
        m_reads = new ReadsAdapter(this, this);

        // Create the pending operations list.
        m_pendingOps = new Stack<>();

        // Use this adapter as the content for the recent reads view.
        ListView recentReads = findViewById(R.id.recent_reads_list);
        recentReads.setAdapter(m_reads);

        // Also register this view for contextual menus.
        registerForContextMenu(recentReads);

        // Register a callback for when a click has been detected on a single
        // item of the list view.
        recentReads.setOnItemClickListener(this);

        // Connect the floating action button to the create read activity.
        FloatingActionButton fab = findViewById(R.id.recent_reads_fab);
        fab.setOnClickListener(this);
    }

    @Override
    public void onReadItemViewClick(int resource, int id) {
        // We've been called because the specified view has been clicked. This should be
        // related to a list item
        ReadDesc desc = m_reads.getItem(id);

        // Check the type of resource that has been clicked: this will tell us what to do
        // with the read description.
        switch (resource) {
            case R.id.read_item_name:
                performAction(AppAction.OpenRead, desc);
                break;
            case R.id.read_item_delete:
                performAction(AppAction.Delete, desc);
                break;
            default:
                // Unknown action.
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // We need to detect which element has been clicked. Depending on this we will
        // start the corresponding activity. As most of the action will need the read
        // that has been clicked on, we retrieve it beforehand.
        ReadDesc read = m_reads.getItem(position);

        switch (view.getId()) {
            case R.id.read_delete_menu_opt:
                performAction(AppAction.Delete, read);
                break;
            case R.id.read_open_source_menu_opt:
                performAction(AppAction.OpenSource, read);
                break;
            case R.id.read_open_menu_opt:
            default:
                // We also end up in this case when no particular element of a view has
                // been clicked: in this case we actually want to open the read as well
                // so we moved this case to the `default`.
                performAction(AppAction.OpenRead, read);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        // When we enter this method it means that the floating action button allowing
        // to create a new read has been pressed. We will use the standard way to start
        // the corresponding activity.
        openActivity(MenuAction.CreateRead);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Call base handler.
        super.onCreateOptionsMenu(menu);

        // Inflate the options menu as described in the corresponding resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.read_actions_menu, menu);

        // The menu was created.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Check which menu item has been selected and perform the appropriate action.
        switch (item.getItemId()) {
            case R.id.create_new_read_menu_opt:
                openActivity(MenuAction.CreateRead);
                return true;
            case R.id.settings_menu_opt:
                openActivity(MenuAction.Settings);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        // Call the base handler.
        super.onCreateContextMenu(menu, view, menuInfo);

        // Inflate the corresponding menu in the provided `menuInfo`.
        getMenuInflater().inflate(R.menu.read_item_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Trigger the correct action based on the menu item selected. We also need to fetch the underlying
        // view to which the contextual menu is associated it: this will enable to delete the corresponding
        // read from the list. The information is contained in the input `item` we just need to access it
        // through the `AdapterContextMenuInfo` interface.
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.read_delete_menu_opt:
                performAction(AppAction.Delete, m_reads.getItem((int)info.id));
                return true;
            case R.id.read_open_menu_opt:
                performAction(AppAction.OpenRead, m_reads.getItem((int)info.id));
                return true;
            case R.id.read_open_source_menu_opt:
                performAction(AppAction.OpenSource, m_reads.getItem((int)info.id));
                return true;
            default:
                break;
        }

        // Use the base handler to provide the return value.
        return super.onContextItemSelected(item);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // Nothing to do if the internal list of pending operations is empty.
        if (m_pendingOps.empty()) {
            return;
        }

        // Process the action.
        Pair<AppAction, ReadDesc> op = m_pendingOps.pop();

        if (op.first == AppAction.Delete) {
            deleteRead(op.second);
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // The dialog was dismissed, we need to remove the most recent pending
        // operations so that the next one can be processed.
        if (!m_pendingOps.empty()) {
            m_pendingOps.pop();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // In case the return code does not indicate a successful execution, we don't do anything.
        // We also don't do anything in case the `requestCode` does not correspond to something we
        // know how to handle.
        Resources res = getResources();
        int createReadReq = res.getInteger(R.integer.new_read_intent_res_code);
        int readCompletedReq = res.getInteger(R.integer.start_read_intent_res_code);

        Log.i("reads", "Result is " + requestCode + " (read: " + createReadReq + ", complete: " + readCompletedReq + "), res: " + resultCode + " (ok: " + RESULT_OK + ")");

        if ((resultCode != RESULT_OK && requestCode == createReadReq) || (requestCode != createReadReq && requestCode != readCompletedReq)) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        // We might want to create the new read from the read intent stored in the `data`.
        if (requestCode == createReadReq) {
            ReadIntent intent = data.getParcelableExtra(res.getString(R.string.new_read_intent_key));

            // Check consistency.
            if (intent == null) {
                return;
            }

            // We need to register the read from its intent.
            ReadDesc read = ReadDesc.fromIntent(intent);

            // Register this read in the internal bank.
            m_reads.addItem(read);

            return;
        }

        // Check whether we successfully read some data from the selected read. If this is the case
        // we can display an information about this, otherwise we can indicate that something went
        // wrong. We should verify that the activity actually sent some results back: indeed we can
        // end up here only when the activity called the `finish` method so if everything went well
        // we don't want to display anything (as there's nothing to display).
        if (data != null) {
            String key = res.getString(R.string.read_mode_success_notification);
            boolean success = data.getBooleanExtra(key, false);

            if (!success) {
                String msg = res.getString(R.string.read_desc_failure_read_mode);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause() {
        // We need to save the reads to the local storage so that we can reload them
        // upon relaunching the application.
        m_reads.save();

        // Call the base handler.
        super.onPause();
    }

    @Override
    public void onResume() {
        // Refresh the reads as some of them might have been modified if we launched
        // a `Reading` activity.
        m_reads.refresh();

        // Call the base handler.
        super.onResume();
    }

    /**
     * Used to create and display a new activity related to the specified action.
     * @param action - the menu action describing the activity to create.
     */
    private void openActivity(MenuAction action) {
        // Depending on the action to perform we might want to create activities with
        // some expected return value and sometimes not. So we will distinguish based
        // on the action.
        Resources res = getResources();

        if (action == MenuAction.CreateRead) {
            // Create the intent to start the `CreateRead` activity.
            Intent act = new Intent(RecentReadsActivity.this, CreateReadActivity.class);

            // Start the activity.
            startActivityForResult(act, res.getInteger(R.integer.new_read_intent_res_code));
        }
        else if (action == MenuAction.Settings) {
            // Create the intent for the `Settings` activity.
            Intent act = new Intent(RecentReadsActivity.this, SettingsActivity.class);

            // Start the activity.
            startActivity(act);
        }
    }

    /**
     * Used to perform the registration of the action in the internal list of pending
     * operations to perform. This mechanism is used to work around the fact that the
     * dialogs are asynchronous: in case we want a confirmation we thus have to wait
     * for the user to answer before taking any actions. Actions are stored in order
     * of their creation and are performed as soon as an answer is received.
     * @param action - the action to perform.
     * @param desc - the read associated to the action (might be null if the action does not
     *               need any read).
     */
    private void registerAction(AppAction action, ReadDesc desc) {
        // Add the corresponding action to the internal stack.
        m_pendingOps.push(new Pair<>(action, desc));
    }

    /**
     * Used to perform the specified action with the current data available in the
     * activity. This usually means retrieving in some way the current selected or
     * at least current read.
     * @param action - the action to perform.
     * @param desc - the read on which the action should be performed. Note that this value
     *               might be null in case of some actions that can be applied on all the
     *               reads at once.
     */
    private void performAction(AppAction action, ReadDesc desc) {
        // Switch the action and handle it correctly.
        switch (action) {
            case Delete:
                // Create a dialog to ask the user for confirmation. Note that as dialogs in
                // android are asynchronous we can't really perform the deletion right now.
                // We need for the user to click on the dialog (either accept it or confirm
                // the deletion to do anything).
                // To do so we will use the internal queue of actions that are pending for
                // dialog confirmation and register the deletion of the `desc` in there. If
                // the user confirms we will process it and discard it otherwise.
                registerAction(action, desc);

                // Create an instance of the delete read item dialog fragment and show it.
                DialogFragment dialog = new DeleteReadItemDialog(desc, this, this);
                dialog.show(getSupportFragmentManager(), "DeleteDialogFor" + desc.getName());
                break;
            case OpenRead:
                startReading(desc);
                break;
            case OpenSource:
                openReadSource(desc);
                break;
            default:
                break;
        }
    }

    /**
     * Called whenever a deletion of the input read should be performed. At this
     * point we already displayed an alert dialog and we are sure that the read
     * should be deleted. We will handle both the deletion of the read from the
     * data model and from the list of items representing the reads.
     * @param desc - the read description to delete.
     */
    private void deleteRead(ReadDesc desc) {
        // Remove the item from the list of reads.
        m_reads.removeItem(desc);
    }

    /**
     * Called whenever the user decides to start reading mode for a specific read.
     * This method will start the corresponding activity and provide it with the
     * relevant data about the read.
     * @param desc - the read for which the reading mode should be started.
     */
    private void startReading(ReadDesc desc) {
        // Create the intent to start the reading activity.
        Intent start = new Intent(RecentReadsActivity.this, ReadActivity.class);

        Resources res = getResources();
        String key = res.getString(R.string.start_reading_intent_desc);
        start.putExtra(key, desc.toReadIntent());

        startActivityForResult(start, res.getInteger(R.integer.start_read_intent_res_code));
    }

    /**
     * Called whenever the user requests to display the source of the read. It is
     * a process that depends on the actual type of the read because we don't want
     * to display in the same way a read coming from a remote location or from a
     * local file.
     * @param desc - the read for which the source should be displayed.
     */
    private void openReadSource(ReadDesc desc) {
        // We want to open the source of the read. Depending on the type of the read
        // it might mean open different type of element (a website, a text file or a
        // e-book for example). We will rely on `Android` providing a selector to let
        // the user select its preferred application.
        Uri uri = Uri.parse(desc.getSource());

        // Create the intent from the read's source `uri`.
        Intent open = new Intent(Intent.ACTION_VIEW, uri);

        // Broadcast it and let the system find the correct application.
        try {
            startActivity(open);
        }
        catch (ActivityNotFoundException e) {
            // In case we can't find a valid activity to handle the user's request,
            // we'll just display a toast.
            Resources res = getResources();
            String msg = String.format(res.getString(R.string.read_desc_failure_open_source), UriUtils.condenseUri(uri, this));

            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

}
