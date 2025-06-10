package simpledb;

import java.util.*;

/**
 * SeqScan is an implementation of a sequential scan access method that reads
 * each tuple of a table in no particular order (e.g., as they are laid out on
 * disk).
 */
public class SeqScan implements OpIterator {

    private static final long serialVersionUID = 1L;

    private TransactionId tid; // The transaction this scan is running as part of
    private int tableId; // The ID of the table being scanned
    private String tableAlias; // The alias for the table (used in queries)
    private DbFileIterator dbFileIterator; // The iterator used to scan the table

    /**
     * Creates a sequential scan over the specified table as a part of the
     * specified transaction.
     *
     * @param tid
     *            The transaction this scan is running as a part of.
     * @param tableid
     *            the table to scan.
     * @param tableAlias
     *            the alias of this table (needed by the parser); the returned
     *            tupleDesc should have fields with name tableAlias.fieldName
     *            (note: this class is not responsible for handling a case where
     *            tableAlias or fieldName are null. It shouldn't crash if they
     *            are, but the resulting name can be null.fieldName,
     *            tableAlias.null, or null.null).
     */
    public SeqScan(TransactionId tid, int tableid, String tableAlias) {
        this.tid = tid; // Assign the transaction ID
        this.tableId = tableid; // Assign the table ID
        this.tableAlias = tableAlias; // Assign the table alias
        this.dbFileIterator = null; // Initialize the file iterator to null
    }

    /**
     * @return return the table name of the table the operator scans. This should
     *         be the actual name of the table in the catalog of the database
     */
    public String getTableName() {
        // Retrieve and return the table name from the catalog
        return Database.getCatalog().getTableName(tableId);
    }

    /**
     * @return Return the alias of the table this operator scans.
     */
    public String getAlias() {
        return tableAlias; // Return the alias of the table
    }

    /**
     * Reset the tableid, and tableAlias of this operator.
     *
     * @param tableid    the table to scan.
     * @param tableAlias the alias of this table (needed by the parser); the returned
     *                   tupleDesc should have fields with name tableAlias.fieldName
     *                   (note: this class is not responsible for handling a case where
     *                   tableAlias or fieldName are null. It shouldn't crash if they
     *                   are, but the resulting name can be null.fieldName,
     *                   tableAlias.null, or null.null).
     */
    public void reset(int tableid, String tableAlias) {
        this.tableId = tableid; // Reset the table ID
        this.tableAlias = tableAlias; // Reset the table alias
    }

    /**
     * Open the iterator to start scanning.
     */
    public void open() throws DbException, TransactionAbortedException {
        // Initialize the file iterator for the table and open it
        dbFileIterator = Database.getCatalog().getDatabaseFile(tableId).iterator(tid);
        dbFileIterator.open();
    }

    /**
     * Returns the TupleDesc with field names from the underlying HeapFile,
     * prefixed with the tableAlias string from the constructor. This prefix
     * becomes useful when joining tables containing a field(s) with the same
     * name. The alias and name should be separated with a "." character
     * (e.g., "alias.fieldName").
     *
     * @return the TupleDesc with field names from the underlying HeapFile,
     *         prefixed with the tableAlias string from the constructor.
     */
    public TupleDesc getTupleDesc() {
        // Get the original TupleDesc for the table from the catalog
        TupleDesc original = Database.getCatalog().getTupleDesc(tableId);
        int numFields = original.numFields(); // Get the number of fields
        Type[] types = new Type[numFields]; // Array to hold field types
        String[] fieldNames = new String[numFields]; // Array to hold field names

        for (int i = 0; i < numFields; i++) {
            types[i] = original.getFieldType(i); // Set the field type
            fieldNames[i] = tableAlias + "." + original.getFieldName(i); // Prefix field name with alias
        }

        // Return a new TupleDesc with the prefixed field names
        return new TupleDesc(types, fieldNames);
    }

    /**
     * Checks if there are more tuples in the iterator.
     */
    public boolean hasNext() throws TransactionAbortedException, DbException {
        // Ensure the iterator is open before checking for more tuples
        if (dbFileIterator == null) {
            throw new IllegalStateException("Iterator not open");
        }
        return dbFileIterator.hasNext(); // Check if there are more tuples
    }

    /**
     * Returns the next tuple in the scan.
     */
    public Tuple next() throws NoSuchElementException, TransactionAbortedException, DbException {
        // Ensure the iterator is open before fetching the next tuple
        if (dbFileIterator == null) {
            throw new IllegalStateException("Iterator not open");
        }
        return dbFileIterator.next(); // Return the next tuple in the iterator
    }

    /**
     * Close the iterator and release resources.
     */
    public void close() {
        if (dbFileIterator != null) {
            dbFileIterator.close(); // Close the iterator
            dbFileIterator = null; // Nullify the iterator
        }
    }

    /**
     * Rewinds the iterator back to the start.
     */
    public void rewind() throws DbException, NoSuchElementException, TransactionAbortedException {
        // Ensure the iterator is open before rewinding
        if (dbFileIterator == null) {
            throw new IllegalStateException("Iterator not open");
        }
        dbFileIterator.rewind(); // Rewind the iterator to the beginning
    }
}
