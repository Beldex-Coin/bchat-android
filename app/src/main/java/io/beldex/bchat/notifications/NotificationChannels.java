package io.beldex.bchat.notifications;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import io.beldex.bchat.database.RecipientDatabase;
import io.beldex.bchat.dependencies.DatabaseComponent;

import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.ServiceUtil;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libbchat.utilities.recipients.Recipient.VibrateState;
import com.beldex.libsignal.utilities.Log;

import io.beldex.bchat.dependencies.DatabaseComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.beldex.bchat.BuildConfig;
import io.beldex.bchat.R;

public class NotificationChannels {

  private static final String TAG = NotificationChannels.class.getSimpleName();

  private static final int VERSION_MESSAGES_CATEGORY = 2;
  private static final int VERSION_BCHAT_CALLS =3;

  private static final int VERSION = 3;

  private static final String CATEGORY_MESSAGES = "messages";
  private static final String CONTACT_PREFIX    = "contact_";
  private static final String MESSAGES_PREFIX   = "messages_";

  public static final String CALLS         = "calls_v3";
  public static final String FAILURES      = "failures";
  public static final String APP_UPDATES   = "app_updates";
  public static final String BACKUPS       = "backups_v2";
  public static final String LOCKED_STATUS = "locked_status_v2";
  public static final String OTHER         = "other_v2";

  /**
   * Ensures all of the notification channels are created. No harm in repeat calls. Call is safely
   * ignored for API < 26.
   */
  public static synchronized void create(@NonNull Context context) {

    NotificationManager notificationManager = ServiceUtil.getNotificationManager(context);

    int oldVersion = TextSecurePreferences.getNotificationChannelVersion(context);
    if (oldVersion != VERSION) {
      onUpgrade(notificationManager, oldVersion, VERSION);
      TextSecurePreferences.setNotificationChannelVersion(context, VERSION);
    }

    onCreate(context, notificationManager);

    AsyncTask.SERIAL_EXECUTOR.execute(() -> {
      ensureCustomChannelConsistency(context);
    });
  }

  /**
   * @return The channel ID for the default messages channel.
   */
  public static synchronized @NonNull String getMessagesChannel(@NonNull Context context) {
    return getMessagesChannelId(TextSecurePreferences.getNotificationMessagesChannelVersion(context));
  }

  /**
   * @return A name suitable to be displayed as the notification channel title.
   */
  public static @NonNull String getChannelDisplayNameFor(@NonNull Context context, @Nullable String systemName, @Nullable String profileName, @NonNull Address address) {
    if (!TextUtils.isEmpty(systemName)) {
      return systemName;
    } else if (!TextUtils.isEmpty(profileName)) {
      return profileName;
    } else if (!TextUtils.isEmpty(address.serialize())) {
      return address.serialize();
    } else {
      return context.getString(R.string.NotificationChannel_missing_display_name);
    }
  }

  /**
   * Updates the LED color for message notifications and all contact-specific message notification
   * channels. Performs database operations and should therefore be invoked on a background thread.
   */
  @WorkerThread
  public static synchronized void updateMessagesLedColor(@NonNull Context context, @NonNull String color) {
    Log.i(TAG, "Updating LED color.");

    NotificationManager notificationManager = ServiceUtil.getNotificationManager(context);

    updateMessageChannel(context, channel -> setLedPreference(channel, color));
    updateAllRecipientChannelLedColors(context, notificationManager, color);

    ensureCustomChannelConsistency(context);
  }

  /**
   * @return The message ringtone set for the default message channel.
   */
  public static synchronized @NonNull Uri getMessageRingtone(@NonNull Context context) {
    Uri sound = ServiceUtil.getNotificationManager(context).getNotificationChannel(getMessagesChannel(context)).getSound();
    return sound == null ? Uri.EMPTY : sound;
  }

  public static synchronized @Nullable Uri getMessageRingtone(@NonNull Context context, @NonNull Recipient recipient) {
    NotificationManager notificationManager = ServiceUtil.getNotificationManager(context);
    NotificationChannel channel             = notificationManager.getNotificationChannel(recipient.getNotificationChannel());

    if (!channelExists(channel)) {
      Log.w(TAG, "Recipient had no channel. Returning null.");
      return null;
    }

    return channel.getSound();
  }

  /**
   * Update the message ringtone for the default message channel.
   */
  public static synchronized void updateMessageRingtone(@NonNull Context context, @Nullable Uri uri) {
    Log.i(TAG, "Updating default message ringtone with URI: " + String.valueOf(uri));

    updateMessageChannel(context, channel -> {
      channel.setSound(uri == null ? Settings.System.DEFAULT_NOTIFICATION_URI : uri, getRingtoneAudioAttributes());
    });
  }

  /**
   * @return The vibrate settings for the default message channel.
   */
  public static synchronized boolean getMessageVibrate(@NonNull Context context) {
    return ServiceUtil.getNotificationManager(context).getNotificationChannel(getMessagesChannel(context)).shouldVibrate();
  }

  /**
   * Sets the vibrate property for the default message channel.
   */
  public static synchronized void updateMessageVibrate(@NonNull Context context, boolean enabled) {
    Log.i(TAG, "Updating default vibrate with value: " + enabled);

    updateMessageChannel(context, channel -> channel.enableVibration(enabled));
  }

  @WorkerThread
  public static synchronized void ensureCustomChannelConsistency(@NonNull Context context) {
    NotificationManager notificationManager = ServiceUtil.getNotificationManager(context);
    RecipientDatabase   db                  = DatabaseComponent.get(context).recipientDatabase();
    List<Recipient>     customRecipients    = new ArrayList<>();
    Set<String>         customChannelIds    = new HashSet<>();
    Set<String>         existingChannelIds  = Stream.of(notificationManager.getNotificationChannels()).map(NotificationChannel::getId).collect(Collectors.toSet());

    try (RecipientDatabase.RecipientReader reader = db.getRecipientsWithNotificationChannels()) {
      Recipient recipient;
      while ((recipient = reader.getNext()) != null) {
        customRecipients.add(recipient);
        customChannelIds.add(recipient.getNotificationChannel());
      }
    }

    for (NotificationChannel existingChannel : notificationManager.getNotificationChannels()) {
      if (existingChannel.getId().startsWith(CONTACT_PREFIX) && !customChannelIds.contains(existingChannel.getId())) {
        notificationManager.deleteNotificationChannel(existingChannel.getId());
      } else if (existingChannel.getId().startsWith(MESSAGES_PREFIX) && !existingChannel.getId().equals(getMessagesChannel(context))) {
        notificationManager.deleteNotificationChannel(existingChannel.getId());
      }
    }

    for (Recipient customRecipient : customRecipients) {
      if (!existingChannelIds.contains(customRecipient.getNotificationChannel())) {
        db.setNotificationChannel(customRecipient, null);
      }
    }
  }

  private static void onCreate(@NonNull Context context, @NonNull NotificationManager notificationManager) {
    NotificationChannelGroup messagesGroup = new NotificationChannelGroup(CATEGORY_MESSAGES, context.getResources().getString(R.string.NotificationChannel_group_messages));
    notificationManager.createNotificationChannelGroup(messagesGroup);

    NotificationChannel messages     = new NotificationChannel(getMessagesChannel(context), context.getString(R.string.NotificationChannel_messages), NotificationManager.IMPORTANCE_HIGH);
    NotificationChannel calls        = new NotificationChannel(CALLS, context.getString(R.string.NotificationChannel_calls), NotificationManager.IMPORTANCE_HIGH);
    NotificationChannel failures     = new NotificationChannel(FAILURES, context.getString(R.string.NotificationChannel_failures), NotificationManager.IMPORTANCE_HIGH);
    NotificationChannel backups      = new NotificationChannel(BACKUPS, context.getString(R.string.NotificationChannel_backups), NotificationManager.IMPORTANCE_LOW);
    NotificationChannel lockedStatus = new NotificationChannel(LOCKED_STATUS, context.getString(R.string.NotificationChannel_locked_status), NotificationManager.IMPORTANCE_LOW);
    NotificationChannel other        = new NotificationChannel(OTHER, context.getString(R.string.NotificationChannel_other), NotificationManager.IMPORTANCE_LOW);

    messages.setGroup(CATEGORY_MESSAGES);
    messages.enableVibration(TextSecurePreferences.isNotificationVibrateEnabled(context));
    messages.setSound(TextSecurePreferences.getNotificationRingtone(context), getRingtoneAudioAttributes());
    setLedPreference(messages, TextSecurePreferences.getNotificationLedColor(context));

    calls.setShowBadge(false);
    calls.setSound(null,null);
    backups.setShowBadge(false);
    lockedStatus.setShowBadge(false);
    other.setShowBadge(false);

    notificationManager.createNotificationChannels(Arrays.asList(messages, calls, failures, backups, lockedStatus, other));

    if (BuildConfig.PLAY_STORE_DISABLED) {
      NotificationChannel appUpdates = new NotificationChannel(APP_UPDATES, context.getString(R.string.NotificationChannel_app_updates), NotificationManager.IMPORTANCE_HIGH);
      notificationManager.createNotificationChannel(appUpdates);
    } else {
      notificationManager.deleteNotificationChannel(APP_UPDATES);
    }
  }

  private static void onUpgrade(@NonNull NotificationManager notificationManager, int oldVersion, int newVersion) {
    Log.i(TAG, "Upgrading channels from " + oldVersion + " to " + newVersion);

    if (oldVersion < VERSION_MESSAGES_CATEGORY) {
      notificationManager.deleteNotificationChannel("messages");
      notificationManager.deleteNotificationChannel("calls");
      notificationManager.deleteNotificationChannel("locked_status");
      notificationManager.deleteNotificationChannel("backups");
      notificationManager.deleteNotificationChannel("other");
    }
    if(oldVersion<VERSION_BCHAT_CALLS){
      notificationManager.deleteNotificationChannel("calls_v2");
    }
  }

  private static void setLedPreference(@NonNull NotificationChannel channel, @NonNull String ledColor) {
    if ("none".equals(ledColor)) {
      channel.enableLights(false);
    } else {
      channel.enableLights(true);
      channel.setLightColor(Color.parseColor(ledColor));
    }
  }


  private static @NonNull String generateChannelIdFor(@NonNull Address address) {
    return CONTACT_PREFIX + address.serialize() + "_" + System.currentTimeMillis();
  }

  private static @NonNull NotificationChannel copyChannel(@NonNull NotificationChannel original, @NonNull String id) {
    NotificationChannel copy = new NotificationChannel(id, original.getName(), original.getImportance());

    copy.setGroup(original.getGroup());
    copy.setSound(original.getSound(), original.getAudioAttributes());
    copy.setBypassDnd(original.canBypassDnd());
    copy.enableVibration(original.shouldVibrate());
    copy.setVibrationPattern(original.getVibrationPattern());
    copy.setLockscreenVisibility(original.getLockscreenVisibility());
    copy.setShowBadge(original.canShowBadge());
    copy.setLightColor(original.getLightColor());
    copy.enableLights(original.shouldShowLights());

    return copy;
  }

  private static String getMessagesChannelId(int version) {
    return MESSAGES_PREFIX + version;
  }

  @WorkerThread
  private static void updateAllRecipientChannelLedColors(@NonNull Context context, @NonNull NotificationManager notificationManager, @NonNull String color) {
    RecipientDatabase database = DatabaseComponent.get(context).recipientDatabase();

    try (RecipientDatabase.RecipientReader recipients = database.getRecipientsWithNotificationChannels()) {
      Recipient recipient;
      while ((recipient = recipients.getNext()) != null) {
        assert recipient.getNotificationChannel() != null;

        String  newChannelId = generateChannelIdFor(recipient.getAddress());
        boolean success      = updateExistingChannel(notificationManager, recipient.getNotificationChannel(), newChannelId, channel -> setLedPreference(channel, color));

        database.setNotificationChannel(recipient, success ? newChannelId : null);
      }
    }

    ensureCustomChannelConsistency(context);
  }

  private static void updateMessageChannel(@NonNull Context context, @NonNull ChannelUpdater updater) {
    NotificationManager notificationManager = ServiceUtil.getNotificationManager(context);
    int existingVersion                     = TextSecurePreferences.getNotificationMessagesChannelVersion(context);
    int newVersion                          = existingVersion + 1;

    Log.i(TAG, "Updating message channel from version " + existingVersion + " to " + newVersion);
    if (updateExistingChannel(notificationManager, getMessagesChannelId(existingVersion), getMessagesChannelId(newVersion), updater)) {
      TextSecurePreferences.setNotificationMessagesChannelVersion(context, newVersion);
    } else {
      onCreate(context, notificationManager);
    }
  }

  private static boolean updateExistingChannel(@NonNull NotificationManager notificationManager,
                                               @NonNull String channelId,
                                               @NonNull String newChannelId,
                                               @NonNull ChannelUpdater updater)
  {
    NotificationChannel existingChannel = notificationManager.getNotificationChannel(channelId);
    if (existingChannel == null) {
      Log.w(TAG, "Tried to update a channel, but it didn't exist.");
      return false;
    }

    notificationManager.deleteNotificationChannel(existingChannel.getId());

    NotificationChannel newChannel = copyChannel(existingChannel, newChannelId);
    updater.update(newChannel);
    notificationManager.createNotificationChannel(newChannel);
    return true;
  }

  private static AudioAttributes getRingtoneAudioAttributes() {
    return new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
        .build();
  }

  private static boolean channelExists(@Nullable NotificationChannel channel) {
    return channel != null && !NotificationChannel.DEFAULT_CHANNEL_ID.equals(channel.getId());
  }

  private interface ChannelUpdater {
    void update(@NonNull NotificationChannel channel);
  }
}
