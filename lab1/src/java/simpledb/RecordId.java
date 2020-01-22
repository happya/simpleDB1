package simpledb;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A RecordId is a reference to a specific tuple on a specific page of a
 * specific table.
 */
public class RecordId implements Serializable {

    private static final long serialVersionUID = 1L;
    private PageId pageId; // the page id of the page where the tuple resides
    private int tupleNo;   // the tuple number in its page

    /**
     * Creates a new RecordId referring to the specified PageId and tuple
     * number.
     * 
     * @param pid
     *            the pageid of the page on which the tuple resides
     * @param tupleno
     *            the tuple number within the page.
     */
    public RecordId(PageId pid, int tupleno) {
        // some code goes here
        this.pageId = pid;
        this.tupleNo = tupleno;
    }

    /**
     * @return the tuple number this RecordId references.
     */
    public int getTupleNumber() {
        // some code goes here
        return tupleNo;
    }

    /**
     * @return the page id this RecordId references.
     */
    public PageId getPageId() {
        // some code goes here
        return pageId;
    }

    /**
     * Two RecordId objects are considered equal if they represent the same
     * tuple.
     * 
     * @return True if this and o represent the same tuple
     */
    @Override
    public boolean equals(Object o) {
        // some code goes here
        return (o instanceof RecordId && ((RecordId) o).pageId.equals(pageId)
                && ((RecordId) o).tupleNo == tupleNo);
        // throw new UnsupportedOperationException("implement this");
    }

    /**
     * You should implement the hashCode() so that two equal RecordId instances
     * (with respect to equals()) have the same hashCode().
     * 
     * @return An int that is the same for equal RecordId objects.
     */
    @Override
    public int hashCode() {
        // some code goes here
        // similar to the HeapPageId
        int[] data = new int[3];
        data[0] = pageId.getTableId();
        data[1] = pageId.getPageNumber();
        data[2] = tupleNo;
        return Arrays.hashCode(data);
    }

}
