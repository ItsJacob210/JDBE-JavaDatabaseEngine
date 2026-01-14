package com.dbengine.lang.lexer;

/**
 * Exception thrown during lexical analysis.
 */
public class LexerException extends RuntimeException {
    public LexerException(String message) {
        super(message);
    }
}
