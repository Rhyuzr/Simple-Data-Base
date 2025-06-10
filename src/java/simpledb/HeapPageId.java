package simpledb;

/**
 * Unique identifier for HeapPage objects.
 */
public class HeapPageId implements PageId {

    // The ID of the table this page belongs to
    private final int tableId;
    // The specific page number within the table
    private final int pageNumber;

    /**
     * Constructor. Create a page id structure for a specific page of a
     * specific table.
     *
     * @param tableId The table that is being referenced
     * @param pgNo    The page number in that table.
     */
    public HeapPageId(int tableId, int pgNo) {
        this.tableId = tableId; // Assign the table ID
        this.pageNumber = pgNo; // Assign the page number
    }

    /**
     * @return the table associated with this PageId
     */
    @Override
    public int getTableId() {
        return this.tableId; // Provide access to the table ID
    }

    /**
     * @return the page number in the table getTableId() associated with
     * this PageId
     */
    @Override
    public int getPageNumber() {
        return this.pageNumber; // Provide access to the page number
    }

    /**
     * @return a hash code for this page, represented by the concatenation of
     * the table number and the page number (needed if a PageId is used as a
     * key in a hash table in the BufferPool, for example.)
     * @see BufferPool
     */
    @Override
    public int hashCode() {
        // Compute a unique hash combining the table ID and page number
        return 31 * this.tableId + this.pageNumber;
    }

    /**
     * Compares one PageId to another.
     *
     * @param o The object to compare against (must be a PageId)
     * @return true if the objects are equal (e.g., page numbers and table
     * ids are the same)
     */
    @Override
    public boolean equals(Object o) {
        // Check if both references point to the same object
        if (this == o) return true;
        // Return false if the object is null or of a different class
        if (o == null || getClass() != o.getClass()) return false;
        // Cast the object to HeapPageId for further comparison
        HeapPageId that = (HeapPageId) o;
        // Check if both table ID and page number are the same
        return this.tableId == that.tableId && this.pageNumber == that.pageNumber;
    }

    /**
     * Return a representation of this object as an array of
     * integers, for writing to disk. Size of returned array must contain
     * number of integers that corresponds to number of args to one of the
     * constructors.
     *
     * @return an array of two integers: {tableId, pageNumber}
     */
    @Override
    public int[] serialize() {
        // Create an array with table ID and page number
        return new int[]{this.tableId, this.pageNumber};
    }
}
