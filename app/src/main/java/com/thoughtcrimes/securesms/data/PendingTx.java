package com.thoughtcrimes.securesms.data;

import com.thoughtcrimes.securesms.model.PendingTransaction;

public class PendingTx {
    public PendingTransaction.Status status;
    final public String error;
    final public long amount;
    final public long dust;
    final public long fee;
    final public String txId;
    final public long txCount;

    public PendingTx(PendingTransaction pendingTransaction) {
        status = pendingTransaction.getStatus();
        error = pendingTransaction.getErrorString();
        amount = pendingTransaction.getAmount();
        dust = pendingTransaction.getDust();
        fee = pendingTransaction.getFee();
        txId = pendingTransaction.getFirstTxId();
        txCount = pendingTransaction.getTxCount();
    }
}
