package knoblauch.readdesc.model;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import knoblauch.readdesc.R;

public class ReadsBank {

    /**
     * Describe the ordering to apply to the reads inside this bank. This means that
     * upon loading all the reads they will be organized using the specified value.
     * It helps having some nice filtering options when displaying all the reads that
     * the user created.
     */
    public enum Ordering {
        Alphabetical,
        LastAccessed,
        CreationDate
    }

    /**
     * Internal utility class used whenever the collection of reads should be sorted.
     * It uses the provided ordering to compare reads between each other.
     */
    private class ReadComparator implements Comparator<ReadDesc> {

        /**
         * The ordering to apply when comparing reads.
         */
        private Ordering m_order;

        /**
         * Used to create a new comparator with the specified ordering. This value is
         * applied when comparing reads.
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
                    return o1.getCreationDate().compareTo(o2.getCreationDate());
                case Alphabetical:
                default:
                    // Assume alphabetical order in case the ordering is unknown.
                    return o1.getName().compareToIgnoreCase(o2.getName());
            }
        }
    }

    /**
     * The context providing access to the data already saved by this application if
     * any regarding reads already created by the user.
     */
    private Context m_context;

    /**
     * Describe the available reads in the application. This list is populated with
     * the data from previous execution where the user might have added some reads
     * from various resources. This is the main resource of the application and is
     * used for virtually any data displayed in activities.
     */
    private List<ReadDesc> m_reads;

    /**
     * Used to describe the local ordering applied to the reads. This ordering is
     * also reflected when picking a read with the `getRead` method so it usually
     * is translated into visual ordering of the components.
     */
    private Ordering m_ordering;

    /**
     * Create a new reads bank with the specified number of reads to create.
     * @param context - the context to use to get access to the data saved by the
     *                  app locally on a previous execution.
     * @param order - the order to apply to reads upon being loaded. This will
     *                also be applied when fetching a read through the `getRead`
     *                interface so it usually also translate into visual
     *                ordering of the reads.
     */
    public ReadsBank(Context context, Ordering order) {
        // Assign the context to use to perform the loading.
        m_context = context;

        // Set the ordering to respect when loading reads.
        m_ordering = order;

        // Load all the available reads.
        load();
    }

    /**
     * Perform the load of the reads descriptions in the internal collection from
     * the storage location. This usually means fetching data from local disk and
     * retrieving the last state of each one of them.
     */
    private void load() {
        // Retrieve the data from the dedicated internal storage containing the
        // existing reads. To do so we need to retrieve the resources manager
        // from the internal context.
        File appDir = m_context.getFilesDir();

        // Retrieve the list of the files registered in the directory: this will
        // correspond to the existing reads.
        File[] reads = appDir.listFiles();

        // Check whether this directory exists: if this is not the case we don't
        // need to bother with loading reads from it.
        if (reads == null) {
            return;
        }

        // Allocate the internal reads array.
        m_reads = new ArrayList<>();

        Resources res = m_context.getResources();
        String msg = res.getString(R.string.read_desc_failure_load_file);

        // Register each read.
        for (File read : reads) {
            // Open the read's description.
            FileInputStream fis;
            try {
                fis = m_context.openFileInput(read.getName());
            }
            catch (FileNotFoundException e) {
                // Do not load this read for now.
                Toast.makeText(m_context, String.format(msg, read.getName()), Toast.LENGTH_SHORT).show();

                continue;
            }

            // Open the file and prepare to parse it.
            InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
            StringBuilder stringBuilder = new StringBuilder();

            // Read the file line by line.
            String content;

            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String line = reader.readLine();
                while (line != null) {
                    stringBuilder.append(line).append('\n');
                    line = reader.readLine();
                }
            }
            catch (IOException e) {
                // Error occurred when opening raw file for reading.
                Toast.makeText(m_context, String.format(msg, read.getName()), Toast.LENGTH_SHORT).show();
            }
            finally {
                content = stringBuilder.toString();
            }

            if (!loadReadFromContent(content)) {
                Toast.makeText(m_context, String.format(msg, read.getName()), Toast.LENGTH_SHORT).show();
            }
        }

        // We need to sort the reads based on the internal ordering.
        orderReads();
    }

    /**
     * Used to perform the loading of the file descibred by the input content and add
     * it to the internal list of reads. The return value allows to indicate whether
     * we could successfully load the file or not.
     * @param content - the content of the file to load.
     * @return - `true` if the content was successfully transformed into a read and
     *           `false` otherwise.
     */
    private boolean loadReadFromContent(String content) {
        Log.i("main", "Content is \"" + content + "\"");

        // TODO: Handle loading of the content of the read file.
        return false;
    }

    /**
     * Used to perform the save of the reads contained in this bank to a local file
     * intended for this application's usage only. This will allow to retrieve the
     * data created by the user in future launches.
     */
    public void save() {
        Resources res = m_context.getResources();

        // We need to save each created reads to local storage. Let's start.
        for (ReadDesc read : m_reads) {
            // Try to save the read to local storage and remember any failure.
            if (!saveRead(read)) {
                String msg = String.format(res.getString(R.string.read_desc_failure_save_file), read.getName());
                Toast.makeText(m_context, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Used to handle the saving of the input read. We check local storage in case
     * the file corresponding to this read already exist in which case we update it
     * and we create it if this is not the case.
     * @param read - the read to save to local storage.
     * @return - `true` if the file could successfully be saved and `false` otherwise.
     */
    private boolean saveRead(ReadDesc read) {
        // Generate the name of this read.
        Resources res = m_context.getResources();
        String name = String.format(res.getString(R.string.read_desc_save_file_name), read.getName());

        // Check whether the file for this read exists in local storage.
        File out = new File(m_context.getFilesDir(), name);

        boolean success = false;

        if(!out.exists()){
            try {
                success = out.createNewFile();
            }
            catch (IOException e) {
                // We couldn't save the file.
            }
        }

        // Check whether the creation of the file was successful.
        if (!success) {
            return false;
        }

        // Dump the content of the file if we successfully created it.
        try{
            success = saveReadToContent(read, out);
        }
        catch (Exception e){
            // Any error result in the failure to save the read.
        }

        // We successfully saved the read to local storage.
        return success;
    }

    /**
     * Used to perform the dump of the input read to the specified file. In case the
     * file already exists this method actually handle the modification of the data
     * so that it stays consistent and create everything that's needed in case said
     * file is empty.
     * The return value allows to indicate whether we could successfully load the
     * file or not.
     * @param desc - the read to save to the file.
     * @param out - the output file to which the content of the `read` is to be saved.
     * @return - `true` if the content was successfully transformed into a file and
     *           `false` otherwise.
     */
    private boolean saveReadToContent(ReadDesc desc, File out) throws IOException {
        Log.i("main", "Should save \"" + desc.getName() + "\" to \"" + out.getName() + "\"");

        // Create a writer for this file.
        FileWriter writer = new FileWriter(out);

        // Append the body of the file.
        // TODO: Handle loading of the content of the read file.
        writer.append("Hello world !");

        // Close the file after writing it.
        writer.flush();
        writer.close();

        // We successfully saved the content of the file.
        return true;
    }

    /**
     * Performs the ordering of the reads using the internal `m_order` value.
     * Assumes that the reads are already loaded and does nothing to do so.
     * The internal `m_reads` collection is modified to reflect the desired
     * ordering.
     */
    private void orderReads() {
        // Sort through the generic algorithm.
        Collections.sort(m_reads, new ReadComparator(m_ordering));
    }

    /**
     * Used to retrieve the read description at index `id`. Note that if there
     * are not enough reads in this bank to satisfy the requested index a `null`
     * value is returned. Similarly if the provided index is negative `null` is
     * also returned.
     * Otherwise the read description corresponding to the `id` slot is returned
     * when sorting using the provided ordering.
     * @param id - the index of the read to retrieve.
     * @return - `null` if the provided `id` does not correspond to any valid
     *           read or the corresponding read if available.
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
     * Return the current number of reads registered in this bank.
     * @return - the number of reads registered in this object.
     */
    public int size() {
        return m_reads.size();
    }

    /**
     * Attempt to remove the input read from the internal collection. In case the
     * read is invalid or cannot be found in this bank this method returns `false`.
     * @param desc - the read description to remove.
     * @return - `true` if the read could be found and was successfully removed and
     *           `false` otherwise.
     */
    public boolean remove(ReadDesc desc) {
        // In case the input read is `null` do nothing.
        if (desc == null) {
            return false;
        }

        // Otherwise attempt to remove the read from the list.
        return m_reads.remove(desc);
    }

    /**
     * Attempt to insert the input read to the internal collection. Note that as
     * we're assigning some sort of identifier to the read we don't have much risk
     * of a name collision.
     * @param desc - the read description to register in this bank.
     */
    public boolean add(ReadDesc desc) {
        // Check consistency.
        if (desc == null) {
            return false;
        }

        // Insert the read into the internal collection.
        m_reads.add(desc);

        // Keep elements sorted in the right order.
        orderReads();

        // We successfully inserted the read.
        return true;
    }

}
