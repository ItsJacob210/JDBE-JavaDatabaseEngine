package com.dbengine.lang.ast;

/**
 * Literal value expression.
 */
public record LiteralExpr(Object value, LiteralType type) implements Expr {
    public enum LiteralType {
        INTEGER, STRING, BOOLEAN, NULL
    }
}
