package com.dbengine.txn;

/**
 * Represents a single log record in the Write-Ahead Log.
 */
public sealed interface LogRecord permits
    BeginRecord,
    CommitRecord,
    AbortRecord,
    UpdateRecord {
    
    int txnId();
    LogRecordType type();
    
    enum LogRecordType {
        BEGIN, COMMIT, ABORT, UPDATE
    }
}

record BeginRecord(int txnId) implements LogRecord {
    @Override
    public LogRecordType type() {
        return LogRecordType.BEGIN;
    }
}

record CommitRecord(int txnId) implements LogRecord {
    @Override
    public LogRecordType type() {
        return LogRecordType.COMMIT;
    }
}

record AbortRecord(int txnId) implements LogRecord {
    @Override
    public LogRecordType type() {
        return LogRecordType.ABORT;
    }
}

record UpdateRecord(int txnId, int pageId, byte[] beforeImage, byte[] afterImage) implements LogRecord {
    @Override
    public LogRecordType type() {
        return LogRecordType.UPDATE;
    }
}
