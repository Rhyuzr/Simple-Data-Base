package simpledb;

import java.util.*;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    private OpIterator child;
    private int afield;
    private int gfield;
    private Aggregator.Op aop;
    private Aggregator aggregator;
    private OpIterator iterator;
    private TupleDesc childTd;

    /**
     * Constructor.
     *
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     *
     *
     * @param child
     *            The OpIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
        this.child = child;
        this.afield = afield;
        this.gfield = gfield;
        this.aop = aop;
        this.childTd = child.getTupleDesc();

        // Create the appropriate aggregator based on the field type
        Type afieldType = childTd.getFieldType(afield);
        if (afieldType == Type.INT_TYPE) {
            this.aggregator = new IntegerAggregator(gfield,
                    gfield == Aggregator.NO_GROUPING ? null : childTd.getFieldType(gfield),
                    afield,
                    aop);
        } else if (afieldType == Type.STRING_TYPE) {
            this.aggregator = new StringAggregator(gfield,
                    gfield == Aggregator.NO_GROUPING ? null : childTd.getFieldType(gfield),
                    afield,
                    aop);
        }
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
        return gfield;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     *         null;
     * */
    public String groupFieldName() {
        return gfield == Aggregator.NO_GROUPING ? null : childTd.getFieldName(gfield);
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
        return afield;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
        return childTd.getFieldName(afield);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
        return aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
        return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
            TransactionAbortedException {
        child.open();
        // Process all input tuples and add them to the aggregator
        while (child.hasNext()) {
            aggregator.mergeTupleIntoGroup(child.next());
        }
        iterator = aggregator.iterator();
        iterator.open();
        super.open();
    }

    public void close() {
        super.close();
        iterator.close();
        child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        iterator.rewind();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     *
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
        Type[] types;
        String[] names;

        if (gfield == Aggregator.NO_GROUPING) {
            // No grouping - just return the aggregate field
            types = new Type[]{ Type.INT_TYPE };
            names = new String[]{ null };
        } else {
            // With grouping - return group field and aggregate field
            types = new Type[]{ childTd.getFieldType(gfield), Type.INT_TYPE };
            names = new String[]{ childTd.getFieldName(gfield), null };
        }

        return new TupleDesc(types, names);
    }

    @Override
    public OpIterator[] getChildren() {
        return new OpIterator[] { child };
    }

    @Override
    public void setChildren(OpIterator[] children) {
        if (children != null && children.length > 0) {
            child = children[0];
        }
    }
}