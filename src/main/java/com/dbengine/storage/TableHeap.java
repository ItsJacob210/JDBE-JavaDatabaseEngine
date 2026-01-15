package com.dbengine.storage;

import com.dbengine.semantic.Schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages the storage of tuples for a table using heap pages.
 * Provides methods for insert, delete, update, and scan operations.
 */
public class TableHeap implements Iterable<Tuple> {
    private final BufferPool bufferPool;
    private final Schema schema;
    private final List<Integer> pageIds;
    private int firstPageId;
    private int currentInsertPageIndex; //track current page for inserts
    
    public TableHeap(BufferPool bufferPool, Schema schema) throws IOException {
        this.bufferPool = bufferPool;
        this.schema = schema;
        this.pageIds = new ArrayList<>();
        this.currentInsertPageIndex = 0;
        
        //create first page
        Page page = bufferPool.newPage();
        this.firstPageId = page.getPageId();
        this.pageIds.add(firstPageId);
        
        new HeapPage(page, schema);  //initialize the heap page
        bufferPool.unpinPage(firstPageId, true);
    }
    
    public TableHeap(BufferPool bufferPool, Schema schema, int firstPageId) {
        this.bufferPool = bufferPool;
        this.schema = schema;
        this.pageIds = new ArrayList<>();
        this.firstPageId = firstPageId;
        this.pageIds.add(firstPageId);
        this.currentInsertPageIndex = 0;
    }
    
    /**
     * Insert a tuple into the table.
     */
    public RecordId insertTuple(Tuple tuple) throws IOException {
        //try to insert into current page
        if (currentInsertPageIndex < pageIds.size()) {
            int pageId = pageIds.get(currentInsertPageIndex);
            Page page = bufferPool.fetchPage(pageId);
            HeapPage heapPage = new HeapPage(page, schema);
            
            int slotNum = heapPage.insertTuple(tuple);
            if (slotNum != -1) {
                bufferPool.unpinPage(pageId, true);
                return new RecordId(pageId, slotNum);
            }
            
            //current page is full, move to next
            bufferPool.unpinPage(pageId, false);
            currentInsertPageIndex++;
        }
        
        //need to allocate a new page
        Page newPage = bufferPool.newPage();
        int newPageId = newPage.getPageId();
        pageIds.add(newPageId);
        currentInsertPageIndex = pageIds.size() - 1;
        
        HeapPage heapPage = new HeapPage(newPage, schema);
        int slotNum = heapPage.insertTuple(tuple);
        bufferPool.unpinPage(newPageId, true);
        
        if (slotNum == -1) {
            throw new IOException("Failed to insert tuple into new page");
        }
        
        return new RecordId(newPageId, slotNum);
    }
    
    /**
     * Get a tuple by its record ID.
     */
    public Tuple getTuple(RecordId rid) throws IOException {
        Page page = bufferPool.fetchPage(rid.pageId());
        HeapPage heapPage = new HeapPage(page, schema);
        Tuple tuple = heapPage.getTuple(rid.slotNum());
        
        if (tuple != null) {
            tuple.setRecordId(rid);
        }
        
        bufferPool.unpinPage(rid.pageId(), false);
        return tuple;
    }
    
    /**
     * Delete a tuple by its record ID.
     */
    public void deleteTuple(RecordId rid) throws IOException {
        Page page = bufferPool.fetchPage(rid.pageId());
        HeapPage heapPage = new HeapPage(page, schema);
        heapPage.deleteTuple(rid.slotNum());
        bufferPool.unpinPage(rid.pageId(), true);
    }
    
    /**
     * Update a tuple by its record ID.
     */
    public boolean updateTuple(RecordId rid, Tuple tuple) throws IOException {
        Page page = bufferPool.fetchPage(rid.pageId());
        HeapPage heapPage = new HeapPage(page, schema);
        boolean success = heapPage.updateTuple(rid.slotNum(), tuple);
        bufferPool.unpinPage(rid.pageId(), true);
        return success;
    }
    
    /**
     * Get all tuples in the table.
     */
    public List<Tuple> getAllTuples() throws IOException {
        List<Tuple> allTuples = new ArrayList<>();
        
        for (int pageId : pageIds) {
            Page page = bufferPool.fetchPage(pageId);
            HeapPage heapPage = new HeapPage(page, schema);
            allTuples.addAll(heapPage.getAllTuples());
            bufferPool.unpinPage(pageId, false);
        }
        
        return allTuples;
    }
    
    @Override
    public Iterator<Tuple> iterator() {
        return new HeapIterator();
    }
    
    public Schema getSchema() {
        return schema;
    }
    
    public int getFirstPageId() {
        return firstPageId;
    }
    
    /**
     * Iterator for scanning all tuples in the table.
     */
    private class HeapIterator implements Iterator<Tuple> {
        private int currentPageIndex = 0;
        private List<Tuple> currentPageTuples = new ArrayList<>();
        private int currentTupleIndex = 0;
        
        public HeapIterator() {
            loadNextPage();
        }
        
        @Override
        public boolean hasNext() {
            while (currentTupleIndex >= currentPageTuples.size() && currentPageIndex < pageIds.size()) {
                loadNextPage();
            }
            return currentTupleIndex < currentPageTuples.size();
        }
        
        @Override
        public Tuple next() {
            if (!hasNext()) {
                throw new java.util.NoSuchElementException();
            }
            return currentPageTuples.get(currentTupleIndex++);
        }
        
        private void loadNextPage() {
            if (currentPageIndex >= pageIds.size()) {
                return;
            }
            
            try {
                int pageId = pageIds.get(currentPageIndex++);
                Page page = bufferPool.fetchPage(pageId);
                HeapPage heapPage = new HeapPage(page, schema);
                currentPageTuples = heapPage.getAllTuples();
                currentTupleIndex = 0;
                bufferPool.unpinPage(pageId, false);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load page", e);
            }
        }
    }
}
