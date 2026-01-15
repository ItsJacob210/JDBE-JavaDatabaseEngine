package com.dbengine.exec;

import com.dbengine.index.BPlusTree;
import com.dbengine.lang.ast.BinaryExpr;
import com.dbengine.lang.ast.Expr;
import com.dbengine.lang.ast.IdentifierExpr;
import com.dbengine.lang.ast.LiteralExpr;
import com.dbengine.storage.RecordId;
import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;

import java.util.Iterator;
import java.util.List;

/**
 * Index scan operator that uses a B+ tree index to retrieve tuples.
 * Supports equality and range predicates.
 */
public class IndexScanOperator implements Operator {
    private final TableHeap tableHeap;
    private final BPlusTree index;
    private final Expr predicate;
    private Iterator<RecordId> ridIterator;
    
    public IndexScanOperator(TableHeap tableHeap, BPlusTree index, Expr predicate) {
        this.tableHeap = tableHeap;
        this.index = index;
        this.predicate = predicate;
    }
    
    @Override
    public void open() {
        List<RecordId> rids = executeIndexScan();
        ridIterator = rids.iterator();
    }
    
    private List<RecordId> executeIndexScan() {
        //extract the comparison from predicate
        if (predicate instanceof BinaryExpr binaryExpr) {
            Object value = extractValue(binaryExpr);
            if (value instanceof Comparable<?> comparable) {
                try {
                    return switch (binaryExpr.op()) {
                        case EQ -> index.search(comparable);
                        case GT, GE -> {
                            //for GT/GE, we need all values >= the given value
                            //b+ tree rangeSearch is inclusive on both ends
                            yield index.rangeSearch(comparable, getMaxValue());
                        }
                        case LT, LE -> {
                            //for LT/LE, we need all values <= the given value
                            yield index.rangeSearch(getMinValue(), comparable);
                        }
                        default -> List.of();
                    };
                } catch (Exception e) {
                    System.err.println("Error in index scan: " + e.getMessage());
                    e.printStackTrace();
                    return List.of();
                }
            }
        }
        return List.of();
    }
    
    private Object extractValue(BinaryExpr expr) {
        if (expr.right() instanceof LiteralExpr litExpr) {
            return litExpr.value();
        }
        return null;
    }
    
    private Comparable<?> getMinValue() {
        return Integer.MIN_VALUE;
    }
    
    private Comparable<?> getMaxValue() {
        return Integer.MAX_VALUE;
    }
    
    @Override
    public Tuple next() throws Exception {
        while (ridIterator != null && ridIterator.hasNext()) {
            RecordId rid = ridIterator.next();
            Tuple tuple = tableHeap.getTuple(rid);
            
            //double-check predicate (for GT/LT edge cases)
            if (tuple != null && evaluatePredicate(tuple)) {
                return tuple;
            }
        }
        return null;
    }
    
    private boolean evaluatePredicate(Tuple tuple) {
        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        Object result = evaluator.evaluate(predicate, tuple);
        return result instanceof Boolean && (Boolean) result;
    }
    
    @Override
    public void close() {
        ridIterator = null;
    }
}
