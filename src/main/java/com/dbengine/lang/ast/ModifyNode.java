package com.dbengine.lang.ast;

import java.util.Map;

/**
 * Updates columns with new values based on expressions.
 */
public record ModifyNode(QueryNode input, Map<String, Expr> updates) implements QueryNode {
}
