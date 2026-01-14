package com.dbengine.lang.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lexical analyzer that converts source text into tokens.
 */
public class Lexer {
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("filter", TokenType.FILTER),
        Map.entry("project", TokenType.PROJECT),
        Map.entry("sort", TokenType.SORT),
        Map.entry("limit", TokenType.LIMIT),
        Map.entry("take", TokenType.TAKE),
        Map.entry("skip", TokenType.SKIP),
        Map.entry("modify", TokenType.MODIFY),
        Map.entry("remove", TokenType.REMOVE),
        Map.entry("add", TokenType.ADD),
        Map.entry("table", TokenType.TABLE),
        Map.entry("values", TokenType.VALUES),
        Map.entry("explain", TokenType.EXPLAIN),
        Map.entry("query", TokenType.QUERY),
        Map.entry("begin", TokenType.BEGIN),
        Map.entry("commit", TokenType.COMMIT),
        Map.entry("abort", TokenType.ABORT),
        Map.entry("asc", TokenType.ASC),
        Map.entry("desc", TokenType.DESC),
        Map.entry("and", TokenType.AND),
        Map.entry("or", TokenType.OR),
        Map.entry("true", TokenType.TRUE),
        Map.entry("false", TokenType.FALSE)
    );
    
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    
    public Lexer(String source) {
        this.source = source;
    }
    
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }
    
    private void scanToken() {
        int startColumn = column;
        char c = advance();
        
        switch (c) {
            case ' ':
            case '\r':
            case '\t':
                //ignore whitespace
                break;
            case '\n':
                line++;
                column = 1;
                break;
            case '(':
                addToken(TokenType.LPAREN, startColumn);
                break;
            case ')':
                addToken(TokenType.RPAREN, startColumn);
                break;
            case ',':
                addToken(TokenType.COMMA, startColumn);
                break;
            case '|':
                if (match('>')) {
                    addToken(TokenType.PIPE_GT, startColumn);
                } else {
                    throw new LexerException("Unexpected character '|' at " + line + ":" + startColumn);
                }
                break;
            case '=':
                if (match('=')) {
                    addToken(TokenType.EQ_EQ, startColumn);
                } else {
                    addToken(TokenType.EQ, startColumn);
                }
                break;
            case '!':
                if (match('=')) {
                    addToken(TokenType.BANG_EQ, startColumn);
                } else {
                    throw new LexerException("Unexpected character '!' at " + line + ":" + startColumn);
                }
                break;
            case '>':
                addToken(match('=') ? TokenType.GE : TokenType.GT, startColumn);
                break;
            case '<':
                addToken(match('=') ? TokenType.LE : TokenType.LT, startColumn);
                break;
            case '"':
                string(startColumn);
                break;
            default:
                if (isDigit(c)) {
                    number(startColumn);
                } else if (isAlpha(c)) {
                    identifier(startColumn);
                } else {
                    throw new LexerException("Unexpected character '" + c + "' at " + line + ":" + startColumn);
                }
                break;
        }
    }
    
    private void identifier(int startColumn) {
        while (isAlphaNumeric(peek())) advance();
        
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        
        if (type == TokenType.TRUE || type == TokenType.FALSE) {
            addToken(type, type == TokenType.TRUE, startColumn);
        } else {
            addToken(type, startColumn);
        }
    }
    
    private void number(int startColumn) {
        while (isDigit(peek())) advance();
        
        String text = source.substring(start, current);
        addToken(TokenType.INTEGER, Integer.parseInt(text), startColumn);
    }
    
    private void string(int startColumn) {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                column = 0;
            }
            advance();
        }
        
        if (isAtEnd()) {
            throw new LexerException("Unterminated string at " + line + ":" + startColumn);
        }
        
        //consume closing "
        advance();
        
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value, startColumn);
    }
    
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        
        current++;
        column++;
        return true;
    }
    
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }
    
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }
    
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
    
    private boolean isAtEnd() {
        return current >= source.length();
    }
    
    private char advance() {
        column++;
        return source.charAt(current++);
    }
    
    private void addToken(TokenType type, int startColumn) {
        addToken(type, null, startColumn);
    }
    
    private void addToken(TokenType type, Object literal, int startColumn) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line, startColumn));
    }
}
