package io.beldex.bchat.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import timber.log.Timber;

public class Wallet {

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

    private int accountIndex = 0;

    public int getAccountIndex() {
        return accountIndex;
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

    public Status getStatus() {
        return statusWithErrorString();
    }

    private native Status statusWithErrorString();

    public native boolean setPassword(String password);

    public String getAddress() {
        return getAddress(accountIndex);
    }

    public String getAddress(int accountIndex) {
        return getAddressJ(accountIndex, 0);
    }

    private native String getAddressJ(int accountIndex, int addressIndex);

    public native String getPath();


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

    public native boolean isWatchOnly();

    boolean synced = false;

    public boolean isSynchronized() {
        return synced;
    }

    public native boolean refresh();

    private native long setListenerJ(WalletListener listener);

    public void setListener(WalletListener listener) {
        this.listenerHandle = setListenerJ(listener);
    }

}
//endregion