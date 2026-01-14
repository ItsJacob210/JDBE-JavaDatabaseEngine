package com.dbengine.lang.ast;

/**
 * Takes the first n rows (synonym for limit).
 */
public record TakeNode(QueryNode input, int count) implements QueryNode {
}
