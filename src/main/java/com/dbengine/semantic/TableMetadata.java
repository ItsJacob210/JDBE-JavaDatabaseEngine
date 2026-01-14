package com.dbengine.semantic;

import java.util.HashSet;
import java.util.Set;

/**
 * Metadata for a single table in the catalog.
 */
public class TableMetadata {
    private final String name;
    private final Schema schema;
    private final Set<String> indexedColumns;
    
    public TableMetadata(String name, Schema schema) {
        this.name = name;
        this.schema = schema;
        this.indexedColumns = new HashSet<>();
    }
    
    public String getName() {
        return name;
    }
    
    public Schema getSchema() {
        return schema;
    }
    
    public void addIndex(String columnName) {
        indexedColumns.add(columnName);
    }
    
    public boolean hasIndex(String columnName) {
        return indexedColumns.contains(columnName);
    }
    
    public Set<String> getIndexedColumns() {
        return new HashSet<>(indexedColumns);
    }
}
