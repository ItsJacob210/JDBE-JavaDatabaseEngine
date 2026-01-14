package com.dbengine.lang.lexer;

/**
 * All token types in the query language.
 */
public enum TokenType {
    //literals
    IDENTIFIER, INTEGER, STRING, TRUE, FALSE,
    
    //operators and punctuation
    PIPE_GT,        //|>
    LPAREN, RPAREN, //( )
    COMMA,          //,
    EQ_EQ,          //==
    BANG_EQ,        //!=
    GT, GE,         //> >=
    LT, LE,         //< <=
    EQ,             //=
    
    //keywords
    FILTER, PROJECT, SORT, LIMIT, TAKE, SKIP,
    MODIFY, REMOVE, ADD, TABLE, VALUES,
    EXPLAIN, QUERY, BEGIN, COMMIT, ABORT,
    ASC, DESC,
    AND, OR,
    
    //special
    EOF
}
