package com.thoughtcrimes.securesms.model;

import android.util.Log;

import com.thoughtcrimes.securesms.ApplicationContext;
import com.thoughtcrimes.securesms.data.Node;
import com.thoughtcrimes.securesms.util.RestoreHeight;

import java.io.File;
import java.util.Date;

import lombok.Getter;
import timber.log.Timber;

public class WalletManager {

    static {
        System.loadLibrary("app");
    }

    // no need to keep a reference to the REAL WalletManager (we get it every tvTime we need it)
    private static WalletManager Instance = null;

    public static synchronized WalletManager getInstance() {
        if (WalletManager.Instance == null) {
            WalletManager.Instance = new WalletManager();
        }

        return WalletManager.Instance;
    }

    public String addressPrefix() {
        return addressPrefix(getNetworkType());
    }

    static public String addressPrefix(NetworkType networkType) {
        switch (networkType) {
            case NetworkType_Testnet:
                return "9A-";
            case NetworkType_Mainnet:
                return "b-";
            case NetworkType_Stagenet:
                return "5-";
            default:
                throw new IllegalStateException("Unsupported Network: " + networkType);
        }
    }

    private Wallet managedWallet = null;

    public Wallet getWallet() {
        return managedWallet;
    }

    private void manageWallet(Wallet wallet) {
        Timber.d("Managing %s", wallet.getName());
        managedWallet = wallet;
    }

    private void unmanageWallet(Wallet wallet) {
        if (wallet == null) {
            throw new IllegalArgumentException("Cannot unmanage null!");
        }
        if (getWallet() == null) {
            throw new IllegalStateException("No wallet under management!");
        }
        if (getWallet() != wallet) {
            throw new IllegalStateException(wallet.getName() + " not under management!");
        }
        managedWallet = null;
    }

    public Wallet createWallet(File aFile, String password, String language, long height) {
        // for Mainnet
        long walletHandle = createWalletJ(aFile.getAbsolutePath(), password, language, getNetworkType().getValue());

        Wallet wallet = new Wallet(walletHandle);
        manageWallet(wallet);
        if (wallet.getStatus().isOk()) {
            // (Re-)Estimate restore height based on what we know
            final long oldHeight = wallet.getRestoreHeight();
            final long restoreHeight = 
                    (height > -1) ? height : RestoreHeight.getInstance().getHeight(new Date());
            wallet.setRestoreHeight(restoreHeight);
            wallet.setPassword(password); // this rewrites the keys file (which contains the restore height)
        } else
            Timber.e(wallet.getStatus().toString());
        return wallet;
    }

    private native long createWalletJ(String path, String password, String language, int networkType);

    public Wallet openWallet(String path, String password) {
        long walletHandle = openWalletJ(path, password, getNetworkType().getValue());
        Wallet wallet = new Wallet(walletHandle);
        manageWallet(wallet);
        return wallet;
    }

    private native long openWalletJ(String path, String password, int networkType);

    public Wallet recoveryWallet(File aFile, String password, String mnemonic, long restoreHeight) {
        long walletHandle = recoveryWalletJ(aFile.getAbsolutePath(), password, mnemonic,
                getNetworkType().getValue(), restoreHeight);
        Wallet wallet = new Wallet(walletHandle);
        manageWallet(wallet);
        return wallet;
    }

    private native long recoveryWalletJ(String path, String password, String mnemonic,
                                        int networkType, long restoreHeight);

    private native long createWalletFromKeysJ(String path, String password,
                                              String language,
                                              int networkType,
                                              long restoreHeight,
                                              String addressString,
                                              String viewKeyString,
                                              String spendKeyString);


    private native long createWalletFromDeviceJ(String path, String password,
                                                int networkType,
                                                String deviceName,
                                                long restoreHeight,
                                                String subaddressLookahead);


    public native boolean closeJ(Wallet wallet);

    public boolean close(Wallet wallet) {
        unmanageWallet(wallet);
        boolean closed = closeJ(wallet);
        if (!closed) {
            // in case we could not close it
            // we manage it again
            manageWallet(wallet);
        }
        return closed;
    }


    public native boolean walletExists(String path);

    public native boolean verifyWalletPassword(String keys_file_name, String password, boolean watch_only);

    public boolean verifyWalletPasswordOnly(String keys_file_name, String password) {
        return queryWalletDeviceJ(keys_file_name, password) >= 0;
    }

    private native int queryWalletDeviceJ(String keys_file_name, String password);


    public class WalletInfo implements Comparable<WalletInfo> {
        @Getter
        final private File path;
        @Getter
        final private String name;

        public WalletInfo(File wallet) {
            path = wallet.getParentFile();
            name = wallet.getName();
        }

        @Override
        public int compareTo(WalletInfo another) {
            return name.toLowerCase().compareTo(another.name.toLowerCase());
        }
    }

    private String daemonAddress = null;
    private final NetworkType networkType = ApplicationContext.getNetworkType();

    public NetworkType getNetworkType() {
        return networkType;
    }

    // this should not be called on the main thread as it connects to the node (and takes a long time)
    public void setDaemon(Node node) {
        if (node != null) {
            this.daemonAddress = node.getAddress();
            if (networkType != node.getNetworkType())
                throw new IllegalArgumentException("network type does not match");
            this.daemonUsername = node.getUsername();
            this.daemonPassword = node.getPassword();
            setDaemonAddressJ(daemonAddress);
        } else {
            this.daemonAddress = null;
            this.daemonUsername = "";
            this.daemonPassword = "";
        }
    }

    public String getDaemonAddress() {
        if (daemonAddress == null) {
            throw new IllegalStateException("use setDaemon() to initialise daemon and net first!");
        }
        return this.daemonAddress;
    }

    private native void setDaemonAddressJ(String address);

    private String daemonUsername = "";

    public String getDaemonUsername() {
        return daemonUsername;
    }

    private String daemonPassword = "";

    public String getDaemonPassword() {
        return daemonPassword;
    }

    public native int getDaemonVersion();

    public native long getBlockchainHeight();

    public native long getBlockchainTargetHeight();

    public native long getBlockTarget();

    //public native String resolveOpenAlias(String address, boolean dnssec_valid);

    static public native void initLogger(String argv0, String defaultLogBaseName);

    static public int LOGLEVEL_SILENT = -1;
    static public int LOGLEVEL_DEBUG = 2;

    static public native void setLogLevel(int level);

    static public native void logDebug(String category, String message);

    static public native void logInfo(String category, String message);

    static public native void logWarning(String category, String message);

    static public native void logError(String category, String message);

    static public native String bChatVersion();
}
//endregion