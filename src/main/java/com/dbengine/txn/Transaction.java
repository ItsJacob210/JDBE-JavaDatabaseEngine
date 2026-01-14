package com.dbengine.txn;

/**
 * Represents a database transaction.
 */
public class Transaction {
    private final int txnId;
    private TransactionState state;
    
    public Transaction(int txnId) {
        this.txnId = txnId;
        this.state = TransactionState.ACTIVE;
    }
    
    public int getTxnId() {
        return txnId;
    }
    
    public TransactionState getState() {
        return state;
    }
    
    public void setState(TransactionState state) {
        this.state = state;
    }
    
    public enum TransactionState {
        ACTIVE,
        COMMITTED,
        ABORTED
    }
}
