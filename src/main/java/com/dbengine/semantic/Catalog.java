package com.dbengine.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * System catalog that stores metadata about tables and their schemas.
 */
public class Catalog {
    private final Map<String, TableMetadata> tables;
    
    public Catalog() {
        this.tables = new HashMap<>();
    }
    
    public void registerTable(String name, Schema schema) {
        tables.put(name, new TableMetadata(name, schema));
    }
    
    public boolean tableExists(String name) {
        return tables.containsKey(name);
    }
    
    public Optional<TableMetadata> getTable(String name) {
        return Optional.ofNullable(tables.get(name));
    }
    
    public void addIndex(String tableName, String columnName) {
        TableMetadata table = tables.get(tableName);
        if (table != null) {
            table.addIndex(columnName);
        }
    }
    
    public Map<String, TableMetadata> getAllTables() {
        return new HashMap<>(tables);
    }
}
