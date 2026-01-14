package com.dbengine.exec;

import com.dbengine.storage.Tuple;

/**
 * Base interface for all physical operators in the execution engine.
 * Uses the Volcano iterator model.
 */
public interface Operator {
    /**
     * Initialize the operator and prepare for execution.
     */
    void open() throws Exception;
    
    /**
     * Get the next tuple, or null if no more tuples.
     */
    Tuple next() throws Exception;
    
    /**
     * Clean up resources after execution.
     */
    void close() throws Exception;
}
