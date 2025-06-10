package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * Tuple maintains information about the contents of a tuple. Tuples have a
 * specified schema specified by a TupleDesc object and contain Field objects
 * with the data for each field.
 */
public class Tuple implements Serializable {

    private static final long serialVersionUID = 1L;

    // Schema of the tuple
    private TupleDesc tupleDesc;

    // Fields of the tuple
    private Field[] fields;

    // Record ID representing the location of this tuple on disk
    private RecordId recordId;

    /**
     * Create a new tuple with the specified schema (type).
     *
     * @param td the schema of this tuple. It must be a valid TupleDesc
     *           instance with at least one field.
     */
    public Tuple(TupleDesc td) {
        // Validate the TupleDesc before initializing the tuple
        if (td == null || td.numFields() == 0) {
            throw new IllegalArgumentException("TupleDesc must have at least one field.");
        }
        this.tupleDesc = td;

        // Initialize the fields array based on the number of fields in the TupleDesc
        this.fields = new Field[td.numFields()];
    }

    /**
     * @return The TupleDesc representing the schema of this tuple.
     */
    public TupleDesc getTupleDesc() {
        return this.tupleDesc;
    }

    /**
     * @return The RecordId representing the location of this tuple on disk. May
     * be null.
     */
    public RecordId getRecordId() {
        return this.recordId;
    }

    /**
     * Set the RecordId information for this tuple.
     *
     * @param rid the new RecordId for this tuple.
     */
    public void setRecordId(RecordId rid) {
        this.recordId = rid;
    }

    /**
     * Change the value of the ith field of this tuple.
     *
     * @param i index of the field to change. It must be a valid index.
     * @param f new value for the field.
     */
    public void setField(int i, Field f) {
        // Validate the field index
        if (i < 0 || i >= fields.length) {
            throw new IllegalArgumentException("Field index out of bounds.");
        }
        fields[i] = f; // Update the value at the specified index
    }

    /**
     * @return the value of the ith field, or null if it has not been set.
     *
     * @param i field index to return. Must be a valid index.
     */
    public Field getField(int i) {
        // Validate the field index
        if (i < 0 || i >= fields.length) {
            throw new IllegalArgumentException("Field index out of bounds.");
        }
        return fields[i]; // Return the value of the field at the specified index
    }

    /**
     * Returns the contents of this Tuple as a string. Note that to pass the
     * system tests, the format needs to be as follows:
     *
     * column1\tcolumn2\tcolumn3\t...\tcolumnN
     *
     * where \t is any whitespace (except a newline)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // Iterate over all fields to build the tab-separated string representation
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append("\t"); // Add tab as a separator between fields
            sb.append(fields[i] == null ? "null" : fields[i].toString());
        }
        return sb.toString();
    }

    /**
     * @return An iterator which iterates over all the fields of this tuple
     */
    public Iterator<Field> fields() {
        // Use Arrays.asList to create a list view of the fields array
        return Arrays.asList(fields).iterator();
    }

    /**
     * Reset the TupleDesc of this tuple (only affecting the TupleDesc).
     *
     * @param td the new TupleDesc for this tuple.
     */
    public void resetTupleDesc(TupleDesc td) {
        // Validate the new TupleDesc
        if (td == null || td.numFields() == 0) {
            throw new IllegalArgumentException("TupleDesc must have at least one field.");
        }

        // Update the schema and reset the fields array based on the new schema
        this.tupleDesc = td;
        this.fields = new Field[td.numFields()];
    }
}
