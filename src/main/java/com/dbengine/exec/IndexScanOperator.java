package com.dbengine.exec;

import com.dbengine.index.BPlusTree;
import com.dbengine.storage.RecordId;
import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;

import java.util.Iterator;
import java.util.List;

/**
 * Index scan operator that uses a B+ tree index to retrieve tuples.
 */
public class IndexScanOperator implements Operator {
    private final TableHeap tableHeap;
    private final BPlusTree index;
    private final Comparable<?> key;
    private Iterator<RecordId> ridIterator;
    
    public IndexScanOperator(TableHeap tableHeap, BPlusTree index, Comparable<?> key) {
        this.tableHeap = tableHeap;
        this.index = index;
        this.key = key;
    }
    
    @Override
    public void open() {
        List<RecordId> rids = index.search(key);
        ridIterator = rids.iterator();
    }
    
    @Override
    public Tuple next() throws Exception {
        if (ridIterator != null && ridIterator.hasNext()) {
            RecordId rid = ridIterator.next();
            return tableHeap.getTuple(rid);
        }
        return null;
    }
    
    @Override
    public void close() {
        ridIterator = null;
    }
}
