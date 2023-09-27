package com.thoughtcrimes.securesms.jobs;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.beldex.libbchat.avatars.AvatarHelper;
import com.beldex.libbchat.messaging.utilities.Data;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.DownloadUtilities;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libsignal.exceptions.PushNetworkException;
import com.beldex.libsignal.streams.ProfileCipherInputStream;
import com.beldex.libsignal.utilities.Log;
import com.thoughtcrimes.securesms.database.RecipientDatabase;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;
import com.thoughtcrimes.securesms.jobmanager.Job;
import com.thoughtcrimes.securesms.jobmanager.impl.NetworkConstraint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

public class RetrieveProfileAvatarJob extends BaseJob {

    public static final String KEY = "RetrieveProfileAvatarJob";

    private static final String TAG = RetrieveProfileAvatarJob.class.getSimpleName();

    private static final int MAX_PROFILE_SIZE_BYTES = 10 * 1024 * 1024;

    private static final String KEY_PROFILE_AVATAR = "profile_avatar";
    private static final String KEY_ADDRESS        = "address";


    private String    profileAvatar;
    private Recipient recipient;

    public RetrieveProfileAvatarJob(Recipient recipient, String profileAvatar) {
        this(new Job.Parameters.Builder()
                        .setQueue("RetrieveProfileAvatarJob" + recipient.getAddress().serialize())
                        .addConstraint(NetworkConstraint.KEY)
                        .setLifespan(TimeUnit.HOURS.toMillis(1))
                        .setMaxAttempts(2)
                        .setMaxInstances(1)
                        .build(),
                recipient,
                profileAvatar);
    }

    private RetrieveProfileAvatarJob(@NonNull Job.Parameters parameters, @NonNull Recipient recipient, String profileAvatar) {
        super(parameters);
        this.recipient     = recipient;
        this.profileAvatar = profileAvatar;
    }

    @Override
    public @NonNull
    Data serialize() {
        return new Data.Builder()
                .putString(KEY_PROFILE_AVATAR, profileAvatar)
                .putString(KEY_ADDRESS, recipient.getAddress().serialize())
                .build();
    }

    @Override
    public @NonNull String getFactoryKey() {
        return KEY;
    }

    @Override
    public void onRun() throws IOException {
        RecipientDatabase database   = DatabaseComponent.get(context).recipientDatabase();
        byte[]            profileKey = recipient.resolve().getProfileKey();

        if (profileKey == null || (profileKey.length != 32 && profileKey.length != 16)) {
            Log.w(TAG, "Recipient profile key is gone!");
            return;
        }

        if (AvatarHelper.avatarFileExists(context, recipient.resolve().getAddress()) && Util.equals(profileAvatar, recipient.resolve().getProfileAvatar())) {
            Log.w(TAG, "Already retrieved profile avatar: " + profileAvatar);
            return;
        }

        if (TextUtils.isEmpty(profileAvatar)) {
            Log.w(TAG, "Removing profile avatar for: " + recipient.getAddress().serialize());
            AvatarHelper.delete(context, recipient.getAddress());
            database.setProfileAvatar(recipient, profileAvatar);
            return;
        }

        File downloadDestination = File.createTempFile("avatar", ".jpg", context.getCacheDir());

        try {
            DownloadUtilities.downloadFile(downloadDestination, profileAvatar);
            InputStream avatarStream       = new ProfileCipherInputStream(new FileInputStream(downloadDestination), profileKey);
            File        decryptDestination = File.createTempFile("avatar", ".jpg", context.getCacheDir());

            Util.copy(avatarStream, new FileOutputStream(decryptDestination));
            decryptDestination.renameTo(AvatarHelper.getAvatarFile(context, recipient.getAddress()));
        } finally {
            if (downloadDestination != null) downloadDestination.delete();
        }

        if (recipient.isLocalNumber()) {
            TextSecurePreferences.setProfileAvatarId(context, new SecureRandom().nextInt());
        }
        database.setProfileAvatar(recipient, profileAvatar);
    }

    @Override
    public boolean onShouldRetry(@NonNull Exception e) {
        if (e instanceof PushNetworkException) return true;
        return false;
    }

    @Override
    public void onCanceled() {
    }

    public static final class Factory implements Job.Factory<RetrieveProfileAvatarJob> {

        private final Application application;

        public Factory(Application application) {
            this.application = application;
        }

        @Override
        public @NonNull RetrieveProfileAvatarJob create(@NonNull Parameters parameters, @NonNull Data data) {
            return new RetrieveProfileAvatarJob(parameters,
                    Recipient.from(application, Address.fromSerialized(data.getString(KEY_ADDRESS)), true),
                    data.getString(KEY_PROFILE_AVATAR));
        }
    }
}
