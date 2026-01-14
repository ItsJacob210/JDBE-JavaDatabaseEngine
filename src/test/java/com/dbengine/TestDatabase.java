package com.dbengine;

import com.dbengine.Database;
import com.dbengine.semantic.DataType;
import com.dbengine.semantic.Schema;

public class TestDatabase {
    public static void main(String[] args) {
        try {
            System.out.println("=".repeat(60));
            System.out.println("JavaDBEngine - Quick Test");
            System.out.println("=".repeat(60));
            
            //create database
            Database db = new Database("test");
            
            //create users table
            Schema schema = new Schema();
            schema.addColumn("id", DataType.INTEGER);
            schema.addColumn("name", DataType.STRING);
            schema.addColumn("age", DataType.INTEGER);
            schema.addColumn("active", DataType.BOOLEAN);
            
            db.createTable("users", schema);
            System.out.println("✓ Created 'users' table");
            
            //test queries
            System.out.println("\n--- Testing Queries ---\n");
            
            //1. simple project
            System.out.println("Query: users |> project(id, name)");
            var result1 = db.execute("users |> project(id, name)");
            System.out.println("Result: " + result1.message());
            System.out.println("Rows: " + result1.tuples().size());
            
            //2. filter query
            System.out.println("\nQuery: users |> filter(age > 25) |> project(name)");
            var result2 = db.execute("users |> filter(age > 25) |> project(name)");
            System.out.println("Result: " + result2.message());
            
            //3. complex query
            System.out.println("\nQuery: users |> filter(active == true) |> sort(age desc) |> limit(10)");
            var result3 = db.execute("users |> filter(active == true) |> sort(age desc) |> limit(10)");
            System.out.println("Result: " + result3.message());
            
            //4. explain query
            System.out.println("\n--- Query Plan ---\n");
            var explain = db.execute("explain users |> filter(age > 25) |> project(name)");
            System.out.println(explain.message());
            
            //5. transactions
            System.out.println("\n--- Testing Transactions ---\n");
            db.execute("begin");
            System.out.println("✓ Transaction started");
            
            db.execute("users |> filter(id == 1) |> project(name)");
            System.out.println("✓ Executed query in transaction");
            
            db.execute("commit");
            System.out.println("✓ Transaction committed");
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("All tests completed successfully!");
            System.out.println("=".repeat(60));
            
            db.shutdown();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
