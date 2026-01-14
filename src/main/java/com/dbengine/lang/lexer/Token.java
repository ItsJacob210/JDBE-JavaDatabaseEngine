package com.dbengine.lang.lexer;

/**
 * Represents a single token in the source query.
 */
public record Token(TokenType type, String lexeme, Object literal, int line, int column) {
    
    public Token(TokenType type, String lexeme, int line, int column) {
        this(type, lexeme, null, line, column);
    }
    
    @Override
    public String toString() {
        return String.format("%s '%s' at %d:%d", type, lexeme, line, column);
    }
}
