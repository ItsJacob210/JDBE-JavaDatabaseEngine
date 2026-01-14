package com.dbengine;

import com.dbengine.exec.Operator;
import com.dbengine.lang.ast.QueryNode;
import com.dbengine.lang.lexer.Lexer;
import com.dbengine.lang.lexer.Token;
import com.dbengine.lang.parser.Parser;
import com.dbengine.planner.PhysicalPlanner;
import com.dbengine.planner.QueryOptimizer;
import com.dbengine.semantic.Catalog;
import com.dbengine.semantic.Schema;
import com.dbengine.semantic.SemanticAnalyzer;
import com.dbengine.storage.BufferPool;
import com.dbengine.storage.DiskManager;
import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;
import com.dbengine.txn.LogManager;
import com.dbengine.txn.Transaction;
import com.dbengine.txn.TransactionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main database engine that coordinates all components.
 */
public class Database {
    private static final int BUFFER_POOL_SIZE = 100;
    
    private final Path dbDirectory;
    private final DiskManager diskManager;
    private final BufferPool bufferPool;
    private final Catalog catalog;
    private final LogManager logManager;
    private final TransactionManager transactionManager;
    private final QueryOptimizer optimizer;
    private final PhysicalPlanner planner;
    private Transaction currentTransaction;
    
    public Database(String dbName) throws IOException {
        this.dbDirectory = Paths.get("db_data", dbName);
        Files.createDirectories(dbDirectory);
        
        Path dbFile = dbDirectory.resolve("data.db");
        Path logFile = dbDirectory.resolve("wal.log");
        
        this.diskManager = new DiskManager(dbFile);
        this.bufferPool = new BufferPool(diskManager, BUFFER_POOL_SIZE);
        this.catalog = new Catalog();
        this.logManager = new LogManager(logFile);
        this.transactionManager = new TransactionManager(logManager, bufferPool);
        this.optimizer = new QueryOptimizer(catalog);
        this.planner = new PhysicalPlanner(catalog, bufferPool);
        
        //recover from crash if needed
        if (Files.exists(logFile) && Files.size(logFile) > 0) {
            transactionManager.recover();
        }
    }
    
    /**
     * Execute a query string and return results.
     */
    public QueryResult execute(String query) {
        try {
            query = query.trim();
            
            //handle transaction commands
            if (query.equalsIgnoreCase("begin")) {
                currentTransaction = transactionManager.begin();
                return new QueryResult("Transaction started", new ArrayList<>());
            } else if (query.equalsIgnoreCase("commit")) {
                if (currentTransaction != null) {
                    transactionManager.commit(currentTransaction);
                    currentTransaction = null;
                    return new QueryResult("Transaction committed", new ArrayList<>());
                } else {
                    return new QueryResult("No active transaction", new ArrayList<>());
                }
            } else if (query.equalsIgnoreCase("abort")) {
                if (currentTransaction != null) {
                    transactionManager.abort(currentTransaction);
                    currentTransaction = null;
                    return new QueryResult("Transaction aborted", new ArrayList<>());
                } else {
                    return new QueryResult("No active transaction", new ArrayList<>());
                }
            }
            
            //handle explain
            boolean isExplain = query.toLowerCase().startsWith("explain");
            if (isExplain) {
                query = query.substring(7).trim();
            }
            
            //compilation pipeline
            Lexer lexer = new Lexer(query);
            List<Token> tokens = lexer.scanTokens();
            
            Parser parser = new Parser(tokens);
            QueryNode ast = parser.parse();
            
            SemanticAnalyzer analyzer = new SemanticAnalyzer(catalog);
            analyzer.analyze(ast);  //validate but don't need the output schema here
            
            QueryNode optimized = optimizer.optimize(ast);
            
            if (isExplain) {
                String plan = explainPlan(optimized);
                return new QueryResult(plan, new ArrayList<>());
            }
            
            Operator physicalPlan = planner.createPlan(optimized);
            
            //execute
            List<Tuple> results = new ArrayList<>();
            physicalPlan.open();
            
            Tuple tuple;
            while ((tuple = physicalPlan.next()) != null) {
                results.add(tuple);
            }
            
            physicalPlan.close();
            
            return new QueryResult("Success", results);
            
        } catch (Exception e) {
            return new QueryResult("Error: " + e.getMessage(), new ArrayList<>());
        }
    }
    
    /**
     * Create a table in the catalog.
     */
    public TableHeap createTable(String name, Schema schema) throws IOException {
        catalog.registerTable(name, schema);
        TableHeap tableHeap = new TableHeap(bufferPool, schema);
        planner.registerTableHeap(name, tableHeap);
        return tableHeap;
    }
    
    /**
     * Get the catalog.
     */
    public Catalog getCatalog() {
        return catalog;
    }
    
    /**
     * Get the planner (for registering table heaps and indexes).
     */
    public PhysicalPlanner getPlanner() {
        return planner;
    }
    
    /**
     * Get the buffer pool (for direct storage access).
     */
    public BufferPool getBufferPool() {
        return bufferPool;
    }
    
    /**
     * Shutdown the database cleanly.
     */
    public void shutdown() throws IOException {
        if (currentTransaction != null) {
            transactionManager.commit(currentTransaction);
        }
        
        bufferPool.flushAllPages();
        logManager.close();
        diskManager.close();
    }
    
    /**
     * Explain a query plan.
     */
    private String explainPlan(QueryNode node) {
        return explainNode(node, 0);
    }
    
    private String explainNode(QueryNode node, int indent) {
        String prefix = "  ".repeat(indent);
        
        return switch (node) {
            case com.dbengine.lang.ast.SourceNode s ->
                prefix + "SeqScan(" + s.table() + ")";
            case com.dbengine.lang.ast.FilterNode f ->
                prefix + "Filter(" + f.predicate() + ")\n" + explainNode(f.input(), indent + 1);
            case com.dbengine.lang.ast.ProjectNode p ->
                prefix + "Project(" + String.join(", ", p.columns()) + ")\n" + explainNode(p.input(), indent + 1);
            case com.dbengine.lang.ast.SortNode s ->
                prefix + "Sort(" + s.column() + " " + s.order() + ")\n" + explainNode(s.input(), indent + 1);
            case com.dbengine.lang.ast.LimitNode l ->
                prefix + "Limit(" + l.count() + ")\n" + explainNode(l.input(), indent + 1);
            case com.dbengine.lang.ast.TakeNode t ->
                prefix + "Take(" + t.count() + ")\n" + explainNode(t.input(), indent + 1);
            case com.dbengine.lang.ast.SkipNode s ->
                prefix + "Skip(" + s.count() + ")\n" + explainNode(s.input(), indent + 1);
            case com.dbengine.lang.ast.ModifyNode m ->
                prefix + "Modify(" + m.updates().keySet() + ")\n" + explainNode(m.input(), indent + 1);
            case com.dbengine.lang.ast.RemoveNode r ->
                prefix + "Remove\n" + explainNode(r.input(), indent + 1);
        };
    }
    
    /**
     * Result of a query execution.
     */
    public record QueryResult(String message, List<Tuple> tuples) {}
}
