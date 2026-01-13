package io.beldex.bchat.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TransactionHistory {
    static {
        System.loadLibrary("app");
    }

    private final long handle;

    int accountIndex;

    public void setAccountFor(Wallet wallet) {
        if (accountIndex != wallet.getAccountIndex()) {
            this.accountIndex = wallet.getAccountIndex();
            refreshWithNotes(wallet);
        }
    }

    public TransactionHistory(long handle, int accountIndex) {
        this.handle = handle;
        this.accountIndex = accountIndex;
    }

    private void loadNotes(Wallet wallet) {
        for (TransactionInfo info : transactions) {
            info.notes = wallet.getUserNote(info.hash);
        }
    }

    public native int getCount(); // over all accounts/subaddresses

    //private native long getTransactionByIndexJ(int i);

    //private native long getTransactionByIdJ(String id);

    public List<TransactionInfo> getAll() {
        return transactions;
    }

    private List<TransactionInfo> transactions = new ArrayList<>();

    void refreshWithNotes(Wallet wallet) {
        refresh();
        loadNotes(wallet);
    }
    private void refresh() {
        List<TransactionInfo> transactionInfos = refreshJ();
        Log.d("refresh size=%d", ""+transactionInfos.size());
        for (Iterator<TransactionInfo> iterator = transactionInfos.iterator(); iterator.hasNext(); ) {
            TransactionInfo info = iterator.next();
            if (info.accountIndex != accountIndex) {
                iterator.remove();
            }
        }
        transactions = transactionInfos;
    }

    private native List<TransactionInfo> refreshJ();
}