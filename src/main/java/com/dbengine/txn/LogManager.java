package com.dbengine.txn;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Write-Ahead Log (WAL) manager for durability and recovery.
 */
public class LogManager {
    private final Path logFilePath;
    private DataOutputStream logStream;
    private long currentLSN;
    
    public LogManager(Path logFilePath) throws IOException {
        this.logFilePath = logFilePath;
        this.currentLSN = 0;
        this.logStream = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(logFilePath.toFile(), true))
        );
    }
    
    /**
     * Append a log record to the WAL.
     */
    public synchronized long appendLogRecord(LogRecord record) throws IOException {
        long lsn = currentLSN++;
        
        switch (record) {
            case BeginRecord begin -> {
                logStream.writeInt(LogRecord.LogRecordType.BEGIN.ordinal());
                logStream.writeInt(begin.txnId());
            }
            case CommitRecord commit -> {
                logStream.writeInt(LogRecord.LogRecordType.COMMIT.ordinal());
                logStream.writeInt(commit.txnId());
            }
            case AbortRecord abort -> {
                logStream.writeInt(LogRecord.LogRecordType.ABORT.ordinal());
                logStream.writeInt(abort.txnId());
            }
            case UpdateRecord update -> {
                logStream.writeInt(LogRecord.LogRecordType.UPDATE.ordinal());
                logStream.writeInt(update.txnId());
                logStream.writeInt(update.pageId());
                logStream.writeInt(update.beforeImage().length);
                logStream.write(update.beforeImage());
                logStream.writeInt(update.afterImage().length);
                logStream.write(update.afterImage());
            }
        }
        
        return lsn;
    }
    
    /**
     * Force log records to disk.
     */
    public synchronized void flush() throws IOException {
        logStream.flush();
    }
    
    /**
     * Read all log records for recovery.
     */
    public List<LogRecord> readLog() throws IOException {
        List<LogRecord> records = new ArrayList<>();
        
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(new FileInputStream(logFilePath.toFile())))) {
            
            while (input.available() > 0) {
                int typeOrdinal = input.readInt();
                LogRecord.LogRecordType type = LogRecord.LogRecordType.values()[typeOrdinal];
                
                switch (type) {
                    case BEGIN -> {
                        int txnId = input.readInt();
                        records.add(new BeginRecord(txnId));
                    }
                    case COMMIT -> {
                        int txnId = input.readInt();
                        records.add(new CommitRecord(txnId));
                    }
                    case ABORT -> {
                        int txnId = input.readInt();
                        records.add(new AbortRecord(txnId));
                    }
                    case UPDATE -> {
                        int txnId = input.readInt();
                        int pageId = input.readInt();
                        int beforeLength = input.readInt();
                        byte[] beforeImage = new byte[beforeLength];
                        input.readFully(beforeImage);
                        int afterLength = input.readInt();
                        byte[] afterImage = new byte[afterLength];
                        input.readFully(afterImage);
                        records.add(new UpdateRecord(txnId, pageId, beforeImage, afterImage));
                    }
                }
            }
        } catch (EOFException e) {
            //end of log file
        }
        
        return records;
    }
    
    /**
     * Close the log manager.
     */
    public synchronized void close() throws IOException {
        if (logStream != null) {
            logStream.flush();
            logStream.close();
        }
    }
}
