package com.dbengine.lang.ast;

/**
 * Filters rows based on a predicate expression.
 */
public record FilterNode(QueryNode input, Expr predicate) implements QueryNode {
}
