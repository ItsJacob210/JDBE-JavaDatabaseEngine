package com.dbengine.exec;

import com.dbengine.lang.ast.SortNode.Order;
import com.dbengine.storage.Tuple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Sort operator that sorts tuples by a specified column.
 * Uses in-memory sorting (could use external merge sort once testing larger datasets).
 */
public class SortOperator implements Operator {
    private final Operator child;
    private final String column;
    private final Order order;
    private Iterator<Tuple> iterator;
    
    public SortOperator(Operator child, String column, Order order) {
        this.child = child;
        this.column = column;
        this.order = order;
    }
    
    @Override
    public void open() throws Exception {
        child.open();
        
        //materialize all tuples
        List<Tuple> tuples = new ArrayList<>();
        Tuple tuple;
        while ((tuple = child.next()) != null) {
            tuples.add(tuple);
        }
        
        //sort tuples
        tuples.sort(createComparator());
        
        iterator = tuples.iterator();
        child.close();
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
    
    private Comparator<Tuple> createComparator() {
        return (t1, t2) -> {
            Object v1 = t1.getValue(column);
            Object v2 = t2.getValue(column);
            
            int comparison = compareValues(v1, v2);
            return order == Order.ASC ? comparison : -comparison;
        };
    }
    
    @SuppressWarnings("unchecked")
    private int compareValues(Object v1, Object v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;
        
        if (v1 instanceof Comparable<?>) {
            return ((Comparable<Object>) v1).compareTo(v2);
        }
        
        return 0;
    }
}
