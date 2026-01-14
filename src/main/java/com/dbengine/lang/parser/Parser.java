package com.dbengine.lang.parser;

import com.dbengine.lang.ast.*;
import com.dbengine.lang.ast.BinaryExpr.BinaryOp;
import com.dbengine.lang.ast.LiteralExpr.LiteralType;
import com.dbengine.lang.ast.SortNode.Order;
import com.dbengine.lang.lexer.Token;
import com.dbengine.lang.lexer.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recursive-descent parser for the functional pipeline query language.
 * Implements the formal grammar specified in the design document.
 */
public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    /**
     * Parse a complete query.
     * query ::= source pipeline+
     */
    public QueryNode parse() {
        QueryNode node = source();
        
        while (!isAtEnd() && check(TokenType.PIPE_GT)) {
            node = pipeline(node);
        }
        
        if (!isAtEnd() && !check(TokenType.EOF)) {
            throw error(peek(), "Expected pipeline operator '|>' or end of query");
        }
        
        return node;
    }
    
    /**
     * source ::= IDENTIFIER
     */
    private QueryNode source() {
        if (!check(TokenType.IDENTIFIER)) {
            throw error(peek(), "Expected table name at start of query");
        }
        Token name = advance();
        return new SourceNode(name.lexeme());
    }
    
    /**
     * pipeline ::= "|>" operation
     */
    private QueryNode pipeline(QueryNode input) {
        consume(TokenType.PIPE_GT, "Expected '|>'");
        return operation(input);
    }
    
    /**
     * operation ::= filter | project | sort | limit | take | skip | modify | remove
     */
    private QueryNode operation(QueryNode input) {
        Token token = peek();
        
        return switch (token.type()) {
            case FILTER -> filter(input);
            case PROJECT -> project(input);
            case SORT -> sort(input);
            case LIMIT -> limit(input);
            case TAKE -> take(input);
            case SKIP -> skip(input);
            case MODIFY -> modify(input);
            case REMOVE -> remove(input);
            default -> throw error(token, "Expected operation name (filter, project, sort, etc.)");
        };
    }
    
    /**
     * filter ::= "filter" "(" expression ")"
     */
    private QueryNode filter(QueryNode input) {
        consume(TokenType.FILTER, "Expected 'filter'");
        consume(TokenType.LPAREN, "Expected '(' after 'filter'");
        Expr predicate = expression();
        consume(TokenType.RPAREN, "Expected ')' after filter expression");
        return new FilterNode(input, predicate);
    }
    
    /**
     * project ::= "project" "(" ident_list ")"
     */
    private QueryNode project(QueryNode input) {
        consume(TokenType.PROJECT, "Expected 'project'");
        consume(TokenType.LPAREN, "Expected '(' after 'project'");
        List<String> columns = identifierList();
        consume(TokenType.RPAREN, "Expected ')' after column list");
        return new ProjectNode(input, columns);
    }
    
    /**
     * sort ::= "sort" "(" IDENTIFIER [("asc"|"desc")] ")"
     */
    private QueryNode sort(QueryNode input) {
        consume(TokenType.SORT, "Expected 'sort'");
        consume(TokenType.LPAREN, "Expected '(' after 'sort'");
        
        Token column = consume(TokenType.IDENTIFIER, "Expected column name");
        Order order = Order.ASC;
        
        if (check(TokenType.ASC)) {
            advance();
            order = Order.ASC;
        } else if (check(TokenType.DESC)) {
            advance();
            order = Order.DESC;
        }
        
        consume(TokenType.RPAREN, "Expected ')' after sort clause");
        return new SortNode(input, column.lexeme(), order);
    }
    
    /**
     * limit ::= "limit" "(" INT ")"
     */
    private QueryNode limit(QueryNode input) {
        consume(TokenType.LIMIT, "Expected 'limit'");
        consume(TokenType.LPAREN, "Expected '(' after 'limit'");
        Token count = consume(TokenType.INTEGER, "Expected integer");
        consume(TokenType.RPAREN, "Expected ')' after limit value");
        return new LimitNode(input, (Integer) count.literal());
    }
    
    /**
     * take ::= "take" "(" INT ")"
     */
    private QueryNode take(QueryNode input) {
        consume(TokenType.TAKE, "Expected 'take'");
        consume(TokenType.LPAREN, "Expected '(' after 'take'");
        Token count = consume(TokenType.INTEGER, "Expected integer");
        consume(TokenType.RPAREN, "Expected ')' after take value");
        return new TakeNode(input, (Integer) count.literal());
    }
    
    /**
     * skip ::= "skip" "(" INT ")"
     */
    private QueryNode skip(QueryNode input) {
        consume(TokenType.SKIP, "Expected 'skip'");
        consume(TokenType.LPAREN, "Expected '(' after 'skip'");
        Token count = consume(TokenType.INTEGER, "Expected integer");
        consume(TokenType.RPAREN, "Expected ')' after skip value");
        return new SkipNode(input, (Integer) count.literal());
    }
    
    /**
     * modify ::= "modify" "(" assignment_list ")"
     */
    private QueryNode modify(QueryNode input) {
        consume(TokenType.MODIFY, "Expected 'modify'");
        consume(TokenType.LPAREN, "Expected '(' after 'modify'");
        Map<String, Expr> updates = assignmentList();
        consume(TokenType.RPAREN, "Expected ')' after assignments");
        return new ModifyNode(input, updates);
    }
    
    /**
     * remove ::= "remove"
     */
    private QueryNode remove(QueryNode input) {
        consume(TokenType.REMOVE, "Expected 'remove'");
        return new RemoveNode(input);
    }
    
    /**
     * ident_list ::= IDENTIFIER ("," IDENTIFIER)*
     */
    private List<String> identifierList() {
        List<String> identifiers = new ArrayList<>();
        identifiers.add(consume(TokenType.IDENTIFIER, "Expected identifier").lexeme());
        
        while (match(TokenType.COMMA)) {
            identifiers.add(consume(TokenType.IDENTIFIER, "Expected identifier").lexeme());
        }
        
        return identifiers;
    }
    
    /**
     * assignment_list ::= IDENTIFIER "=" expression ("," IDENTIFIER "=" expression)*
     */
    private Map<String, Expr> assignmentList() {
        Map<String, Expr> assignments = new HashMap<>();
        
        do {
            Token name = consume(TokenType.IDENTIFIER, "Expected column name");
            consume(TokenType.EQ, "Expected '=' after column name");
            Expr value = expression();
            assignments.put(name.lexeme(), value);
        } while (match(TokenType.COMMA));
        
        return assignments;
    }
    
    /**
     * expression ::= logical_or
     */
    private Expr expression() {
        return logicalOr();
    }
    
    /**
     * logical_or ::= logical_and ("or" logical_and)*
     */
    private Expr logicalOr() {
        Expr expr = logicalAnd();
        
        while (match(TokenType.OR)) {
            Expr right = logicalAnd();
            expr = new BinaryExpr(expr, BinaryOp.OR, right);
        }
        
        return expr;
    }
    
    /**
     * logical_and ::= equality ("and" equality)*
     */
    private Expr logicalAnd() {
        Expr expr = equality();
        
        while (match(TokenType.AND)) {
            Expr right = equality();
            expr = new BinaryExpr(expr, BinaryOp.AND, right);
        }
        
        return expr;
    }
    
    /**
     * equality ::= relational (("=="|"!=") relational)*
     */
    private Expr equality() {
        Expr expr = relational();
        
        while (match(TokenType.EQ_EQ, TokenType.BANG_EQ)) {
            Token operator = previous();
            Expr right = relational();
            BinaryOp op = operator.type() == TokenType.EQ_EQ ? BinaryOp.EQ : BinaryOp.NE;
            expr = new BinaryExpr(expr, op, right);
        }
        
        return expr;
    }
    
    /**
     * relational ::= primary ((">"|"<"|">="|"<=") primary)?
     */
    private Expr relational() {
        Expr expr = primary();
        
        if (match(TokenType.GT, TokenType.GE, TokenType.LT, TokenType.LE)) {
            Token operator = previous();
            Expr right = primary();
            BinaryOp op = switch (operator.type()) {
                case GT -> BinaryOp.GT;
                case GE -> BinaryOp.GE;
                case LT -> BinaryOp.LT;
                case LE -> BinaryOp.LE;
                default -> throw new AssertionError();
            };
            expr = new BinaryExpr(expr, op, right);
        }
        
        return expr;
    }
    
    /**
     * primary ::= IDENTIFIER | literal | "(" expression ")"
     */
    private Expr primary() {
        if (match(TokenType.IDENTIFIER)) {
            return new IdentifierExpr(previous().lexeme());
        }
        
        if (match(TokenType.INTEGER)) {
            return new LiteralExpr(previous().literal(), LiteralType.INTEGER);
        }
        
        if (match(TokenType.STRING)) {
            return new LiteralExpr(previous().literal(), LiteralType.STRING);
        }
        
        if (match(TokenType.TRUE)) {
            return new LiteralExpr(true, LiteralType.BOOLEAN);
        }
        
        if (match(TokenType.FALSE)) {
            return new LiteralExpr(false, LiteralType.BOOLEAN);
        }
        
        if (match(TokenType.LPAREN)) {
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expected ')' after expression");
            return expr;
        }
        
        throw error(peek(), "Expected expression");
    }
    
    //helper methods
    
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }
    
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }
    
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    
    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }
    
    private Token peek() {
        return tokens.get(current);
    }
    
    private Token previous() {
        return tokens.get(current - 1);
    }
    
    private ParseException error(Token token, String message) {
        return new ParseException(message + " at " + token.line() + ":" + token.column());
    }
}
