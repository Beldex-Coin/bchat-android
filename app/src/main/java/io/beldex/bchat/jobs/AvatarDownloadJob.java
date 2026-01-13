package io.beldex.bchat.jobs;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import io.beldex.bchat.mms.AttachmentStreamUriLoader;
import com.beldex.libbchat.messaging.utilities.Data;
import com.beldex.libbchat.utilities.DownloadUtilities;
import com.beldex.libbchat.utilities.GroupRecord;
import com.beldex.libsignal.exceptions.InvalidMessageException;
import com.beldex.libsignal.exceptions.NonSuccessfulResponseCodeException;
import com.beldex.libsignal.messages.SignalServiceAttachmentPointer;
import com.beldex.libsignal.streams.AttachmentCipherInputStream;
import com.beldex.libsignal.utilities.Hex;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.utilities.guava.Optional;
import io.beldex.bchat.database.GroupDatabase;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.jobmanager.Job;
import io.beldex.bchat.jobmanager.impl.NetworkConstraint;
import io.beldex.bchat.util.BitmapDecodingException;
import io.beldex.bchat.util.BitmapUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AvatarDownloadJob extends BaseJob {

    public static final String KEY = "AvatarDownloadJob";

    private static final String TAG = AvatarDownloadJob.class.getSimpleName();

    private static final int MAX_AVATAR_SIZE = 20 * 1024 * 1024;

    private static final String KEY_GROUP_ID = "group_id";

    private final String groupId;

    public AvatarDownloadJob(@NonNull String groupId) {
        this(new Job.Parameters.Builder()
                        .addConstraint(NetworkConstraint.KEY)
                        .setMaxAttempts(10)
                        .build(),
                groupId);
    }

    private AvatarDownloadJob(@NonNull Job.Parameters parameters, @NonNull String groupId) {
        super(parameters);
        this.groupId = groupId;
    }

    @Override
    public @NonNull Data serialize() {
        return new Data.Builder().putString(KEY_GROUP_ID, groupId).build();
    }

    @Override
    public @NonNull String getFactoryKey() {
        return KEY;
    }

    @Override
    public void onRun() throws IOException {
        GroupDatabase         database   = DatabaseComponent.get(context).groupDatabase();
        Optional<GroupRecord> record     = database.getGroup(groupId);
        File                  attachment = null;

        try {
            if (record.isPresent()) {
                long             avatarId    = record.get().getAvatarId();
                String           contentType = record.get().getAvatarContentType();
                byte[]           key         = record.get().getAvatarKey();
                String           relay       = record.get().getRelay();
                Optional<byte[]> digest      = Optional.fromNullable(record.get().getAvatarDigest());
                Optional<String> fileName    = Optional.absent();
                String url = record.get().getUrl();

                if (avatarId == -1 || key == null || url.isEmpty()) {
                    return;
                }

                if (digest.isPresent()) {
                    Log.i(TAG, "Downloading group avatar with digest: " + Hex.toString(digest.get()));
                }

                attachment = File.createTempFile("avatar", "tmp", context.getCacheDir());
                attachment.deleteOnExit();

                SignalServiceAttachmentPointer pointer = new SignalServiceAttachmentPointer(avatarId, contentType, key, Optional.of(0), Optional.absent(), 0, 0, digest, fileName, false, Optional.absent(), url);

                if (pointer.getUrl().isEmpty()) throw new InvalidMessageException("Missing attachment URL.");
                DownloadUtilities.downloadFile(attachment, pointer.getUrl());

                // Assume we're retrieving an attachment for an social group server if the digest is not set
                InputStream inputStream;
                if (!pointer.getDigest().isPresent()) {
                    inputStream = new FileInputStream(attachment);
                } else {
                    inputStream = AttachmentCipherInputStream.createForAttachment(attachment, pointer.getSize().or(0), pointer.getKey(), pointer.getDigest().get());
                }

                Bitmap avatar = BitmapUtil.createScaledBitmap(context, new AttachmentStreamUriLoader.AttachmentModel(attachment, key, 0, digest), 500, 500);

                database.updateProfilePicture(groupId, avatar);
                inputStream.close();
            }
        } catch (BitmapDecodingException | NonSuccessfulResponseCodeException | InvalidMessageException e) {
            Log.w(TAG, e);
        } finally {
            if (attachment != null)
                attachment.delete();
        }
    }

    @Override
    public void onCanceled() {}

    @Override
    public boolean onShouldRetry(@NonNull Exception exception) {
        return exception instanceof IOException;
    }

    public static final class Factory implements Job.Factory<AvatarDownloadJob> {
        @Override
        public @NonNull AvatarDownloadJob create(@NonNull Parameters parameters, @NonNull Data data) {
            return new AvatarDownloadJob(parameters, data.getString(KEY_GROUP_ID));
        }
    }
}
