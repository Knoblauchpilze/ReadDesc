package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import java.util.ArrayList;

import knoblauch.readdesc.R;
import knoblauch.readdesc.model.ReadDesc;
import knoblauch.readdesc.model.ReadIntent;

public class CreateReadActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, Button.OnClickListener, TextWatcher {

    /**
     * Private class used to implement the `TextWatcher` interface in order to detect
     * changes to the source of the read.
     */
    private class SourceTextWatcher implements TextWatcher {

        /**
         * Holds the current text watched by this listener.
         */
        private String m_text;

        /**
         * Create a new source text watcher. The text is by default set to the input
         * string upon building the object and can be changed later with the `setText`
         * method if needed.
         * @param text - the text to assign to this watcher.
         */
        SourceTextWatcher(String text) {
            setText(text);
        }

        /**
         * Used to reset the text associated to the producer watched by this object to
         * a known value. This can be useful to keep things synchronized when the text
         * edit listened to by this watcher is modified programmatically. Note that it
         * should be used with care as it might bring inconsistencies between the edit
         * text and the watcher.
         */
        void setText(String text) {
            m_text = text;
        }

        /**
         * Return the texts associated to this watcher. This is the result accumulated
         * so far by listening to the producers used by this watcher.
         * @return - the text that should be displayed in the producer.
         */
        String getText() {
            return m_text;
        }

        /**
         * Allow to determine whether the text contained in this watcher is not empty.
         * @return - `true` if the internal text is not empty and `false` otherwise.
         */
        boolean isNotEmpty() {
            return !getText().isEmpty();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Nothing to do here, we prefer to react through the `onTextChanged` method.
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Update the internal text.
            m_text = s.toString();

            // Also we need to update the status of the `accept` button in the parent
            // class.
            if (m_text.isEmpty()) {
                m_accept.setEnabled(false);
            }
            else if (!m_readName.getText().toString().isEmpty()) {
                m_accept.setEnabled(true);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Nothing to do here, we prefer to react through the `onTextChanged` method.
        }
    }

    /**
     * Holds the properties needed to create a read with each mode. This basically
     * means the source (described as a text edit) and the button which we should
     * use to trigger a file explorer window to select the file.
     * Based on the type of the read to create we will instantiate a distinct app
     * to browse the source (web browser, file explorer, etc.).
     */
    private class CreationModeProps {
        RadioButton active;
        EditText source;
        Button browse;

        LinearLayout layout;
    }

    /**
     * Define all the needed properties to select the thumbnail of the read if any
     * should be attached. Indeed this property is optional and the user can choose
     * to assign one to a new read by ticking the checkbox.
     */
    private class ThumbnailSelectionProps {
        CheckBox active;
        EditText source;
        Button browse;
    }

    /**
     * Holds the current desired type of the read. This is directly linked to the
     * radio button currently active in this view. Note that this value is updated
     * each time the user selects a new button. It is also used to determine which
     * kind of value should be fetched for the source of the read more easily than
     * to loop through the possible types.
     */
    ReadDesc.Type m_type;

    /**
     * Holds the text edit representing the name of the read to create. Filled by
     * the user to provide an identifier for the read.
     */
    EditText m_readName;

    /**
     * Holds the graphic properties used to display the read to create from a file.
     * Only relevant when the corresponding checkbox is ticked.
     */
    CreationModeProps m_fileProps;

    /**
     * Holds a similar property as `m_fileProps` but in the case of the creation of
     * a read from a web page.
     */
    CreationModeProps m_websiteProps;

    /**
     * Similar to `m_fileProps` and `m_webpageProps` but used in the case of a read
     * created from a e-book.
     */
    CreationModeProps m_eBookProps;

    /**
     * Defines a text watcher used to react on modification of the text of the desired
     * source of the read. Depending on the type of the read it will automatically bind
     * to the correct producer.
     */
    SourceTextWatcher m_sourceWatcher;

    /**
     * Holds the properties used to reference graphic elements controlling the selection
     * of a thumbnail.
     */
    ThumbnailSelectionProps m_thumbnail;

    /**
     * Holds a reference to the cancellation button. Hitting this button will finish the
     * activity and bring back to the recent reads screen.
     */
    Button m_cancel;

    /**
     * Holds a reference to the cancellation button. Only possible to use it when the
     * name of the read contains at least a letter and when a source has been selected.
     */
    Button m_accept;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Create using the parent handler.
        super.onCreate(savedInstanceState);

        // Assign the main content.
        setContentView(R.layout.activity_create_read);

        // Default type is a file read.
        m_type = ReadDesc.Type.File;

        // Retrieve views so that we can react to the actions of the user.
        m_readName = findViewById(R.id.new_read_name);

        m_fileProps = new CreationModeProps();
        m_websiteProps = new CreationModeProps();
        m_eBookProps = new CreationModeProps();

        m_sourceWatcher = new SourceTextWatcher("");

        m_fileProps.active = findViewById(R.id.new_read_file);
        m_fileProps.source= findViewById(R.id.new_read_file_source);
        m_fileProps.browse = findViewById(R.id.new_read_file_source_browse);
        m_fileProps.layout = findViewById(R.id.new_read_file_layout);

        m_websiteProps.active = findViewById(R.id.new_read_website);
        m_websiteProps.source= findViewById(R.id.new_read_website_source);
        m_websiteProps.browse = findViewById(R.id.new_read_website_source_browse);
        m_websiteProps.layout = findViewById(R.id.new_read_website_layout);

        m_eBookProps.active = findViewById(R.id.new_read_e_book);
        m_eBookProps.source= findViewById(R.id.new_read_e_book_source);
        m_eBookProps.browse = findViewById(R.id.new_read_e_book_source_browse);
        m_eBookProps.layout = findViewById(R.id.new_read_e_book_layout);

        m_thumbnail = new ThumbnailSelectionProps();

        m_thumbnail.active = findViewById(R.id.new_read_thumbnail_enable);
        m_thumbnail.source = findViewById(R.id.new_read_thumbnail_location);
        m_thumbnail.browse = findViewById(R.id.new_read_thumbnail_browse);

        m_cancel = findViewById(R.id.new_read_cancel);
        m_accept = findViewById(R.id.new_read_accept);

        // Register to relevant signals to be able to detect actions in the
        // interface.
        m_readName.addTextChangedListener(this);

        m_fileProps.active.setOnCheckedChangeListener(this);
        m_websiteProps.active.setOnCheckedChangeListener(this);
        m_eBookProps.active.setOnCheckedChangeListener(this);

        m_fileProps.source.addTextChangedListener(m_sourceWatcher);
        m_websiteProps.source.addTextChangedListener(m_sourceWatcher);
        m_eBookProps.source.addTextChangedListener(m_sourceWatcher);

        m_thumbnail.active.setOnCheckedChangeListener(this);

        m_fileProps.browse.setOnClickListener(this);
        m_websiteProps.browse.setOnClickListener(this);
        m_eBookProps.browse.setOnClickListener(this);

        m_thumbnail.browse.setOnClickListener(this);

        m_cancel.setOnClickListener(this);
        m_accept.setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // We need to process two main kind of events in here: either one of the radio buttons
        // controlling the type of the read to create has been selected or the thumbnail has
        // been enabled.
        // In the first case we want to make the corresponding layout visible (and hide the
        // current one) and in the second case we want to enable the selection of the thumbnail
        // source.

        // Handle thumbnail de/activation.
        if (buttonView == m_thumbnail.active) {
            m_thumbnail.source.setEnabled(isChecked);
            m_thumbnail.browse.setEnabled(isChecked);

            // We will obviously fail to find that the `buttonView` corresponds to one of the
            // read type.
            return;
        }

        // Discard any case where the button has been ticked off: we don't react to this unless
        // this is the thumbnail checkbox so it's not relevant for us.
        if (!isChecked) {
            return;
        }

        // Detect cases where the user selected one of the read types.
        boolean valid =
                buttonView == m_fileProps.active ||
                buttonView == m_websiteProps.active ||
                buttonView == m_eBookProps.active;

        int fVis = (buttonView == m_fileProps.active ? View.VISIBLE : View.GONE);
        int wVis = (buttonView == m_websiteProps.active ? View.VISIBLE : View.GONE);
        int eVis = (buttonView == m_eBookProps.active ? View.VISIBLE : View.GONE);

        // If the `buttonView` actually corresponds to one of the read type, assign the
        // new layout's visibility statuses.
        if (valid) {
            m_fileProps.layout.setVisibility(fVis);
            m_websiteProps.layout.setVisibility(wVis);
            m_eBookProps.layout.setVisibility(eVis);
        }

        // Update the read type and the text use for the source or the read. We will also
        // de/activate the `create` button based on whether the read has a valid name and
        // a valid source.
        if (fVis != View.GONE) {
            m_type = ReadDesc.Type.File;
            m_sourceWatcher.setText(m_fileProps.source.getText().toString());
        }
        if (wVis != View.GONE) {
            m_type = ReadDesc.Type.WebPage;
            m_sourceWatcher.setText(m_websiteProps.source.getText().toString());
        }
        if (eVis != View.GONE) {
            m_type = ReadDesc.Type.EBook;
            m_sourceWatcher.setText(m_eBookProps.source.getText().toString());
        }

        m_accept.setEnabled(!m_readName.getText().toString().isEmpty() && m_sourceWatcher.isNotEmpty());
    }

    @Override
    public void onClick(View v) {
        // Detect which button has been clicked. We need to handle both the cases where the
        // user either cancelled or validated the creation of the read and when a request to
        // set the source of the read should be processed.

        // Handle cancellation.
        if (v == m_cancel) {
            // Terminate the activity, the user does not want to create a read after all.
            finish();

            return;
        }

        // Handle creation of the read.
        if (v == m_accept) {
            // Perform the creation of the read from the internal data. We will monitor the
            // return status as it is the value that will be returned by the activity. It is
            // then interpreted by the parent activity to actually create it in the data bank
            // if possible and potentially start the reading mode.
            // The creation should always succeed as the `Create` button is only clickable if
            // all relevant properties have valid values but better be safe than sorry.
            ReadIntent read = createRead();

            if (read != null) {
                // Register this result.
                Resources res = getResources();
                String key = res.getString(R.string.new_read_intent_key);

                Intent ret = new Intent();
                ret.putExtra(key, read);
                setResult(RESULT_OK, ret);

                // Finish this activity as we're done with the read creation.
                finish();

                return;
            }
        }

        // Handle the request to browse for a new source of the read or for
        // a new thumbnail file.
        Resources res = getResources();

        Intent browsing = new Intent(Intent.ACTION_GET_CONTENT);
        browsing.addCategory(Intent.CATEGORY_OPENABLE);
        ArrayList<String> mimeTypes = new ArrayList<>();
        int requestCode = res.getInteger(R.integer.new_read_source_selected_res_code);

        if (v == m_fileProps.browse) {
            mimeTypes.add("text/plain");
            mimeTypes.add("application/pdf");
        }

        if (v == m_websiteProps.browse) {
            mimeTypes.add("text/html");
        }

        if (v == m_eBookProps.browse) {
            mimeTypes.add("application/epub+zip");
        }

        if (v == m_thumbnail.browse) {
            mimeTypes.add("image/*");

            requestCode = res.getInteger(R.integer.new_read_thumbnail_source_selected_res_code);
        }

        // Create the intent if we could find at least a single type of
        // file to retrieve.
        if (mimeTypes.size() > 0) {
            browsing.setType("*/*");
            browsing.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            startActivityForResult(browsing, requestCode);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nothing to do: we will react on the `onTextChanged` event.
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Determine whether we have a valid char sequence in which case we need to enable
        // the `create` button if the source is valid.

        // If the text for the read's name is empty we can't possibly activate the `accept`
        // button.
        if (s.toString().isEmpty()) {
            m_accept.setEnabled(false);

            return;
        }

        // Otherwise, check whether the source is valid: if this the case we can enable
        // the `accept` button.
        if (m_sourceWatcher.isNotEmpty()) {
            m_accept.setEnabled(true);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Nothing to do: we will react on the `onTextChanged` event.
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // In case the return code does not indicate a successful execution, we don't do anything.
        // We also don't do anything in case the `requestCode` does not correspond to something we
        // know how to handle.
        Resources res = getResources();
        int fBrowseCode = res.getInteger(R.integer.new_read_source_selected_res_code);
        int tBrowseCode = res.getInteger(R.integer.new_read_thumbnail_source_selected_res_code);

        if (resultCode != RESULT_OK || (requestCode != fBrowseCode && requestCode != tBrowseCode)) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        // Retrieve the data provided by the user and save it into the source of the corresponding
        // type of read. We only want to react for cases where the user chose a valid path for the
        // source of the read.
        Uri path = data.getData();

        if (path != null) {
            String uri = path.toString();

            // Handle thumbnail source first.
            if (requestCode == tBrowseCode) {
                m_thumbnail.source.setText(uri);
                return;
            }

            // Handle read source.
            switch (m_type) {
                case File:
                    m_fileProps.source.setText(uri);
                    break;
                case WebPage:
                    m_websiteProps.source.setText(uri);
                    break;
                case EBook:
                    m_eBookProps.source.setText(uri);
                    break;
            }
        }
    }

    private ReadIntent createRead() {
        // We need to retrieve each property.
        boolean valid = true;

        String name = m_readName.getText().toString();
        if (name.isEmpty()) {
            valid = false;
        }

        String data = null;
        switch (m_type) {
            case File:
                data = m_fileProps.source.getText().toString();
                break;
            case WebPage:
                data = m_websiteProps.source.getText().toString();
                break;
            case EBook:
                data = m_eBookProps.source.getText().toString();
                break;
        }
        if (data.isEmpty()) {
            valid = false;
        }

        String thumbnail = m_thumbnail.source.getText().toString();

        // Depending on whether we could fetch all properties, we can return the read intent.
        if (!valid) {
            return null;
        }

        return new ReadIntent(name, m_type, data, thumbnail);
    }
}
