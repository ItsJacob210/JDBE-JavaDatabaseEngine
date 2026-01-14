package com.dbengine;

import com.dbengine.lang.ast.*;
import com.dbengine.lang.lexer.Lexer;
import com.dbengine.lang.lexer.Token;
import com.dbengine.lang.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    
    @Test
    void testSourceNode() {
        String query = "users";
        QueryNode ast = parse(query);
        
        assertTrue(ast instanceof SourceNode);
        assertEquals("users", ((SourceNode) ast).table());
    }
    
    @Test
    void testFilterNode() {
        String query = "users |> filter(age > 18)";
        QueryNode ast = parse(query);
        
        assertTrue(ast instanceof FilterNode);
        FilterNode filter = (FilterNode) ast;
        assertTrue(filter.input() instanceof SourceNode);
        assertTrue(filter.predicate() instanceof BinaryExpr);
    }
    
    @Test
    void testProjectNode() {
        String query = "users |> project(id, name, age)";
        QueryNode ast = parse(query);
        
        assertTrue(ast instanceof ProjectNode);
        ProjectNode project = (ProjectNode) ast;
        assertEquals(3, project.columns().size());
        assertTrue(project.columns().contains("id"));
        assertTrue(project.columns().contains("name"));
    }
    
    @Test
    void testComplexQuery() {
        String query = "users |> filter(age >= 18) |> project(name) |> sort(name asc) |> limit(10)";
        QueryNode ast = parse(query);
        
        assertTrue(ast instanceof LimitNode);
        LimitNode limit = (LimitNode) ast;
        assertTrue(limit.input() instanceof SortNode);
        
        SortNode sort = (SortNode) limit.input();
        assertTrue(sort.input() instanceof ProjectNode);
        
        ProjectNode project = (ProjectNode) sort.input();
        assertTrue(project.input() instanceof FilterNode);
        
        FilterNode filter = (FilterNode) project.input();
        assertTrue(filter.input() instanceof SourceNode);
    }
    
    private QueryNode parse(String query) {
        Lexer lexer = new Lexer(query);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }
}
