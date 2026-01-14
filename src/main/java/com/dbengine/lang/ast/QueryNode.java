package com.dbengine.lang.ast;

/**
 * Base interface for all query AST nodes.
 * AST nodes never contain execution logic - they are pure data structures
 * representing the relational algebra.
 */
public sealed interface QueryNode permits
    SourceNode,
    FilterNode,
    ProjectNode,
    SortNode,
    LimitNode,
    TakeNode,
    SkipNode,
    ModifyNode,
    RemoveNode {
}
