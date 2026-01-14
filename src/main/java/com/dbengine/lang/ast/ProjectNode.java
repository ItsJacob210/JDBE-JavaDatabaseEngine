package com.dbengine.lang.ast;

import java.util.List;

/**
 * Projects specific columns from the input relation.
 */
public record ProjectNode(QueryNode input, List<String> columns) implements QueryNode {
}
