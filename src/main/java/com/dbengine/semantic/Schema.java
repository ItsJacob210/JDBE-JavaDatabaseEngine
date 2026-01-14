package com.dbengine.semantic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the schema of a relation (table or intermediate result).
 */
public class Schema {
    private final Map<String, DataType> columns;
    
    public Schema() {
        this.columns = new LinkedHashMap<>();
    }
    
    public Schema(Map<String, DataType> columns) {
        this.columns = new LinkedHashMap<>(columns);
    }
    
    public void addColumn(String name, DataType type) {
        columns.put(name, type);
    }
    
    public boolean hasColumn(String name) {
        return columns.containsKey(name);
    }
    
    public Optional<DataType> getColumnType(String name) {
        return Optional.ofNullable(columns.get(name));
    }
    
    public Set<String> getColumnNames() {
        return columns.keySet();
    }
    
    public Map<String, DataType> getColumns() {
        return new LinkedHashMap<>(columns);
    }
    
    public int getColumnCount() {
        return columns.size();
    }
    
    public Schema project(Set<String> columnNames) {
        Map<String, DataType> projected = new LinkedHashMap<>();
        for (String name : columnNames) {
            if (columns.containsKey(name)) {
                projected.put(name, columns.get(name));
            }
        }
        return new Schema(projected);
    }
    
    @Override
    public String toString() {
        return columns.toString();
    }
}
