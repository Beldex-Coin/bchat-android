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
package io.beldex.bchat;

import static nl.komponents.kovenant.android.KovenantAndroid.startKovenant;
import static nl.komponents.kovenant.android.KovenantAndroid.stopKovenant;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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
import com.beldex.libbchat.utilities.Device;
import com.beldex.libbchat.utilities.ProfilePictureUtilities;
import com.beldex.libbchat.utilities.SSKEnvironment;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper;
import com.beldex.libbchat.utilities.dynamiclanguage.LocaleParser;
import com.beldex.libsignal.utilities.HTTP;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.utilities.ThreadUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import io.beldex.bchat.components.TypingStatusSender;
import io.beldex.bchat.crypto.KeyPairUtilities;
import io.beldex.bchat.database.BeldexAPIDatabase;
import io.beldex.bchat.database.JobDatabase;
import io.beldex.bchat.database.Storage;
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.dependencies.DatabaseModule;
import io.beldex.bchat.groups.OpenGroupManager;
import io.beldex.bchat.home.HomeActivity;
import io.beldex.bchat.jobmanager.JobManager;
import io.beldex.bchat.jobmanager.impl.JsonDataSerializer;
import io.beldex.bchat.jobmanager.impl.NetworkConstraint;
import io.beldex.bchat.jobs.FastJobStorage;
import io.beldex.bchat.jobs.JobManagerFactories;
import io.beldex.bchat.logging.AndroidLogger;
import io.beldex.bchat.logging.PersistentLogger;
import io.beldex.bchat.logging.UncaughtExceptionLogger;
import io.beldex.bchat.model.NetworkType;
import io.beldex.bchat.notifications.BackgroundPollWorker;
import io.beldex.bchat.notifications.DefaultMessageNotifier;
import io.beldex.bchat.notifications.PushRegistry;
import io.beldex.bchat.notifications.NotificationChannels;
import io.beldex.bchat.notifications.OptimizedMessageNotifier;
import io.beldex.bchat.providers.BlobProvider;
import io.beldex.bchat.service.ExpiringMessageManager;
import io.beldex.bchat.service.KeyCachingService;
import io.beldex.bchat.sskenvironment.ProfileManager;
import io.beldex.bchat.sskenvironment.ReadReceiptManager;
import io.beldex.bchat.sskenvironment.TypingStatusRepository;
import io.beldex.bchat.util.Broadcaster;
import io.beldex.bchat.util.FirebaseRemoteConfigUtil;
import io.beldex.bchat.util.UiModeUtilities;
import io.beldex.bchat.util.dynamiclanguage.LocaleParseHelper;
import io.beldex.bchat.webrtc.CallMessageProcessor;

import org.conscrypt.Conscrypt;
import org.signal.aesgcmprovider.AesGcmProvider;
import org.webrtc.PeerConnectionFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Security;
import java.util.Date;
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
    @Inject Device device;
    @Inject PushRegistry pushRegistry;
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
                device,
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
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(BuildConfig.CRASHLYTICS_ENABLED);
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
            // possibly add update apk job
        }
    }

    private void initializeWebRtc() {
        try {
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());
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
        if (firebaseInstanceIdJob != null && firebaseInstanceIdJob.isActive()) {
            firebaseInstanceIdJob.cancel(null);
        }
        String displayName = TextSecurePreferences.getProfileName(this);
        boolean isUsingFCM = TextSecurePreferences.isPushEnabled(this);
        TextSecurePreferences.clearAll(this);
        if (isMigratingToV2KeyPair) {
            TextSecurePreferences.setPushEnabled(this, isUsingFCM);
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
