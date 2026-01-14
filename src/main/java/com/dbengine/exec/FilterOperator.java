package com.dbengine.exec;

import com.dbengine.lang.ast.Expr;
import com.dbengine.storage.Tuple;

/**
 * Filter operator that applies a predicate to tuples from a child operator.
 */
public class FilterOperator implements Operator {
    private final Operator child;
    private final Expr predicate;
    private final ExpressionEvaluator evaluator;
    
    public FilterOperator(Operator child, Expr predicate) {
        this.child = child;
        this.predicate = predicate;
        this.evaluator = new ExpressionEvaluator();
    }
    
    @Override
    public void open() throws Exception {
        child.open();
    }
    
    @Override
    public Tuple next() throws Exception {
        while (true) {
            Tuple tuple = child.next();
            if (tuple == null) {
                return null;
            }
            
            Object result = evaluator.evaluate(predicate, tuple);
            if (result instanceof Boolean && (Boolean) result) {
                return tuple;
            }
        }
    }
    
    @Override
    public void close() throws Exception {
        child.close();
    }
}
