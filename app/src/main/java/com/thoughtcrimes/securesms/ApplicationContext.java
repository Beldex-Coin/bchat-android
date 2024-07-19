/* Copyright (C) 2013 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.thoughtcrimes.securesms;

import static nl.komponents.kovenant.android.KovenantAndroid.startKovenant;
import static nl.komponents.kovenant.android.KovenantAndroid.stopKovenant;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.beldex.libbchat.avatars.AvatarHelper;
import com.beldex.libbchat.database.MessageDataProvider;
import com.beldex.libbchat.messaging.MessagingModuleConfiguration;
import com.beldex.libbchat.messaging.sending_receiving.notifications.MessageNotifier;
import com.beldex.libbchat.messaging.sending_receiving.pollers.ClosedGroupPollerV2;
import com.beldex.libbchat.messaging.sending_receiving.pollers.Poller;
import com.beldex.libbchat.messaging.utilities.WindowDebouncer;
import com.beldex.libbchat.mnode.MnodeModule;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.ProfilePictureUtilities;
import com.beldex.libbchat.utilities.SSKEnvironment;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper;
import com.beldex.libbchat.utilities.dynamiclanguage.LocaleParser;
import com.beldex.libsignal.utilities.HTTP;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.utilities.ThreadUtils;
import com.thoughtcrimes.securesms.components.TypingStatusSender;
import com.thoughtcrimes.securesms.crypto.KeyPairUtilities;
import com.thoughtcrimes.securesms.database.BeldexAPIDatabase;
import com.thoughtcrimes.securesms.database.JobDatabase;
import com.thoughtcrimes.securesms.database.Storage;
import com.thoughtcrimes.securesms.database.helpers.SQLCipherOpenHelper;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;
import com.thoughtcrimes.securesms.dependencies.DatabaseModule;
import com.thoughtcrimes.securesms.groups.OpenGroupManager;
import com.thoughtcrimes.securesms.home.HomeActivity;
import com.thoughtcrimes.securesms.jobmanager.JobManager;
import com.thoughtcrimes.securesms.jobmanager.impl.JsonDataSerializer;
import com.thoughtcrimes.securesms.jobmanager.impl.NetworkConstraint;
import com.thoughtcrimes.securesms.jobs.FastJobStorage;
import com.thoughtcrimes.securesms.jobs.JobManagerFactories;
import com.thoughtcrimes.securesms.logging.AndroidLogger;
import com.thoughtcrimes.securesms.logging.PersistentLogger;
import com.thoughtcrimes.securesms.logging.UncaughtExceptionLogger;
import com.thoughtcrimes.securesms.model.NetworkType;
import com.thoughtcrimes.securesms.notifications.BackgroundPollWorker;
import com.thoughtcrimes.securesms.notifications.BeldexPushNotificationManager;
import com.thoughtcrimes.securesms.notifications.DefaultMessageNotifier;
import com.thoughtcrimes.securesms.notifications.FcmUtils;
import com.thoughtcrimes.securesms.notifications.NotificationChannels;
import com.thoughtcrimes.securesms.notifications.OptimizedMessageNotifier;
import com.thoughtcrimes.securesms.providers.BlobProvider;
import com.thoughtcrimes.securesms.service.ExpiringMessageManager;
import com.thoughtcrimes.securesms.service.KeyCachingService;
import com.thoughtcrimes.securesms.service.UpdateApkRefreshListener;
import com.thoughtcrimes.securesms.sskenvironment.ProfileManager;
import com.thoughtcrimes.securesms.sskenvironment.ReadReceiptManager;
import com.thoughtcrimes.securesms.sskenvironment.TypingStatusRepository;
import com.thoughtcrimes.securesms.util.Broadcaster;
import com.thoughtcrimes.securesms.util.FirebaseRemoteConfigUtil;
import com.thoughtcrimes.securesms.util.UiModeUtilities;
import com.thoughtcrimes.securesms.util.dynamiclanguage.LocaleParseHelper;
import com.thoughtcrimes.securesms.webrtc.CallMessageProcessor;

import org.conscrypt.Conscrypt;
import org.signal.aesgcmprovider.AesGcmProvider;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnectionFactory.InitializationOptions;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Security;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

import javax.inject.Inject;

import dagger.hilt.EntryPoints;
import dagger.hilt.android.HiltAndroidApp;
import io.beldex.bchat.BuildConfig;
import kotlin.Unit;
import kotlinx.coroutines.Job;

/**
 * Will be called once when the TextSecure process is created.
 * <p>
 * We're using this as an insertion point to patch up the Android PRNG disaster,
 * to initialize the job manager, and to check for GCM registration freshness.
 *
 * @author Moxie Marlinspike
 */
@HiltAndroidApp
public class ApplicationContext extends Application implements DefaultLifecycleObserver {

    public static final String PREFERENCES_NAME = "SecureSMS-Preferences";

    private static final String TAG = ApplicationContext.class.getSimpleName();

    private ExpiringMessageManager expiringMessageManager;
    private TypingStatusRepository typingStatusRepository;
    private TypingStatusSender typingStatusSender;
    private JobManager jobManager;
    private ReadReceiptManager readReceiptManager;
    private ProfileManager profileManager;
    public MessageNotifier messageNotifier = null;
    public Poller poller = null;
    public Broadcaster broadcaster = null;
    private Job firebaseInstanceIdJob;
    private WindowDebouncer conversationListDebouncer;
    private HandlerThread conversationListHandlerThread;
    private Handler conversationListHandler;
    private PersistentLogger persistentLogger;

    @Inject BeldexAPIDatabase beldexAPIDatabase;
    @Inject Storage storage;
    @Inject MessageDataProvider messageDataProvider;
    @Inject JobDatabase jobDatabase;
    //New Line
    @Inject TextSecurePreferences textSecurePreferences;
    CallMessageProcessor callMessageProcessor;
    MessagingModuleConfiguration messagingModuleConfiguration;

    @Inject
    FirebaseRemoteConfigUtil remoteConfigUtil;

    private volatile boolean isAppVisible;

    @Override
    public Object getSystemService(String name) {
        if (MessagingModuleConfiguration.MESSAGING_MODULE_SERVICE.equals(name)) {
            return messagingModuleConfiguration;
        }
        return super.getSystemService(name);
    }

    public static ApplicationContext getInstance(Context context) {
        return (ApplicationContext) context.getApplicationContext();
    }

    public DatabaseComponent getDatabaseComponent() {
        return EntryPoints.get(getApplicationContext(), DatabaseComponent.class);
    }

    public Handler getConversationListNotificationHandler() {
        if (this.conversationListHandlerThread == null) {
            conversationListHandlerThread = new HandlerThread("ConversationListHandler");
            conversationListHandlerThread.start();
        }
        if (this.conversationListHandler == null) {
            conversationListHandler = new Handler(conversationListHandlerThread.getLooper());
        }
        return conversationListHandler;
    }

    public WindowDebouncer getConversationListDebouncer() {
        if (conversationListDebouncer == null) {
            conversationListDebouncer = new WindowDebouncer(1000, new Timer());
        }
        return conversationListDebouncer;
    }

    public PersistentLogger getPersistentLogger() {
        return this.persistentLogger;
    }

    @Override
    public void onCreate() {
        DatabaseModule.init(this);
        MessagingModuleConfiguration.configure(this);
        super.onCreate();
        messagingModuleConfiguration = new MessagingModuleConfiguration(this,
                storage,
                messageDataProvider,
                ()-> KeyPairUtilities.INSTANCE.getUserED25519KeyPair(this));
        callMessageProcessor = new CallMessageProcessor(this, textSecurePreferences, ProcessLifecycleOwner.get().getLifecycle(), storage);
        Log.i(TAG, "onCreate()");
        startKovenant();
        initializeSecurityProvider();
        initializeLogging();
        initializeCrashHandling();
        NotificationChannels.create(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        AppContext.INSTANCE.configureKovenant();
        messageNotifier = new OptimizedMessageNotifier(new DefaultMessageNotifier());
        broadcaster = new Broadcaster(this);
        BeldexAPIDatabase apiDB = getDatabaseComponent().beldexAPIDatabase();
        MnodeModule.Companion.configure(apiDB, broadcaster);
        String userPublicKey = TextSecurePreferences.getLocalNumber(this);
        if (userPublicKey != null) {
            registerForFCMIfNeeded(false);
        }
        UiModeUtilities.setupUiModeToUserSelected(this);
        initializeExpiringMessageManager();
        initializeTypingStatusRepository();
        initializeTypingStatusSender();
        initializeReadReceiptManager();
        initializeProfileManager();
        initializePeriodicTasks();
        SSKEnvironment.Companion.configure(getTypingStatusRepository(), getReadReceiptManager(), getProfileManager(), messageNotifier, getExpiringMessageManager());
        initializeJobManager();
        initializeWebRtc();
        initializeBlobProvider();
        resubmitProfilePictureIfNeeded();

        NetworkConstraint networkConstraint = new NetworkConstraint.Factory(this).create();
        HTTP.INSTANCE.setConnectedToNetwork(networkConstraint::isMet);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        TextSecurePreferences.setRefreshDynamicNodesStatus(this, true);
        isAppVisible = true;
        Log.i(TAG, "App is now visible.");
        KeyCachingService.onAppForegrounded(this);
        ThreadUtils.queue(()->{
            if (poller != null) {
                poller.setCaughtUp(false);
            }

            startPollingIfNeeded();

            OpenGroupManager.INSTANCE.startPolling();
        });
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isAppVisible = false;
        Log.i(TAG, "App is no longer visible.");
        KeyCachingService.onAppBackgrounded(this);
        messageNotifier.setVisibleThread(-1);
        if (poller != null) {
            poller.stopIfNeeded();
        }
        ClosedGroupPollerV2.getShared().stop();
    }

    @Override
    public void onTerminate() {
        stopKovenant(); // Beldex
        OpenGroupManager.INSTANCE.stopPolling();
        super.onTerminate();
    }

    public void initializeLocaleParser() {
        LocaleParser.Companion.configure(new LocaleParseHelper());
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public ExpiringMessageManager getExpiringMessageManager() {
        return expiringMessageManager;
    }

    public TypingStatusRepository getTypingStatusRepository() {
        return typingStatusRepository;
    }

    public TypingStatusSender getTypingStatusSender() {
        return typingStatusSender;
    }

    public ReadReceiptManager getReadReceiptManager() {
        return readReceiptManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public boolean isAppVisible() {
        return isAppVisible;
    }

    // Beldex

    private void initializeSecurityProvider() {
        try {
            Class.forName("org.signal.aesgcmprovider.AesGcmCipher");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Failed to find AesGcmCipher class");
            throw new ProviderInitializationException();
        }catch(UnsatisfiedLinkError e){
            Log.w(TAG, e);
        }

        int aesPosition = Security.insertProviderAt(new AesGcmProvider(), 1);
        Log.i(TAG, "Installed AesGcmProvider: " + aesPosition);

        if (aesPosition < 0) {
            Log.e(TAG, "Failed to install AesGcmProvider()");
            throw new ProviderInitializationException();
        }

        int conscryptPosition = Security.insertProviderAt(Conscrypt.newProvider(), 2);
        Log.i(TAG, "Installed Conscrypt provider: " + conscryptPosition);

        if (conscryptPosition < 0) {
            Log.w(TAG, "Did not install Conscrypt provider. May already be present.");
        }
    }

    private void initializeLogging() {
        if (persistentLogger == null) {
            persistentLogger = new PersistentLogger(this);
        }
        Log.initialize(new AndroidLogger(), persistentLogger);
    }

    private void initializeCrashHandling() {
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionLogger(originalHandler));
    }

    private void initializeJobManager() {
        this.jobManager = new JobManager(this, new JobManager.Configuration.Builder()
                .setDataSerializer(new JsonDataSerializer())
                .setJobFactories(JobManagerFactories.getJobFactories(this))
                .setConstraintFactories(JobManagerFactories.getConstraintFactories(this))
                .setConstraintObservers(JobManagerFactories.getConstraintObservers(this))
                .setJobStorage(new FastJobStorage(jobDatabase))
                .build());
    }

    private void initializeExpiringMessageManager() {
        this.expiringMessageManager = new ExpiringMessageManager(this);
    }

    private void initializeTypingStatusRepository() {
        this.typingStatusRepository = new TypingStatusRepository();
    }

    private void initializeReadReceiptManager() {
        this.readReceiptManager = new ReadReceiptManager();
    }

    private void initializeProfileManager() {
        this.profileManager = new ProfileManager();
    }

    private void initializeTypingStatusSender() {
        this.typingStatusSender = new TypingStatusSender(this);
    }

    private void initializePeriodicTasks() {
        BackgroundPollWorker.schedulePeriodic(this);

        if (BuildConfig.PLAY_STORE_DISABLED) {
            UpdateApkRefreshListener.schedule(this);
        }
    }

    private void initializeWebRtc() {
        try {
            Set<String> HARDWARE_AEC_BLACKLIST = new HashSet<String>() {{
                add("Pixel");
                add("Pixel XL");
                add("Moto G5");
                add("Moto G (5S) Plus");
                add("Moto G4");
                add("TA-1053");
                add("Mi A1");
                add("E5823"); // Sony z5 compact
                add("Redmi Note 5");
                add("FP2"); // Fairphone FP2
                add("MI 5");
            }};

            Set<String> OPEN_SL_ES_WHITELIST = new HashSet<String>() {{
                add("Pixel");
                add("Pixel XL");
            }};

            if (HARDWARE_AEC_BLACKLIST.contains(Build.MODEL)) {
                WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
            }

            if (!OPEN_SL_ES_WHITELIST.contains(Build.MODEL)) {
                WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true);
            }

            PeerConnectionFactory.initialize(InitializationOptions.builder(this).createInitializationOptions());
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, e);
        }
    }

    private void initializeBlobProvider() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            BlobProvider.getInstance().onBchatStart(this);
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        initializeLocaleParser();
        super.attachBaseContext(DynamicLanguageContextWrapper.updateContext(base, TextSecurePreferences.getLanguage(base)));
    }

    private static class ProviderInitializationException extends RuntimeException { }

    public void registerForFCMIfNeeded(final Boolean force) {
        if (firebaseInstanceIdJob != null && firebaseInstanceIdJob.isActive() && !force) return;
        if (force && firebaseInstanceIdJob != null) {
            firebaseInstanceIdJob.cancel(null);
        }
        firebaseInstanceIdJob = FcmUtils.getFcmInstanceId(task->{
            if (!task.isSuccessful()) {
                Log.w("Beldex", "FirebaseMessaging.getInstance().token failed." + task.getException());
                return Unit.INSTANCE;
            }
            String token = task.getResult();
            String userPublicKey = TextSecurePreferences.getLocalNumber(this);
            if (userPublicKey == null) return Unit.INSTANCE;
            if (TextSecurePreferences.isUsingFCM(this)) {
                BeldexPushNotificationManager.register(token, userPublicKey, this, force);
            } else {
                BeldexPushNotificationManager.unregister(token, this);
            }
            return Unit.INSTANCE;
        });
    }

    private void setUpPollingIfNeeded() {
        String userPublicKey = TextSecurePreferences.getLocalNumber(this);
        if (userPublicKey == null) return;
        if (poller != null) {
            poller.setUserPublicKey(userPublicKey);
            return;
        }
        poller = new Poller();
    }

    public void startPollingIfNeeded() {
        setUpPollingIfNeeded();
        if (poller != null) {
            poller.startIfNeeded();
        }
        ClosedGroupPollerV2.getShared().start();
    }

    private void resubmitProfilePictureIfNeeded() {
        // Files expire on the file server after a while, so we simply re-upload the user's profile picture
        // at a certain interval to ensure it's always available.
        String userPublicKey = TextSecurePreferences.getLocalNumber(this);
        if (userPublicKey == null) return;
        long now = new Date().getTime();
        long lastProfilePictureUpload = TextSecurePreferences.getLastProfilePictureUpload(this);
        if (now - lastProfilePictureUpload <= 14 * 24 * 60 * 60 * 1000) return;
        ThreadUtils.queue(() -> {
            // Don't generate a new profile key here; we do that when the user changes their profile picture
            String encodedProfileKey = TextSecurePreferences.getProfileKey(ApplicationContext.this);
            try {
                // Read the file into a byte array
                InputStream inputStream = AvatarHelper.getInputStreamFor(ApplicationContext.this, Address.fromSerialized(userPublicKey));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int count;
                byte[] buffer = new byte[1024];
                while ((count = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    baos.write(buffer, 0, count);
                }
                baos.flush();
                byte[] profilePicture = baos.toByteArray();
                // Re-upload it
                ProfilePictureUtilities.INSTANCE.upload(profilePicture, encodedProfileKey, ApplicationContext.this).success(unit -> {
                    // Update the last profile picture upload date
                    TextSecurePreferences.setLastProfilePictureUpload(ApplicationContext.this, new Date().getTime());
                    return Unit.INSTANCE;
                });
            } catch (Exception exception) {
                // Do nothing
            }
        });
    }

    public void clearAllData(boolean isMigratingToV2KeyPair) {
        String token = TextSecurePreferences.getFCMToken(this);
        if (token != null && !token.isEmpty()) {
            BeldexPushNotificationManager.unregister(token, this);
        }
        if (firebaseInstanceIdJob != null && firebaseInstanceIdJob.isActive()) {
            firebaseInstanceIdJob.cancel(null);
        }
        String displayName = TextSecurePreferences.getProfileName(this);
        boolean isUsingFCM = TextSecurePreferences.isUsingFCM(this);
        TextSecurePreferences.clearAll(this);
        if (isMigratingToV2KeyPair) {
            TextSecurePreferences.setIsUsingFCM(this, isUsingFCM);
            TextSecurePreferences.setProfileName(this, displayName);
        }
        getSharedPreferences(PREFERENCES_NAME, 0).edit().clear().commit();
        if (!deleteDatabase(SQLCipherOpenHelper.DATABASE_NAME)) {
            Log.d("Beldex", "Failed to delete database.");
        }
        Util.runOnMain(() -> new Handler().postDelayed(ApplicationContext.this::restartApplication, 200));
    }

    public void restartApplication() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(Intent.makeRestartActivityTask(intent.getComponent()));
        Runtime.getRuntime().exit(0);
    }

    // endregion

    static public NetworkType getNetworkType() {
        switch (BuildConfig.NETWORK_TYPE) {
            case "mainnet":
                return NetworkType.NetworkType_Mainnet;
            case "stagenet":
                return NetworkType.NetworkType_Stagenet;
            case "devnet": // flavors cannot start with "test"
                return NetworkType.NetworkType_Testnet;
            default:
                throw new IllegalStateException("unknown net flavor " + BuildConfig.FLAVOR);
        }
    }
}
