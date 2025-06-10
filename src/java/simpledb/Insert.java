package simpledb;

import java.io.*;

/**
 * Inserts tuples read from the child operator into the tableId specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;

    private TransactionId tid;
    private OpIterator child;
    private int tableId;
    private boolean called;

    /**
     * Constructor.
     *
     * @param t
     *            The transaction running the insert.
     * @param child
     *            The child operator from which to read tuples to be inserted.
     * @param tableId
     *            The table in which to insert tuples.
     * @throws DbException
     *             if TupleDesc of child differs from table into which we are to
     *             insert.
     */
    public Insert(TransactionId t, OpIterator child, int tableId)
            throws DbException {
        this.tid = t;
        this.child = child;
        this.tableId = tableId;
        this.called = false;

        // Check if the child's TupleDesc matches the table's TupleDesc
        TupleDesc tableTd = Database.getCatalog().getTupleDesc(tableId);
        TupleDesc childTd = child.getTupleDesc();

        // They should have the same number of fields and compatible types
        if (childTd.numFields() != tableTd.numFields()) {
            throw new DbException("Number of fields in child does not match table");
        }

        // Check each field type matches
        for (int i = 0; i < childTd.numFields(); i++) {
            if (!childTd.getFieldType(i).equals(tableTd.getFieldType(i))) {
                throw new DbException("Field type mismatch at position " + i);
            }
        }
    }

    public TupleDesc getTupleDesc() {
        return new TupleDesc(new Type[]{Type.INT_TYPE});
    }

    public void open() throws DbException, TransactionAbortedException {
        super.open();
        child.open();
    }

    public void close() {
        super.close();
        child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        child.rewind();
    }

    /**
     * Inserts tuples read from child into the tableId specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     *
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws DbException, TransactionAbortedException {
        if (called) {
            return null;
        }
        called = true;

        int count = 0;
        BufferPool bufferPool = Database.getBufferPool();

        while (child.hasNext()) {
            Tuple tuple = child.next();
            try {
                bufferPool.insertTuple(tid, tableId, tuple);
                count++;
            } catch (IOException e) {
                throw new DbException("Error inserting tuple: " + e.getMessage());
            }
        }

        // Return a tuple with the count of inserted records
        Tuple result = new Tuple(getTupleDesc());
        result.setField(0, new IntField(count));
        return result;
    }

    @Override
    public OpIterator[] getChildren() {
        return new OpIterator[]{child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        child = children[0];
    }
}