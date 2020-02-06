package knoblauch.readdesc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReadsBank {

    /**
     * @brief - Describe the ordering to apply to the reads inside this bank. This means that
     *          upon loading all the reads they will be organized using the specified value.
     *          It helps having some nice filtering options when displaying all the reads that
     *          the user created.
     */
    public enum Ordering {
        Alphabetical,
        LastAccessed,
        CreationDate
    }

    /**
     * @brief - Internal utility class used whenever the collection of reads should be sorted.
     *          It uses the provided ordering to compare reads between each other.
     */
    private class ReadComparator implements Comparator<ReadDesc> {

        /**
         * @brief - The ordering to apply when comparing reads.
         */
        private Ordering m_order;

        /**
         * @brief - Used to create a new comparator with the specified ordering. This value is
         *          applied when comparing reads.
         * @param order - the ordering to apply to compare reads.
         */
        ReadComparator(Ordering order) {
            m_order = order;
        }

        @Override
        public int compare(ReadDesc o1, ReadDesc o2) {
            // We need to return:
            //   - `< 0` if `o1 < o2`
            //   - `= 0` if `o1 == o2`
            //   - `> 0` if `o1 > o2`
            // We will use the type of ordering to perform the comparison on the required field
            // of the provided reads.
            switch (m_order) {
                case LastAccessed:
                    return o1.getLastAccessedDate().compareTo(o2.getLastAccessedDate());
                case CreationDate:
                    return o1.getCreationDate().compareTo(o1.getCreationDate());
                case Alphabetical:
                default:
                    // Assume alphabetical order in case the ordering is unknown.
                    return o1.getName().compareToIgnoreCase(o2.getName());
            }
        }
    }

    /**
     * @brief - Describe the available reads in the application. This list is populated with
     *          the data from previous execution where the user might have added some reads
     *          from various resources. This is the main resource of the application and is
     *          used for virtually any data displayed in activities.
     */
    private List<ReadDesc> m_reads;

    /**
     * @brief - Temporary variable allowing to store how many reads should be created upon
     *          creating such an object.
     *          TODO: Remove this ASAP.
     */
    private int m_count;

    /**
     * @brief - Used to describe the local ordering applied to the reads. This ordering is
     *          also reflected when picking a read with the `getRead` method so it usually
     *          is translated into visual ordering of the components.
     */
    private Ordering m_ordering;

    /**
     * @brief - Perform the load of the reads descriptions in the internal collection from
     *          the storage location. This usually means fetching data from local disk and
     *          retrieving the last state of each one of them.
     */
    private void load() {
        // Allocate the internal reads array.
        m_reads = new ArrayList<>();

        // Create as many reads as needed.
        for (int id = 0 ; id < m_count ; ++id) {
            // Generate a random name.
            String name = "Generated read " + id;

            // Generate a type.
            ReadDesc.Type type = ReadDesc.Type.File;
            if (id % 3 == 1) {
                type = ReadDesc.Type.Webpage;
            }
            else if (id % 3 == 2) {
                type = ReadDesc.Type.Ebook;
            }

            // Generate the source from the type.
            String source;
            switch (type) {
                case Webpage:
                    source = "Website " + id;
                    break;
                case Ebook:
                    source = "E-Book " + id;
                    break;
                default:
                case File:
                    source = "File " + id;
                    break;
            }

            m_reads.add(new ReadDesc(name, type, source));
        }

        // We need to sort the reads based on the internal ordering.
        orderReads();
    }

    /**
     * @brief - Performs the ordering of the reads using the internal `m_order` value.
     *          Assumes that the reads are already loaded and does nothing to do so.
     *          The internal `m_reads` collection is modified to reflect the desired
     *          ordering.
     */
    private void orderReads() {
        // Sort through the generic algorithm.
        Collections.sort(m_reads, new ReadComparator(m_ordering));
    }

    /**
     * @brief - Create a new reads bank with the specified number of reads to create. For now
     *          it is not populated with actual data but this should be changed ASAP (i.e. as
     *          soon as we learn how to actually persist information to the disk).
     * @param order - the order to apply to reads upon being loaded. This will also be applied
     *                when fetching a read through the `getRead` interface so it usually also
     *                translate into visual ordering of the reads.
     * @param count - the number of reads to create when building this bank.
     */
    public ReadsBank(Ordering order, int count) {
        // Set the ordering to respect when loading reads.
        m_ordering = order;

        // Initialize the count.
        m_count = Math.max(0, count);

        // Load all the available reads.
        load();
    }

    /**
     * @brief - Used to retrieve the read description at index `id`. Note that if there
     *          are not enough reads in this bank to satisfy the requested index a `null`
     *          value is returned. Similarly if the provided index is negative `null` is
     *          also returned.
     *          Otherwise the read description corresponding to the `id` slot is returned
     *          when sorting using the provided ordering.
     * @param id - the index of the read to retrieve.
     * @return - `null` if the provided `id` does not correspond to any valid read or the
     *           corresponding read if available.
     */
    public ReadDesc getRead(int id) {
        // Prevent invalid access to the internal reads collection.
        if (id < 0 || id >= m_reads.size()) {
            return null;
        }

        // The required `id` can be satisfied, use it.
        return m_reads.get(id);
    }

    /**
     * @brief - Return the current number of reads registered in this bank.
     * @return - the number of reads registered in this object.
     */
    public int size() {
        return m_reads.size();
    }

    /**
     * @brief - Attempt to remove the input read from the internal collection. In case the
     *          read is invalid or cannot be found in this bank this method returns `false`.
     * @param desc - the read description to remove.
     * @return - `true` if the read could be found and was successfully removed and `false`
     *           otherwise.
     */
    public boolean remove(ReadDesc desc) {
        // In case the input read is `null` do nothing.
        if (desc == null) {
            return false;
        }

        // Otherwise attempt to remove the read from the list.
        return m_reads.remove(desc);
    }
}