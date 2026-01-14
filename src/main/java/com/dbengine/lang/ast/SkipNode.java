package com.dbengine.lang.ast;

/**
 * Skips the first n rows.
 */
public record SkipNode(QueryNode input, int count) implements QueryNode {
}
