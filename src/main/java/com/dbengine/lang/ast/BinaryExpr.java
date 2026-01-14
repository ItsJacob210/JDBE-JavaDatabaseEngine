package com.dbengine.lang.ast;

/**
 * Binary expression with left operand, operator, and right operand.
 */
public record BinaryExpr(Expr left, BinaryOp op, Expr right) implements Expr {
    public enum BinaryOp {
        //logical
        OR, AND,
        //equality
        EQ, NE,
        //relational
        LT, LE, GT, GE
    }
}
