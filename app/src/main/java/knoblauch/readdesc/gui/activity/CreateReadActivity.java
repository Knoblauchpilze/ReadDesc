package knoblauch.readdesc.gui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import knoblauch.readdesc.R;

public class CreateReadActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

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
     * Holds the properties used to reference graphic elements controlling the selection
     * of a thumbnail.
     */
    ThumbnailSelectionProps m_thumbnail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Create using the parent handler.
        super.onCreate(savedInstanceState);

        // Assign the main content.
        setContentView(R.layout.activity_create_read);

        // Retrieve views so that we can react to the actions of the user.
        m_readName = findViewById(R.id.new_read_name);

        m_fileProps = new CreationModeProps();
        m_websiteProps = new CreationModeProps();
        m_eBookProps = new CreationModeProps();

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

        // Register to relevant signals to be able to detect actions in the
        // interface.
        m_fileProps.active.setOnCheckedChangeListener(this);
        m_websiteProps.active.setOnCheckedChangeListener(this);
        m_eBookProps.active.setOnCheckedChangeListener(this);

        m_thumbnail.active.setOnCheckedChangeListener(this);
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
    }

}
