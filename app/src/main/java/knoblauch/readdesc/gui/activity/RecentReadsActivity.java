package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.content.Intent;
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

import java.util.Stack;

import knoblauch.readdesc.R;
import knoblauch.readdesc.gui.DeleteReadItemDialog;
import knoblauch.readdesc.gui.ReadItemClickListener;
import knoblauch.readdesc.gui.ReadsAdapter;
import knoblauch.readdesc.model.ReadDesc;

public class RecentReadsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, ReadItemClickListener, DeleteReadItemDialog.NoticeDialogListener {

    /**
     * @brief - Convenience enumeration describing the possible actions to take in the
     *          application. Not all actions might be interpreted by all the activities.
     */
    private enum AppAction {
        OpenRead,
        OpenSource,
        Delete
    }

    /**
     * @brief - The possible menu actions for this view. This corresponds to the possible
     *          actions that are always available to the user no matter what the current
     *          activity is.
     */
    private enum MenuAction {
        CreateRead,
        Settings
    }

    /**
     * @brief - The adapter containing all the reads to be displayed by this activity.
     */
    private ReadsAdapter m_reads;

    /**
     * @brief - The list of pending operations that are awaiting dialog confirmation. We
     *          process them using a LIFO system because we assume that confirmations are
     *          the result of some process: if a new process awaits a confirmation this
     *          probably means that the operation to be processed was a consequence of an
     *          existing operation (and should thus be processed first).
     */
    Stack<Pair<AppAction, ReadDesc>> m_pendingOps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter to display recent reads.
        m_reads = new ReadsAdapter(this, 10, this);

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
    }

    @Override
    public void onReadItemViewClick(int resource, int id, String name) {
        // We've been called because the specified view has been clicked. This should be
        // related to a list item
        ReadDesc desc = m_reads.getItem(id);

        Log.i("main", "Clicked on read item menu at " + id + " view id being " + resource + ", play: " + R.id.read_item_play + " read is " + desc.getName() + " (name: " + name + ")");

        // Check the type of resource that has been clicked: this will tell us what to do
        // with the read description.
        switch (resource) {
            case R.id.read_item_name:
            case R.id.read_item_play:
                performAction(AppAction.OpenRead, desc);
                break;
            case R.id.read_item_source:
                performAction(AppAction.OpenSource, desc);
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
            case R.id.read_view_menu_opt:
                performAction(AppAction.OpenSource, read);
                break;
            case R.id.read_open_menu_opt:
                performAction(AppAction.OpenRead, read);
            default:
                // Do nothing.
                break;
        }
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
            case R.id.read_view_menu_opt:
            default:
                // TODO: Should handle this.
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

        switch (op.first) {
            case Delete:
                deleteRead(op.second);
                break;
            default:
                // Other actions do not require anything to be done.
                break;
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

    /**
     * @brief  - Used to create and display a new activity related to the specified action.
     * @param action - the menu action describing the activity to create.
     */
    private void openActivity(MenuAction action) {
        // Create the intent object to use to describe the activity to open.
        Intent act = null;

        // Populate it based on the input action to perform.
        switch (action) {
            case CreateRead:
                act = new Intent(RecentReadsActivity.this, CreateReadActivity.class);
                break;
            case Settings:
                act = new Intent(RecentReadsActivity.this, SettingsActivity.class);
                break;
            default:
                break;
        }

        // Launch the activity if we could find the corresponding view.
        if (act != null) {
            startActivity(act);
        }
    }

    /**
     * @brief - Used to perform the registration of the action in the internal list of pending
     *          operations to perform. This mechanism is used to work around the fact that the
     *          dialogs are asynchronous: in case we want a confirmation we thus have to wait
     *          for the user to answer before taking any actions. Actions are stored in order
     *          of their creation and are performed as soon as an answer is received.
     * @param action - the action to perform.
     * @param desc - the read associated to the action (might be null if the action does not
     *               need any read).
     */
    private void registerAction(AppAction action, ReadDesc desc) {
        // Add the corresponding action to the internal stack.
        m_pendingOps.push(new Pair<>(action, desc));
    }

    /**
     * @brief - Used to perform the specified action with the current data available in the
     *          activity. This usually means retrieving in some way the current selected or
     *          at least current read.
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
            case OpenSource:
            default:
                // TODO: Implementation.
                break;
        }
    }

    /**
     * @brief - Called whenever a deletion of the input read should be performed. At this
     *          point we already displayed an alert dialog and we are sure that the read
     *          should be deleted. We will handle both the deletion of the read from the
     *          data model and from the list of items representing the reads.
     * @param desc - the read description to delete.
     */
    private void deleteRead(ReadDesc desc) {
        // Remove the item from the list of reads.
        m_reads.removeItem(desc);
    }

}
