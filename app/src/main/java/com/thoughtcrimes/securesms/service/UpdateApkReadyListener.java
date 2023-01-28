package com.thoughtcrimes.securesms.service;


import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.thoughtcrimes.securesms.util.FileProviderUtil;
import com.beldex.libsignal.utilities.Log;

import io.beldex.bchat.R;
import com.thoughtcrimes.securesms.notifications.NotificationChannels;
import com.beldex.libbchat.utilities.FileUtils;
import com.beldex.libsignal.utilities.Hex;
import com.beldex.libbchat.utilities.ServiceUtil;
import com.beldex.libbchat.utilities.TextSecurePreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

public class UpdateApkReadyListener extends BroadcastReceiver {

  private static final String TAG = UpdateApkReadyListener.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.i(TAG, "onReceive()");

    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
      long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);

      if (downloadId == TextSecurePreferences.getUpdateApkDownloadId(context)) {
        Uri    uri           = getLocalUriForDownloadId(context, downloadId);
        String encodedDigest = TextSecurePreferences.getUpdateApkDigest(context);

        if (uri == null) {
          Log.w(TAG, "Downloaded local URI is null?");
          return;
        }

        if (isMatchingDigest(context, downloadId, encodedDigest)) {
          displayInstallNotification(context, uri);
        } else {
          Log.w(TAG, "Downloaded APK doesn't match digest...");
        }
      }
    }
  }

  private void displayInstallNotification(Context context, Uri uri) {
    Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setData(uri);

    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

    Notification notification = new NotificationCompat.Builder(context, NotificationChannels.APP_UPDATES)
        .setOngoing(true)
        .setContentTitle(context.getString(R.string.UpdateApkReadyListener_Signal_update))
        .setContentText(context.getString(R.string.UpdateApkReadyListener_a_new_version_of_signal_is_available_tap_to_update))
        .setSmallIcon(R.drawable.ic_notification_)
        .setColor(context.getResources().getColor(R.color.textsecure_primary))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setContentIntent(pendingIntent)
        .build();

    ServiceUtil.getNotificationManager(context).notify(666, notification);
  }

  private @Nullable Uri getLocalUriForDownloadId(Context context, long downloadId) {
    DownloadManager       downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    DownloadManager.Query query           = new DownloadManager.Query();
    query.setFilterById(downloadId);

    Cursor cursor = downloadManager.query(query);

    try {
      if (cursor != null && cursor.moveToFirst()) {
        String localUri  = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));

        if (localUri != null) {
          File   localFile = new File(Uri.parse(localUri).getPath());
          return FileProviderUtil.getUriFor(context, localFile);
        }
      }
    } finally {
      if (cursor != null) cursor.close();
    }

    return null;
  }

  private boolean isMatchingDigest(Context context, long downloadId, String theirEncodedDigest) {
    try {
      if (theirEncodedDigest == null) return false;

      byte[]          theirDigest     = Hex.fromStringCondensed(theirEncodedDigest);
      DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
      FileInputStream fin             = new FileInputStream(downloadManager.openDownloadedFile(downloadId).getFileDescriptor());
      byte[]          ourDigest       = FileUtils.getFileDigest(fin);

      fin.close();

      return MessageDigest.isEqual(ourDigest, theirDigest);
    } catch (IOException e) {
      Log.w(TAG, e);
      return false;
    }
  }
}
