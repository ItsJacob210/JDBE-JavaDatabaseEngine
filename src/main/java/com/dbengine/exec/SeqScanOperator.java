package com.dbengine.exec;

import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;

import java.util.Iterator;

/**
 * Sequential scan operator that reads all tuples from a table.
 */
public class SeqScanOperator implements Operator {
    private final TableHeap tableHeap;
    private Iterator<Tuple> iterator;
    
    public SeqScanOperator(TableHeap tableHeap) {
        this.tableHeap = tableHeap;
    }
    
    @Override
    public void open() {
        iterator = tableHeap.iterator();
    }
    
    @Override
    public Tuple next() {
        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }
    
    @Override
    public void close() {
        iterator = null;
    }
}
