package io.beldex.bchat.wallet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.beldex.bchat.R
import io.beldex.bchat.data.TxData
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.model.PendingTransaction
import io.beldex.bchat.model.Wallet
import io.beldex.bchat.model.Wallet.ConnectionStatus
import io.beldex.bchat.model.WalletListener
import io.beldex.bchat.model.WalletManager
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.LocalHelper
import io.beldex.bchat.wallet.CheckOnline.Companion.isOnline
import io.beldex.bchat.wallet.WalletFragment.Companion.syncingBlocks
import io.beldex.bchat.wallet.utils.WalletCallbackType
import timber.log.Timber

class WalletService : Service() {
    private var listener: MyWalletListener? = null

    private inner class MyWalletListener : WalletListener {
        var updated = true
        fun start() {
            val wallet: Wallet = wallet
            try {
                if (wallet != null) {
                    wallet.setListener(this)
                    wallet.startRefresh()
                }
            } catch (ex: IllegalStateException) {
                Log.d("Beldex", "IllegalStateException $ex")
            }
        }

        fun stop() {
            val wallet: Wallet = wallet
            try {
                if (wallet != null) {
                    wallet.pauseRefresh()
                    wallet.setListener(null)
                }
            } catch (ex: IllegalStateException) {
                Log.d("Beldex", "IllegalStateException $ex")
            }
        }

        // WalletListener callbacks
        override fun moneySpent(txId: String, amount: Long) {
            Timber.d("moneySpent() %d @ %s", amount, txId)
        }

        override fun moneyReceived(txId: String, amount: Long) {
            Timber.d("moneyReceived() %d @ %s", amount, txId)
        }

        override fun unconfirmedMoneyReceived(txId: String, amount: Long) {
            Timber.d("unconfirmedMoneyReceived() %d @ %s", amount, txId)
        }

        private var lastBlockTime: Long = 0
        override fun newBlock(height: Long) {
            val wallet: Wallet = wallet
            if (wallet != null) {
                // don't flood with an update for every block ...
                if (lastBlockTime < System.currentTimeMillis() - 2000) {
                    lastBlockTime = System.currentTimeMillis()
                    if (observer != null) {
                        updateDaemonState(wallet, if (wallet.isSynchronized) height else 0)
                        if (!wallet.isSynchronized) {
                            updated = true
                        }
                        if (observer != null) observer!!.onRefreshed(wallet, false)
                    }
                    val bundle = Bundle()
                    bundle.putSerializable("type", WalletCallbackType.WalletRefreshed)
                    bundle.putBoolean("full_refresh", false)
                    sendBroadCast(bundle)
                }
            }
        }

        override fun updated() {
            val wallet: Wallet = wallet
            if (wallet != null) {
                updated = true
            }
        }

        override fun refreshed() { // this means it's synced
            val wallet: Wallet = wallet
            val latestBlock = syncingBlocks
            if (wallet != null) {
                if (isOnline(applicationContext)) {
                    val blockChainHeight = wallet.daemonBlockChainHeight
                    val syncedBlockHeight = wallet.blockChainHeight
                    val latestSyncedBlockHeight = blockChainHeight - syncedBlockHeight
                    if (latestBlock < 50L || latestSyncedBlockHeight < 50L) {
                        wallet.setSynchronized()
                    }
                }
                if (updated) {
                    updateDaemonState(wallet, wallet.blockChainHeight)
                    if (observer != null) {
                        updated = !observer!!.onRefreshed(wallet, true)
                    }
                    val bundle = Bundle()
                    bundle.putSerializable("type", WalletCallbackType.WalletRefreshed)
                    bundle.putBoolean("full_refresh", true)
                    sendBroadCast(bundle)
                }
            }
        }
    }

    private var lastDaemonStatusUpdate: Long = 0
    var daemonHeight: Long = 0
        private set
    var connectionStatus = ConnectionStatus.ConnectionStatus_Connecting
        private set

    private fun updateDaemonState(wallet: Wallet, height: Long) {
        val t = System.currentTimeMillis()
        if (height > 0) { // if we get a height, we are connected
            daemonHeight = height
            connectionStatus = ConnectionStatus.ConnectionStatus_Connected
            lastDaemonStatusUpdate = t
        } else {
            if (t - lastDaemonStatusUpdate > STATUS_UPDATE_INTERVAL) {
                lastDaemonStatusUpdate = t
                // these calls really connect to the daemon - wasting time
                daemonHeight = wallet.daemonBlockChainHeight
                connectionStatus = if (daemonHeight > 0) {
                    // if we get a valid height, then obviously we are connected
                    ConnectionStatus.ConnectionStatus_Connected
                } else {
                    ConnectionStatus.ConnectionStatus_Disconnected
                }
            }
        }
    }

    /////////////////////////////////////////////
    // communication back to client (activity) //
    /////////////////////////////////////////////
    // NB: This allows for only one observer, i.e. only a single activity bound here
    private var observer: Observer? = null
    fun setObserver(anObserver: Observer?) {
        observer = anObserver
    }

    interface Observer {
        fun onRefreshed(wallet: Wallet?, full: Boolean): Boolean
        fun onProgress(text: String?)
        fun onProgress(n: Float)
        fun onWalletStored(success: Boolean)
        fun onTransactionCreated(tag: String?, pendingTransaction: PendingTransaction?)
        fun onTransactionSent(txid: String?)
        fun onSendTransactionFailed(error: String?)
        fun onWalletStarted(walletStatus: Wallet.Status?)
        fun onWalletOpen(device: Wallet.Device?)
        fun onWalletFinish()
    }

    var progressText: String? = null
    var progressValue = 4f

    private fun showProgress(text: String) {
        progressText = text
        if (observer != null) {
            observer!!.onProgress(text)
        }
        val bundle = Bundle()
        bundle.putSerializable("type", WalletCallbackType.ProgressString)
        bundle.putString("data", text)
        sendBroadCast(bundle)
    }

    private fun showProgress(n: Float) {
        progressValue = n
        if (observer != null) {
            observer!!.onProgress(n)
        }
        val bundle = Bundle()
        bundle.putSerializable("type", WalletCallbackType.ProgressInt)
        bundle.putFloat("data", n)
        sendBroadCast(bundle)
    }

    val wallet: Wallet
        get() = WalletManager.getInstance().wallet

    /////////////////////////////////////////////
    /////////////////////////////////////////////
    private var mServiceHandler: ServiceHandler? = null
    private var errorState = false

    // Handler that receives messages from the thread
    private inner class ServiceHandler internal constructor(looper: Looper?) : Handler(
        looper!!
    ) {
        override fun handleMessage(msg: Message) {
            if (errorState) {
                // also, we have already stopped ourselves
                return
            }
            when (msg.arg2) {
                START_SERVICE -> {
                     val extras = msg.data
                     val cmd = extras.getString(
                         REQUEST,
                         null
                     )
                     when (cmd) {
                         REQUEST_CMD_LOAD -> {
                              val walletId = extras.getString(
                                  REQUEST_WALLET,
                                  null
                              )
                              val walletPw = extras.getString(
                                  REQUEST_CMD_LOAD_PW,
                                  null
                              )
                              if (walletId != null) {
                                  showProgress(getString(R.string.status_wallet_loading))
                                  showProgress(0.1f)
                                  val walletStatus = start(walletId, walletPw)
                                  if (observer != null) {
                                      try {
                                          observer!!.onWalletStarted(walletStatus)
                                      } catch (ex: NullPointerException) {
                                          Log.d("Exception", ex.toString())
                                      }
                                  }
                                  val bundle = Bundle()
                                  bundle.putSerializable(
                                      "type",
                                      WalletCallbackType.WalletStarted
                                  )
                                  sendBroadCast(bundle)
                                  if (walletStatus == null || !walletStatus.isOk) {
                                      errorState = true
                                      stop()
                                  }
                              }
                         }

                         REQUEST_CMD_STORE -> {
                             val myWallet: Wallet = wallet
                             if(myWallet != null) {
                                 try {
                                     val rc = myWallet.store()
                                     if (observer != null) observer!!.onWalletStored(rc)
                                     val bundle = Bundle()
                                     bundle.putSerializable(
                                         "type",
                                         WalletCallbackType.WalletRestored
                                     )
                                     bundle.putBoolean("data", rc)
                                     sendBroadCast(bundle)
                                 } catch (e: Exception) {
                                     Log.d("WalletService ", e.toString())
                                 }
                             }
                         }

                         REQUEST_CMD_TX -> {
                             val myWallet: Wallet = wallet
                             if(myWallet != null) {
                                 myWallet.disposePendingTransaction() // remove any old pending tx
                                 val txData =
                                     extras.getParcelable<TxData>(REQUEST_CMD_TX_DATA)
                                 val pendingTransaction =
                                     myWallet.createTransaction(txData)
                                 if (observer != null) {
                                     observer!!.onTransactionCreated("txTag", pendingTransaction)
                                 } else {
                                     myWallet.disposePendingTransaction()
                                 }
                                 val bundle = Bundle()
                                 bundle.putSerializable(
                                     "type",
                                     WalletCallbackType.TransactionCreated
                                 )
                                 bundle.putSerializable("data", pendingTransaction)
                                 bundle.putString("tag", "txTag")
                                 sendBroadCast(bundle)
                             }
                         }

                         REQUEST_CMD_SWEEP -> {
                             val myWallet: Wallet = wallet
                             if(myWallet != null) {
                                 myWallet.disposePendingTransaction() // remove any old pending tx
                                 val txTag =
                                     extras.getString(REQUEST_CMD_TX_TAG)
                                 val pendingTransaction =
                                     myWallet.createSweepUnmixableTransaction()
                                 if (observer != null) {
                                     observer!!.onTransactionCreated(txTag, pendingTransaction)
                                 } else {
                                     myWallet.disposePendingTransaction()
                                 }
                                 val bundle = Bundle()
                                 bundle.putSerializable(
                                     "type",
                                     WalletCallbackType.TransactionCreated
                                 )
                                 bundle.putSerializable("data", pendingTransaction)
                                 bundle.putString("tag", "txTag")
                                 sendBroadCast(bundle)
                             }
                         }

                         REQUEST_CMD_SEND -> {
                             val myWallet: Wallet = wallet
                             if(myWallet != null) {
                                 val pendingTransaction =
                                     myWallet.pendingTransaction
                                         ?: throw IllegalArgumentException("PendingTransaction is null")
                                 if (pendingTransaction.status != PendingTransaction.Status.Status_Ok) {
                                     val error = pendingTransaction.errorString
                                     myWallet.disposePendingTransaction() // it's broken anyway
                                     if (observer != null) observer!!.onSendTransactionFailed(error)
                                     val bundle = Bundle()
                                     bundle.putSerializable(
                                         "type",
                                         WalletCallbackType.SendTransactionFailed
                                     )
                                     bundle.putString("data", error)
                                     sendBroadCast(bundle)
                                     return
                                 }
                                 val txid =
                                     pendingTransaction.firstTxId // tx ids vanish after commit()!
                                 val success = pendingTransaction.commit("", true)
                                 if (success) {
                                     myWallet.disposePendingTransaction()
                                     if (observer != null) observer!!.onTransactionSent(txid)
                                     val bundle = Bundle()
                                     bundle.putSerializable(
                                         "type",
                                         WalletCallbackType.TransactionSent
                                     )
                                     bundle.putString("data", txid)
                                     sendBroadCast(bundle)
                                     val notes =
                                         extras.getString(REQUEST_CMD_SEND_NOTES)
                                     if (notes != null && !notes.isEmpty()) {
                                         myWallet.setUserNote(txid, notes)
                                     }
                                     try {
                                         val rc = myWallet.store()
                                         if (observer != null) observer!!.onWalletStored(rc)
                                         val bundle1 = Bundle()
                                         bundle.putSerializable(
                                             "type",
                                             WalletCallbackType.WalletRestored
                                         )
                                         bundle.putBoolean("data", rc)
                                         sendBroadCast(bundle1)
                                         listener!!.updated = true
                                     } catch (e: Exception) {
                                         Log.d("WalletService", e.toString())
                                     }
                                 } else {
                                     val error = pendingTransaction.errorString
                                     myWallet.disposePendingTransaction()
                                     if (observer != null) observer!!.onSendTransactionFailed(error)
                                     val bundle = Bundle()
                                     bundle.putSerializable(
                                         "type",
                                         WalletCallbackType.SendTransactionFailed
                                     )
                                     bundle.putString("data", error)
                                     sendBroadCast(bundle)
                                     return
                                 }
                             }
                         }

                         else -> Timber.e("UNKNOWN %s", msg.arg2)
                     }
                }

                STOP_SERVICE -> {
                    stop()
                    Timber.e("UNKNOWN %s", msg.arg2)
                }

                else -> Timber.e("UNKNOWN %s", msg.arg2)
            }
        }
    }

    override fun onCreate() {
        // We are using a HandlerThread and a Looper to avoid loading and closing
        // concurrency
        val thread = BchatHandlerThread(
            "WalletService",
            Process.THREAD_PRIORITY_BACKGROUND
        )
        thread.start()
        /*Task task =new Task(Process.THREAD_PRIORITY_BACKGROUND);
        ThreadUtils.queue(task);*/


        // Get the HandlerThread's Looper and use it for our Handler
        val serviceLooper = thread.looper
        mServiceHandler = ServiceHandler(serviceLooper)
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(LocalHelper.setPreferredLocale(context))
    }

    inner class WalletServiceBinder : Binder() {
        val service: WalletService
            get() = this@WalletService
    }

    private val mBinder: IBinder = WalletServiceBinder()
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Running = true
        // when the activity starts the service, it expects to start it for a new wallet
        // the service is possibly still occupied with saving the last opened wallet
        // so we queue the open request
        // this should not matter since the old activity is not getting updates
        // and the new one is not listening yet (although it will be bound)
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        val msg = mServiceHandler!!.obtainMessage()
        msg.arg2 = START_SERVICE
        return if (intent != null) {
            msg.data = intent.extras
            mServiceHandler!!.sendMessage(msg)
            START_STICKY
        } else {
            // process restart - don't do anything - let system kill it again
            stop()
            START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        // Very first client binds
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        val msg = mServiceHandler!!.obtainMessage()
        msg.arg2 = STOP_SERVICE
        mServiceHandler!!.sendMessage(msg)
        return true // true is important so that onUnbind is also called next time
    }

    private fun start(walletName: String, walletPassword: String): Wallet.Status? {
        startNotification()
        showProgress(getString(R.string.status_wallet_loading))
        showProgress(0.1f)
        if (listener == null) {
            val aWallet = loadWallet(walletName, walletPassword) ?: return null
            if (isOnline(applicationContext)) {
                val walletStatus = aWallet.fullStatus
                if (!walletStatus.isOk) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.please_try_after_some_time),
                        Toast.LENGTH_SHORT
                    ).show()
                    return walletStatus
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.please_check_your_internet_connection),
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }
            listener = MyWalletListener()
            listener!!.start()
            showProgress(1f)
        }
        showProgress(getString(R.string.status_wallet_connecting))
        showProgress(2f)
        // if we try to refresh the history here we get occasional segfaults!
        // doesnt matter since we update as soon as we get a new block anyway
        return wallet.fullStatus
    }

    fun stop() {
        setObserver(null) // in case it was not reset already
        if (listener != null) {
            listener!!.stop()
            try {
                val myWallet = wallet
                myWallet.close()
            } catch (e: Exception) {
                Log.d("WalletService", e.toString())
            }
            listener = null
        }
        stopForeground(true)
        stopSelf()
        Running = false
    }

    private fun loadWallet(walletName: String, walletPassword: String): Wallet? {
        val wallet = openWallet(walletName, walletPassword)
        if (wallet != null) {
            val walletRestoreHeight = wallet.restoreHeight
            showProgress(0.55f)
            if (!isOnline(applicationContext)) {
                return null
            } else {
                try {
                    wallet.init(0)
                } catch (e: Exception) {
                    Log.d("WalletService", e.toString())
                }
                wallet.restoreHeight = walletRestoreHeight
                showProgress(0.9f)
            }
        }
        return wallet
    }

    private fun openWallet(walletName: String, walletPassword: String): Wallet? {
        val path = Helper.getWalletFile(applicationContext, walletName).absolutePath
        showProgress(0.2f)
        var wallet: Wallet? = null
        val walletMgr = WalletManager.getInstance()
        showProgress(0.3f)
        if (walletMgr.walletExists(path)) {
            wallet = walletMgr.openWallet(path, walletPassword)
            showProgress(0.6f)
            val walletStatus = wallet.status
            if (!walletStatus.isOk) {
                WalletManager.getInstance().close(wallet)
                wallet = null
            }
        }
        return wallet
    }

    private fun startNotification() {
        val notificationIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel() else ""
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_description))
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification_)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.service_description),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(channel)
        return CHANNEL_ID
    }

    private fun sendBroadCast(data: Bundle) {
        val intent = Intent()
        intent.setAction("io.beldex.WALLET_ACTION")
        intent.putExtra("io.beldex.WALLET_DATA", data)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    companion object {
        var Running = false
        const val NOTIFICATION_ID = 2049
        const val CHANNEL_ID = "m_service"
        const val REQUEST_WALLET = "wallet"
        const val REQUEST = "request"
        const val REQUEST_CMD_LOAD = "load"
        const val REQUEST_CMD_LOAD_PW = "walletPassword"
        const val REQUEST_CMD_STORE = "store"
        const val REQUEST_CMD_TX = "createTX"
        const val REQUEST_CMD_TX_DATA = "data"
        const val REQUEST_CMD_TX_TAG = "tag"
        const val REQUEST_CMD_SWEEP = "sweepTX"
        const val REQUEST_CMD_SEND = "send"
        const val REQUEST_CMD_SEND_NOTES = "notes"
        const val START_SERVICE = 1
        const val STOP_SERVICE = 2
        private const val STATUS_UPDATE_INTERVAL: Long = 120000 // 120s (blocktime)
    }
}
