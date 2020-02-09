package knoblauch.readdesc.model;

import java.util.Date;
import java.util.UUID;

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
     * Retrieves the name associated to this read.
     * @return - the name of this read (as defined by the user).
     */
    public String getName() { return m_name; }

    /**
     * Return the type of the source associated to this read. The type can be
     * used to display some sort of information about how the data is fetched.
     * @return - the type of this read.
     */
    public Type getType() {
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
    public Date getCreationDate() {
        return m_creationDate;
    }

    /**
     * Returns the last access date for this read. Should usually be more
     * recent than the creation date.
     * @return - the last time the suer accessed this read.
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
     * Used to determine whether the user has finished this read or not. It
     * queries the completion percentage and check whether it is set to 100%.
     * @return - `true` if the user has reached the end of this read and `false` if
     *           this is not the case.
     */
    public boolean isCompleted() {
        return getCompletionPercentage() >= 100.0f;
    }
}
