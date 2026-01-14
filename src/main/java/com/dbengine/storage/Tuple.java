package com.dbengine.storage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single row/tuple in the database.
 */
public class Tuple {
    private final Object[] values;
    private final Map<String, Integer> columnIndexMap;
    private RecordId rid;
    
    public Tuple(int numColumns) {
        this.values = new Object[numColumns];
        this.columnIndexMap = new HashMap<>();
    }
    
    public Tuple(Object[] values, Map<String, Integer> columnIndexMap) {
        this.values = values;
        this.columnIndexMap = new HashMap<>(columnIndexMap);
    }
    
    public void setValue(int index, Object value) {
        if (index < 0 || index >= values.length) {
            throw new IndexOutOfBoundsException("Invalid column index: " + index);
        }
        values[index] = value;
    }
    
    public Object getValue(int index) {
        if (index < 0 || index >= values.length) {
            throw new IndexOutOfBoundsException("Invalid column index: " + index);
        }
        return values[index];
    }
    
    public Object getValue(String columnName) {
        Integer index = columnIndexMap.get(columnName);
        if (index == null) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }
        return values[index];
    }
    
    public void setColumnIndex(String columnName, int index) {
        columnIndexMap.put(columnName, index);
    }
    
    public int getSize() {
        return values.length;
    }
    
    public Object[] getValues() {
        return values;
    }
    
    public Map<String, Integer> getColumnIndexMap() {
        return new HashMap<>(columnIndexMap);
    }
    
    public RecordId getRecordId() {
        return rid;
    }
    
    public void setRecordId(RecordId rid) {
        this.rid = rid;
    }
    
    public Tuple copy() {
        Object[] newValues = Arrays.copyOf(values, values.length);
        Tuple copy = new Tuple(newValues, columnIndexMap);
        copy.setRecordId(rid);
        return copy;
    }
    
    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
