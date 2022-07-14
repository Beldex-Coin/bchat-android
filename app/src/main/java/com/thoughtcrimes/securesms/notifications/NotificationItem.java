package com.thoughtcrimes.securesms.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.TaskStackBuilder;


import com.thoughtcrimes.securesms.conversation.v2.ConversationActivityV2;
import com.thoughtcrimes.securesms.mms.SlideDeck;

import com.thoughtcrimes.securesms.conversation.v2.ConversationActivityV2;

import com.beldex.libbchat.utilities.recipients.Recipient;

public class NotificationItem {

  private final long                        id;
  private final boolean                     mms;
  private final @NonNull  Recipient         conversationRecipient;
  private final @NonNull  Recipient         individualRecipient;
  private final @Nullable Recipient         threadRecipient;
  private final long                        threadId;
  private final @Nullable CharSequence      text;
  private final long                        timestamp;
  private final @Nullable
  SlideDeck slideDeck;

  public NotificationItem(long id, boolean mms,
                          @NonNull   Recipient individualRecipient,
                          @NonNull   Recipient conversationRecipient,
                          @Nullable  Recipient threadRecipient,
                          long threadId, @Nullable CharSequence text, long timestamp,
                          @Nullable SlideDeck slideDeck)
  {
    this.id                    = id;
    this.mms                   = mms;
    this.individualRecipient   = individualRecipient;
    this.conversationRecipient = conversationRecipient;
    this.threadRecipient       = threadRecipient;
    this.text                  = text;
    this.threadId              = threadId;
    this.timestamp             = timestamp;
    this.slideDeck             = slideDeck;
  }

  public @NonNull  Recipient getRecipient() {
    return threadRecipient == null ? conversationRecipient : threadRecipient;
  }

  public @NonNull  Recipient getIndividualRecipient() {
    return individualRecipient;
  }

  public @Nullable CharSequence getText() {
    return text;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getThreadId() {
    return threadId;
  }

  public @Nullable SlideDeck getSlideDeck() {
    return slideDeck;
  }

  public PendingIntent getPendingIntent(Context context) {
    Intent     intent           = new Intent(context, ConversationActivityV2.class);
    Recipient  notifyRecipients = threadRecipient != null ? threadRecipient : conversationRecipient;
    if (notifyRecipients != null) intent.putExtra(ConversationActivityV2.ADDRESS, notifyRecipients.getAddress());

    intent.putExtra(ConversationActivityV2.THREAD_ID, threadId);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));

    return TaskStackBuilder.create(context)
                           .addNextIntentWithParentStack(intent)
                           .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public long getId() {
    return id;
  }

  public boolean isMms() {
    return mms;
  }
}
