package com.dbengine.lang.ast;

/**
 * Removes rows from the input relation.
 */
public record RemoveNode(QueryNode input) implements QueryNode {
}
