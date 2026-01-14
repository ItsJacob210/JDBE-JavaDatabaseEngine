package com.dbengine.exec;

import com.dbengine.lang.ast.*;
import com.dbengine.storage.Tuple;

/**
 * Evaluates expressions against tuples at runtime.
 */
public class ExpressionEvaluator {
    
    public Object evaluate(Expr expr, Tuple tuple) {
        return switch (expr) {
            case IdentifierExpr i -> evaluateIdentifier(i, tuple);
            case LiteralExpr l -> l.value();
            case BinaryExpr b -> evaluateBinary(b, tuple);
        };
    }
    
    private Object evaluateIdentifier(IdentifierExpr expr, Tuple tuple) {
        return tuple.getValue(expr.name());
    }
    
    private Object evaluateBinary(BinaryExpr expr, Tuple tuple) {
        Object left = evaluate(expr.left(), tuple);
        Object right = evaluate(expr.right(), tuple);
        
        return switch (expr.op()) {
            case OR -> evaluateOr(left, right);
            case AND -> evaluateAnd(left, right);
            case EQ -> evaluateEquals(left, right);
            case NE -> !evaluateEquals(left, right);
            case LT -> evaluateLessThan(left, right);
            case LE -> evaluateLessThanOrEquals(left, right);
            case GT -> evaluateGreaterThan(left, right);
            case GE -> evaluateGreaterThanOrEquals(left, right);
        };
    }
    
    private boolean evaluateOr(Object left, Object right) {
        if (left instanceof Boolean && right instanceof Boolean) {
            return (Boolean) left || (Boolean) right;
        }
        return false;
    }
    
    private boolean evaluateAnd(Object left, Object right) {
        if (left instanceof Boolean && right instanceof Boolean) {
            return (Boolean) left && (Boolean) right;
        }
        return false;
    }
    
    private boolean evaluateEquals(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return left.equals(right);
    }
    
    @SuppressWarnings("unchecked")
    private boolean evaluateLessThan(Object left, Object right) {
        if (left instanceof Comparable<?> && right instanceof Comparable<?>) {
            return ((Comparable<Object>) left).compareTo(right) < 0;
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private boolean evaluateLessThanOrEquals(Object left, Object right) {
        if (left instanceof Comparable<?> && right instanceof Comparable<?>) {
            return ((Comparable<Object>) left).compareTo(right) <= 0;
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private boolean evaluateGreaterThan(Object left, Object right) {
        if (left instanceof Comparable<?> && right instanceof Comparable<?>) {
            return ((Comparable<Object>) left).compareTo(right) > 0;
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private boolean evaluateGreaterThanOrEquals(Object left, Object right) {
        if (left instanceof Comparable<?> && right instanceof Comparable<?>) {
            return ((Comparable<Object>) left).compareTo(right) >= 0;
        }
        return false;
    }
}
