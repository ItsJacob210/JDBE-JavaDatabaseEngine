package com.dbengine.semantic;

/**
 * Data types supported by the database engine.
 */
public enum DataType {
    INTEGER,
    STRING,
    BOOLEAN,
    NULL;
    
    public boolean isCompatible(DataType other) {
        if (this == NULL || other == NULL) return true;
        return this == other;
    }
}
