package knoblauch.readdesc.model;

import android.content.Context;
import android.content.res.Resources;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Date;
import java.util.UUID;

import knoblauch.readdesc.R;

class ReadDescXMLParser extends DefaultHandler {

    /**
     * Describes the potential attribute that can be retrieved from the
     * `XML` file.
     */
    enum Key {
        UUID,
        Name,
        Type,
        Source,
        CreationDate,
        AccessedDate,
        Completion,
        Thumbnail,
        None
    }

    /**
     * The context allowing to retrieve the name of the keys to use to get
     * information from the `XML` file.
     */
    private Context m_context;

    /**
     * The key currently being built by the `XML` parser. The `None` value
     * indicates that nothing is currently parsed.
     */
    private Key m_key;

    /**
     * Holds the uuid for the read.
     */
    UUID uuid;

    /**
     * Holds the parsed name for the read.
     */
    String name;

    /**
     * Holds the parsed type for the read.
     */
    ReadDesc.Type type;

    /**
     * Holds the parsed source for the read.
     */
    String source;

    /**
     * Holds the value representing the creation date for this read.
     */
    Date creation;

    /**
     * Holds the parsed date representing the last access to this read.
     */
    Date access;

    /**
     * Holds the parsed completion percentage for the read.
     */
    float completion;

    /**
     * Holds the parsed value for the thumbnail of this read. May be `null`
     * in case no thumbnail is defined for this read.
     */
    String thumbnail;

    /**
     * Create a parser with the specified context which allows to retrieve
     * the name of the keys to use to represent the properties of the read
     * in the `XML` file.
     * @param context - a context allowing to retrieve keys to use to fetch
     *                  info from the `XML` file.
     */
    ReadDescXMLParser(Context context) {
        // Initialize the parser.
        m_context = context;
        m_key = Key.None;

        thumbnail = null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // Detect which kind of element is active for now.
        Resources res = m_context.getResources();

        String uuidKey = res.getString(R.string.read_desc_xml_key_uuid);
        String nameKey = res.getString(R.string.read_desc_xml_key_name);
        String typeKey = res.getString(R.string.read_desc_xml_key_type);
        String sourceKey = res.getString(R.string.read_desc_xml_key_source);
        String creationKey = res.getString(R.string.read_desc_xml_key_creation_date);
        String accessedKey = res.getString(R.string.read_desc_xml_key_accessed_date);
        String completionKey = res.getString(R.string.read_desc_xml_key_completion);
        String thumbnailKey = res.getString(R.string.read_desc_xml_key_thumbnail);

        if (qName.equals(uuidKey)) {
            m_key = Key.UUID;
        }
        else if (qName.equals(nameKey)) {
            m_key = Key.Name;
        }
        else if (qName.equals(typeKey)) {
            m_key = Key.Type;
        }
        else if (qName.equals(sourceKey)) {
            m_key = Key.Source;
        }
        else if (qName.equals(creationKey)) {
            m_key = Key.CreationDate;
        }
        else if (qName.equals(accessedKey)) {
            m_key = Key.AccessedDate;
        }
        else if (qName.equals(completionKey)) {
            m_key = Key.Completion;
        }
        else if (qName.equals(thumbnailKey)) {
            m_key = Key.Thumbnail;
        }

        // Use base handler to perform needed operations.
        super.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // We stopped to parse any key we were parsing until then.
        m_key = Key.None;

        // Use the base handler to perform needed operations.
        super.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // Check whether we're parsing a known tag.
        if (m_key != Key.None) {
            // Convert input sequence to a string.
            String str = new String(ch, start, length);

            // Try to convert the string into a valid number if needed.
            long time = new Date().getTime();
            if (m_key == Key.CreationDate || m_key == Key.AccessedDate) {
                time = Long.valueOf(str);
            }

            float perc = 0.0f;
            if (m_key == Key.Completion) {
                perc = Float.valueOf(str);
            }

            // Save this value in the correct attribute.
            switch (m_key) {
                case UUID:
                    uuid = UUID.fromString(str);
                    break;
                case Name:
                    name = str;
                    break;
                case Type:
                    type = ReadDesc.Type.valueOf(str);
                    break;
                case Source:
                    source = str;
                    break;
                case CreationDate:
                    creation = new Date(time);
                    break;
                case AccessedDate:
                    access = new Date(time);
                    break;
                case Completion:
                    completion = perc;
                    break;
                case Thumbnail:
                    thumbnail = str;
                    break;
            }
        }

        // Use the base handler to move in the character's sequence.
        super.characters(ch, start, length);
    }
}