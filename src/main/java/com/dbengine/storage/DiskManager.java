package com.dbengine.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Manages page-level I/O with the disk.
 * Provides methods to read and write pages from/to disk files.
 */
public class DiskManager {
    private final RandomAccessFile file;
    private final FileChannel channel;
    
    public DiskManager(Path dbFilePath) throws IOException {
        this.file = new RandomAccessFile(dbFilePath.toFile(), "rw");
        this.channel = file.getChannel();
    }
    
    /**
     * Read a page from disk.
     */
    public Page readPage(int pageId) throws IOException {
        long offset = (long) pageId * Page.PAGE_SIZE;
        byte[] buffer = new byte[Page.PAGE_SIZE];
        
        synchronized (channel) {
            channel.position(offset);
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            int bytesRead = channel.read(byteBuffer);
            
            if (bytesRead < Page.PAGE_SIZE) {
                //page doesn't exist yet, return empty page
                return new Page(pageId);
            }
        }
        
        return new Page(pageId, buffer);
    }
    
    /**
     * Write a page to disk.
     */
    public void writePage(Page page) throws IOException {
        long offset = (long) page.getPageId() * Page.PAGE_SIZE;
        byte[] data = page.getBytes();
        
        synchronized (channel) {
            channel.position(offset);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            channel.write(buffer);
            channel.force(false);
        }
        
        page.setDirty(false);
    }
    
    /**
     * Allocate a new page on disk.
     */
    public int allocatePage() throws IOException {
        synchronized (channel) {
            long fileSize = channel.size();
            int newPageId = (int) (fileSize / Page.PAGE_SIZE);
            
            //extend file to accommodate new page
            channel.position(fileSize + Page.PAGE_SIZE - 1);
            channel.write(ByteBuffer.wrap(new byte[]{0}));
            
            return newPageId;
        }
    }
    
    /**
     * Get the total number of pages.
     */
    public int getNumPages() throws IOException {
        synchronized (channel) {
            return (int) (channel.size() / Page.PAGE_SIZE);
        }
    }
    
    /**
     * Close the disk manager and release resources.
     */
    public void close() throws IOException {
        channel.close();
        file.close();
    }
    
    /**
     * Flush all changes to disk.
     */
    public void flush() throws IOException {
        synchronized (channel) {
            channel.force(true);
        }
    }
}
