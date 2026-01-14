package com.dbengine.storage;

import java.nio.ByteBuffer;

/**
 * Represents a fixed-size page (4KB) in the database.
 * Pages are the fundamental unit of storage and I/O.
 */
public class Page {
    public static final int PAGE_SIZE = 4096;
    
    private final int pageId;
    private final ByteBuffer data;
    private boolean dirty;
    private int pinCount;
    
    public Page(int pageId) {
        this.pageId = pageId;
        this.data = ByteBuffer.allocate(PAGE_SIZE);
        this.dirty = false;
        this.pinCount = 0;
    }
    
    public Page(int pageId, byte[] pageData) {
        this.pageId = pageId;
        this.data = ByteBuffer.wrap(pageData);
        this.dirty = false;
        this.pinCount = 0;
    }
    
    public int getPageId() {
        return pageId;
    }
    
    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }
    
    public byte[] getBytes() {
        byte[] bytes = new byte[PAGE_SIZE];
        data.position(0);
        data.get(bytes);
        data.position(0);
        return bytes;
    }
    
    public void setData(byte[] newData) {
        if (newData.length != PAGE_SIZE) {
            throw new IllegalArgumentException("Page data must be exactly " + PAGE_SIZE + " bytes");
        }
        data.position(0);
        data.put(newData);
        data.position(0);
        dirty = true;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public int getPinCount() {
        return pinCount;
    }
    
    public void pin() {
        pinCount++;
    }
    
    public void unpin() {
        if (pinCount > 0) {
            pinCount--;
        }
    }
    
    public boolean isPinned() {
        return pinCount > 0;
    }
    
    public void clear() {
        data.clear();
        for (int i = 0; i < PAGE_SIZE; i++) {
            data.put((byte) 0);
        }
        data.position(0);
        dirty = true;
    }
}
