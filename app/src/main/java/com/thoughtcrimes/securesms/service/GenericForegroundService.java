package com.thoughtcrimes.securesms.service;


import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.thoughtcrimes.securesms.home.HomeActivity;
import com.beldex.libsignal.utilities.Log;
import com.thoughtcrimes.securesms.notifications.NotificationChannels;
import com.beldex.libsignal.utilities.guava.Preconditions;

import io.beldex.bchat.R;

public class GenericForegroundService extends Service {

  private static final String TAG = GenericForegroundService.class.getSimpleName();

  private static final int    NOTIFICATION_ID  = 827353982;
  private static final String EXTRA_TITLE      = "extra_title";
  private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
  private static final String EXTRA_ICON_RES   = "extra_icon_res";

  private static final String ACTION_START = "start";
  private static final String ACTION_STOP  = "stop";

  private int    foregroundCount;
  private String activeTitle;
  private String activeChannelId;
  private int    activeIconRes;

  @Override
  public void onCreate() {

  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    synchronized (GenericForegroundService.class) {
      if      (intent != null && ACTION_START.equals(intent.getAction())) handleStart(intent);
      else if (intent != null && ACTION_STOP.equals(intent.getAction()))  handleStop();
      else                                                                throw new IllegalStateException("Action needs to be START or STOP.");

      return START_NOT_STICKY;
    }
  }


  private void handleStart(@NonNull Intent intent) {
    String title     = Preconditions.checkNotNull(intent.getStringExtra(EXTRA_TITLE));
    String channelId = Preconditions.checkNotNull(intent.getStringExtra(EXTRA_CHANNEL_ID));
    int    iconRes   = intent.getIntExtra(EXTRA_ICON_RES, R.drawable.ic_bchat_logo);

    Log.i(TAG, "handleStart() Title: " + title + "  ChannelId: " + channelId);

    foregroundCount++;

    if (foregroundCount == 1) {
      Log.d(TAG, "First request. Title: " + title + "  ChannelId: " + channelId);
      activeTitle     = title;
      activeChannelId = channelId;
      activeIconRes   = iconRes;
    }

    postObligatoryForegroundNotification(activeTitle, activeChannelId, activeIconRes);
  }

  private void handleStop() {
    Log.i(TAG, "handleStop()");

    postObligatoryForegroundNotification(activeTitle, activeChannelId, activeIconRes);

    foregroundCount--;

    if (foregroundCount == 0) {
      Log.d(TAG, "Last request. Ending foreground service.");
      stopForeground(true);
      stopSelf();
    }
  }

  private void postObligatoryForegroundNotification(String title, String channelId, @DrawableRes int iconRes) {
    startForeground(NOTIFICATION_ID, new NotificationCompat.Builder(this, channelId)
                                                           .setSmallIcon(iconRes)
                                                           .setContentTitle(title)
                                                           .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, HomeActivity.class), PendingIntent.FLAG_IMMUTABLE))
                                                           .build());
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  public static void startForegroundTask(@NonNull Context context, @NonNull String task) {
    startForegroundTask(context, task, NotificationChannels.OTHER);
  }

  public static void startForegroundTask(@NonNull Context context, @NonNull String task, @NonNull String channelId) {
    startForegroundTask(context, task, channelId, R.drawable.ic_bchat_logo);
  }

  public static void startForegroundTask(@NonNull Context context, @NonNull String task, @NonNull String channelId, @DrawableRes int iconRes) {
    Intent intent = new Intent(context, GenericForegroundService.class);
    intent.setAction(ACTION_START);
    intent.putExtra(EXTRA_TITLE, task);
    intent.putExtra(EXTRA_CHANNEL_ID, channelId);
    intent.putExtra(EXTRA_ICON_RES, iconRes);

    ContextCompat.startForegroundService(context, intent);
  }

  public static void stopForegroundTask(@NonNull Context context) {
    Intent intent = new Intent(context, GenericForegroundService.class);
    intent.setAction(ACTION_STOP);

    ContextCompat.startForegroundService(context, intent);
  }
}
