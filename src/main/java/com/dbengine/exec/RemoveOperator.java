package com.dbengine.exec;

import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;

/**
 * Remove operator that deletes tuples from a table.
 */
public class RemoveOperator implements Operator {
    private final Operator child;
    private final TableHeap tableHeap;
    private int deletedCount;
    
    public RemoveOperator(Operator child, TableHeap tableHeap) {
        this.child = child;
        this.tableHeap = tableHeap;
    }
    
    @Override
    public void open() throws Exception {
        child.open();
        deletedCount = 0;
    }
    
    @Override
    public Tuple next() throws Exception {
        Tuple tuple = child.next();
        if (tuple == null) {
            return null;
        }
        
        //delete from storage
        tableHeap.deleteTuple(tuple.getRecordId());
        deletedCount++;
        
        return tuple;
    }
    
    @Override
    public void close() throws Exception {
        child.close();
    }
    
    public int getDeletedCount() {
        return deletedCount;
    }
}
