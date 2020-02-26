package simpledb;

import java.util.*;

/**
 * The Join operator implements the relational join operation.
 */
public class Join extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor. Accepts two children to join and the predicate to join them
     * on
     * 
     * @param p
     *            The predicate to use to join the children
     * @param child1
     *            Iterator for the left(outer) relation to join
     * @param child2
     *            Iterator for the right(inner) relation to join
     */
    private JoinPredicate p;
    private OpIterator[] children;
    private List<Tuple> tuples;
    private Iterator<Tuple> itt;
    private TupleDesc td;

    public Join(JoinPredicate p, OpIterator child1, OpIterator child2) {
        // some code goes here
        this.p = p;
        children = new OpIterator[2];
        children[0] = child1;
        children[1] = child2;
        this.tuples = new ArrayList<>();
        this.td = TupleDesc.merge(child1.getTupleDesc(), child2.getTupleDesc());
    }

    public JoinPredicate getJoinPredicate() {
        // some code goes here
        return p;
    }

    /**
     * @return
     *       the field name of join field1. Should be quantified by
     *       alias or table name.
     * */
    public String getJoinField1Name() {
        // some code goes here
        // get the field index from the predicate
        return children[0].getTupleDesc().getFieldName(p.getField1());
    }

    /**
     * @return
     *       the field name of join field2. Should be quantified by
     *       alias or table name.
     * */
    public String getJoinField2Name() {
        // some code goes here
        return children[1].getTupleDesc().getFieldName(p.getField2());
    }

    /**
     * @see simpledb.TupleDesc#merge(TupleDesc, TupleDesc) for possible
     *      implementation logic.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
    }
    // the join operation, i.e., the nested loop
    // is implemented in the open() method
    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        super.open();
        children[0].open();
        children[1].open();

        // to be efficient, when Op is EQUALS
        // USE hash join
        if(p.getOperator().equals(Predicate.Op.EQUALS)) {
            HashMap<Field, List<Tuple>> fieldsMap = new HashMap<>();
            while(children[0].hasNext()){
                Tuple curTuple = children[0].next();
                Field curField = curTuple.getField(p.getField1());
                if(!fieldsMap.containsKey(curField)){
                    fieldsMap.put(curField, new ArrayList<>());
                }
                fieldsMap.get(curField).add(curTuple);
            }
            while(children[1].hasNext()){
                Tuple curTuple = children[1].next();
                Field curField = curTuple.getField(p.getField2());
                // join only if curField exists in child1's tuples
                if(fieldsMap.containsKey(curField)){
                    for(Tuple t1 : fieldsMap.get(curField)){
                        tuples.add(mergeTuple(t1, curTuple, td));
                    }
                }
            }
        }
        else {
            // nested loop join
            while(children[0].hasNext()) {
                Tuple t1 = children[0].next();
                // Join all tuples in child2 with t1
                while(children[1].hasNext()){
                    Tuple t2 = children[1].next();
                    if(p.filter(t1, t2)){
                        tuples.add(mergeTuple(t1, t2, getTupleDesc()));
                    }
                }
                // reset child2
                children[1].rewind();
            }

        }
        // initialize itt as the iterator of the joined tuple list
        itt = tuples.iterator();
    }
    private Tuple mergeTuple(Tuple t1, Tuple t2, TupleDesc td) {
        Tuple t = new Tuple(td);
        int len1 = t1.getTupleDesc().numFields();
        int len2 = t2.getTupleDesc().numFields();
        for(int i = 0; i < len1; i++){
            t.setField(i, t1.getField(i));
        }
        for(int j = 0; j < len2; j++){
            t.setField(j + len1, t2.getField(j));
        }
        return t;
    }
    public void close() {
        // some code goes here
        children[1].close();
        children[0].close();
        super.close();

    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        // no need to read and join tuples again
        // only have to reset the itt
        itt = tuples.iterator();
    }

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, if an equality predicate is used there will be two
     * copies of the join attribute in the results. (Removing such duplicate
     * columns can be done with an additional projection operator if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     * 
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        if(itt.hasNext())
            return itt.next();
        return null;
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
        return children;
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here
        this.children[0] = children[0];
        this.children[1] = children[1];
    }

}