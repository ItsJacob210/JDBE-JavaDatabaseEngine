package com.dbengine.lang.ast;

/**
 * Limits the result to the first n rows.
 */
public record LimitNode(QueryNode input, int count) implements QueryNode {
}
