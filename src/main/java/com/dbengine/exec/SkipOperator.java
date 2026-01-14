package com.dbengine.exec;

import com.dbengine.storage.Tuple;

/**
 * Skip operator that skips the first n tuples.
 */
public class SkipOperator implements Operator {
    private final Operator child;
    private final int skip;
    private int count;
    
    public SkipOperator(Operator child, int skip) {
        this.child = child;
        this.skip = skip;
    }
    
    @Override
    public void open() throws Exception {
        child.open();
        count = 0;
    }
    
    @Override
    public Tuple next() throws Exception {
        //skip the first n tuples
        while (count < skip) {
            Tuple tuple = child.next();
            if (tuple == null) {
                return null;
            }
            count++;
        }
        
        return child.next();
    }
    
    @Override
    public void close() throws Exception {
        child.close();
    }
}
