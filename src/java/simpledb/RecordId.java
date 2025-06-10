package simpledb;

import java.io.Serializable;
import java.util.Objects;

/**
 * A RecordId is a reference to a specific tuple on a specific page of a
 * specific table.
 */
public class RecordId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PageId pid; // The PageId of the page containing the tuple
    private final int tupleno; // The tuple number within the page

    /**
     * Creates a new RecordId referring to the specified PageId and tuple
     * number.
     *
     * @param pid     the PageId of the page on which the tuple resides
     * @param tupleno the tuple number within the page.
     */
    public RecordId(PageId pid, int tupleno) {
        // Validate that the PageId is not null
        if (pid == null) {
            throw new IllegalArgumentException("PageId cannot be null");
        }
        this.pid = pid; // Assign the page ID
        this.tupleno = tupleno; // Assign the tuple number
    }

    /**
     * @return the tuple number this RecordId references.
     */
    public int getTupleNumber() {
        return tupleno; // Return the tuple number
    }

    /**
     * @return the PageId this RecordId references.
     */
    public PageId getPageId() {
        return pid; // Return the PageId
    }

    /**
     * Two RecordId objects are considered equal if they represent the same
     * tuple.
     *
     * @param o the object to compare against
     * @return True if this and o represent the same tuple
     */
    @Override
    public boolean equals(Object o) {
        // If comparing the same object, return true
        if (this == o) return true;

        // If the object is null or of a different class, return false
        if (o == null || getClass() != o.getClass()) return false;

        // Cast the object to RecordId and compare the tuple number and PageId
        RecordId recordId = (RecordId) o;
        return tupleno == recordId.tupleno && pid.equals(recordId.pid);
    }

    /**
     * You should implement the hashCode() so that two equal RecordId instances
     * (with respect to equals()) have the same hashCode().
     *
     * @return An int that is the same for equal RecordId objects.
     */
    @Override
    public int hashCode() {
        // Combine the hash codes of the PageId and tuple number to create a unique hashCode for this RecordId
        return Objects.hash(pid, tupleno);
    }
}
