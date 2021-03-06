package simpledb;

import javax.xml.crypto.Data;
import java.io.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private int maxPages;
    private LinkedHashMap<PageId, Page> pageMap;

    // add lockManage
    private LockManager lockManager;



    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        this.maxPages = numPages;
        pageMap = new LinkedHashMap<>();
        this.lockManager = new LockManager();
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
        // some code goes here

        if(perm == Permissions.READ_ONLY){
            lockManager.acquireReadLock(tid, pid);
        }
        else {
            if(perm != Permissions.READ_WRITE)
                throw new DbException("no such permission");
            lockManager.acquireWriteLock(tid, pid);
        }

        synchronized (this){
            Page curPage = pageMap.get(pid);
            // if cannot found in the buffer bool
            // read from disk and put it to the buffer pool (if there are still spaces)
            if(curPage == null) {
                if(pageMap.size() >= maxPages){
                    evictPage();
                }
                curPage = Database.getCatalog().getDatabaseFile(pid.getTableId()).readPage(pid);
                pageMap.put(pid, curPage);
            }
            return curPage;
        }
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
        lockManager.releaseLock(tid, pid);

    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        this.transactionComplete(tid, true);

    }



    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
        return lockManager.holdsLock(tid, pid);
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

        if(lockManager.getLockedPages(tid) == null)
            return;
        HashSet<PageId> dirtyPages = lockManager.getLockedPages(tid);
        // commit
        // force: force all dirty pages to disk after transaction
        if(commit){
            for(PageId pid : dirtyPages){
                flushPage(pid);
            }
        }
        //abort transaction
        else {
            for(PageId pid : dirtyPages){
                Page cur = pageMap.get(pid);
                if(cur != null && cur.isDirty() != null && cur.isDirty().equals(tid)){
                    pageMap.put(pid, cur.getBeforeImage());
                    cur.markDirty(false, null);
                }

            }
        }
        // release all locks charged by this tid
        lockManager.releaseAllLocks(tid);
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
        // some code goes here
        // not necessary for lab1
        // insert tuple through the heapfile access method
        // and get the returned list of pages
        ArrayList<Page> pages = Database.getCatalog().getDatabaseFile(tableId).insertTuple(tid, t);
        for(Page page : pages){
            page.markDirty(true, tid);
            pageMap.put(page.getId(), page);
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
        // some code goes here
        // not necessary for lab1
        int tableid = t.getRecordId().getPageId().getTableId();
        ArrayList<Page> pages = Database.getCatalog().getDatabaseFile(tableid).deleteTuple(tid, t);
        for(Page page : pages){
            page.markDirty(true, tid);
            pageMap.put(page.getId(), page);
        }


    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        for(PageId pid : pageMap.keySet()){
            if(pageMap.get(pid).isDirty() != null)
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
        // some code goes here
        // not necessary for lab1
        pageMap.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        try {
            HeapPage page = (HeapPage)pageMap.get(pid);
//            if(page == null)
////                return;
//                throw new IOException("the requested page is not in the buffer pool");
//            if(page.isDirty() == null)
//                return;

            //need heapfile access method to write page to disk
            // get file through the table id
            // and use the .writepPage()
            // then mark this page as non-dirty
            if(page != null && page.isDirty() != null){
                int tableId = pid.getTableId();
                HeapFile file = (HeapFile)Database.getCatalog().getDatabaseFile(tableId);
                file.writePage(page);
                page.setBeforeImage();
                page.markDirty(false, null);
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized  void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1
        // evict policy: evict the first pages that added to the buffer
        // 1. get keyset for pageMap
        Iterator<PageId> iter = pageMap.keySet().iterator();
        while(pageMap.size() >= maxPages && iter.hasNext()){
            PageId curPid = iter.next();
            if(pageMap.get(curPid).isDirty() == null){
                pageMap.remove(curPid);
                return;
            }
        }
        throw new DbException("No pages can be evicted");
    }

}
