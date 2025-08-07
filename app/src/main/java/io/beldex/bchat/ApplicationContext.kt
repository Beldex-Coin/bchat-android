package io.beldex.bchat

import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.beldex.libbchat.avatars.AvatarHelper
import com.beldex.libbchat.database.MessageDataProvider
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.MessagingModuleConfiguration.Companion.configure
import com.beldex.libbchat.messaging.sending_receiving.notifications.MessageNotifier
import com.beldex.libbchat.messaging.sending_receiving.pollers.ClosedGroupPollerV2.Companion.shared
import com.beldex.libbchat.messaging.sending_receiving.pollers.Poller
import com.beldex.libbchat.messaging.utilities.WindowDebouncer
import com.beldex.libbchat.mnode.MnodeModule.Companion.configure
import com.beldex.libbchat.utilities.Address.Companion.fromSerialized
import com.beldex.libbchat.utilities.Device
import com.beldex.libbchat.utilities.ProfilePictureUtilities.upload
import com.beldex.libbchat.utilities.SSKEnvironment.Companion.configure
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.clearAll
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getLanguage
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getLastProfilePictureUpload
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getLocalNumber
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getProfileKey
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getProfileName
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.isPushEnabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setLastProfilePictureUpload
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setProfileName
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setPushEnabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setRefreshDynamicNodesStatus
import com.beldex.libbchat.utilities.Util.runOnMain
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper
import com.beldex.libbchat.utilities.dynamiclanguage.LocaleParser.Companion.configure
import com.beldex.libsignal.utilities.HTTP.isConnectedToNetwork
import com.beldex.libsignal.utilities.JsonUtil
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.ThreadUtils.queue
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.EntryPoints
import dagger.hilt.android.HiltAndroidApp
import io.beldex.bchat.AppContext.configureKovenant
import io.beldex.bchat.components.TypingStatusSender
import io.beldex.bchat.crypto.KeyPairUtilities.getUserED25519KeyPair
import io.beldex.bchat.database.BeldexAPIDatabase
import io.beldex.bchat.database.EmojiSearchDatabase
import io.beldex.bchat.database.JobDatabase
import io.beldex.bchat.database.Storage
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper
import io.beldex.bchat.database.model.EmojiSearchData
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.dependencies.DatabaseModule.init
import io.beldex.bchat.emoji.EmojiSource.Companion.refresh
import io.beldex.bchat.groups.OpenGroupManager.startPolling
import io.beldex.bchat.groups.OpenGroupManager.stopPolling
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.jobmanager.JobManager
import io.beldex.bchat.jobmanager.impl.JsonDataSerializer
import io.beldex.bchat.jobmanager.impl.NetworkConstraint
import io.beldex.bchat.jobs.FastJobStorage
import io.beldex.bchat.jobs.JobManagerFactories
import io.beldex.bchat.logging.AndroidLogger
import io.beldex.bchat.logging.PersistentLogger
import io.beldex.bchat.logging.UncaughtExceptionLogger
import io.beldex.bchat.model.NetworkType
import io.beldex.bchat.notifications.BackgroundPollWorker.Companion.schedulePeriodic
import io.beldex.bchat.notifications.DefaultMessageNotifier
import io.beldex.bchat.notifications.NotificationChannels
import io.beldex.bchat.notifications.OptimizedMessageNotifier
import io.beldex.bchat.notifications.PushRegistry
import io.beldex.bchat.providers.BlobProvider
import io.beldex.bchat.service.ExpiringMessageManager
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.sskenvironment.ProfileManager
import io.beldex.bchat.sskenvironment.ReadReceiptManager
import io.beldex.bchat.sskenvironment.TypingStatusRepository
import io.beldex.bchat.util.Broadcaster
import io.beldex.bchat.util.FirebaseRemoteConfigUtil
import io.beldex.bchat.util.UiModeUtilities.setupUiModeToUserSelected
import io.beldex.bchat.util.dynamiclanguage.LocaleParseHelper
import io.beldex.bchat.webrtc.CallMessageProcessor
import kotlinx.coroutines.Job
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import org.conscrypt.Conscrypt
import org.signal.aesgcmprovider.AesGcmProvider
import org.webrtc.PeerConnectionFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.Security
import java.util.Date
import java.util.Timer
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.concurrent.Volatile

@HiltAndroidApp
class ApplicationContext:  Application(), DefaultLifecycleObserver {

    val PREFERENCES_NAME="SecureSMS-Preferences"
    val TAG=ApplicationContext::class.java.simpleName

    lateinit var expiringMessageManager: ExpiringMessageManager
    lateinit var typingStatusRepository : TypingStatusRepository
    lateinit var typingStatusSender : TypingStatusSender
    lateinit var jobManager : JobManager


    lateinit var readReceiptManager : ReadReceiptManager

    lateinit var profileManager : ProfileManager
    lateinit var messageNotifier : MessageNotifier
    @JvmField
    var poller : Poller?=null
    var broadcaster : Broadcaster?=null
    val firebaseInstanceIdJob : Job?=null
    var conversationListDebouncer: WindowDebouncer? = null
        get() {
            if (field == null) {
                field = WindowDebouncer(1000, Timer())
            }
            return field
        }
        private set
    var conversationListHandlerThread : HandlerThread?=null
    var conversationListHandler : Handler?=null
    var persistentLogger : PersistentLogger?= null

    @Inject lateinit var beldexAPIDatabase : BeldexAPIDatabase
    @Inject lateinit var storage: Storage
    @Inject lateinit var device: Device
    @Inject lateinit var pushRegistry : PushRegistry
    @Inject lateinit var messageDataProvider: MessageDataProvider
    @Inject lateinit var jobDatabase : JobDatabase
    @Inject lateinit var textSecurePreferences: TextSecurePreferences
    @Inject lateinit var remoteConfigUtil : FirebaseRemoteConfigUtil
    private var messagingModuleConfiguration: MessagingModuleConfiguration? = null
    lateinit var callMessageProcessor : CallMessageProcessor

    @Volatile
    var isAppVisible: Boolean = false
    val KEYGUARD_LOCK_TAG="BChat Messenger" + ":KeyguardLock"
    val WAKELOCK_TAG="BChat Messenger" + ":WakeLock"

    override fun getSystemService(name: String): Any? {
        if (MessagingModuleConfiguration.MESSAGING_MODULE_SERVICE == name) {
            return messagingModuleConfiguration
        }
        return super.getSystemService(name)
    }

    @get:Deprecated(message = "Use proper DI to inject this component")
    val databaseComponent: DatabaseComponent
        get() = EntryPoints.get(
            applicationContext,
            DatabaseComponent::class.java
        )

    fun conversationListNotificationHandler() : Handler? {
        if (this.conversationListHandlerThread == null) {
            conversationListHandlerThread=HandlerThread("ConversationListHandler")
            conversationListHandlerThread!!.start()
        }
        if (this.conversationListHandler == null) {
            conversationListHandler=Handler(conversationListHandlerThread!!.looper)
        }
        return conversationListHandler
    }

    override fun onCreate() {
        init(this)
        configure(this)
        super<Application>.onCreate()
        messagingModuleConfiguration=MessagingModuleConfiguration(
            this,
            storage,
            device,
            messageDataProvider
        ) { getUserED25519KeyPair(this) }
        callMessageProcessor=CallMessageProcessor(
            this,
            textSecurePreferences, ProcessLifecycleOwner.get().lifecycle, storage
        )
        Log.i(TAG, "onCreate()")
        startKovenant()
        initializeSecurityProvider()
        initializeLogging()
        initializeCrashHandling()
        NotificationChannels.create(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        configureKovenant()
        messageNotifier=OptimizedMessageNotifier(DefaultMessageNotifier())
        broadcaster=Broadcaster(this)
        val apiDB=databaseComponent.beldexAPIDatabase()
        configure(apiDB, broadcaster!!)
        setupUiModeToUserSelected(this)
        initializeExpiringMessageManager()
        initializeTypingStatusRepository()
        initializeTypingStatusSender()
        initializeReadReceiptManager()
        initializeProfileManager()
        initializePeriodicTasks()
        configure(
            typingStatusRepository,
            readReceiptManager,
            profileManager,
            messageNotifier,
            expiringMessageManager
        )
        initializeJobManager()
        initializeWebRtc()
        initializeBlobProvider()
        resubmitProfilePictureIfNeeded()
        loadEmojiSearchIndexIfNeeded()
        refresh()
        val networkConstraint=NetworkConstraint.Factory(this).create()
        isConnectedToNetwork={ networkConstraint.isMet }
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(BuildConfig.CRASHLYTICS_ENABLED)
    }

    override fun onStart(owner : LifecycleOwner) {
        setRefreshDynamicNodesStatus(this, true)
        isAppVisible=true
        Log.i(TAG, "App is now visible.")
        KeyCachingService.onAppForegrounded(this)
        queue(Runnable {
            if (poller != null) {
                poller!!.isCaughtUp=false
            }
            startPollingIfNeeded()
            startPolling()
        })
    }

    override fun onStop(owner : LifecycleOwner) {
        isAppVisible=false
        Log.i(TAG, "App is no longer visible.")
        KeyCachingService.onAppBackgrounded(this)
        messageNotifier.setVisibleThread(-1)
        poller?.stopIfNeeded()
        shared.stop()
    }

    override fun onTerminate() {
        stopKovenant() // Beldex
        stopPolling()
        super.onTerminate()
    }

    fun initializeLocaleParser() {
        configure(LocaleParseHelper())
    }

    private fun initializeSecurityProvider() {
        try {
            Class.forName("org.signal.aesgcmprovider.AesGcmCipher")
            val aesPosition = Security.insertProviderAt(AesGcmProvider(), 1)
            Log.i(TAG, "Installed AesGcmProvider: $aesPosition")
            if (aesPosition < 0) {
                Log.e(TAG, "Failed to install AesGcmProvider()")
                // Only throw if you truly can't proceed without it
                // throw ProviderInitializationException()
            }
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "AesGcmCipher class not found - skipping AesGcmProvider", e)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native libs not found - skipping AesGcmProvider", e)
        }

        val conscryptPosition = Security.insertProviderAt(Conscrypt.newProvider(), 2)
        Log.i(TAG, "Installed Conscrypt provider: $conscryptPosition")
        if (conscryptPosition < 0) {
            Log.w(TAG, "Did not install Conscrypt provider. May already be present.")
        }
    }


    private fun initializeLogging() {
        persistentLogger = PersistentLogger(this)
        Log.initialize(AndroidLogger(), persistentLogger)
    }

    private fun initializeCrashHandling() {
        val originalHandler=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionLogger(originalHandler!!))
    }

    private fun initializeJobManager() {
        jobManager=JobManager(
            this, JobManager.Configuration.Builder()
                .setDataSerializer(JsonDataSerializer())
                .setJobFactories(JobManagerFactories.getJobFactories(this))
                .setConstraintFactories(JobManagerFactories.getConstraintFactories(this))
                .setConstraintObservers(JobManagerFactories.getConstraintObservers(this))
                .setJobStorage(FastJobStorage(jobDatabase))
                .build()
        )
    }

    private fun initializeExpiringMessageManager() {
        expiringMessageManager=ExpiringMessageManager(this)
    }

    private fun initializeTypingStatusRepository() {
        typingStatusRepository=TypingStatusRepository()
    }

    private fun initializeReadReceiptManager() {
        readReceiptManager=ReadReceiptManager()
    }

    private fun initializeProfileManager() {
        profileManager=ProfileManager()
    }

    private fun initializeTypingStatusSender() {
        typingStatusSender=TypingStatusSender(this)
    }

    private fun initializePeriodicTasks() {
        schedulePeriodic(this)
        if (BuildConfig.PLAY_STORE_DISABLED) {
            // possibly add update apk job
        }
    }

    private fun initializeWebRtc() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(
                    this
                ).createInitializationOptions()
            )
        } catch (e : UnsatisfiedLinkError) {
            Log.w(TAG, e)
        }
    }

    private fun initializeBlobProvider() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            BlobProvider.getInstance().onBchatStart(this)
        }
    }

    override fun attachBaseContext(base : Context?) {
        initializeLocaleParser()
        super.attachBaseContext(
            DynamicLanguageContextWrapper.updateContext(
                base, getLanguage(
                    base!!
                )
            )
        )
    }


    private class ProviderInitializationException() : RuntimeException()


    private fun setUpPollingIfNeeded() {
        val userPublicKey=getLocalNumber(this) ?: return
        if (poller != null) {
            poller!!.userPublicKey=userPublicKey
            return
        }
        poller=Poller()
    }

    fun startPollingIfNeeded() {
        setUpPollingIfNeeded()
        poller?.startIfNeeded()
        shared.start()
    }

    private fun resubmitProfilePictureIfNeeded() {
        // Files expire on the file server after a while, so we simply re-upload the user's profile picture
        // at a certain interval to ensure it's always available.
        val userPublicKey=getLocalNumber(this) ?: return
        val now=Date().time
        val lastProfilePictureUpload=getLastProfilePictureUpload(this)
        if (now - lastProfilePictureUpload <= 14 * 24 * 60 * 60 * 1000) return
        queue(Runnable {

            // Don't generate a new profile key here; we do that when the user changes their profile picture
            val encodedProfileKey : String?=
                getProfileKey(this)
            try {
                // Read the file into a byte array
                val inputStream : InputStream=AvatarHelper.getInputStreamFor(
                    this,
                    fromSerialized(userPublicKey),
                    false
                )
                val baos : ByteArrayOutputStream=ByteArrayOutputStream()
                var count : Int
                val buffer : ByteArray=ByteArray(1024)
                while ((inputStream.read(buffer, 0, buffer.size).also { count=it }) != -1) {
                    baos.write(buffer, 0, count)
                }
                baos.flush()
                val profilePicture : ByteArray=baos.toByteArray()
                // Re-upload it
                upload(
                    profilePicture,
                    (encodedProfileKey)!!,
                    this
                ).success {
                    // Update the last profile picture upload date
                    setLastProfilePictureUpload(
                        this,
                        Date().time
                    )
                    Unit
                }
            } catch (exception : Exception) {
                // Do nothing
            }
        })
    }

    // Method to clear the local data - returns true on success otherwise false
    fun clearAllData(isMigratingToV2KeyPair : Boolean) {
        if (firebaseInstanceIdJob != null && firebaseInstanceIdJob.isActive) {
            firebaseInstanceIdJob.cancel(null)
        }
        val displayName=getProfileName(this)
        val isUsingFCM=isPushEnabled(this)
        clearAll(this)
        if (isMigratingToV2KeyPair) {
            setPushEnabled(this, isUsingFCM)
            setProfileName(this, displayName)
        }
        getSharedPreferences(PREFERENCES_NAME, 0).edit().clear().commit()
        if (!deleteDatabase(SQLCipherOpenHelper.DATABASE_NAME)) {
            Log.d("Beldex", "Failed to delete database.")
        }
        runOnMain {
            Handler().postDelayed(
                Runnable { this.restartApplication() }, 200
            )
        }
    }

    fun restartApplication() {
        val intent=Intent(this, HomeActivity::class.java)
        startActivity(Intent.makeRestartActivityTask(intent.component))
        startActivity(Intent.makeRestartActivityTask(intent.component))
        Runtime.getRuntime().exit(0)
    }

    private fun loadEmojiSearchIndexIfNeeded() {
        Executors.newSingleThreadExecutor().execute {
            val emojiSearchDb : EmojiSearchDatabase=databaseComponent.emojiSearchDatabase()
            if (emojiSearchDb.query("face", 1).isEmpty()) {
                try {
                    assets.open("emoji/emoji_search_index.json").use { inputStream ->
                        val searchIndex : List<EmojiSearchData> =
                            java.util.Arrays.asList(
                                *JsonUtil.fromJson<Array<EmojiSearchData>>(
                                    inputStream,
                                    Array<EmojiSearchData>::class.java
                                )
                            )
                        emojiSearchDb.setSearchIndex(searchIndex)
                    }
                } catch (e : IOException) {
                    Log.e("Beldex", "Failed to load emoji search index")
                }
            }
        }
    }


    // Method to wake up the screen and dismiss the keyguard
    fun wakeUpDeviceAndDismissKeyguardIfRequired() {
        // Get the KeyguardManager and PowerManager
        val keyguardManager=getSystemService(KEYGUARD_SERVICE) as KeyguardManager?
        val powerManager=getSystemService(POWER_SERVICE) as PowerManager?
        // Check if the phone is locked & if the screen is awake
        val isPhoneLocked=keyguardManager!!.isKeyguardLocked
        val isScreenAwake=powerManager!!.isInteractive
        if (!isScreenAwake) {
            val wakeLock=powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK
                        or PowerManager.ACQUIRE_CAUSES_WAKEUP
                        or PowerManager.ON_AFTER_RELEASE,
                WAKELOCK_TAG
            )
            // Acquire the wake lock to wake up the device
            wakeLock.acquire(3000)
        }
        // Dismiss the keyguard.
        // Note: This will not bypass any app-level (BChat) lock; only the device-level keyguard.
        // TODO: When moving to a minimum Android API of 27, replace these deprecated calls with new APIs.
        if (isPhoneLocked) {
            val keyguardLock=keyguardManager.newKeyguardLock(KEYGUARD_LOCK_TAG)
            keyguardLock.disableKeyguard()
        }
    }



    companion object {
        @JvmStatic
        fun getInstance(context: Context): ApplicationContext {
            return context.applicationContext as ApplicationContext
        }

        @JvmStatic
        fun getNetworkType() : NetworkType {
            return when (BuildConfig.NETWORK_TYPE) {
                "mainnet" -> NetworkType.NetworkType_Mainnet
                "stagenet" -> NetworkType.NetworkType_Stagenet
                "devnet" -> NetworkType.NetworkType_Testnet
                else -> throw IllegalStateException("unknown net flavor " + BuildConfig.FLAVOR)
            }
        }
    }
    // endregion

}