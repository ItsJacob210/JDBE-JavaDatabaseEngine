package com.dbengine.txn;

import com.dbengine.storage.BufferPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages transactions and coordinates with the log manager.
 */
public class  TransactionManager {
    private final LogManager logManager;
    private final BufferPool bufferPool;
    private final AtomicInteger nextTxnId;
    private final Map<Integer, Transaction> activeTransactions;
    
    public TransactionManager(LogManager logManager, BufferPool bufferPool) {
        this.logManager = logManager;
        this.bufferPool = bufferPool;
        this.nextTxnId = new AtomicInteger(0);
        this.activeTransactions = new HashMap<>();
    }
    
    /**
     * Begin a new transaction.
     */
    public Transaction begin() throws IOException {
        int txnId = nextTxnId.getAndIncrement();
        Transaction txn = new Transaction(txnId);
        
        logManager.appendLogRecord(new BeginRecord(txnId));
        activeTransactions.put(txnId, txn);
        
        return txn;
    }
    
    /**
     * Commit a transaction.
     */
    public void commit(Transaction txn) throws IOException {
        if (txn.getState() != Transaction.TransactionState.ACTIVE) {
            throw new IllegalStateException("Transaction is not active");
        }
        
        //write commit record
        logManager.appendLogRecord(new CommitRecord(txn.getTxnId()));
        logManager.flush();
        
        //flush dirty pages
        bufferPool.flushAllPages();
        
        txn.setState(Transaction.TransactionState.COMMITTED);
        activeTransactions.remove(txn.getTxnId());
    }
    
    /**
     * Abort a transaction.
     */
    public void abort(Transaction txn) throws IOException {
        if (txn.getState() != Transaction.TransactionState.ACTIVE) {
            throw new IllegalStateException("Transaction is not active");
        }
        
        //write abort record
        logManager.appendLogRecord(new AbortRecord(txn.getTxnId()));
        logManager.flush();
        
        txn.setState(Transaction.TransactionState.ABORTED);
        activeTransactions.remove(txn.getTxnId());
    }
    
    /**
     * Recover from a crash using the log.
     */
    public void recover() throws IOException {
        var logRecords = logManager.readLog();
        
        Map<Integer, Transaction.TransactionState> txnStates = new HashMap<>();
        
        //analyze phase: determine transaction states
        for (LogRecord record : logRecords) {
            switch (record) {
                case BeginRecord begin ->
                    txnStates.put(begin.txnId(), Transaction.TransactionState.ACTIVE);
                case CommitRecord commit ->
                    txnStates.put(commit.txnId(), Transaction.TransactionState.COMMITTED);
                case AbortRecord abort ->
                    txnStates.put(abort.txnId(), Transaction.TransactionState.ABORTED);
                case UpdateRecord update -> {}
            }
        }
        
        //redo phase: replay committed transactions
        for (LogRecord record : logRecords) {
            if (record instanceof UpdateRecord update) {
                Transaction.TransactionState state = txnStates.get(update.txnId());
                if (state == Transaction.TransactionState.COMMITTED) {
                    //redo the update (simplified - would need to actually apply changes)
                    System.out.println("Redoing update for txn " + update.txnId() + 
                                     " on page " + update.pageId());
                }
            }
        }
    }
    
    public LogManager getLogManager() {
        return logManager;
    }
}
