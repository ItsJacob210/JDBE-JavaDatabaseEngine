package com.dbengine.storage;

/**
 * Identifier for a record in the database.
 * Consists of a page ID and a slot number within that page.
 */
public record RecordId(int pageId, int slotNum) {
    
    @Override
    public String toString() {
        return String.format("(%d, %d)", pageId, slotNum);
    }
}
