package simpledb;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op what;
    private HashMap<Field, AggregateData> groups;

    private class AggregateData {
        int count;
        int sum;
        int min;
        int max;

        public AggregateData() {
            count = 0;
            sum = 0;
            min = Integer.MAX_VALUE;
            max = Integer.MIN_VALUE;
        }

        public void addValue(int value) {
            count++;
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        public int getResult() {
            switch (what) {
                case MIN: return min;
                case MAX: return max;
                case SUM: return sum;
                case COUNT: return count;
                case AVG: return sum / count;
                default: throw new IllegalStateException("Unsupported operation");
            }
        }
    }

    /**
     * Aggregate constructor
     *
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.groups = new HashMap<>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     *
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        Field groupVal = (gbfield == NO_GROUPING) ? null : tup.getField(gbfield);
        int value = ((IntField) tup.getField(afield)).getValue();

        groups.putIfAbsent(groupVal, new AggregateData());
        groups.get(groupVal).addValue(value);
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public OpIterator iterator() {
        TupleDesc td;
        ArrayList<Tuple> tuples = new ArrayList<>();

        if (gbfield == NO_GROUPING) {
            td = new TupleDesc(new Type[]{Type.INT_TYPE});
            Tuple t = new Tuple(td);
            t.setField(0, new IntField(groups.get(null).getResult()));
            tuples.add(t);
        } else {
            td = new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE});
            for (Map.Entry<Field, AggregateData> entry : groups.entrySet()) {
                Tuple t = new Tuple(td);
                t.setField(0, entry.getKey());
                t.setField(1, new IntField(entry.getValue().getResult()));
                tuples.add(t);
            }
        }

        return new TupleIterator(td, tuples);
    }
}