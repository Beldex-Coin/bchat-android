package com.thoughtcrimes.securesms.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thoughtcrimes.securesms.data.Node;
import com.thoughtcrimes.securesms.data.Subaddress;
import com.thoughtcrimes.securesms.data.TxData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import timber.log.Timber;

public class Wallet {
    final static public long SWEEP_ALL = Long.MAX_VALUE;

    static {
        System.loadLibrary("app");
    }

    static public class Status {
        Status(int status, String errorString) {
            this.status = StatusEnum.values()[status];
            this.errorString = errorString;
        }

        final private StatusEnum status;
        final private String errorString;
        @Nullable
        private ConnectionStatus connectionStatus; // optional

        public StatusEnum getStatus() {
            return status;
        }

        public String getErrorString() {
            return errorString;
        }

        public void setConnectionStatus(@Nullable ConnectionStatus connectionStatus) {
            this.connectionStatus = connectionStatus;
        }

        @Nullable
        public ConnectionStatus getConnectionStatus() {
            return connectionStatus;
        }

        public boolean isOk() {
            return (getStatus() == StatusEnum.Status_Ok)
                    && ((getConnectionStatus() == null) ||
                    (getConnectionStatus() == ConnectionStatus.ConnectionStatus_Connected));
        }

        @Override
        @NonNull
        public String toString() {
            return "Wallet.Status: (" + status + "/" + errorString + ", " + connectionStatus;
        }
    }

    static public class KeysStatus {
        KeysStatus(byte[] publicKey, byte[] privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        final private byte[] publicKey;
        final private byte[] privateKey;

        public byte[] getPublicKey() {
            return publicKey;
        }

        public byte[] getPrivateKey() {
            return privateKey;
        }
    }

    static public class SecretKeysStatus {
        SecretKeysStatus(byte[] publicKey, byte[] privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        final private byte[] publicKey;
        final private byte[] privateKey;

        public byte[] getPublicKey() {
            return publicKey;
        }

        public byte[] getPrivateKey() {
            return privateKey;
        }
    }

    private int accountIndex = 0;

    public int getAccountIndex() {
        return accountIndex;
    }

    public void setAccountIndex(int accountIndex) {
        Timber.d("setAccountIndex(%d)", accountIndex);
        this.accountIndex = accountIndex;
        getHistory().setAccountFor(this);
    }

    public String getName() {
        return new File(getPath()).getName();
    }

    private long handle = 0;
    private long listenerHandle = 0;

    Wallet(long handle) {
        this.handle = handle;
    }

    @RequiredArgsConstructor
    @Getter
    public enum Device {
        Device_Undefined(0, 0),
        Device_Software(50, 200),
        Device_Ledger(5, 20);
        private final int accountLookahead;
        private final int subaddressLookahead;

        Device(int i, int i1) {
            this.accountLookahead=i;
            this.subaddressLookahead=i1;
        }
    }

    public enum StatusEnum {
        Status_Ok,
        Status_Error,
        Status_Critical
    }

    public enum ConnectionStatus {
        ConnectionStatus_Disconnected,
        ConnectionStatus_Connected,
        ConnectionStatus_WrongVersion,
        ConnectionStatus_Connecting
    }

    public native String getSeed();

    public native String getSeedLanguage();

    public native void setSeedLanguage(String language);

    public Status getStatus() {
        return statusWithErrorString();
    }
    private native void getViewKeyAndSpendKey(String s);
    //New Line
    public void getKeysFromAddress(String s){
        getViewKeyAndSpendKey(s);
    }
    public Status getFullStatus() {
        Wallet.Status walletStatus = statusWithErrorString();
        walletStatus.setConnectionStatus(getConnectionStatus());
        return walletStatus;
    }

    private native Status statusWithErrorString();

    public native boolean setPassword(String password);

    public String getAddress() {
        return getAddress(accountIndex);
    }

    public String getAddress(int accountIndex) {
        return getAddressJ(accountIndex, 0);
    }

    public String getSubaddress(int addressIndex) {
        return getAddressJ(accountIndex, addressIndex);
    }

    public String getSubaddress(int accountIndex, int addressIndex) {
        return getAddressJ(accountIndex, addressIndex);
    }

    private native String getAddressJ(int accountIndex, int addressIndex);

    public Subaddress getSubaddressObject(int accountIndex, int subAddressIndex) {
        return new Subaddress(accountIndex, subAddressIndex,
                getSubaddress(subAddressIndex), getSubaddressLabel(subAddressIndex));
    }

    public Subaddress getSubaddressObject(int subAddressIndex) {
        Subaddress subaddress = getSubaddressObject(accountIndex, subAddressIndex);
        long amount = 0;
        for (TransactionInfo info : getHistory().getAll()) {
            if ((info.addressIndex == subAddressIndex)
                    && (info.direction == TransactionInfo.Direction.Direction_In)) {
                amount += info.amount;
            }
        }
        subaddress.setAmount(amount);
        return subaddress;
    }

    public native String getPath();

    public NetworkType getNetworkType() {
        return NetworkType.fromInteger(nettype());
    }

    public native int nettype();

    public native String getIntegratedAddress(String payment_id);

    public native String getSecretViewKey();

    public native String getSecretSpendKey();

    public native String getPublicViewKey();

    public native String getPublicSpendKey();

    public boolean store() {
        final boolean ok = store("");
        Timber.d("stored");
        return ok;
    }

    public native boolean store(String path);

    public boolean close() {
        disposePendingTransaction();
        return WalletManager.getInstance().close(this);
    }

    public native String getFilename();

    public boolean init(long upper_transaction_size_limit) {
        return initJ(WalletManager.getInstance().getDaemonAddress(), upper_transaction_size_limit,
                WalletManager.getInstance().getDaemonUsername(),
                WalletManager.getInstance().getDaemonPassword());
    }

    private native boolean initJ(String daemon_address, long upper_transaction_size_limit,
                                 String daemon_username, String daemon_password);

    public native void setRestoreHeight(long height);

    public native long getRestoreHeight();

    public ConnectionStatus getConnectionStatus() {
        int s = getConnectionStatusJ();
        return Wallet.ConnectionStatus.values()[s];
    }

    private native int getConnectionStatusJ();

    public long getBalance() {
        return getBalance(accountIndex);
    }

    public native long getBalance(int accountIndex);

    public native long getBalanceAll();

    public long getUnlockedBalance() {
        return getUnlockedBalance(accountIndex);
    }

    public native long getUnlockedBalanceAll();

    public native long getUnlockedBalance(int accountIndex);

    public native boolean isWatchOnly();

    public native long getBlockChainHeight();

    public native long getApproximateBlockChainHeight();

    public native long getDaemonBlockChainHeight();

    public native long getDaemonBlockChainTargetHeight();

    boolean synced = false;

    public boolean isSynchronized() {
        return synced;
    }

    public void setSynchronized() {
        this.synced = true;
    }

    public static native String getDisplayAmount(long amount);

    public static native long getAmountFromString(String amount);

    public static native long getAmountFromDouble(double amount);

    public static native String generatePaymentId();

    public static native boolean isPaymentIdValid(String payment_id);

    public static boolean isAddressValid(String address) {
        return isAddressValid(address, WalletManager.getInstance().getNetworkType().getValue());
    }

    public static native boolean isAddressValid(String address, int networkType);

    public static native String getPaymentIdFromAddress(String address, int networkType);

    public static native long getMaximumAllowedAmount();

    public native void startRefresh();

    public native void pauseRefresh();

    public native boolean refresh();

    public native void refreshAsync();

    public native void rescanBlockchainAsyncJ();

    public void rescanBlockchainAsync() {
        synced = false;
        rescanBlockchainAsyncJ();
    }

    private PendingTransaction pendingTransaction = null;

    public PendingTransaction getPendingTransaction() {
        return pendingTransaction;
    }

    public void disposePendingTransaction() {
        if (pendingTransaction != null) {
            disposeTransaction(pendingTransaction);
            pendingTransaction = null;
        }
    }

    public PendingTransaction createTransaction(TxData txData) {
        return createTransaction(
                txData.getDestinationAddress(),
                txData.getAmount(),
                txData.getMixin(),
                txData.getPriority());
    }

    public PendingTransaction createTransaction(String dst_addr,
                                                long amount, int mixin_count,
                                                PendingTransaction.Priority priority) {
        disposePendingTransaction();
        int _priority = priority.getValue();
        long txHandle =
                (amount == SWEEP_ALL ?
                        createSweepTransaction(dst_addr, "", mixin_count, _priority,
                                accountIndex) :
                        createTransactionJ(dst_addr, "", amount, mixin_count, _priority,
                                accountIndex));
        pendingTransaction = new PendingTransaction(txHandle);
        return pendingTransaction;
    }

    private native long createTransactionJ(String dst_addr, String payment_id,
                                           long amount, int mixin_count,
                                           int priority, int accountIndex);

    private native long createSweepTransaction(String dst_addr, String payment_id,
                                               int mixin_count,
                                               int priority, int accountIndex);


    public PendingTransaction createSweepUnmixableTransaction() {
        disposePendingTransaction();
        long txHandle = createSweepUnmixableTransactionJ();
        pendingTransaction = new PendingTransaction(txHandle);
        return pendingTransaction;
    }

    private native long createSweepUnmixableTransactionJ();

    public native void disposeTransaction(PendingTransaction pendingTransaction);

    private TransactionHistory history = null;

    public TransactionHistory getHistory() {
        if (history == null) {
            history = new TransactionHistory(getHistoryJ(), accountIndex);
        }
        return history;
    }

    private native long getHistoryJ();

    public void refreshHistory() {
        getHistory().refreshWithNotes(this);
    }

    public double belDexAmountToDouble(int amount){
      return (double) amount / 1000000000;
    }

    public double estimateTransactionFee(int priority){
        return belDexAmountToDouble(estimateTransactionFee(priority,1));
    }

    public int estimateTransactionFee(int priority,int recipients){
        return estimateTransactionFeeJ(priority,recipients);
    }

    private native int estimateTransactionFeeJ(int priorityRaw, int recipients);//int recipients = 1

    private native long setListenerJ(WalletListener listener);

    public void setListener(WalletListener listener) {
        this.listenerHandle = setListenerJ(listener);
    }

    public native boolean setUserNote(String txid, String note);

    public native String getUserNote(String txid);

    public native String getTxKey(String txid);

    private static final String NEW_ACCOUNT_NAME = "Untitled account"; // src/wallet/wallet2.cpp:941

    public void addAccount() {
        addAccount(NEW_ACCOUNT_NAME);
    }

    public native void addAccount(String label);

    public String getAccountLabel() {
        return getAccountLabel(accountIndex);
    }

    public String getAccountLabel(int accountIndex) {
        String label = getSubaddressLabel(accountIndex, 0);
        if (label.equals(NEW_ACCOUNT_NAME)) {
            String address = getAddress(accountIndex);
            int len = address.length();
            label = address.substring(0, 6) +
                    "\u2026" + address.substring(len - 6, len);
        }
        return label;
    }

    public String getSubaddressLabel(int addressIndex) {
        return getSubaddressLabel(accountIndex, addressIndex);
    }

    public native String getSubaddressLabel(int accountIndex, int addressIndex);

    public void setAccountLabel(String label) {
        setAccountLabel(accountIndex, label);
    }

    public void setAccountLabel(int accountIndex, String label) {
        setSubaddressLabel(accountIndex, 0, label);
    }

    public void setSubaddressLabel(int addressIndex, String label) {
        setSubaddressLabel(accountIndex, addressIndex, label);
        refreshHistory();
    }

    public native void setSubaddressLabel(int accountIndex, int addressIndex, String label);

    public native int getNumAccounts();

    public int getNumSubaddresses() {
        return getNumSubaddresses(accountIndex);
    }

    public native int getNumSubaddresses(int accountIndex);

    public String getNewSubaddress() {
        return getNewSubaddress(accountIndex);
    }

    public String getNewSubaddress(int accountIndex) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.US).format(new Date());
        addSubaddress(accountIndex, timeStamp);
        String subaddress = getLastSubaddress(accountIndex);
        Timber.d("%d: %s", getNumSubaddresses(accountIndex) - 1, subaddress);
        return subaddress;
    }

    public native void addSubaddress(int accountIndex, String label);

    public String getLastSubaddress(int accountIndex) {
        return getSubaddress(accountIndex, getNumSubaddresses(accountIndex) - 1);
    }

    public Wallet.Device getDeviceType() {
        int device = getDeviceTypeJ();
        return Wallet.Device.values()[device + 1]; // mapping is monero+1=android
    }

    private native int getDeviceTypeJ();

    public native boolean reConnectToDaemon(Node node, boolean useSSL, boolean isLightWallet);

}
//endregion