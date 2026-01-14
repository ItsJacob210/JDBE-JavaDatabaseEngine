package com.dbengine.lang.ast;

/**
 * Base interface for all expression AST nodes.
 * Expressions are typed during semantic analysis, not parsing.
 */
public sealed interface Expr permits
    BinaryExpr,
    IdentifierExpr,
    LiteralExpr {
}
