package com.dbengine.lang.ast;

/**
 * Sorts rows by a column in ascending or descending order.
 */
public record SortNode(QueryNode input, String column, Order order) implements QueryNode {
    public enum Order {
        ASC, DESC
    }
}
