package knoblauch.readdesc.model;

import java.util.Date;

public class ReadDesc {

    /**
     * Define the possible source types for a read. A read can have various
     * resources attached to it, each one supposing a certain way of access
     * to the content. This information is vital in order to fetch and use
     * the data in order to present it to the user in the application.
     */
    public enum Type {
        File,
        Webpage,
        Ebook
    }

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
    ReadDesc(String name, Type type, String source) {
        // Initialize the read from input properties.
        m_name = name;
        m_type = type;
        m_source = source;

        // Assign default date.
        m_creationDate = new Date();
        m_lastAccessDate = m_creationDate;

        m_completionPercentage = 0.0f;
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
