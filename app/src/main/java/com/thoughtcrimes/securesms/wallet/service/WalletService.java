package com.thoughtcrimes.securesms.wallet.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.thoughtcrimes.securesms.data.TxData;
import com.thoughtcrimes.securesms.home.HomeActivity;
import com.thoughtcrimes.securesms.model.PendingTransaction;
import com.thoughtcrimes.securesms.model.Wallet;
import com.thoughtcrimes.securesms.model.WalletListener;
import com.thoughtcrimes.securesms.model.WalletManager;
import com.thoughtcrimes.securesms.util.Helper;
import com.thoughtcrimes.securesms.util.LocalHelper;
import com.thoughtcrimes.securesms.wallet.CheckOnline;
import com.thoughtcrimes.securesms.wallet.WalletFragment;
import com.thoughtcrimes.securesms.wallet.utils.WalletCallbackType;

import io.beldex.bchat.R;
import timber.log.Timber;

public class WalletService extends Service {
    public static boolean Running = false;

    final static int NOTIFICATION_ID = 2049;
    final static String CHANNEL_ID = "m_service";

    public static final String REQUEST_WALLET = "wallet";
    public static final String REQUEST = "request";

    public static final String REQUEST_CMD_LOAD = "load";
    public static final String REQUEST_CMD_LOAD_PW = "walletPassword";

    public static final String REQUEST_CMD_STORE = "store";

    public static final String REQUEST_CMD_TX = "createTX";
    public static final String REQUEST_CMD_TX_DATA = "data";
    public static final String REQUEST_CMD_TX_TAG = "tag";

    public static final String REQUEST_CMD_SWEEP = "sweepTX";

    public static final String REQUEST_CMD_SEND = "send";
    public static final String REQUEST_CMD_SEND_NOTES = "notes";

    public static final int START_SERVICE = 1;
    public static final int STOP_SERVICE = 2;

    private MyWalletListener listener = null;

    private class MyWalletListener implements WalletListener {
        boolean updated = true;

        void start() {
            Wallet wallet = getWallet();
            try {
                if (wallet != null) {
                    wallet.setListener(this);
                    wallet.startRefresh();
                }
            } catch (IllegalStateException ex) {
                Log.d("Beldex", "IllegalStateException " + ex);
            }
        }

        void stop() {
            Wallet wallet = getWallet();
            try {
                if (wallet != null) {
                    wallet.pauseRefresh();
                    wallet.setListener(null);
                }
            } catch (IllegalStateException ex) {
                Log.d("Beldex", "IllegalStateException " + ex);
            }
        }

        // WalletListener callbacks
        public void moneySpent(String txId, long amount) {
            Timber.d("moneySpent() %d @ %s", amount, txId);
        }

        public void moneyReceived(String txId, long amount) {
            Timber.d("moneyReceived() %d @ %s", amount, txId);
        }

        public void unconfirmedMoneyReceived(String txId, long amount) {
            Timber.d("unconfirmedMoneyReceived() %d @ %s", amount, txId);
        }

        private long lastBlockTime = 0;

        public void newBlock(long height) {
            final Wallet wallet = getWallet();
            if(wallet != null) {
                // don't flood with an update for every block ...
                if (lastBlockTime < System.currentTimeMillis() - 2000) {
                    lastBlockTime = System.currentTimeMillis();
                    if (observer != null) {
                        updateDaemonState(wallet, wallet.isSynchronized() ? height : 0);
                        if (!wallet.isSynchronized()) {
                            updated = true;
                        }
                        if (observer != null)
                            observer.onRefreshed(wallet, false);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("type", WalletCallbackType.WalletRefreshed);
                    bundle.putBoolean("full_refresh", false);
                    sendBroadCast(bundle);
                }
            }
        }

        public void updated() {
            Wallet wallet = getWallet();
            if(wallet!= null){
                updated = true;
            }
        }

        public void refreshed() { // this means it's synced
            final Wallet wallet = getWallet();
            long latestBlock =   WalletFragment.Companion.getSyncingBlocks();
            if (wallet != null) {
                if (CheckOnline.Companion.isOnline(getApplicationContext())) {
                    long blockChainHeight = wallet.getDaemonBlockChainHeight();
                    long syncedBlockHeight = wallet.getBlockChainHeight();
                    long latestSyncedBlockHeight = blockChainHeight - syncedBlockHeight;
                    if(latestBlock <50L || latestSyncedBlockHeight <50L) {
                        wallet.setSynchronized();
                    }
                }
                if (updated) {
                    updateDaemonState(wallet, wallet.getBlockChainHeight());
                    if (observer != null) {
                        updated = !observer.onRefreshed(wallet, true);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("type", WalletCallbackType.WalletRefreshed);
                    bundle.putBoolean("full_refresh", true);
                    sendBroadCast(bundle);
                }

            }
        }
    }

    private long lastDaemonStatusUpdate = 0;
    private long daemonHeight = 0;
    private Wallet.ConnectionStatus connectionStatus = Wallet.ConnectionStatus.ConnectionStatus_Connecting;
    private static final long STATUS_UPDATE_INTERVAL = 120000; // 120s (blocktime)

    private void updateDaemonState(Wallet wallet, long height) {
        long t = System.currentTimeMillis();
        if (height > 0) { // if we get a height, we are connected
            daemonHeight = height;
            connectionStatus = Wallet.ConnectionStatus.ConnectionStatus_Connected;
            lastDaemonStatusUpdate = t;
        } else {
            if (t - lastDaemonStatusUpdate > STATUS_UPDATE_INTERVAL) {
                lastDaemonStatusUpdate = t;
                // these calls really connect to the daemon - wasting time
                daemonHeight = wallet.getDaemonBlockChainHeight();
                if (daemonHeight > 0) {
                   // if we get a valid height, then obviously we are connected
                    connectionStatus = Wallet.ConnectionStatus.ConnectionStatus_Connected;
                } else {
                    connectionStatus = Wallet.ConnectionStatus.ConnectionStatus_Disconnected;
                }
            }
        }
    }

    public long getDaemonHeight() {
        return this.daemonHeight;
    }

    public Wallet.ConnectionStatus getConnectionStatus() {
        return this.connectionStatus;
    }

    /////////////////////////////////////////////
    // communication back to client (activity) //
    /////////////////////////////////////////////
    // NB: This allows for only one observer, i.e. only a single activity bound here

    private Observer observer = null;

    public void setObserver(Observer anObserver) {
        observer = anObserver;
    }

    public interface Observer {
        boolean onRefreshed(Wallet wallet, boolean full);

        void onProgress(String text);

        void onProgress(int n);

        void onWalletStored(boolean success);

        void onTransactionCreated(String tag, PendingTransaction pendingTransaction);

        void onTransactionSent(String txid);

        void onSendTransactionFailed(String error);

        void onWalletStarted(Wallet.Status walletStatus);

        void onWalletOpen(Wallet.Device device);

        void onWalletFinish();
    }

    String progressText = null;
    int progressValue = -1;

    private void showProgress(String text) {
        progressText = text;
        if (observer != null) {
            observer.onProgress(text);
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable("type", WalletCallbackType.ProgressString);
        bundle.putString("data", text);
        sendBroadCast(bundle);
    }

    private void showProgress(int n) {
        progressValue = n;
        if (observer != null) {
            observer.onProgress(n);
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable("type", WalletCallbackType.ProgressInt);
        bundle.putInt("data", n);
        sendBroadCast(bundle);
    }

    public String getProgressText() {
        return progressText;
    }

    public int getProgressValue() {
        return progressValue;
    }

    public Wallet getWallet() {
        return WalletManager.getInstance().getWallet();
    }

    /////////////////////////////////////////////
    /////////////////////////////////////////////

    private WalletService.ServiceHandler mServiceHandler;

    private boolean errorState = false;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (errorState) {
                // also, we have already stopped ourselves
                return;
            }
            switch (msg.arg2) {
                case START_SERVICE -> {
                    Bundle extras = msg.getData();
                    String cmd = extras.getString(REQUEST, null);
                    switch (cmd) {
                        case REQUEST_CMD_LOAD -> {
                            String walletId = extras.getString(REQUEST_WALLET, null);
                            String walletPw = extras.getString(REQUEST_CMD_LOAD_PW, null);
                            if (walletId != null) {
                                showProgress(getString(R.string.status_wallet_loading));
                                showProgress(10);
                                Wallet.Status walletStatus = start(walletId, walletPw);
                                if (observer != null) {
                                    try {
                                        observer.onWalletStarted(walletStatus);
                                    }catch (NullPointerException ex){
                                        Log.d("Exception", ex.toString());
                                    }
                                }
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("type", WalletCallbackType.WalletStarted);
                                sendBroadCast(bundle);
                                if ((walletStatus == null) || !walletStatus.isOk()) {
                                    errorState = true;
                                    stop();
                                }
                            }
                        }
                        case REQUEST_CMD_STORE -> {
                            Wallet myWallet = getWallet();
                            if (myWallet == null) break;
                            try {
                                boolean rc = myWallet.store();
                                if (observer != null)
                                    observer.onWalletStored(rc);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("type", WalletCallbackType.WalletRestored);
                                bundle.putBoolean("data", rc);
                                sendBroadCast(bundle);
                            }catch(Exception e){
                                Log.d("WalletService ",e.toString());
                            }
                        }
                        case REQUEST_CMD_TX -> {
                            Wallet myWallet = getWallet();
                            if (myWallet == null) break;
                            myWallet.disposePendingTransaction(); // remove any old pending tx
                            TxData txData = extras.getParcelable(REQUEST_CMD_TX_DATA);
                            PendingTransaction pendingTransaction = myWallet.createTransaction(txData);
                            if (observer != null) {
                                //observer.onTransactionCreated("txTag", pendingTransaction);
                            } else {
                                myWallet.disposePendingTransaction();
                            }
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("type", WalletCallbackType.TransactionCreated);
                            bundle.putSerializable("data", pendingTransaction);
                            bundle.putString("tag", "txTag");
                            sendBroadCast(bundle);
                        }
                        case REQUEST_CMD_SWEEP -> {
                            Wallet myWallet = getWallet();
                            if (myWallet == null) break;
                            myWallet.disposePendingTransaction(); // remove any old pending tx

                            String txTag = extras.getString(REQUEST_CMD_TX_TAG);
                            PendingTransaction pendingTransaction = myWallet.createSweepUnmixableTransaction();
                            if (observer != null) {
                                //observer.onTransactionCreated(txTag, pendingTransaction);
                            } else {
                                myWallet.disposePendingTransaction();
                            }
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("type", WalletCallbackType.TransactionCreated);
                            bundle.putSerializable("data", pendingTransaction);
                            bundle.putString("tag", "txTag");
                            sendBroadCast(bundle);
                        }
                        case REQUEST_CMD_SEND -> {
                            Wallet myWallet = getWallet();
                            if (myWallet == null) break;
                            PendingTransaction pendingTransaction = myWallet.getPendingTransaction();
                            if (pendingTransaction == null) {
                                throw new IllegalArgumentException("PendingTransaction is null"); // die
                            }
                            if (pendingTransaction.getStatus() != PendingTransaction.Status.Status_Ok) {
                                final String error = pendingTransaction.getErrorString();
                                myWallet.disposePendingTransaction(); // it's broken anyway
                                // if (observer != null) observer.onSendTransactionFailed(error);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("type", WalletCallbackType.SendTransactionFailed);
                                bundle.putString("data", error);
                                sendBroadCast(bundle);
                                return;
                            }
                            final String txid = pendingTransaction.getFirstTxId(); // tx ids vanish after commit()!
                            boolean success = pendingTransaction.commit("", true);
                            if (success) {
                                myWallet.disposePendingTransaction();
                                // if (observer != null) observer.onTransactionSent(txid);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("type", WalletCallbackType.TransactionSent);
                                bundle.putString("data", txid);
                                sendBroadCast(bundle);
                                String notes = extras.getString(REQUEST_CMD_SEND_NOTES);
                                if ((notes != null) && (!notes.isEmpty())) {
                                    myWallet.setUserNote(txid, notes);
                                }
                                try {
                                    boolean rc = myWallet.store();
                                    if (observer != null) observer.onWalletStored(rc);
                                    Bundle bundle1 = new Bundle();
                                    bundle.putSerializable("type", WalletCallbackType.WalletRestored);
                                    bundle.putBoolean("data", rc);
                                    sendBroadCast(bundle1);
                                    listener.updated = true;
                                }catch(Exception e){
                                    Log.d("WalletService",e.toString());
                                }
                            } else {
                                final String error = pendingTransaction.getErrorString();
                                myWallet.disposePendingTransaction();
                                // if (observer != null) observer.onSendTransactionFailed(error);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("type", WalletCallbackType.SendTransactionFailed);
                                bundle.putString("data", error);
                                sendBroadCast(bundle);
                                return;
                            }
                        }
                    }
                }
                case STOP_SERVICE -> stop();
                default -> Timber.e("UNKNOWN %s", msg.arg2);
            }
        }
    }

    @Override
    public void onCreate() {
        // We are using a HandlerThread and a Looper to avoid loading and closing
        // concurrency
        BchatHandlerThread thread = new BchatHandlerThread("WalletService",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        /*Task task =new Task(Process.THREAD_PRIORITY_BACKGROUND);
        ThreadUtils.queue(task);*/

        // Get the HandlerThread's Looper and use it for our Handler
        final Looper serviceLooper = thread.getLooper();

        mServiceHandler = new WalletService.ServiceHandler(serviceLooper);
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(LocalHelper.setPreferredLocale(context));
    }

    public class WalletServiceBinder extends Binder {
        public WalletService getService() {
            return WalletService.this;
        }
    }

    private final IBinder mBinder = new WalletServiceBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Running = true;
        // when the activity starts the service, it expects to start it for a new wallet
        // the service is possibly still occupied with saving the last opened wallet
        // so we queue the open request
        // this should not matter since the old activity is not getting updates
        // and the new one is not listening yet (although it will be bound)
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg2 = START_SERVICE;
        if (intent != null) {
            msg.setData(intent.getExtras());
            mServiceHandler.sendMessage(msg);
            return START_STICKY;
        } else {
            // process restart - don't do anything - let system kill it again
            stop();
            return START_NOT_STICKY;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Very first client binds
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        Message msg = mServiceHandler.obtainMessage();
        msg.arg2 = STOP_SERVICE;
        mServiceHandler.sendMessage(msg);
        return true; // true is important so that onUnbind is also called next time
    }

    @Nullable
    private Wallet.Status start(String walletName, String walletPassword) {
        startNotification();
        showProgress(getString(R.string.status_wallet_loading));
        showProgress(10);
        if (listener == null) {
            Wallet aWallet = loadWallet(walletName, walletPassword);
            if (aWallet == null) return null;
            if(CheckOnline.Companion.isOnline(getApplicationContext())) {
                Wallet.Status walletStatus = aWallet.getFullStatus();
                if (!walletStatus.isOk()) {
                    Toast.makeText(getApplicationContext(),getString(R.string.please_try_after_some_time),Toast.LENGTH_SHORT).show();
                    return walletStatus;
                }
            }
            else{
                Toast.makeText(getApplicationContext(),getString(R.string.please_check_your_internet_connection),Toast.LENGTH_SHORT).show();
                return null;
            }
            listener = new MyWalletListener();
            listener.start();
            showProgress(100);
        }
        showProgress(getString(R.string.status_wallet_connecting));
        showProgress(101);
        // if we try to refresh the history here we get occasional segfaults!
        // doesnt matter since we update as soon as we get a new block anyway
        return getWallet().getFullStatus();
    }

    public void stop() {
        setObserver(null); // in case it was not reset already
        if (listener != null) {
            listener.stop();
            try {
                Wallet myWallet = getWallet();
                myWallet.close();
            } catch (Exception e) {
                Log.d("WalletService", e.toString());
            }
            listener = null;
        }
        stopForeground(true);
        stopSelf();
        Running = false;
    }

    private Wallet loadWallet(String walletName, String walletPassword) {
        Wallet wallet = openWallet(walletName, walletPassword);
        if (wallet != null) {
            long walletRestoreHeight = wallet.getRestoreHeight();
            showProgress(55);
            if (!CheckOnline.Companion.isOnline(getApplicationContext())) {
                return null;
            } else {
                try {
                    wallet.init(0);
                }catch (Exception e){
                    Log.d("WalletService",e.toString());
                }
                wallet.setRestoreHeight(walletRestoreHeight);
                showProgress(90);
            }

        }
        return wallet;
    }

    private Wallet openWallet(String walletName, String walletPassword) {
        String path = Helper.getWalletFile(getApplicationContext(), walletName).getAbsolutePath();
        showProgress(20);
        Wallet wallet = null;
        WalletManager walletMgr = WalletManager.getInstance();
        showProgress(30);
        if (walletMgr.walletExists(path)) {
            wallet = walletMgr.openWallet(path, walletPassword);
            showProgress(60);
            Wallet.Status walletStatus = wallet.getStatus();
            if (!walletStatus.isOk()) {
                WalletManager.getInstance().close(wallet);
                wallet = null;
            }
        }
        return wallet;
    }

    private void startNotification() {
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel() : "";
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.service_description))
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification_)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(
                    NOTIFICATION_ID,
                    notification);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.service_description),
                NotificationManager.IMPORTANCE_LOW);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return CHANNEL_ID;
    }

    private void sendBroadCast(Bundle data) {
        Intent intent = new Intent();
        intent.setAction("io.beldex.WALLET_ACTION");
        intent.putExtra("io.beldex.WALLET_DATA", data);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}
//endregion

