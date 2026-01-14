package com.dbengine.exec;

import com.dbengine.storage.Tuple;

/**
 * Limit operator that restricts the number of output tuples.
 */
public class LimitOperator implements Operator {
    private final Operator child;
    private final int limit;
    private int count;
    
    public LimitOperator(Operator child, int limit) {
        this.child = child;
        this.limit = limit;
    }
    
    @Override
    public void open() throws Exception {
        child.open();
        count = 0;
    }
    
    @Override
    public Tuple next() throws Exception {
        if (count >= limit) {
            return null;
        }
        
        Tuple tuple = child.next();
        if (tuple != null) {
            count++;
        }
        return tuple;
    }
    
    @Override
    public void close() throws Exception {
        child.close();
    }
}
