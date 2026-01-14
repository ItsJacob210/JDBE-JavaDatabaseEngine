package com.dbengine;

import com.dbengine.Database.QueryResult;
import com.dbengine.semantic.DataType;
import com.dbengine.semantic.Schema;
import com.dbengine.storage.RecordId;
import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    private Database db;
    private Path dbDir;
    
    @BeforeEach
    void setup() throws IOException {
        dbDir = Files.createTempDirectory("test_db");
        db = new Database(dbDir.getFileName().toString());
        
        // Create test table
        Schema schema = new Schema();
        schema.addColumn("id", DataType.INTEGER);
        schema.addColumn("name", DataType.STRING);
        schema.addColumn("age", DataType.INTEGER);
        schema.addColumn("active", DataType.BOOLEAN);
        
        db.createTable("users", schema);
    }
    
    @AfterEach
    void cleanup() throws IOException {
        db.shutdown();
        //clean up test directory
        Files.walk(dbDir)
            .sorted((a, b) -> -a.compareTo(b))
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    //ignore
                }
            });
    }
    
    @Test
    void testSimpleQuery() {
        QueryResult result = db.execute("users |> project(id, name)");
        assertNotNull(result);
        assertEquals("Success", result.message());
    }
    
    @Test
    void testFilterQuery() {
        QueryResult result = db.execute("users |> filter(age > 18) |> project(name)");
        assertNotNull(result);
        assertEquals("Success", result.message());
    }
    
    @Test
    void testComplexQuery() {
        QueryResult result = db.execute(
            "users |> filter(age >= 18) |> project(id, name, age) |> sort(age desc) |> limit(5)"
        );
        assertNotNull(result);
        assertEquals("Success", result.message());
    }
    
    @Test
    void testExplainQuery() {
        QueryResult result = db.execute("explain users |> filter(age > 18) |> project(name)");
        assertNotNull(result);
        assertTrue(result.message().contains("Query Plan"));
    }
    
    @Test
    void testTransactions() {
        QueryResult begin = db.execute("begin");
        assertEquals("Transaction started", begin.message());
        
        QueryResult commit = db.execute("commit");
        assertEquals("Transaction committed", commit.message());
    }
    
    @Test
    void testInvalidQuery() {
        QueryResult result = db.execute("invalid query syntax");
        assertTrue(result.message().startsWith("Error:"));
    }
}
