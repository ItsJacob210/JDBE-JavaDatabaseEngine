package com.dbengine;

import com.dbengine.lang.lexer.Lexer;
import com.dbengine.lang.lexer.Token;
import com.dbengine.lang.lexer.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {
    
    @Test
    void testSimpleQuery() {
        String query = "users |> filter(age > 18)";
        Lexer lexer = new Lexer(query);
        List<Token> tokens = lexer.scanTokens();
        
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("users", tokens.get(0).lexeme());
        
        assertEquals(TokenType.PIPE_GT, tokens.get(1).type());
        assertEquals(TokenType.FILTER, tokens.get(2).type());
    }
    
    @Test
    void testOperators() {
        String query = "age >= 18 and active == true";
        Lexer lexer = new Lexer(query);
        List<Token> tokens = lexer.scanTokens();
        
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.GE));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.AND));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.EQ_EQ));
    }
    
    @Test
    void testLiterals() {
        String query = "name == \"John\" and age == 25 and active == true";
        Lexer lexer = new Lexer(query);
        List<Token> tokens = lexer.scanTokens();
        
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.STRING && "John".equals(t.literal())));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.INTEGER && Integer.valueOf(25).equals(t.literal())));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.TRUE));
    }
}
