package knoblauch.readdesc.model;

import android.content.Context;
import android.content.res.Resources;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import knoblauch.readdesc.R;

public class ReadDesc {

    /**
     * Define the possible source types for a read. A read can have various
     * resources attached to it, each one supposing a certain way of access
     * to the content. This information is vital in order to fetch and use
     * the data in order to present it to the user in the application.
     */
    public enum Type {
        File,
        WebPage,
        EBook
    }

    /**
     * The identifier for this read. Allows to ensure that reads can have
     * similar names and yet still be considered unique. This is cool so
     * as to allow the user to not really care about the names given to
     * the reads, the system handles collisions in a transparent way.
     */
    private UUID m_uuid;

    /**
     * The name of the read: presents a user-defined strings identifying
     * the read in the UI. It is not used internally but rather as some
     * sort of visual aid for the user.
     */
    private String m_name;

    /**
     * The type of the source associated to this read. Depending on the
     * type of the original content describing the read the application
     * needs to perform different type of fetching and parsing before
     * printing the content to the user.
     */
    private Type m_type;

    /**
     * The source of the read. Presents a string identifying the read
     * as an external resource of this application. It is useful to
     * keep track of the original content of the read.
     */
    private String m_source;

    /**
     * The creation date of this read. Expressed using the default date
     * class, useful to display the time since the user created the read
     * or some other relevant statistics (like the time to read, etc.).
     */
    private Date m_creationDate;

    /**
     * A date representing the last time the user accessed this read to
     * progress in it. Is initialized at first with the creation date
     * and is then updated each time the user open this read.
     */
    private Date m_lastAccessDate;

    /**
     * A string representing the path to the thumbnail assigned to this
     * read. The path should reference a local file which the user has
     * defined when creating the read to easily identify it.
     * It can also be empty in case no thumbnail is associated yet.
     */
    private String m_thumbnail;

    /**
     * An indication of how far the user has progressed through this read.
     * This is initially set to `0` and advances each time the user selects
     * this element to read it.
     * The range for this value is `[0; 1]`.
     */
    private float m_completionPercentage;

    /**
     * Creates a new read with the specified name and source. The creation
     * date is set to now (as the last accessed date) and the completion is
     * set to `0`.
     * The user needs to specify the type of the source so as to know how to
     * fetch the data associated to this read. This information will help
     * to display and parse the content in a meaningful way.
     * @param name - the name of the read.
     * @param type - the type of the source: possible values are described by the
     *               related enumeration and helps fetching the content of the read.
     * @param source - a link to the source of the read.
     */
    private ReadDesc(String name, Type type, String source) {
        // Generate a random identifier for this read.
        m_uuid = UUID.randomUUID();

        // Initialize the read from input properties.
        m_name = name;
        m_type = type;
        m_source = source;

        // Assign default date.
        m_creationDate = new Date();
        m_lastAccessDate = m_creationDate;

        m_completionPercentage = 0.0f;

        m_thumbnail = null;
    }

    /**
     * Used to generate the name of the file where the data for a read named `name`
     * should be saved. This is especially useful for people trying to access the
     * data save for a given file.
     * @param context - the context to use to retrieve relevant information to use
     *                  to compute the read's save file name.
     * @param name - the name of the read.
     * @return - the name of the file where the data for the `read` is saved.
     */
    static String generateReadSaveName(Context context, String name) {
        // Generate the name of this read.
        Resources res = context.getResources();
        return String.format(res.getString(R.string.activity_read_save_name), name);
    }

    /**
     * Create a new read from the input intent. Some of the properties not defined in
     * the intent will be assigned an automatic value consistent with their purpose.
     * In case the `intent` is `null` the returned value is `null` as well.
     * @param intent - the intent to use to retrieve general properties of the read to
     *                 create.
     * @return - a read description created from the properties of the `intent`.
     */
    public static ReadDesc fromIntent(ReadIntent intent) {
        // In case the `intent` is `null`, return early.
        if (intent == null) {
            return null;
        }

        // Create the read description.
        ReadDesc desc = new ReadDesc(intent.getName(), intent.getType(), intent.getDataUri());

        // Assign the thumbnail path if any.
        String thumbnail = intent.getThumbnailUri();
        if (!thumbnail.isEmpty()) {
            desc.m_thumbnail = thumbnail;
        }

        return desc;
    }

    /**
     * Used to convert the current `ReadDesc` into a valid `ReadIntent` object. This
     * is useful in order to communicate information about a read to persist between
     * activities (typically when a read has been selected and it should be set up
     * for reading mode.
     * @return - a valid `ReadIntent` built from this `ReadDesc`.
     */
    public ReadIntent toReadIntent() {
        // Create the read from this object's properties.
        ReadIntent ri = new ReadIntent(getName(), getType(), getSource(), getThumbnailPath());

        // Assign the identifier.
        ri.setCompletion(m_completionPercentage);

        // We're good.
        return ri;
    }

    /**
     * Create a new read from the input file content. We suppose the file represents
     * a `XML` file describing the keys needed by the `ReadDesc` to fill in the props
     * internally.
     * In case the read content does not allow to build a valid read a `null` return
     * value is set. This indicates a failure to parse the file.
     * @param context - the context used to retrieve the keys to fetch the information
     *                  about the read in the `XML` file.
     * @param stream - the input stream containing the data for this read.
     * @return - the read description built from the file and `null` if a failure has
     *           occurred during the parsing.
     */
    static ReadDesc fromInputStream(Context context, InputStream stream) {
        // We will assume that the input `stream` describes a `XML` file reader from
        // which all properties defining the read can be retrieved. The keys to use
        // to serialize (and thus in this case de-serialize the content of the read
        // are defined in the `R` class. We will use it to instantiate the parser to
        // handle the `XML` document.
        ReadDesc desc = null;

        try {
            // Create the parser that we will use to read the `XML` file.
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            // Create the handler that will populate internal attributes in order
            // to create the `ReadDesc` later on. The parser itself is based on
            // the information found here:
            // https://mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
            ReadDescXMLParser handler = new ReadDescXMLParser(context);

            saxParser.parse(stream, handler);

            // In case we reach this point it means that the parsing went well and
            // that we now have all the needed information to create the `ReadDesc`.
            desc = new ReadDesc(handler.name, handler.type, handler.source);
            desc.m_uuid = handler.uuid;
            desc.m_creationDate = handler.creation;
            desc.m_lastAccessDate = handler.access;
            desc.m_completionPercentage = handler.completion;
            desc.m_thumbnail = handler.thumbnail;
        }
        catch (Exception e) {
            // In any case we failed to parse the file, this cannot result in valid
            // `ReadDesc` object.
        }

        return desc;
    }

    /**
     * Used to perform the dump of the content allowing to describe this read to the
     * provided stream. This will provide a syntax that can then be used to construct
     * a valid build through the `fromInputStream` method.
     * Failure to save the file will return `false`.
     * @param context - the context to use to retrieve the names of the keys to use
     *                  to save the properties of this read.
     * @param stream - the stream to which the content should be saved.
     * @return - `true` if the save operation was a success and `false` otherwise.
     */
    boolean save(Context context, OutputStreamWriter stream) {
        // In order to serialize this read we need to create a valid `XML` structure
        // and then perform the serialization of the content to the provided stream.
        Document xmlDoc;
        String str;

        // Retrieve props keys.
        Resources res = context.getResources();
        String rootKey = res.getString(R.string.activity_read_save_xml_key_root);

        String uuidKey = res.getString(R.string.activity_read_save_xml_key_uuid);
        String nameKey = res.getString(R.string.activity_read_save_xml_key_name);
        String typeKey = res.getString(R.string.activity_read_save_xml_key_type);
        String sourceKey = res.getString(R.string.activity_read_save_xml_key_source);
        String creationKey = res.getString(R.string.activity_read_save_xml_key_created_at);
        String accessedKey = res.getString(R.string.activity_read_save_xml_key_last_accessed);
        String completionKey = res.getString(R.string.activity_read_save_xml_key_completion);
        String thumbnailKey = res.getString(R.string.activity_read_save_xml_key_thumbnail);

        // Build the `XML` structure. The following resource proved very useful for
        // providing insights about this:
        // https://stackoverflow.com/questions/23520208/how-to-create-xml-file-with-specific-structure-in-java
        try {
            // Create the factory to build the `XML` structure.
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Create the document.
            xmlDoc = docBuilder.newDocument();

            // Register the root element.
            Element root = xmlDoc.createElement(rootKey);
            xmlDoc.appendChild(root);

            // Save the uuid of the read.
            Element uuid = xmlDoc.createElement(uuidKey);
            uuid.appendChild(xmlDoc.createTextNode(m_uuid.toString()));
            root.appendChild(uuid);

            // Save the name of the read.
            Element name = xmlDoc.createElement(nameKey);
            name.appendChild(xmlDoc.createTextNode(m_name));
            root.appendChild(name);

            // Save the type of the read.
            Element type = xmlDoc.createElement(typeKey);
            type.appendChild(xmlDoc.createTextNode(m_type.name()));
            root.appendChild(type);

            // Save the source of the read.
            Element source = xmlDoc.createElement(sourceKey);
            source.appendChild(xmlDoc.createTextNode(m_source));
            root.appendChild(source);

            // Save the creation date of the read.
            str = String.valueOf(m_creationDate.getTime());
            Element creation = xmlDoc.createElement(creationKey);
            creation.appendChild(xmlDoc.createTextNode(str));
            root.appendChild(creation);

            // Save the last access date of the read.
            str = String.valueOf(m_lastAccessDate.getTime());
            Element access = xmlDoc.createElement(accessedKey);
            access.appendChild(xmlDoc.createTextNode(str));
            root.appendChild(access);

            // Save the completion percentage.
            str = String.valueOf(m_completionPercentage);
            Element completion = xmlDoc.createElement(completionKey);
            completion.appendChild(xmlDoc.createTextNode(str));
            root.appendChild(completion);

            // Save the thumbnail if needed.
            if (hasThumbnail()) {
                Element thumbnail = xmlDoc.createElement(thumbnailKey);
                thumbnail.appendChild(xmlDoc.createTextNode(m_thumbnail));
                root.appendChild(thumbnail);
            }
        }
        catch (Exception e) {
            // Failed to build the read's `XML` structure, nothing to expect from the
            // serialization.
            return false;
        }

        // Now that we have a valid `XML` structure we can serialize it to the disk.
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            DOMSource source = new DOMSource(xmlDoc);

            StreamResult result = new StreamResult(stream);

            // Write the XML to file
            transformer.transform(source, result);
        }
        catch (Exception e) {
            // Catching an error cannot possibly mean that we succeeded in serializing
            // the data.
            return false;
        }

        // Successfully serialized this read to the provided location.
        return true;
    }

    /**
     * Used to refresh the completion percentage for this read based on the
     * value stored in the local storage file. This is usually triggered if
     * the read's local description has been updated by external activities.
     * @param context - the context to use to retrieve information from the
     *                  local storage's space for this read.
     * @return - `true` if the read has been updated and `false` otherwise.
     */
    boolean refresh(Context context) {
        // Create parser from the local file for this read and retrieve
        // the completion percentage and the last accessed date.

        try {
            // Create the parser that we will use to read the `XML` file.
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            String name = generateReadSaveName(context, getName());

            // Open the instance of the read saved to local storage.
            FileInputStream stream;
            try {
                stream = context.openFileInput(name);
            }
            catch (FileNotFoundException e) {
                // We won't update the read's completion percentage.
                return false;
            }

            // Create the handler to parse the `XML` file for this read.
            ReadDescXMLParser handler = new ReadDescXMLParser(context);

            saxParser.parse(stream, handler);

            // We successfully parsed the file, update the completion percentage
            // and the last accessed date.
            m_completionPercentage = handler.completion;
            m_lastAccessDate = handler.access;
        }
        catch (Exception e) {
            // We most certainly did not successfully updated anything.
            return false;
        }

        // We successfully updated the completion percentage.
        return true;
    }

    /**
     * Retrieves the name associated to this read.
     * @return - the name of this read (as defined by the user).
     */
    public String getName() { return m_name; }

    /**
     * Return the type of the source associated to this read. The type can be
     * used to display some sort of information about how the data is fetched.
     * @return - the type of this read.
     */
    private Type getType() {
        return m_type;
    }

    /**
     * The source of the read as a string. Refers to the external resource
     * containing the data for this read.
     * @return - the string representing the source for this read.
     */
    public String getSource() {
        return m_source;
    }

    /**
     * Retrieves the creation date of this read as a `Date` object.
     * @return - the creation date of this read.
     */
    Date getCreationDate() {
        return m_creationDate;
    }

    /**
     * Returns the last access date for this read. Should usually be more
     * recent than the creation date.
     * @return - the last time the user accessed this read.
     */
    public Date getLastAccessedDate() {
        return m_lastAccessDate;
    }

    /**
     * Used to determine whether this read has an attached thumbnail path or
     * not.
     * @return - `true` if the `getThumbnailPath` returns a valid value and
     *           `false` otherwise.
     */
    public boolean hasThumbnail() { return getThumbnailPath() != null; }

    /**
     * Returns the possibly empty path to the thumbnail. In case no thumbnail
     * is attached to this read the returned value is `null`. One can check
     * whether the thumbnail exists through the `hasThumbnail` method.
     */
    public String getThumbnailPath() { return m_thumbnail; }

    /**
     * Returns the completion percentage of this read. Represents how far
     * the user has progressed in the content of this read. Note that this
     * method returns a value in the range `[0; 100]`.
     * @return - the completion percentage of this read.
     */
    public float getCompletionPercentage() {
        return 100.0f * Math.max(Math.min(m_completionPercentage, 1.0f), 0.0f);
    }

    /**
     * Assign a new value for the progression of this read. This is usually
     * used by the parser allowing to advance on the read when the user has
     * finished or left the reading activity.
     * Note that the input completion is clamped in the range `[0; 1]`.
     * @param completion - the new completion for this read.
     */
    void setProgression(float completion) {
        m_completionPercentage = Math.max(Math.min(completion, 1.0f), 0.0f);
    }

    /**
     * Used to update the last access date for this read to the current time. It
     * is usually meant to indicate that the read has been accessed recently so
     * that we can persist this information.
     */
    void touch() {
        m_lastAccessDate = new Date();
    }
}
