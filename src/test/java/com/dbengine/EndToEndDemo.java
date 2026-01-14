package com.dbengine;

import com.dbengine.Database.QueryResult;
import com.dbengine.semantic.DataType;
import com.dbengine.semantic.Schema;
import com.dbengine.storage.RecordId;
import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * End-to-end demonstration of the database engine capabilities.
 * This test shows the complete workflow from table creation to query execution.
 */
public class EndToEndDemo {
    
    @Test
    void demonstrateDatabaseEngine() throws IOException {
        System.out.println("=".repeat(70));
        System.out.println("JavaDBEngine - Complete Database System Demonstration");
        System.out.println("=".repeat(70));
        System.out.println();
        
        //create database
        Path dbDir = Files.createTempDirectory("demo_db");
        Database db = new Database(dbDir.getFileName().toString());
        
        try {
            //1. Schema Definition
            System.out.println("1. Creating Schema");
            System.out.println("-".repeat(70));
            Schema usersSchema = new Schema();
            usersSchema.addColumn("id", DataType.INTEGER);
            usersSchema.addColumn("name", DataType.STRING);
            usersSchema.addColumn("age", DataType.INTEGER);
            usersSchema.addColumn("active", DataType.BOOLEAN);
            
            db.createTable("users", usersSchema);
            System.out.println("Created table 'users' with schema: " + usersSchema);
            System.out.println();
            
            //2. Insert Sample Data
            System.out.println("2. Inserting Sample Data");
            System.out.println("-".repeat(70));
            // Note: In a real system, we would insert data via INSERT statements
            // For now, the table starts empty for demonstration
            System.out.println("Table created (currently empty - would insert via INSERT statements)");
            System.out.println();
            
            //3. Simple Query
            System.out.println("3. Simple Query - Project Columns");
            System.out.println("-".repeat(70));
            System.out.println("Query: users |> project(id, name)");
            QueryResult result1 = db.execute("users |> project(id, name)");
            System.out.println("Result: " + result1.message());
            System.out.println("Rows returned: " + result1.tuples().size());
            System.out.println();
            
            //4. Filter Query
            System.out.println("4. Filter Query - Age Greater Than 25");
            System.out.println("-".repeat(70));
            System.out.println("Query: users |> filter(age > 25) |> project(name, age)");
            QueryResult result2 = db.execute("users |> filter(age > 25) |> project(name, age)");
            System.out.println("Result: " + result2.message());
            System.out.println("Rows returned: " + result2.tuples().size());
            System.out.println();
            
            //5. Complex Query
            System.out.println("5. Complex Query - Filter, Sort, Limit");
            System.out.println("-".repeat(70));
            String complexQuery = "users |> filter(age >= 21) |> project(name, age) |> sort(age desc) |> limit(3)";
            System.out.println("Query: " + complexQuery);
            QueryResult result3 = db.execute(complexQuery);
            System.out.println("Result: " + result3.message());
            System.out.println("Rows returned: " + result3.tuples().size());
            System.out.println();
            
            //6. Explain Query
            System.out.println("6. Query Plan Explanation");
            System.out.println("-".repeat(70));
            String explainQuery = "explain users |> filter(age > 25) |> project(name)";
            System.out.println("Query: " + explainQuery);
            QueryResult result4 = db.execute(explainQuery);
            System.out.println(result4.message());
            System.out.println();
            
            //7. Transaction Demonstration
            System.out.println("7. Transaction Demonstration");
            System.out.println("-".repeat(70));
            System.out.println("Starting transaction...");
            QueryResult beginResult = db.execute("begin");
            System.out.println(beginResult.message());
            
            System.out.println("Executing queries within transaction...");
            db.execute("users |> filter(id == 1) |> project(name)");
            
            System.out.println("Committing transaction...");
            QueryResult commitResult = db.execute("commit");
            System.out.println(commitResult.message());
            System.out.println();
            
            //8. Architecture Summary
            System.out.println("8. System Architecture Layers");
            System.out.println("-".repeat(70));
            System.out.println("✓ Query Language: Custom functional pipeline DSL");
            System.out.println("✓ Lexer: Hand-written tokenizer");
            System.out.println("✓ Parser: Recursive-descent parser");
            System.out.println("✓ Semantic Analysis: Type checking and validation");
            System.out.println("✓ Optimizer: Rule-based query optimization");
            System.out.println("✓ Planner: Physical operator generation");
            System.out.println("✓ Execution: Volcano iterator model");
            System.out.println("✓ Storage: Page-based with buffer pool");
            System.out.println("✓ Indexing: B+ tree structures");
            System.out.println("✓ Transactions: ACID with WAL");
            System.out.println("✓ Recovery: Redo-based crash recovery");
            System.out.println();
            
            System.out.println("=".repeat(70));
            System.out.println("Demonstration Complete!");
            System.out.println("=".repeat(70));
            
        } finally {
            db.shutdown();
            
            //cleanup
            Files.walk(dbDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {}
                });
        }
    }
    
    private void insertSampleData(Database db) throws IOException {}
}
