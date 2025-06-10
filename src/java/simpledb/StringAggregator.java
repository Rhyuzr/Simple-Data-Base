package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op what;
    private HashMap<Field, Integer> groups;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        if (what != Op.COUNT) {
            throw new IllegalArgumentException("Only COUNT is supported for StringAggregator");
        }
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.groups = new HashMap<>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        Field groupVal = (gbfield == NO_GROUPING) ? null : tup.getField(gbfield);
        groups.merge(groupVal, 1, Integer::sum);
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        TupleDesc td;
        ArrayList<Tuple> tuples = new ArrayList<>();

        if (gbfield == NO_GROUPING) {
            td = new TupleDesc(new Type[]{Type.INT_TYPE});
            Tuple t = new Tuple(td);
            t.setField(0, new IntField(groups.getOrDefault(null, 0)));
            tuples.add(t);
        } else {
            td = new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE});
            for (Map.Entry<Field, Integer> entry : groups.entrySet()) {
                Tuple t = new Tuple(td);
                t.setField(0, entry.getKey());
                t.setField(1, new IntField(entry.getValue()));
                tuples.add(t);
            }
        }

        return new TupleIterator(td, tuples);
    }
}