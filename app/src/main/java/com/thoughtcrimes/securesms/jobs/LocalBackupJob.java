package com.thoughtcrimes.securesms.jobs;

import androidx.annotation.NonNull;

import com.beldex.libbchat.messaging.utilities.Data;
import com.beldex.libsignal.utilities.NoExternalStorageException;
import com.thoughtcrimes.securesms.jobmanager.Job;
import com.beldex.libsignal.utilities.Log;
import com.thoughtcrimes.securesms.database.BackupFileRecord;
import com.thoughtcrimes.securesms.notifications.NotificationChannels;
import com.thoughtcrimes.securesms.service.GenericForegroundService;
import com.thoughtcrimes.securesms.util.BackupUtil;

import java.io.IOException;
import java.util.Collections;

import io.beldex.bchat.R;

public class LocalBackupJob extends BaseJob {

  public static final String KEY = "LocalBackupJob";

  private static final String TAG = LocalBackupJob.class.getSimpleName();

  public LocalBackupJob() {
    this(new Job.Parameters.Builder()
                           .setQueue("__LOCAL_BACKUP__")
                           .setMaxInstances(1)
                           .setMaxAttempts(3)
                           .build());
  }

  private LocalBackupJob(@NonNull Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull
  Data serialize() {
    return Data.EMPTY;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws NoExternalStorageException, IOException {
    Log.i(TAG, "Executing backup job...");

    GenericForegroundService.startForegroundTask(context,
                                                 context.getString(R.string.LocalBackupJob_creating_backup),
                                                 NotificationChannels.BACKUPS,
                                                 R.drawable.ic_launcher_foreground);

    // TODO: Maybe create a new backup icon like ic_signal_backup?

    try {
      BackupFileRecord record = BackupUtil.createBackupFile(context);
      BackupUtil.deleteAllBackupFiles(context, Collections.singletonList(record));

    } finally {
      GenericForegroundService.stopForegroundTask(context);
    }
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    return false;
  }

  @Override
  public void onCanceled() {
  }

  public static class Factory implements Job.Factory<LocalBackupJob> {
    @Override
    public @NonNull LocalBackupJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new LocalBackupJob(parameters);
    }
  }
}
