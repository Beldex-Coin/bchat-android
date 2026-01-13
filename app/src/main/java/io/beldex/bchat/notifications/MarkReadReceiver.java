package io.beldex.bchat.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.beldex.libbchat.mnode.MnodeAPI;
import com.beldex.libbchat.utilities.recipients.Recipient;
import io.beldex.bchat.ApplicationContext;
import io.beldex.bchat.database.MessagingDatabase;
import io.beldex.bchat.service.ExpiringMessageManager;

import com.beldex.libbchat.messaging.messages.control.ReadReceipt;
import com.beldex.libbchat.messaging.sending_receiving.MessageSender;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libsignal.utilities.Log;

import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.util.BchatMetaProtocol;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MarkReadReceiver extends BroadcastReceiver {

  private static final String TAG                   = MarkReadReceiver.class.getSimpleName();
  public static final  String CLEAR_ACTION          = "io.beldex.securesms.notifications.CLEAR";
  public static final  String THREAD_IDS_EXTRA      = "thread_ids";
  public static final  String NOTIFICATION_ID_EXTRA = "notification_id";

  @SuppressLint("StaticFieldLeak")
  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!CLEAR_ACTION.equals(intent.getAction()))
      return;

    final long[] threadIds = intent.getLongArrayExtra(THREAD_IDS_EXTRA);

    if (threadIds != null) {
      NotificationManagerCompat.from(context).cancel(intent.getIntExtra(NOTIFICATION_ID_EXTRA, -1));

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          List<MessagingDatabase.MarkedMessageInfo> messageIdsCollection = new LinkedList<>();

          for (long threadId : threadIds) {
            Log.i(TAG, "Marking as read: " + threadId);
            List<MessagingDatabase.MarkedMessageInfo> messageIds = DatabaseComponent.get(context).threadDatabase().setRead(threadId, true);
            messageIdsCollection.addAll(messageIds);
          }

          process(context, messageIdsCollection);

          ApplicationContext.getInstance(context).messageNotifier.updateNotification(context);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  public static void process(@NonNull Context context, @NonNull List<MessagingDatabase.MarkedMessageInfo> markedReadMessages) {
    if (markedReadMessages.isEmpty()) return;

    for (MessagingDatabase.MarkedMessageInfo messageInfo : markedReadMessages) {
      scheduleDeletion(context, messageInfo.getExpirationInfo());
    }

    if (!TextSecurePreferences.isReadReceiptsEnabled(context)) return;

    Map<Address, List<MessagingDatabase.SyncMessageId>> addressMap = Stream.of(markedReadMessages)
                                                         .map(MessagingDatabase.MarkedMessageInfo::getSyncMessageId)
                                                         .collect(Collectors.groupingBy(MessagingDatabase.SyncMessageId::getAddress));

    for (Address address : addressMap.keySet()) {
      List<Long> timestamps = Stream.of(addressMap.get(address)).map(MessagingDatabase.SyncMessageId::getTimetamp).toList();
      /*Hales63*/
      if (!BchatMetaProtocol.shouldSendReadReceipt(Recipient.from(context, address, false))) { continue; }
      ReadReceipt readReceipt = new ReadReceipt(timestamps);
      readReceipt.setSentTimestamp(MnodeAPI.getNowWithOffset());
      MessageSender.send(readReceipt, address);
    }
  }

  public static void scheduleDeletion(Context context, MessagingDatabase.ExpirationInfo expirationInfo) {
    if (expirationInfo.getExpiresIn() > 0 && expirationInfo.getExpireStarted() <= 0) {
      ExpiringMessageManager expirationManager = ApplicationContext.getInstance(context).getExpiringMessageManager();

      if (expirationInfo.isMms()) DatabaseComponent.get(context).mmsDatabase().markExpireStarted(expirationInfo.getId());
      else                        DatabaseComponent.get(context).smsDatabase().markExpireStarted(expirationInfo.getId());

      expirationManager.scheduleDeletion(expirationInfo.getId(), expirationInfo.isMms(), expirationInfo.getExpiresIn());
    }
  }
}
