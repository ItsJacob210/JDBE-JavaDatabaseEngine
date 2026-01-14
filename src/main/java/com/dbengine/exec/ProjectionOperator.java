package com.dbengine.exec;

import com.dbengine.storage.Tuple;

import java.util.HashMap;
import java.util.List;

/**
 * Projection operator that selects specific columns from tuples.
 */
public class ProjectionOperator implements Operator {
    private final Operator child;
    private final List<String> columns;
    
    public ProjectionOperator(Operator child, List<String> columns) {
        this.child = child;
        this.columns = columns;
    }
    
    @Override
    public void open() throws Exception {
        child.open();
    }
    
    @Override
    public Tuple next() throws Exception {
        Tuple tuple = child.next();
        if (tuple == null) {
            return null;
        }
        
        //project the tuple to selected columns
        Object[] values = new Object[columns.size()];
        java.util.Map<String, Integer> columnIndexMap = new HashMap<>();
        
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            values[i] = tuple.getValue(column);
            columnIndexMap.put(column, i);
        }
        
        Tuple projected = new Tuple(values, columnIndexMap);
        projected.setRecordId(tuple.getRecordId());
        return projected;
    }
    
    @Override
    public void close() throws Exception {
        child.close();
    }
}
