/*
 * Copyright (C) 2011 Whisper Systems
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
package com.thoughtcrimes.securesms.notifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.thoughtcrimes.securesms.ApplicationContext;
import com.thoughtcrimes.securesms.contactshare.ContactUtil;
import com.thoughtcrimes.securesms.conversation.v2.ConversationActivityV2;
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionManagerUtilities;
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionUtilities;
import com.thoughtcrimes.securesms.database.MessagingDatabase;
import com.thoughtcrimes.securesms.database.MmsSmsDatabase;
import com.thoughtcrimes.securesms.database.RecipientDatabase;
import com.thoughtcrimes.securesms.database.ThreadDatabase;
import com.thoughtcrimes.securesms.database.model.MediaMmsMessageRecord;
import com.thoughtcrimes.securesms.database.model.MessageRecord;
import com.thoughtcrimes.securesms.database.model.MmsMessageRecord;
import com.thoughtcrimes.securesms.database.model.Quote;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;
import com.thoughtcrimes.securesms.mms.SlideDeck;
import com.thoughtcrimes.securesms.service.KeyCachingService;
import com.thoughtcrimes.securesms.util.BchatMetaProtocol;
import com.thoughtcrimes.securesms.util.SpanUtil;

import com.beldex.libbchat.messaging.sending_receiving.notifications.MessageNotifier;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.Contact;
import com.beldex.libbchat.utilities.ServiceUtil;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.utilities.Util;

import com.thoughtcrimes.securesms.conversation.v2.ConversationActivityV2;
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionManagerUtilities;
import com.thoughtcrimes.securesms.conversation.v2.utilities.MentionUtilities;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;
import com.thoughtcrimes.securesms.util.BchatMetaProtocol;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import me.leolin.shortcutbadger.ShortcutBadger;
import io.beldex.bchat.R;

/**
 * Handles posting system notifications for new messages.
 *
 *
 * @author Moxie Marlinspike
 */

public class DefaultMessageNotifier implements MessageNotifier {

  private static final String TAG = DefaultMessageNotifier.class.getSimpleName();

  public static final  String EXTRA_REMOTE_REPLY        = "extra_remote_reply";
  public static final  String LATEST_MESSAGE_ID_TAG     = "extra_latest_message_id";

  private static final int    FOREGROUND_ID              = 313399;
  private static final int    SUMMARY_NOTIFICATION_ID    = 1338;
  private static final int    PENDING_MESSAGES_ID       = 1111;
  private static final String NOTIFICATION_GROUP        = "messages";
  private static final long   MIN_AUDIBLE_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(5);
  private static final long   DESKTOP_ACTIVITY_PERIOD   = TimeUnit.MINUTES.toMillis(1);

  private volatile static       long               visibleThread                = -1;
  private volatile static       boolean            homeScreenVisible            = false;
  private volatile static       long               lastDesktopActivityTimestamp = -1;
  private volatile static       long               lastAudibleNotification      = -1;
  private          static final CancelableExecutor executor                     = new CancelableExecutor();

  @Override
  public void setVisibleThread(long threadId) {
    visibleThread = threadId;
  }

  @Override
  public void setHomeScreenVisible(boolean isVisible) {
    homeScreenVisible = isVisible;
  }

  @Override
  public void setLastDesktopActivityTimestamp(long timestamp) {
    lastDesktopActivityTimestamp = timestamp;
  }

  @Override
  public void notifyMessageDeliveryFailed(Context context, Recipient recipient, long threadId) {
    if (visibleThread == threadId) {
      sendInThreadNotification(context, recipient);
    } else {
      Intent intent = new Intent(context, ConversationActivityV2.class);
      intent.putExtra(ConversationActivityV2.ADDRESS, recipient.getAddress());
      intent.putExtra(ConversationActivityV2.THREAD_ID, threadId);
      intent.setData((Uri.parse("custom://" + System.currentTimeMillis())));

      FailedNotificationBuilder builder = new FailedNotificationBuilder(context, TextSecurePreferences.getNotificationPrivacy(context), intent);
      ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
        .notify((int)threadId, builder.build());
    }
  }

  public void notifyMessagesPending(Context context) {
    if (!TextSecurePreferences.isNotificationsEnabled(context)) {
      return;
    }

    PendingMessageNotificationBuilder builder = new PendingMessageNotificationBuilder(context, TextSecurePreferences.getNotificationPrivacy(context));
    ServiceUtil.getNotificationManager(context).notify(PENDING_MESSAGES_ID, builder.build());
  }

  @Override
  public void cancelDelayedNotifications() {
    executor.cancel();
  }

  private void cancelActiveNotifications(@NonNull Context context) {
    NotificationManager notifications = ServiceUtil.getNotificationManager(context);
    notifications.cancel(SUMMARY_NOTIFICATION_ID);

    if (Build.VERSION.SDK_INT >= 23) {
      try {
        StatusBarNotification[] activeNotifications = notifications.getActiveNotifications();

        for (StatusBarNotification activeNotification : activeNotifications) {
          notifications.cancel(activeNotification.getId());
        }
      } catch (Throwable e) {
        // XXX Appears to be a ROM bug, see #6043
        Log.w(TAG, e);
        notifications.cancelAll();
      }
    }
  }

  /*Hales63*/
  private void cancelOrphanedNotifications(@NonNull Context context, NotificationState notificationState) {
    try {
      NotificationManager     notifications       = ServiceUtil.getNotificationManager(context);
      StatusBarNotification[] activeNotifications = notifications.getActiveNotifications();

      for (StatusBarNotification notification : activeNotifications) {
        boolean validNotification = false;

        if (notification.getId() != SUMMARY_NOTIFICATION_ID &&
                notification.getId() != KeyCachingService.SERVICE_RUNNING_ID          &&
                notification.getId() != FOREGROUND_ID         &&
                notification.getId() != PENDING_MESSAGES_ID)
        {
          for (NotificationItem item : notificationState.getNotifications()) {
            if (notification.getId() == (SUMMARY_NOTIFICATION_ID + item.getThreadId())) {
              validNotification = true;
              break;
            }
          }

          if (!validNotification) {
            notifications.cancel(notification.getId());
          }
        }
      }
    } catch (Throwable e) {
      // XXX Android ROM Bug, see #6043
      Log.w(TAG, e);
    }
  }

  @Override
  public void updateNotification(@NonNull Context context) {
    if (!TextSecurePreferences.isNotificationsEnabled(context)) {
      return;
    }

    updateNotification(context, false, 0);
  }

  @Override
  public void updateNotification(@NonNull Context context, long threadId)
  {
    if (System.currentTimeMillis() - lastDesktopActivityTimestamp < DESKTOP_ACTIVITY_PERIOD) {
      Log.i(TAG, "Scheduling delayed notification...");
      executor.execute(new DelayedNotification(context, threadId));
    } else {
      updateNotification(context, threadId, true);
    }
  }

  @Override
  public void updateNotification(@NonNull Context context, long threadId, boolean signal)
  {
    boolean    isVisible  = visibleThread == threadId;

    ThreadDatabase threads    = DatabaseComponent.get(context).threadDatabase();
    Recipient      recipients = threads.getRecipientForThreadId(threadId);

    //New Line
    if (!recipients.isGroupRecipient() && threads.getMessageCount(threadId) == 1 &&
            !(recipients.isApproved() || threads.getLastSeenAndHasSent(threadId).second())) {
      TextSecurePreferences.removeHasHiddenMessageRequests(context);
    }

    if (isVisible && recipients != null) {
      List<MessagingDatabase.MarkedMessageInfo> messageIds = threads.setRead(threadId, false);
      if (BchatMetaProtocol.shouldSendReadReceipt(recipients)) { MarkReadReceiver.process(context, messageIds); }
    }

    if (!TextSecurePreferences.isNotificationsEnabled(context) ||
        (recipients != null && recipients.isMuted()))
    {
      return;
    }

    if (isVisible) {
      sendInThreadNotification(context, threads.getRecipientForThreadId(threadId));
    } else if (!homeScreenVisible) {
      updateNotification(context, signal, 0);
    }
  }

  @Override
  public void updateNotification(@NonNull Context context, boolean signal, int reminderCount)
  {
    Cursor telcoCursor = null;
    Cursor pushCursor  = null;

    try {
      telcoCursor = DatabaseComponent.get(context).mmsSmsDatabase().getUnread();
      pushCursor = DatabaseComponent.get(context).pushDatabase().getPending();

      if (((telcoCursor == null || telcoCursor.isAfterLast()) &&
              (pushCursor == null || pushCursor.isAfterLast())) || !TextSecurePreferences.hasSeenWelcomeScreen(context)) {
        cancelActiveNotifications(context);
        updateBadge(context, 0);
        clearReminder(context);
        return;
      }

      NotificationState notificationState = constructNotificationState(context, telcoCursor);

      if (signal && (System.currentTimeMillis() - lastAudibleNotification) < MIN_AUDIBLE_PERIOD_MILLIS) {
        signal = false;
      } else if (signal) {
        lastAudibleNotification = System.currentTimeMillis();
      }
      try{
      if (notificationState.hasMultipleThreads()) {
        for (long threadId : notificationState.getThreads()) {
          sendSingleThreadNotification(context, new NotificationState(notificationState.getNotificationsForThread(threadId)), false, true);
        }
        sendMultipleThreadNotification(context, notificationState, signal);
      } else if (notificationState.getMessageCount() > 0) {
        sendSingleThreadNotification(context, notificationState, signal, false);
      } else {
        cancelActiveNotifications(context);
      }
      }catch(Exception e){
        Log.e(TAG,"Error creating notification",e);
      }
      cancelOrphanedNotifications(context, notificationState);
      updateBadge(context, notificationState.getMessageCount());

      if (signal) {
        scheduleReminder(context, reminderCount);
      }
    } finally {
      if (telcoCursor != null) telcoCursor.close();
      if (pushCursor != null)  pushCursor.close();
    }
  }

  private String getTrimmedText(CharSequence text) {    String trimmedText = "";    if (text != null) {      int trimEnd = Math.min(text.length(), 50);      trimmedText = text.subSequence(0,trimEnd) + (text.length() > 50 ? "..." : "");    }    return trimmedText;  }

  private void sendSingleThreadNotification(@NonNull  Context context,
                                            @NonNull  NotificationState notificationState,
                                            boolean signal, boolean bundled)
  {
    Log.i(TAG, "sendSingleThreadNotification()  signal: " + signal + "  bundled: " + bundled);

    if (notificationState.getNotifications().isEmpty()) {
      if (!bundled) cancelActiveNotifications(context);
      Log.i(TAG, "Empty notification state. Skipping.");
      return;
    }

    SingleRecipientNotificationBuilder builder        = new SingleRecipientNotificationBuilder(context, TextSecurePreferences.getNotificationPrivacy(context));
    List<NotificationItem>             notifications  = notificationState.getNotifications();
    Recipient                          recipient      = notifications.get(0).getRecipient();
    int                                notificationId = (int) (SUMMARY_NOTIFICATION_ID + (bundled ? notifications.get(0).getThreadId() : 0));
    String                             messageIdTag   = String.valueOf(notifications.get(0).getTimestamp());

    NotificationManager notificationManager = ServiceUtil.getNotificationManager(context);
    for (StatusBarNotification notification: notificationManager.getActiveNotifications()) {
      if ( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && notification.isAppGroup() == bundled)
              && messageIdTag.equals(notification.getNotification().extras.getString(LATEST_MESSAGE_ID_TAG))) {
        return;
      }
    }

    long timestamp = notifications.get(0).getTimestamp();
    if (timestamp != 0) builder.setWhen(timestamp);

    builder.putStringExtra(LATEST_MESSAGE_ID_TAG, messageIdTag);

    CharSequence text = notifications.get(0).getText();
    String trimmedText = getTrimmedText(text);

    builder.setThread(notifications.get(0).getRecipient());
    builder.setMessageCount(notificationState.getMessageCount());
    MentionManagerUtilities.INSTANCE.populateUserPublicKeyCacheIfNeeded(notifications.get(0).getThreadId(),context);
    builder.setPrimaryMessageBody(recipient, notifications.get(0).getIndividualRecipient(),
                                  MentionUtilities.highlightMentions(trimmedText,
                                          notifications.get(0).getThreadId(),
                                          context),
                                  notifications.get(0).getSlideDeck());
    builder.setContentIntent(notifications.get(0).getPendingIntent(context));
    builder.setDeleteIntent(notificationState.getDeleteIntent(context));
    builder.setOnlyAlertOnce(!signal);
    builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
    builder.setAutoCancel(true);

    ReplyMethod replyMethod = ReplyMethod.forRecipient(context, recipient);

    boolean canReply = BchatMetaProtocol.canUserReplyToNotification(recipient);

    PendingIntent quickReplyIntent = canReply ? notificationState.getQuickReplyIntent(context, recipient) :  null;
    PendingIntent remoteReplyIntent = canReply ? notificationState.getRemoteReplyIntent(context, recipient, replyMethod) : null;

    builder.addActions(notificationState.getMarkAsReadIntent(context, notificationId),
                       quickReplyIntent,
                       remoteReplyIntent,
                       replyMethod);

    if (canReply) {
      builder.addAndroidAutoAction(notificationState.getAndroidAutoReplyIntent(context, recipient),
                                   notificationState.getAndroidAutoHeardIntent(context, notificationId),
                                   notifications.get(0).getTimestamp());
    }

    ListIterator<NotificationItem> iterator = notifications.listIterator(notifications.size());

    while(iterator.hasPrevious()) {
      NotificationItem item = iterator.previous();
      builder.addMessageBody(item.getRecipient(), item.getIndividualRecipient(), item.getText());
    }

    if (signal) {
      builder.setAlarms(notificationState.getRingtone(context), notificationState.getVibrate());
      builder.setTicker(notifications.get(0).getIndividualRecipient(),
                        notifications.get(0).getText());
    }

    if (bundled) {
      builder.setGroup(NOTIFICATION_GROUP);
      builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
    }

    Notification notification = builder.build();
    NotificationManagerCompat.from(context).notify(notificationId, notification);
    Log.i(TAG, "Posted notification. " + notification.toString());
  }

  private void sendMultipleThreadNotification(@NonNull  Context context,
                                              @NonNull  NotificationState notificationState,
                                              boolean signal)
  {
    Log.i(TAG, "sendMultiThreadNotification()  signal: " + signal);

    MultipleRecipientNotificationBuilder builder       = new MultipleRecipientNotificationBuilder(context, TextSecurePreferences.getNotificationPrivacy(context));
    List<NotificationItem>               notifications = notificationState.getNotifications();

    builder.setMessageCount(notificationState.getMessageCount(), notificationState.getThreadCount());
    builder.setMostRecentSender(notifications.get(0).getIndividualRecipient(), notifications.get(0).getRecipient());
    builder.setGroup(NOTIFICATION_GROUP);
    builder.setDeleteIntent(notificationState.getDeleteIntent(context));
    builder.setOnlyAlertOnce(!signal);
    builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
    builder.setAutoCancel(true);

    String messageIdTag = String.valueOf(notifications.get(0).getTimestamp());

    NotificationManager notificationManager = ServiceUtil.getNotificationManager(context);
    for (StatusBarNotification notification: notificationManager.getActiveNotifications()) {
      if (notification.getId() == SUMMARY_NOTIFICATION_ID
              && messageIdTag.equals(notification.getNotification().extras.getString(LATEST_MESSAGE_ID_TAG))) {
        return;
      }
    }

    long timestamp = notifications.get(0).getTimestamp();
    if (timestamp != 0) builder.setWhen(timestamp);

    builder.addActions(notificationState.getMarkAsReadIntent(context, SUMMARY_NOTIFICATION_ID));

    ListIterator<NotificationItem> iterator = notifications.listIterator(notifications.size());

    while(iterator.hasPrevious()) {
      NotificationItem item = iterator.previous();
      builder.addMessageBody(item.getIndividualRecipient(), item.getRecipient(),
                             MentionUtilities.highlightMentions(item.getText(), item.getThreadId(), context));
    }

    if (signal) {
      builder.setAlarms(notificationState.getRingtone(context), notificationState.getVibrate());
      builder.setTicker(notifications.get(0).getIndividualRecipient(),
                        MentionUtilities.highlightMentions(notifications.get(0).getText(), notifications.get(0).getThreadId(), context));
    }

    builder.putStringExtra(LATEST_MESSAGE_ID_TAG, messageIdTag);

    Notification notification = builder.build();
    NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, builder.build());
    Log.i(TAG, "Posted notification. " + notification.toString());
  }

  private void sendInThreadNotification(Context context, Recipient recipient) {
    if (!TextSecurePreferences.isInThreadNotifications(context) ||
            ServiceUtil.getAudioManager(context).getRingerMode() != AudioManager.RINGER_MODE_NORMAL ||
            (System.currentTimeMillis() - lastAudibleNotification) < MIN_AUDIBLE_PERIOD_MILLIS)
    {
      return;
    } else {
      lastAudibleNotification = System.currentTimeMillis();
    }

    Uri uri = null;
    if (recipient != null) {
      uri = NotificationChannels.supported() ? NotificationChannels.getMessageRingtone(context, recipient) : recipient.getMessageRingtone();
    }

    if (uri == null) {
      uri = NotificationChannels.supported() ? NotificationChannels.getMessageRingtone(context) : TextSecurePreferences.getNotificationRingtone(context);
    }

    if (uri.toString().isEmpty()) {
      Log.d(TAG, "ringtone uri is empty");
      return;
    }

    Ringtone ringtone = RingtoneManager.getRingtone(context, uri);

    if (ringtone == null) {
      Log.w(TAG, "ringtone is null");
      return;
    }

    if (Build.VERSION.SDK_INT >= 21) {
      ringtone.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                                                               .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                                                               .build());
    } else {
      ringtone.setStreamType(AudioManager.STREAM_NOTIFICATION);
    }

    ringtone.play();
  }

 /* Hales63*/
  private NotificationState constructNotificationState(@NonNull  Context context,
                                                       @NonNull  Cursor cursor)
  {
    NotificationState     notificationState = new NotificationState();
    MmsSmsDatabase.Reader reader            = DatabaseComponent.get(context).mmsSmsDatabase().readerFor(cursor);
    ThreadDatabase        threadDatabase    = DatabaseComponent.get(context).threadDatabase();

    MessageRecord record;

    while ((record = reader.getNext()) != null) {
      long         id                    = record.getId();
      boolean      mms                   = record.isMms() || record.isMmsNotification();
      Recipient    recipient             = record.getIndividualRecipient();
      Recipient    conversationRecipient = record.getRecipient();
      long         threadId              = record.getThreadId();
      CharSequence body                  = record.getDisplayBody(context);
      Recipient    threadRecipients      = null;
      SlideDeck slideDeck             = null;
      long         timestamp             = record.getTimestamp();
      boolean      messageRequest        = false;


      if (threadId != -1) {
        threadRecipients = threadDatabase.getRecipientForThreadId(threadId);
        messageRequest = threadRecipients != null && !threadRecipients.isGroupRecipient() &&
                !threadRecipients.isApproved() && !threadDatabase.getLastSeenAndHasSent(threadId).second();
        if (messageRequest && (threadDatabase.getMessageCount(threadId) > 1 || !TextSecurePreferences.hasHiddenMessageRequests(context))) {
          continue;
        }
      }

      if (messageRequest) {
        body = SpanUtil.italic(context.getString(R.string.message_requests_notification));
      } else if (KeyCachingService.isLocked(context)) {
        body = SpanUtil.italic(context.getString(R.string.MessageNotifier_locked_message));
      } else if (record.isMms() && !((MmsMessageRecord) record).getSharedContacts().isEmpty()) {
        Contact contact = ((MmsMessageRecord) record).getSharedContacts().get(0);
        body = ContactUtil.getStringSummary(context, contact);
      } else if (record.isMms() && TextUtils.isEmpty(body) && !((MmsMessageRecord) record).getSlideDeck().getSlides().isEmpty()) {
        slideDeck = ((MediaMmsMessageRecord)record).getSlideDeck();
        body = SpanUtil.italic(slideDeck.getBody());
      } else if (record.isMms() && !record.isMmsNotification() && !((MmsMessageRecord) record).getSlideDeck().getSlides().isEmpty()) {
        slideDeck = ((MediaMmsMessageRecord)record).getSlideDeck();
        String message      = slideDeck.getBody() + ": " + record.getBody();
        int    italicLength = message.length() - body.length();
        body = SpanUtil.italic(message, italicLength);
      } else if (record.isOpenGroupInvitation()) {
        body = SpanUtil.italic(context.getString(R.string.ThreadRecord_open_group_invitation));
      }

      if (threadRecipients == null || !threadRecipients.isMuted()) {
        if (threadRecipients != null && threadRecipients.notifyType == RecipientDatabase.NOTIFY_TYPE_MENTIONS) {
          // check if mentioned here
          boolean isQuoteMentioned = false;
          if (record instanceof MmsMessageRecord) {
            Quote quote = ((MmsMessageRecord) record).getQuote();
            Address quoteAddress = quote != null ? quote.getAuthor() : null;
            String serializedAddress = quoteAddress != null ? quoteAddress.serialize() : null;
            isQuoteMentioned = serializedAddress != null && Objects.equals(TextSecurePreferences.getLocalNumber(context), serializedAddress);
          }
          if (body.toString().contains("@"+TextSecurePreferences.getLocalNumber(context))
                  || isQuoteMentioned) {
            notificationState.addNotification(new NotificationItem(id, mms, recipient, conversationRecipient, threadRecipients, threadId, body, timestamp, slideDeck));
          }
        } else if (threadRecipients != null && threadRecipients.notifyType == RecipientDatabase.NOTIFY_TYPE_NONE) {
          // do nothing, no notifications
        } else {
          notificationState.addNotification(new NotificationItem(id, mms, recipient, conversationRecipient, threadRecipients, threadId, body, timestamp, slideDeck));
        }
      }
    }

    reader.close();
    return notificationState;
  }

  private void updateBadge(Context context, int count) {
    try {
      if (count == 0) ShortcutBadger.removeCount(context);
      else            ShortcutBadger.applyCount(context, count);
    } catch (Throwable t) {
      // NOTE :: I don't totally trust this thing, so I'm catching
      // everything.
      Log.w("MessageNotifier", t);
    }
  }

  private void scheduleReminder(Context context, int count) {
    if (count >= TextSecurePreferences.getRepeatAlertsCount(context)) {
      return;
    }

    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    Intent       alarmIntent  = new Intent(ReminderReceiver.REMINDER_ACTION);
    alarmIntent.putExtra("reminder_count", count);

    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
    long          timeout       = TimeUnit.MINUTES.toMillis(2);

    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout, pendingIntent);
  }

  @Override
  public void clearReminder(Context context) {
    Intent        alarmIntent   = new Intent(ReminderReceiver.REMINDER_ACTION);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
    AlarmManager  alarmManager  = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    alarmManager.cancel(pendingIntent);
  }

  public static class ReminderReceiver extends BroadcastReceiver {

    public static final String REMINDER_ACTION = "io.beldex.securesms.MessageNotifier.REMINDER_ACTION";

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onReceive(final Context context, final Intent intent) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          int reminderCount = intent.getIntExtra("reminder_count", 0);
          ApplicationContext.getInstance(context).messageNotifier.updateNotification(context, true, reminderCount + 1);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private static class DelayedNotification implements Runnable {

    private static final long DELAY = TimeUnit.SECONDS.toMillis(5);

    private final AtomicBoolean canceled = new AtomicBoolean(false);

    private final Context context;
    private final long    threadId;
    private final long    delayUntil;

    private DelayedNotification(Context context, long threadId) {
      this.context    = context;
      this.threadId   = threadId;
      this.delayUntil = System.currentTimeMillis() + DELAY;
    }

    @Override
    public void run() {
      long delayMillis = delayUntil - System.currentTimeMillis();
      Log.i(TAG, "Waiting to notify: " + delayMillis);

      if (delayMillis > 0) {
        Util.sleep(delayMillis);
      }

      if (!canceled.get()) {
        Log.i(TAG, "Not canceled, notifying...");
        ApplicationContext.getInstance(context).messageNotifier.updateNotification(context, threadId, true);
        ApplicationContext.getInstance(context).messageNotifier.cancelDelayedNotifications();
      } else {
        Log.w(TAG, "Canceled, not notifying...");
      }
    }

    public void cancel() {
      canceled.set(true);
    }
  }

  private static class CancelableExecutor {

    private final Executor                 executor = Executors.newSingleThreadExecutor();
    private final Set<DelayedNotification> tasks    = new HashSet<>();

    public void execute(final DelayedNotification runnable) {
      synchronized (tasks) {
        tasks.add(runnable);
      }

      Runnable wrapper = new Runnable() {
        @Override
        public void run() {
          runnable.run();

          synchronized (tasks) {
            tasks.remove(runnable);
          }
        }
      };

      executor.execute(wrapper);
    }

    public void cancel() {
      synchronized (tasks) {
        for (DelayedNotification task : tasks) {
          task.cancel();
        }
      }
    }
  }
}
