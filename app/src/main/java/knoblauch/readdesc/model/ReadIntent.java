package knoblauch.readdesc.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

public class ReadIntent implements Parcelable {

    /**
     * Holds the desired name for the read created from this intent.
     */
    private String m_name;

    /**
     * The type of the read to create from this intent. Will help to
     * interpret correctly the `uri` path for this element.
     */
    private ReadDesc.Type m_type;

    /**
     * The path describing the resource associated to the read. This
     * can either be a web page, a file or anything else as lons as
     * it is consistent with the `m_type` of the resource.
     */
    private String m_dataUri;

    /**
     * A path describing the resource representing the thumbnail for
     * this read. Note that this value may be empty in which case the
     * user did not want to assign any image to the read.
     */
    private String m_thumbnailUri;

    /**
     * The identifier of the read associated to this read intent. Note
     * that this value is set to some randomly generated identifier on
     * creating this object but can be modified through the `setUuid`
     * method (typically if the read intent is created from an existing
     * read.
     */
    private UUID m_uuid;

    /**
     * Create a new intent with the specified properties. Note that this
     * does not in any case change the reads actually existing in the model.
     * It only indicates an intent to create the read.
     * @param name - the desired name of the read.
     * @param type - the desired type for this read.
     * @param dataUri - the uri of the data associated to the read.
     * @param thumbnailUri - a possibly empty string describing the desired
     *                       resource associated to this read.
     */
    public ReadIntent(String name, ReadDesc.Type type, String dataUri, String thumbnailUri) {
        m_name = name;
        m_type = type;
        m_dataUri = dataUri;

        m_thumbnailUri = thumbnailUri;

        m_uuid = UUID.randomUUID();
    }

    /**
     * Create a valid read intent by deserializing the content defined in the
     * input parcel. This will populate internal fields from it.
     * @param in - the parcel from which the internal fields should be filled.
     */
    private ReadIntent(Parcel in) {
        m_name = in.readString();
        m_type = ReadDesc.Type.valueOf(in.readString());
        m_dataUri = in.readString();
        m_thumbnailUri = in.readString();
        m_uuid = UUID.fromString(in.readString());
    }

    /**
     * Create a valid `ReadIntent` from the input parcel.
     */
    public static final Parcelable.Creator<ReadIntent> CREATOR = new Parcelable.Creator<ReadIntent>() {
        public ReadIntent createFromParcel(Parcel in) {
            return new ReadIntent(in);
        }

        public ReadIntent[] newArray(int size) {
            return new ReadIntent [size];
        }
    };

    /**
     * Used to retrieve the desired name of the read to create.
     * @return - the name of the read.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Used to retrieve the desired type for this read.
     * @return - the type of the read.
     */
    public ReadDesc.Type getType() {
        return m_type;
    }

    /**
     * Used to retrieve the desired uri to the resource containing the data
     * for this read.
     * @return - the string representing the data for this read.
     */
    String getDataUri() {
        return m_dataUri;
    }

    /**
     * Used to retrieve the (possibly empty) uri for the resource describing
     * the thumbnail to associate to the read.
     * @return - the string of the thumbnail for this read.
     */
    String getThumbnailUri() {
        return m_thumbnailUri;
    }

    /**
     * Used to retrieve the identifier associated to this read intent. Typically
     * auto-generated but can be set through the `setUuid` method.
     * @return - the identifier associated to this read intent.
     */
    UUID getUuid() { return m_uuid; }

    /**
     * Define a new identifier for this read intent.
     * @param uuid - the new identifier to assign.
     */
    void setUuid(UUID uuid) { m_uuid = uuid; }

    @Override
    public int describeContents() {
        // This class does not define any file descriptor.
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Write internal fields to the destination.
        dest.writeString(m_name);
        dest.writeString(m_type.name());
        dest.writeString(m_dataUri);
        dest.writeString(m_thumbnailUri);
        dest.writeString(m_uuid.toString());
    }


}
