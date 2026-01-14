package com.dbengine.exec;

import com.dbengine.lang.ast.Expr;
import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;

import java.util.Map;

/**
 * Modify operator that updates tuples in a table.
 */
public class ModifyOperator implements Operator {
    private final Operator child;
    private final Map<String, Expr> updates;
    private final TableHeap tableHeap;
    private final ExpressionEvaluator evaluator;
    private int modifiedCount;
    
    public ModifyOperator(Operator child, Map<String, Expr> updates, TableHeap tableHeap) {
        this.child = child;
        this.updates = updates;
        this.tableHeap = tableHeap;
        this.evaluator = new ExpressionEvaluator();
    }
    
    @Override
    public void open() throws Exception {
        child.open();
        modifiedCount = 0;
    }
    
    @Override
    public Tuple next() throws Exception {
        Tuple tuple = child.next();
        if (tuple == null) {
            return null;
        }
        
        //create modified tuple
        Tuple modified = tuple.copy();
        for (Map.Entry<String, Expr> entry : updates.entrySet()) {
            String column = entry.getKey();
            Expr expr = entry.getValue();
            Object value = evaluator.evaluate(expr, tuple);
            
            Integer index = modified.getColumnIndexMap().get(column);
            if (index != null) {
                modified.setValue(index, value);
            }
        }
        
        //update in storage
        tableHeap.updateTuple(tuple.getRecordId(), modified);
        modifiedCount++;
        
        return modified;
    }
    
    @Override
    public void close() throws Exception {
        child.close();
    }
    
    public int getModifiedCount() {
        return modifiedCount;
    }
}
