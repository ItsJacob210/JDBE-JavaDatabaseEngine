package com.dbengine.lang.parser;

/**
 * Exception thrown during parsing.
 */
public class ParseException extends RuntimeException {
    public ParseException(String message) {
        super(message);
    }
}
