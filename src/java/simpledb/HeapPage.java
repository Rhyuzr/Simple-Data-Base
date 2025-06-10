package simpledb;

import java.util.*;
import java.io.*;

/**
 * Each instance of HeapPage stores data for one page of HeapFiles and 
 * implements the Page interface that is used by BufferPool.
 *
 * @see HeapFile
 * @see BufferPool
 */
public class HeapPage implements Page {

    final HeapPageId pid;
    final TupleDesc td;
    final byte header[];
    final Tuple tuples[];
    final int numSlots;

    byte[] oldData;
    private final Byte oldDataLock = new Byte((byte) 0);

    private TransactionId dirtyId = null;

    /**
     * Create a HeapPage from a set of bytes of data read from disk.
     * The format of a HeapPage is a set of header bytes indicating
     * the slots of the page that are in use, some number of tuple slots.
     *  Specifically, the number of tuples is equal to: <p>
     *          floor((BufferPool.getPageSize()*8) / (tuple size * 8 + 1))
     * <p> where tuple size is the size of tuples in this
     * database table, which can be determined via {@link Catalog#getTupleDesc}.
     * The number of 8-bit header words is equal to:
     * <p>
     *      ceiling(no. tuple slots / 8)
     * <p>
     * @see Database#getCatalog
     * @see Catalog#getTupleDesc
     * @see BufferPool#getPageSize()
     */
    public HeapPage(HeapPageId id, byte[] data) throws IOException {
        this.pid = id;
        this.td = Database.getCatalog().getTupleDesc(id.getTableId());
        this.numSlots = getNumTuples();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        // allocate and read the header slots of this page
        header = new byte[getHeaderSize()];
        for (int i = 0; i < header.length; i++)
            header[i] = dis.readByte();

        tuples = new Tuple[numSlots];
        try {
            // allocate and read the actual records of this page
            for (int i = 0; i < tuples.length; i++)
                tuples[i] = readNextTuple(dis, i);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        dis.close();

        setBeforeImage();
    }

    /** Retrieve the number of tuples on this page.
     @return the number of tuples on this page
     */
    private int getNumTuples() {
        // Get the page size in bytes
        int pageSize = BufferPool.getPageSize();

        // Get the size of a single tuple in bytes
        int tupleSize = td.getSize();

        // Calculate and return the number of tuples that can fit in the page
        return pageSize * 8 / (tupleSize * 8 + 1);
    }

    /**
     * Computes the number of bytes in the header of a page in a HeapFile with each tuple occupying tupleSize bytes
     * @return the number of bytes in the header of a page in a HeapFile with each tuple occupying tupleSize bytes
     */
    private int getHeaderSize() {
        // The number of header bytes is ceiling(numSlots / 8)
        return (int) Math.ceil(numSlots / 8.0);
    }

    /** Return a view of this page before it was modified
     -- used by recovery */
    public HeapPage getBeforeImage() {
        try {
            byte[] oldDataRef = null;
            synchronized (oldDataLock) {
                oldDataRef = oldData;
            }
            return new HeapPage(pid, oldDataRef);
        } catch (IOException e) {
            e.printStackTrace();
            //should never happen -- we parsed it OK before!
            System.exit(1);
        }
        return null;
    }

    public void setBeforeImage() {
        synchronized (oldDataLock) {
            oldData = getPageData().clone();
        }
    }

    /**
     * @return the PageId associated with this page.
     */
    @Override
    public HeapPageId getId() {
        return this.pid;
    }

    /**
     * Suck up tuples from the source file.
     */
    private Tuple readNextTuple(DataInputStream dis, int slotId) throws NoSuchElementException {
        // if associated bit is not set, read forward to the next tuple, and
        // return null.
        if (!isSlotUsed(slotId)) {
            for (int i = 0; i < td.getSize(); i++) {
                try {
                    dis.readByte();
                } catch (IOException e) {
                    throw new NoSuchElementException("error reading empty tuple");
                }
            }
            return null;
        }

        // read fields in the tuple
        Tuple t = new Tuple(td);
        RecordId rid = new RecordId(pid, slotId);
        t.setRecordId(rid);
        try {
            for (int j = 0; j < td.numFields(); j++) {
                Field f = td.getFieldType(j).parse(dis);
                t.setField(j, f);
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            throw new NoSuchElementException("parsing error!");
        }

        return t;
    }

    /**
     * Generates a byte array representing the contents of this page.
     * Used to serialize this page to disk.
     *
     * @see #HeapPage
     * @return A byte array correspond to the bytes of this page.
     */
    public byte[] getPageData() {
        int len = BufferPool.getPageSize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
        DataOutputStream dos = new DataOutputStream(baos);

        // create the header of the page
        for (int i = 0; i < header.length; i++) {
            try {
                dos.writeByte(header[i]);
            } catch (IOException e) {
                // this really shouldn't happen
                e.printStackTrace();
            }
        }

        // create the tuples
        for (int i = 0; i < tuples.length; i++) {

            // empty slot
            if (!isSlotUsed(i)) {
                for (int j = 0; j < td.getSize(); j++) {
                    try {
                        dos.writeByte(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                continue;
            }

            // non-empty slot
            for (int j = 0; j < td.numFields(); j++) {
                Field f = tuples[i].getField(j);
                try {
                    f.serialize(dos);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // padding
        int zerolen = BufferPool.getPageSize() - (header.length + td.getSize() * tuples.length);
        byte[] zeroes = new byte[zerolen];
        try {
            dos.write(zeroes, 0, zerolen);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    public static byte[] createEmptyPageData() {
        int len = BufferPool.getPageSize();
        return new byte[len]; //all 0
    }

    public void deleteTuple(Tuple t) throws DbException {
        // Verify that the tuple belongs to this page
        RecordId rid = t.getRecordId();
        if (rid == null || !rid.getPageId().equals(pid)) {
            throw new DbException("Tuple does not belong to this page");
        }

        int tupleNo = rid.getTupleNumber();
        if (!isSlotUsed(tupleNo)) {
            throw new DbException("Tuple slot is already empty");
        }

        // Mark the slot as unused
        markSlotUsed(tupleNo, false);
        tuples[tupleNo] = null;
    }

    public void insertTuple(Tuple t) throws DbException {
        // Find an empty slot
        int emptySlot = -1;
        for (int i = 0; i < numSlots; i++) {
            if (!isSlotUsed(i)) {
                emptySlot = i;
                break;
            }
        }

        if (emptySlot == -1) {
            throw new DbException("Page is full");
        }

        // Verify tuple has the correct schema
        if (!t.getTupleDesc().equals(td)) {
            throw new DbException("Tuple schema does not match page schema");
        }

        // Insert the tuple and mark the slot as used
        t.setRecordId(new RecordId(pid, emptySlot));
        tuples[emptySlot] = t;
        markSlotUsed(emptySlot, true);
    }

    public void markDirty(boolean dirty, TransactionId tid) {
        dirtyId = dirty ? tid : null;
    }

    public TransactionId isDirty() {
        return dirtyId;
    }

    /**
     * Calculates the number of empty slots (unused tuple slots) in the page.
     *
     * @return the number of empty slots.
     */
    public int getNumEmptySlots() {
        int count = 0; // Initialize counter for empty slots
        for (int i = 0; i < numSlots; i++) { // Iterate through all slots
            if (!isSlotUsed(i)) { // Check if the slot is not in use
                count++; // Increment counter for empty slots
            }
        }
        return count; // Return the total count of empty slots
    }

    /**
     * Checks whether a specific slot is in use.
     *
     * @param i the slot index to check.
     * @return true if the slot is in use, false otherwise.
     * @throws IllegalArgumentException if the slot index is out of bounds.
     */
    public boolean isSlotUsed(int i) {
        if (i >= numSlots) { // Validate the slot index
            throw new IllegalArgumentException("Slot index out of bounds");
        }
        int byteIndex = i / 8; // Determine which byte in the header contains the bit for this slot
        int bitIndex = i % 8;  // Determine the specific bit position within the byte
        // Check if the bit at (byteIndex, bitIndex) is set
        return (header[byteIndex] & (1 << bitIndex)) != 0;
    }

    private void markSlotUsed(int i, boolean value) {
        int byteNum = i / 8;
        int bitNum = i % 8;

        if (value) {
            // Set the bit to 1
            header[byteNum] |= (1 << bitNum);
        } else {
            // Set the bit to 0
            header[byteNum] &= ~(1 << bitNum);
        }
    }

    /**
     * Returns an iterator over all tuples in this page that are currently in use.
     *
     * @return an iterator over the tuples in this page.
     */
    public Iterator<Tuple> iterator() {
        ArrayList<Tuple> tuples = new ArrayList<>(); // Create a list to store used tuples
        for (int i = 0; i < numSlots; i++) { // Iterate over all slots
            if (isSlotUsed(i)) { // Check if the slot is in use
                tuples.add(this.tuples[i]); // Add the tuple to the list
            }
        }
        return tuples.iterator(); // Return an iterator over the list of tuples
    }
}