package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 * A tuple schema defines the types and optional names of fields in a database tuple.
 */
public class TupleDesc implements Serializable {

    // Unique identifier for serialization compatibility.
    private static final long serialVersionUID = 1L;

    // List of TDItem objects that store metadata about fields in the tuple schema
    private final List<TDItem> fields;

    /**
     * A helper class to encapsulate metadata about each field in the tuple.
     */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The data type of the field (e.g., integer, string).
         */
        public final Type fieldType;

        /**
         * The name of the field (can be null for anonymous fields).
         */
        public final String fieldName;

        // Constructor to initialize field type and name
        public TDItem(Type t, String n) {
            this.fieldType = t;
            this.fieldName = n;
        }

        /**
         * @return A string representation of the field, including its type and name.
         */
        public String toString() {
            return fieldType + "(" + fieldName + ")";
        }
    }

    /**
     * @return An iterator over all field metadata (TDItem objects) in this TupleDesc.
     */
    public Iterator<TDItem> iterator() {
        return fields.iterator();
    }

    /**
     * Constructor to create a TupleDesc with specified types and names for fields.
     *
     * @param typeAr  Array specifying the types of fields. Must have at least one element.
     * @param fieldAr Array specifying the names of fields. Can be null or shorter than typeAr.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        // Validate that at least one field type is provided
        if (typeAr == null || typeAr.length == 0) {
            throw new IllegalArgumentException("Type array must contain at least one element.");
        }

        // Initialize the fields list and populate it with TDItems
        fields = new ArrayList<>();
        for (int i = 0; i < typeAr.length; i++) {
            fields.add(new TDItem(typeAr[i], (fieldAr != null && i < fieldAr.length) ? fieldAr[i] : null));
        }
    }

    /**
     * Constructor to create a TupleDesc with specified types and anonymous fields.
     *
     * @param typeAr Array specifying the types of fields.
     */
    public TupleDesc(Type[] typeAr) {
        // Delegates to the primary constructor with null field names
        this(typeAr, new String[typeAr.length]);
    }

    /**
     * @return The number of fields in the TupleDesc.
     */
    public int numFields() {
        return fields.size();
    }

    /**
     * Retrieves the name of the specified field.
     *
     * @param i Index of the field (must be valid).
     * @return The name of the field or null if unnamed.
     * @throws NoSuchElementException If the index is out of range.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        if (i < 0 || i >= fields.size()) {
            throw new NoSuchElementException("Invalid field index: " + i);
        }
        return fields.get(i).fieldName;
    }

    /**
     * Retrieves the type of the specified field.
     *
     * @param i Index of the field (must be valid).
     * @return The data type of the field.
     * @throws NoSuchElementException If the index is out of range.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        if (i < 0 || i >= fields.size()) {
            throw new NoSuchElementException("Invalid field index: " + i);
        }
        return fields.get(i).fieldType;
    }

    /**
     * Finds the index of the first field with the specified name.
     *
     * @param name Name of the field to search for.
     * @return The index of the field.
     * @throws NoSuchElementException If no field with the given name exists.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
        for (int i = 0; i < fields.size(); i++) {
            if (Objects.equals(fields.get(i).fieldName, name)) {
                return i;
            }
        }
        throw new NoSuchElementException("Field name not found: " + name);
    }

    /**
     * @return The total size (in bytes) of a tuple with this schema.
     * The size is calculated based on field types.
     */
    public int getSize() {
        int size = 0;
        for (TDItem item : fields) {
            size += item.fieldType.getLen();
        }
        return size;
    }

    /**
     * Merges two TupleDescs into a single TupleDesc by concatenating their fields.
     *
     * @param td1 The first TupleDesc.
     * @param td2 The second TupleDesc.
     * @return A new TupleDesc containing fields from both td1 and td2.
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        List<Type> types = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();

        // Add fields from the first TupleDesc.
        for (TDItem item : td1.fields) {
            types.add(item.fieldType);
            fieldNames.add(item.fieldName);
        }

        // Add fields from the second TupleDesc.
        for (TDItem item : td2.fields) {
            types.add(item.fieldType);
            fieldNames.add(item.fieldName);
        }

        return new TupleDesc(types.toArray(new Type[0]), fieldNames.toArray(new String[0]));
    }

    /**
     * Checks if two TupleDesc objects are equal.
     * Equality is based on the number of fields and their respective types and names.
     *
     * @param o The object to compare with.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TupleDesc)) return false;

        TupleDesc other = (TupleDesc) o;

        if (this.numFields() != other.numFields()) return false;

        for (int i = 0; i < this.numFields(); i++) {
            if (!this.getFieldType(i).equals(other.getFieldType(i))) {
                return false;
            }
            String thisName = this.getFieldName(i);
            String otherName = other.getFieldName(i);

            if (thisName == null && otherName == null) continue;
            if (thisName == null || otherName == null || !thisName.equals(otherName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return A hash code for the TupleDesc based on its fields.
     */
    @Override
    public int hashCode() {
        return Objects.hash(fields);
    }

    /**
     * @return A string representation of the TupleDesc, listing all fields and their metadata.
     */
    @Override
    public String toString() {
        return String.join(", ", fields.stream().map(TDItem::toString).toArray(String[]::new));
    }
}
