package com.dbengine;

import com.dbengine.semantic.DataType;
import com.dbengine.semantic.Schema;
import com.dbengine.storage.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StorageTest {
    private Path tempDbFile;
    private DiskManager diskManager;
    private BufferPool bufferPool;
    private Schema schema;
    
    @BeforeEach
    void setup() throws IOException {
        tempDbFile = Files.createTempFile("test_db", ".db");
        diskManager = new DiskManager(tempDbFile);
        bufferPool = new BufferPool(diskManager, 10);
        
        schema = new Schema();
        schema.addColumn("id", DataType.INTEGER);
        schema.addColumn("name", DataType.STRING);
        schema.addColumn("age", DataType.INTEGER);
    }
    
    @AfterEach
    void cleanup() throws IOException {
        diskManager.close();
        Files.deleteIfExists(tempDbFile);
    }
    
    @Test
    void testPageAllocation() throws IOException {
        Page page = bufferPool.newPage();
        assertNotNull(page);
        assertTrue(page.getPageId() >= 0);
    }
    
    @Test
    void testTupleInsertAndRetrieve() throws IOException {
        TableHeap tableHeap = new TableHeap(bufferPool, schema);
        
        // Create tuple
        Map<String, Integer> columnMap = new HashMap<>();
        columnMap.put("id", 0);
        columnMap.put("name", 1);
        columnMap.put("age", 2);
        
        Tuple tuple = new Tuple(new Object[]{1, "Alice", 25}, columnMap);
        
        // Insert
        RecordId rid = tableHeap.insertTuple(tuple);
        assertNotNull(rid);
        
        // Retrieve
        Tuple retrieved = tableHeap.getTuple(rid);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.getValue("id"));
        assertEquals("Alice", retrieved.getValue("name"));
        assertEquals(25, retrieved.getValue("age"));
    }
    
    @Test
    void testMultipleTuples() throws IOException {
        TableHeap tableHeap = new TableHeap(bufferPool, schema);
        
        Map<String, Integer> columnMap = new HashMap<>();
        columnMap.put("id", 0);
        columnMap.put("name", 1);
        columnMap.put("age", 2);
        
        // Insert multiple tuples
        for (int i = 0; i < 10; i++) {
            Tuple tuple = new Tuple(new Object[]{i, "User" + i, 20 + i}, columnMap);
            tableHeap.insertTuple(tuple);
        }
        
        //retrieve all
        List<Tuple> allTuples = tableHeap.getAllTuples();
        assertEquals(10, allTuples.size());
    }
}
