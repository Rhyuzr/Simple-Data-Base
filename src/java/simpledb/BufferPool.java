package simpledb;

import java.io.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedList;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;

    /** Default number of pages passed to the constructor. This is used by
     other classes. BufferPool should use the numPages argument to the
     constructor instead. */
    public static final int DEFAULT_PAGES = 50;
    private final int numPages; // Maximum number of pages in the buffer pool
    private final ConcurrentHashMap<PageId, Page> pageCache; // Cache for pages
    private final LinkedList<PageId> pageOrder; // Order of pages in the cache

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        this.numPages = numPages;
        this.pageCache = new ConcurrentHashMap<>(numPages); // Initialize cache
        this.pageOrder = new LinkedList<>();
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
        BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException {
        // Check if the page is already in the buffer pool
        if (pageCache.containsKey(pid)) {
            // Page is cached, update its position in LRU order
            pageOrder.remove(pid);
            pageOrder.addFirst(pid);
            return pageCache.get(pid);
        }

        // Page is not in the buffer pool, we need to load it from disk
        if (pageCache.size() >= numPages) {
            // Buffer pool is full, evict the least recently used page
            evictPage();
        }

        // Fetch the page from the disk via the DbFile
        int tableId = pid.getTableId();
        DbFile table = Database.getCatalog().getDatabaseFile(tableId);
        if (table == null) {
            throw new DbException("Table not found in the catalog for table ID: " + tableId);
        }

        // Read the page from the disk
        Page newPage = table.readPage(pid);

        // Add the page to the buffer pool and update LRU order
        pageCache.put(pid, newPage);
        pageOrder.addFirst(pid);

        return newPage;
    }

    /**
     * Evicts the least recently used page from the buffer pool.
     * @throws DbException if no page could be evicted
     */
    private synchronized void evictPage() throws DbException {
        if (pageOrder.isEmpty()) {
            throw new DbException("No pages to evict");
        }

        // Get the least recently used page
        PageId victimId = pageOrder.removeLast();
        Page victimPage = pageCache.get(victimId);

        // If the page is dirty, write it back to disk
        if (victimPage.isDirty() != null) {
            // Get the table file and write the page
            DbFile tableFile = Database.getCatalog().getDatabaseFile(victimId.getTableId());
            try {
                tableFile.writePage(victimPage);
            } catch (IOException e) {
                throw new DbException("Failed to write evicted page to disk: " + e.getMessage());
            }
        }

        // Remove the page from the cache
        pageCache.remove(victimId);
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        return false;
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed for lab2).
     * May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // Get the table (DbFile) from the catalog
        DbFile file = Database.getCatalog().getDatabaseFile(tableId);

        // Insert the tuple into the table
        ArrayList<Page> modifiedPages = file.insertTuple(tid, t);

        // Add all modified pages to buffer pool and mark them as dirty
        for (Page page : modifiedPages) {
            page.markDirty(true, tid);
            if (!pageCache.containsKey(page.getId())) {
                if (pageCache.size() >= numPages) {
                    evictPage();
                }
                pageCache.put(page.getId(), page);
                pageOrder.add(page.getId());
            }
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // Get the table ID from the tuple's record ID
        int tableId = t.getRecordId().getPageId().getTableId();

        // Get the table (DbFile) from the catalog
        DbFile file = Database.getCatalog().getDatabaseFile(tableId);

        // Delete the tuple from the table
        ArrayList<Page> modifiedPages = file.deleteTuple(tid, t);

        // Add all modified pages to buffer pool and mark them as dirty
        for (Page page : modifiedPages) {
            page.markDirty(true, tid);
            if (!pageCache.containsKey(page.getId())) {
                if (pageCache.size() >= numPages) {
                    evictPage();
                }
                pageCache.put(page.getId(), page);
                pageOrder.add(page.getId());
            }
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        for (PageId pid : pageCache.keySet()) {
            flushPage(pid);
        }
    }

    /** Remove the specific page id from the buffer pool.
     Needed by the recovery manager to ensure that the
     buffer pool doesn't keep a rolled back page in its
     cache.

     Also used by B+ tree files to ensure that deleted pages
     are removed from the cache so they can be reused safely
     */
    public synchronized void discardPage(PageId pid) {
        pageCache.remove(pid);
        pageOrder.remove(pid);
    }

    public synchronized void flushPage(PageId pid) throws IOException {
        Page page = pageCache.get(pid);
        if (page != null && page.isDirty() != null) {
            // Write the page back to disk
            Database.getCatalog().getDatabaseFile(pid.getTableId()).writePage(page);
            // Reset the page's dirty flag
            page.markDirty(false, null);
        }
    }

    public void flushPages(TransactionId tid) throws IOException {
        for (Page page : pageCache.values()) {
            if (page.isDirty() == tid) {
                flushPage(page.getId());
            }
        }
    }
}