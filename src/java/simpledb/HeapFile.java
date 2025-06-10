package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private final File file; // The file on disk that backs this HeapFile
    private final TupleDesc tupleDesc; // The TupleDesc describing the structure of tuples in this file

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        this.file = f; // Initialize the file
        this.tupleDesc = td; // Initialize the tuple descriptor
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        return this.file; // Return the file
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // Generate a unique ID for the HeapFile based on the absolute file path
        return this.file.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        return this.tupleDesc; // Return the tuple description for this file
    }

    /**
     * Reads the page with the given PageId from disk.
     *
     * @param pid the PageId of the page to read.
     * @return the Page object read from disk.
     */
    public Page readPage(PageId pid) {
        int pageSize = BufferPool.getPageSize(); // Get the page size from the buffer pool
        byte[] pageData = new byte[pageSize]; // Allocate byte array for page data
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // Calculate the offset for the page based on its number and page size
            long offset = (long) pid.getPageNumber() * pageSize;

            // Ensure the offset is within bounds
            if (offset >= file.length()) {
                throw new IllegalArgumentException("Page number out of bounds");
            }

            raf.seek(offset); // Seek to the correct position in the file
            raf.readFully(pageData); // Read the page data into the byte array

            // Return a new HeapPage created from the page data
            return new HeapPage((HeapPageId) pid, pageData);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading page from disk", e); // Handle I/O errors
        }
    }

    /**
     * Writes the given page to disk.
     *
     * @param page the Page to write.
     * @throws IOException if an IO error occurs.
     */
    public void writePage(Page page) throws IOException {
        int pageSize = BufferPool.getPageSize(); // Get the page size
        byte[] pageData = page.getPageData(); // Get the byte data of the page
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            // Calculate the offset for the page based on its number
            long offset = (long) page.getId().getPageNumber() * pageSize;
            raf.seek(offset); // Seek to the correct position in the file
            raf.write(pageData); // Write the page data to the file
        }
    }

    /**
     * Returns the number of pages in this HeapFile.
     *
     * @return the number of pages in this HeapFile.
     */
    public int numPages() {
        // Calculate the number of pages by dividing the file size by the page size
        return (int) Math.ceil((double) file.length() / BufferPool.getPageSize());
    }

    /**
     * Returns an iterator over all tuples in this HeapFile.
     *
     * @param tid the TransactionId for this iterator.
     * @return a DbFileIterator over all tuples in this HeapFile.
     */
    public DbFileIterator iterator(TransactionId tid) {
        return new DbFileIterator() {
            private int currentPage = -1; // Tracks the current page number
            private Iterator<Tuple> tupleIterator; // Iterator for the tuples on the current page

            @Override
            public void open() throws DbException, TransactionAbortedException {
                currentPage = 0; // Start with the first page
                loadPageTuples(); // Load the tuples for the first page
            }

            @Override
            public boolean hasNext() throws DbException, TransactionAbortedException {
                if (tupleIterator == null) {
                    return false; // No tuples to iterate over if the iterator is null
                }
                if (tupleIterator.hasNext()) {
                    return true; // Return true if there are more tuples on the current page
                }
                // Move to the next page if available
                while (currentPage < numPages() - 1) {
                    currentPage++; // Move to the next page
                    loadPageTuples(); // Load tuples from the next page
                    if (tupleIterator.hasNext()) {
                        return true; // Return true if there are tuples on the new page
                    }
                }
                return false; // No more tuples available
            }

            @Override
            public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
                if (tupleIterator == null || !tupleIterator.hasNext()) {
                    throw new NoSuchElementException("No more tuples"); // Throw exception if no tuples are left
                }
                return tupleIterator.next(); // Return the next tuple
            }

            @Override
            public void rewind() throws DbException, TransactionAbortedException {
                close(); // Close the current iterator
                open(); // Re-open and start from the first page
            }

            @Override
            public void close() {
                currentPage = -1; // Reset the current page number
                tupleIterator = null; // Reset the tuple iterator
            }

            private void loadPageTuples() throws DbException, TransactionAbortedException {
                if (currentPage >= 0 && currentPage < numPages()) {
                    PageId pid = new HeapPageId(getId(), currentPage); // Create a PageId for the current page
                    // Load the page from the buffer pool
                    HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);
                    tupleIterator = page.iterator(); // Set the tuple iterator to the one from the page
                } else {
                    tupleIterator = null; // No more pages to load
                }
            }
        };
    }

    /**
     * Insert a tuple into the HeapFile.
     */
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        ArrayList<Page> modifiedPages = new ArrayList<>();

        // Check if tuple schema matches table schema
        if (!t.getTupleDesc().equals(tupleDesc)) {
            throw new DbException("Tuple schema does not match table schema");
        }

        // Try to find a page with empty slot
        BufferPool bufferPool = Database.getBufferPool();
        for (int i = 0; i < numPages(); i++) {
            HeapPageId pid = new HeapPageId(getId(), i);
            HeapPage page = (HeapPage) bufferPool.getPage(tid, pid, Permissions.READ_WRITE);

            try {
                page.insertTuple(t);
                page.markDirty(true, tid);
                modifiedPages.add(page);
                return modifiedPages;
            } catch (DbException e) {
                // Page is full, continue to next page
            }
        }

        // No existing page has space, create a new page
        HeapPageId newPid = new HeapPageId(getId(), numPages());
        HeapPage newPage = new HeapPage(newPid, HeapPage.createEmptyPageData());

        // Write the empty page to disk
        writePage(newPage);

        // Get the page through the buffer pool and insert the tuple
        HeapPage page = (HeapPage) bufferPool.getPage(tid, newPid, Permissions.READ_WRITE);
        page.insertTuple(t);
        page.markDirty(true, tid);
        modifiedPages.add(page);

        return modifiedPages;
    }

    /**
     * Delete a tuple from the HeapFile.
     */
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t)
            throws DbException, TransactionAbortedException {
        ArrayList<Page> modifiedPages = new ArrayList<>();

        RecordId rid = t.getRecordId();
        if (rid == null) {
            throw new DbException("Tuple has no record ID");
        }

        PageId pid = rid.getPageId();
        if (pid.getTableId() != getId()) {
            throw new DbException("Tuple does not belong to this table");
        }

        // Get the page through buffer pool
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
        page.deleteTuple(t);
        page.markDirty(true, tid);
        modifiedPages.add(page);

        return modifiedPages;
    }
}