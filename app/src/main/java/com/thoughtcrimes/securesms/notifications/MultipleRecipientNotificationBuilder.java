package com.thoughtcrimes.securesms.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.thoughtcrimes.securesms.database.BchatContactDatabase;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;
import com.thoughtcrimes.securesms.home.HomeActivity;

import com.beldex.libbchat.messaging.contacts.Contact;
import com.beldex.libbchat.utilities.NotificationPrivacyPreference;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.thoughtcrimes.securesms.database.BchatContactDatabase;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;
import com.thoughtcrimes.securesms.home.HomeActivity;

import java.util.LinkedList;
import java.util.List;

import io.beldex.bchat.R;

public class MultipleRecipientNotificationBuilder extends AbstractNotificationBuilder {

  private final List<CharSequence> messageBodies = new LinkedList<>();

  public MultipleRecipientNotificationBuilder(Context context, NotificationPrivacyPreference privacy) {
    super(context, privacy);

    setColor(context.getResources().getColor(R.color.textsecure_primary));
    setSmallIcon(R.drawable.ic_notification);
    setContentTitle(context.getString(R.string.app_name));
    setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, HomeActivity.class), 0));
    setCategory(NotificationCompat.CATEGORY_MESSAGE);
    setGroupSummary(true);

    if (!NotificationChannels.supported()) {
      setPriority(TextSecurePreferences.getNotificationPriority(context));
    }
  }

  public void setMessageCount(int messageCount, int threadCount) {
    setSubText(context.getString(R.string.MessageNotifier_d_new_messages_in_d_conversations,
                                 messageCount, threadCount));
    setContentInfo(String.valueOf(messageCount));
    setNumber(messageCount);
  }

  public void setMostRecentSender(Recipient recipient, Recipient threadRecipient) {
    String displayName = recipient.toShortString();
    if (threadRecipient.isOpenGroupRecipient()) {
      displayName = getOpenGroupDisplayName(recipient);
    }
    if (privacy.isDisplayContact()) {
      setContentText(context.getString(R.string.MessageNotifier_most_recent_from_s, displayName));
    }

    if (recipient.getNotificationChannel() != null) {
      setChannelId(recipient.getNotificationChannel());
    }
  }

  public void addActions(PendingIntent markAsReadIntent) {
    NotificationCompat.Action markAllAsReadAction = new NotificationCompat.Action(R.drawable.check,
                                            context.getString(R.string.MessageNotifier_mark_all_as_read),
                                            markAsReadIntent);
    addAction(markAllAsReadAction);
    extend(new NotificationCompat.WearableExtender().addAction(markAllAsReadAction));
  }

  public void putStringExtra(String key, String value) {
    extras.putString(key,value);
  }

  public void addMessageBody(@NonNull Recipient sender, Recipient threadRecipient, @Nullable CharSequence body) {
    String displayName = sender.toShortString();
    if (threadRecipient.isOpenGroupRecipient()) {
      displayName = getOpenGroupDisplayName(sender);
    }
    if (privacy.isDisplayMessage()) {
      SpannableStringBuilder builder = new SpannableStringBuilder();
      builder.append(Util.getBoldedString(displayName));
      builder.append(": ");
      builder.append(body == null ? "" : body);

      messageBodies.add(builder);
    } else if (privacy.isDisplayContact()) {
      messageBodies.add(Util.getBoldedString(displayName));
    }

    if (privacy.isDisplayContact() && sender.getContactUri() != null) {
      addPerson(sender.getContactUri().toString());
    }
  }

  @Override
  public Notification build() {
    if (privacy.isDisplayMessage() || privacy.isDisplayContact()) {
      NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

      for (CharSequence body : messageBodies) {
        style.addLine(trimToDisplayLength(body));
      }

      setStyle(style);
    }

    return super.build();
  }

  /**
   * @param recipient the * individual * recipient for which to get the social group display name.
   */
  private String getOpenGroupDisplayName(Recipient recipient) {
    BchatContactDatabase contactDB = DatabaseComponent.get(context).bchatContactDatabase();
    String bchatID = recipient.getAddress().serialize();
    Contact contact = contactDB.getContactWithBchatID(bchatID);
    if (contact == null) { return bchatID; }
    String displayName = contact.displayName(Contact.ContactContext.OPEN_GROUP);
    if (displayName == null) { return bchatID; }
    return displayName;
  }
}
