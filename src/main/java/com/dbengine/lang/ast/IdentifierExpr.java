package com.dbengine.lang.ast;

/**
 * Identifier expression representing a column reference.
 */
public record IdentifierExpr(String name) implements Expr {
}
