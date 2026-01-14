package com.dbengine.lang.ast;

/**
 * Represents a table source in the query.
 */
public record SourceNode(String table) implements QueryNode {
}
