package com.thoughtcrimes.securesms.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

import io.beldex.bchat.R;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libbchat.utilities.NotificationPrivacyPreference;

public class FailedNotificationBuilder extends AbstractNotificationBuilder {

  public FailedNotificationBuilder(Context context, NotificationPrivacyPreference privacy, Intent intent) {
    super(context, privacy);

    setSmallIcon(R.drawable.ic_bchat_logo);
    setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                              R.drawable.ic_action_warning_red));
    setContentTitle(context.getString(R.string.MessageNotifier_message_delivery_failed));
    setContentText(context.getString(R.string.MessageNotifier_failed_to_deliver_message));
    setTicker(context.getString(R.string.MessageNotifier_error_delivering_message));
    setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE));
    setAutoCancel(true);
    setAlarms(null, Recipient.VibrateState.DEFAULT);
    setChannelId(NotificationChannels.FAILURES);
  }



}
