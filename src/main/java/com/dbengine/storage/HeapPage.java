package com.dbengine.storage;

import com.dbengine.semantic.DataType;
import com.dbengine.semantic.Schema;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Heap page that stores tuples in a slotted page format.
 * Layout:
 * - Header: page type (1 byte), tuple count (4 bytes), free space pointer (4 bytes)
 * - Slot array: grows from start, each slot is (offset: 4 bytes, length: 4 bytes)
 * - Tuples: grow from end towards start
 */
public class HeapPage {
    private static final int HEADER_SIZE = 9;  // 1 + 4 + 4
    private static final int SLOT_SIZE = 8;    // 4 + 4
    
    private final Page page;
    private final Schema schema;
    
    public HeapPage(Page page, Schema schema) {
        this.page = page;
        this.schema = schema;
        
        //initialize if this is a new page
        ByteBuffer buffer = page.getData();
        if (buffer.get(0) == 0) {
            initializePage();
        }
    }
    
    private void initializePage() {
        byte[] data = new byte[Page.PAGE_SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        buffer.put(PageType.HEAP_PAGE.getId());
        buffer.putInt(0);  //tuple count
        buffer.putInt(Page.PAGE_SIZE);  //free space pointer
        
        page.setData(data);
    }
    
    /**
     * Insert a tuple into the page.
     * Returns the slot number, or -1 if page is full.
     */
    public int insertTuple(Tuple tuple) {
        byte[] tupleData = serializeTuple(tuple);
        
        ByteBuffer buffer = ByteBuffer.wrap(page.getBytes());
        buffer.position(1);
        int tupleCount = buffer.getInt();
        int freeSpacePtr = buffer.getInt();
        
        int requiredSpace = SLOT_SIZE + tupleData.length;
        int availableSpace = freeSpacePtr - (HEADER_SIZE + tupleCount * SLOT_SIZE);
        
        if (availableSpace < requiredSpace) {
            return -1;  //page is full
        }
        
        //update free space pointer
        freeSpacePtr -= tupleData.length;
        
        //write tuple data
        byte[] pageData = page.getBytes();
        System.arraycopy(tupleData, 0, pageData, freeSpacePtr, tupleData.length);
        
        //write slot entry
        ByteBuffer newBuffer = ByteBuffer.wrap(pageData);
        newBuffer.position(HEADER_SIZE + tupleCount * SLOT_SIZE);
        newBuffer.putInt(freeSpacePtr);
        newBuffer.putInt(tupleData.length);
        
        //update header
        newBuffer.position(1);
        newBuffer.putInt(tupleCount + 1);
        newBuffer.putInt(freeSpacePtr);
        
        page.setData(pageData);
        
        return tupleCount;
    }
    
    /**
     * Get a tuple from a specific slot.
     */
    public Tuple getTuple(int slotNum) {
        ByteBuffer buffer = ByteBuffer.wrap(page.getBytes());
        buffer.position(1);
        int tupleCount = buffer.getInt();
        
        if (slotNum >= tupleCount) {
            return null;
        }
        
        buffer.position(HEADER_SIZE + slotNum * SLOT_SIZE);
        int offset = buffer.getInt();
        int length = buffer.getInt();
        
        if (offset == -1) {
            return null;  //slot is deleted
        }
        
        byte[] tupleData = new byte[length];
        buffer.position(offset);
        buffer.get(tupleData);
        
        return deserializeTuple(tupleData);
    }
    
    /**
     * Get all tuples in the page.
     */
    public List<Tuple> getAllTuples() {
        List<Tuple> tuples = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(page.getBytes());
        buffer.position(1);
        int tupleCount = buffer.getInt();
        
        for (int i = 0; i < tupleCount; i++) {
            Tuple tuple = getTuple(i);
            if (tuple != null) {
                tuple.setRecordId(new RecordId(page.getPageId(), i));
                tuples.add(tuple);
            }
        }
        
        return tuples;
    }
    
    /**
     * Delete a tuple at a specific slot.
     */
    public void deleteTuple(int slotNum) {
        byte[] pageData = page.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(pageData);
        
        buffer.position(HEADER_SIZE + slotNum * SLOT_SIZE);
        buffer.putInt(-1);  //mark as deleted
        buffer.putInt(0);
        
        page.setData(pageData);
    }
    
    /**
     * Update a tuple at a specific slot.
     */
    public boolean updateTuple(int slotNum, Tuple tuple) {
        deleteTuple(slotNum);
        int newSlot = insertTuple(tuple);
        return newSlot != -1;
    }
    
    private byte[] serializeTuple(Tuple tuple) {
        //calculate size
        int size = 0;
        for (Object value : tuple.getValues()) {
            size += getValueSize(value);
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(size);
        
        for (Object value : tuple.getValues()) {
            serializeValue(buffer, value);
        }
        
        return buffer.array();
    }
    
    private Tuple deserializeTuple(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int numColumns = schema.getColumnCount();
        Object[] values = new Object[numColumns];
        
        List<String> columnNames = new ArrayList<>(schema.getColumnNames());
        
        for (int i = 0; i < numColumns; i++) {
            String columnName = columnNames.get(i);
            DataType type = schema.getColumnType(columnName).orElseThrow();
            values[i] = deserializeValue(buffer, type);
        }
        
        return new Tuple(values, createColumnIndexMap(columnNames));
    }
    
    private int getValueSize(Object value) {
        if (value == null) {
            return 1;  //type byte
        } else if (value instanceof Integer) {
            return 1 + 4;  //type + int
        } else if (value instanceof String) {
            return 1 + 4 + ((String) value).getBytes(StandardCharsets.UTF_8).length;  //type + length + data
        } else if (value instanceof Boolean) {
            return 1 + 1;  //type + boolean
        }
        throw new IllegalArgumentException("Unsupported type: " + value.getClass());
    }
    
    private void serializeValue(ByteBuffer buffer, Object value) {
        if (value == null) {
            buffer.put((byte) 0);
        } else if (value instanceof Integer) {
            buffer.put((byte) 1);
            buffer.putInt((Integer) value);
        } else if (value instanceof String) {
            buffer.put((byte) 2);
            byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        } else if (value instanceof Boolean) {
            buffer.put((byte) 3);
            buffer.put((byte) (((Boolean) value) ? 1 : 0));
        } else {
            throw new IllegalArgumentException("Unsupported type: " + value.getClass());
        }
    }
    
    private Object deserializeValue(ByteBuffer buffer, DataType type) {
        byte typeId = buffer.get();
        
        if (typeId == 0) {
            return null;
        } else if (typeId == 1) {
            return buffer.getInt();
        } else if (typeId == 2) {
            int length = buffer.getInt();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } else if (typeId == 3) {
            return buffer.get() == 1;
        }
        
        throw new IllegalArgumentException("Unknown type id: " + typeId);
    }
    
    private java.util.Map<String, Integer> createColumnIndexMap(List<String> columnNames) {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            map.put(columnNames.get(i), i);
        }
        return map;
    }
    
    public int getTupleCount() {
        ByteBuffer buffer = ByteBuffer.wrap(page.getBytes());
        buffer.position(1);
        return buffer.getInt();
    }
}
