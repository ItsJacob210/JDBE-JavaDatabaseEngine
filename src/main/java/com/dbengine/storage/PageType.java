package com.dbengine.storage;

/**
 * Types of pages in the storage system.
 */
public enum PageType {
    HEAP_PAGE(1),
    BTREE_INTERNAL(2),
    BTREE_LEAF(3),
    FREE_LIST(4),
    METADATA(5);
    
    private final byte id;
    
    PageType(int id) {
        this.id = (byte) id;
    }
    
    public byte getId() {
        return id;
    }
    
    public static PageType fromId(byte id) {
        for (PageType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown page type: " + id);
    }
}
