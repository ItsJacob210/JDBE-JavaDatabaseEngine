package com.dbengine;

import com.dbengine.exec.*;
import com.dbengine.lang.ast.*;
import com.dbengine.semantic.DataType;
import com.dbengine.semantic.Schema;
import com.dbengine.storage.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionTest {
    private Path tempDbFile;
    private DiskManager diskManager;
    private BufferPool bufferPool;
    private Schema schema;
    private TableHeap tableHeap;
    
    @BeforeEach
    void setup() throws IOException {
        tempDbFile = Files.createTempFile("test_db", ".db");
        diskManager = new DiskManager(tempDbFile);
        bufferPool = new BufferPool(diskManager, 10);
        
        schema = new Schema();
        schema.addColumn("id", DataType.INTEGER);
        schema.addColumn("name", DataType.STRING);
        schema.addColumn("age", DataType.INTEGER);
        
        tableHeap = new TableHeap(bufferPool, schema);
        
        //insert test data
        Map<String, Integer> columnMap = new HashMap<>();
        columnMap.put("id", 0);
        columnMap.put("name", 1);
        columnMap.put("age", 2);
        
        for (int i = 0; i < 10; i++) {
            Tuple tuple = new Tuple(new Object[]{i, "User" + i, 20 + i}, columnMap);
            tableHeap.insertTuple(tuple);
        }
    }
    
    @AfterEach
    void cleanup() throws IOException {
        diskManager.close();
        Files.deleteIfExists(tempDbFile);
    }
    
    @Test
    void testSeqScan() throws Exception {
        SeqScanOperator scan = new SeqScanOperator(tableHeap);
        scan.open();
        
        List<Tuple> results = new ArrayList<>();
        Tuple tuple;
        while ((tuple = scan.next()) != null) {
            results.add(tuple);
        }
        
        scan.close();
        assertEquals(10, results.size());
    }
    
    @Test
    void testFilter() throws Exception {
        SeqScanOperator scan = new SeqScanOperator(tableHeap);
        
        //filter: age > 25
        BinaryExpr predicate = new BinaryExpr(
            new IdentifierExpr("age"),
            BinaryExpr.BinaryOp.GT,
            new LiteralExpr(25, LiteralExpr.LiteralType.INTEGER)
        );
        
        FilterOperator filter = new FilterOperator(scan, predicate);
        filter.open();
        
        List<Tuple> results = new ArrayList<>();
        Tuple tuple;
        while ((tuple = filter.next()) != null) {
            results.add(tuple);
        }
        
        filter.close();
        assertTrue(results.size() < 10);
        
        //all results should have age > 25
        for (Tuple t : results) {
            Integer age = (Integer) t.getValue("age");
            assertTrue(age > 25);
        }
    }
    
    @Test
    void testProjection() throws Exception {
        SeqScanOperator scan = new SeqScanOperator(tableHeap);
        ProjectionOperator project = new ProjectionOperator(scan, List.of("id", "name"));
        
        project.open();
        
        Tuple tuple = project.next();
        assertNotNull(tuple);
        assertEquals(2, tuple.getSize());
        assertNotNull(tuple.getValue("id"));
        assertNotNull(tuple.getValue("name"));
        
        project.close();
    }
    
    @Test
    void testLimit() throws Exception {
        SeqScanOperator scan = new SeqScanOperator(tableHeap);
        LimitOperator limit = new LimitOperator(scan, 3);
        
        limit.open();
        
        List<Tuple> results = new ArrayList<>();
        Tuple tuple;
        while ((tuple = limit.next()) != null) {
            results.add(tuple);
        }
        
        limit.close();
        assertEquals(3, results.size());
    }
}
