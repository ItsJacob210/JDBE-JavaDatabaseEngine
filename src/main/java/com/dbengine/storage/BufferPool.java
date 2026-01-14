package com.dbengine.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Buffer pool with LRU eviction policy.
 * Manages in-memory pages and handles page replacement when full.
 */
public class BufferPool {
    private final DiskManager diskManager;
    private final int poolSize;
    private final Map<Integer, Page> pages;
    private final LinkedList<Integer> lruList;
    
    public BufferPool(DiskManager diskManager, int poolSize) {
        this.diskManager = diskManager;
        this.poolSize = poolSize;
        this.pages = new HashMap<>();
        this.lruList = new LinkedList<>();
    }
    
    /**
     * Fetch a page from the buffer pool. If not present, load from disk.
     */
    public synchronized Page fetchPage(int pageId) throws IOException {
        //check if page is already in buffer pool
        if (pages.containsKey(pageId)) {
            Page page = pages.get(pageId);
            page.pin();
            updateLRU(pageId);
            return page;
        }
        
        //need to load from disk
        if (pages.size() >= poolSize) {
            evictPage();
        }
        
        Page page = diskManager.readPage(pageId);
        page.pin();
        pages.put(pageId, page);
        lruList.addFirst(pageId);
        
        return page;
    }
    
    /**
     * Unpin a page, making it eligible for eviction.
     */
    public synchronized void unpinPage(int pageId, boolean isDirty) {
        Page page = pages.get(pageId);
        if (page != null) {
            page.unpin();
            if (isDirty) {
                page.setDirty(true);
            }
        }
    }
    
    /**
     * Flush a specific page to disk.
     */
    public synchronized void flushPage(int pageId) throws IOException {
        Page page = pages.get(pageId);
        if (page != null && page.isDirty()) {
            diskManager.writePage(page);
        }
    }
    
    /**
     * Flush all dirty pages to disk.
     */
    public synchronized void flushAllPages() throws IOException {
        for (Page page : pages.values()) {
            if (page.isDirty()) {
                diskManager.writePage(page);
            }
        }
    }
    
    /**
     * Create a new page and add it to the buffer pool.
     */
    public synchronized Page newPage() throws IOException {
        if (pages.size() >= poolSize) {
            evictPage();
        }
        
        int pageId = diskManager.allocatePage();
        Page page = new Page(pageId);
        page.pin();
        pages.put(pageId, page);
        lruList.addFirst(pageId);
        
        return page;
    }
    
    /**
     * Delete a page from the buffer pool.
     */
    public synchronized void deletePage(int pageId) throws IOException {
        Page page = pages.get(pageId);
        if (page != null) {
            if (page.isPinned()) {
                throw new IllegalStateException("Cannot delete pinned page " + pageId);
            }
            pages.remove(pageId);
            lruList.remove(Integer.valueOf(pageId));
        }
    }
    
    /**
     * Evict a page using LRU policy.
     */
    private void evictPage() throws IOException {
        //find an unpinned page to evict
        for (int i = lruList.size() - 1; i >= 0; i--) {
            int pageId = lruList.get(i);
            Page page = pages.get(pageId);
            
            if (page != null && !page.isPinned()) {
                if (page.isDirty()) {
                    diskManager.writePage(page);
                }
                pages.remove(pageId);
                lruList.remove(i);
                return;
            }
        }
        
        throw new IllegalStateException("No pages available for eviction - all pages are pinned");
    }
    
    /**
     * Update LRU list when a page is accessed.
     */
    private void updateLRU(int pageId) {
        lruList.remove(Integer.valueOf(pageId));
        lruList.addFirst(pageId);
    }
    
    /**
     * Get buffer pool statistics.
     */
    public synchronized int getPoolSize() {
        return poolSize;
    }
    
    public synchronized int getNumPages() {
        return pages.size();
    }
}
