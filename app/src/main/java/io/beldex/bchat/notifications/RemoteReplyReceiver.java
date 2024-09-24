/*
 * Copyright (C) 2016 Open Whisper Systems
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

package io.beldex.bchat.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.core.app.RemoteInput;

import io.beldex.bchat.ApplicationContext;
import io.beldex.bchat.database.MessagingDatabase;
import io.beldex.bchat.database.ThreadDatabase;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.mms.MmsException;
import com.beldex.libbchat.messaging.messages.signal.OutgoingMediaMessage;
import com.beldex.libbchat.messaging.messages.signal.OutgoingTextMessage;
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage;
import com.beldex.libbchat.messaging.sending_receiving.MessageSender;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libsignal.utilities.Log;

import io.beldex.bchat.dependencies.DatabaseComponent;

import java.util.Collections;
import java.util.List;

/**
 * Get the response text from the Wearable Device and sends an message as a reply
 */
public class RemoteReplyReceiver extends BroadcastReceiver {

  public static final String TAG           = RemoteReplyReceiver.class.getSimpleName();
  public static final String REPLY_ACTION  = "io.beldex.securesms.notifications.WEAR_REPLY";
  public static final String ADDRESS_EXTRA = "address";
  public static final String REPLY_METHOD  = "reply_method";

  @SuppressLint("StaticFieldLeak")
  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!REPLY_ACTION.equals(intent.getAction())) return;

    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

    if (remoteInput == null) return;

    final Address      address      = intent.getParcelableExtra(ADDRESS_EXTRA);
    final ReplyMethod  replyMethod  = (ReplyMethod) intent.getSerializableExtra(REPLY_METHOD);
    final CharSequence responseText = remoteInput.getCharSequence(DefaultMessageNotifier.EXTRA_REMOTE_REPLY);

    if (address     == null) throw new AssertionError("No address specified");
    if (replyMethod == null) throw new AssertionError("No reply method specified");

    if (responseText != null) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          Recipient recipient = Recipient.from(context, address, false);
          ThreadDatabase threadDatabase = DatabaseComponent.get(context).threadDatabase();
          long threadId = threadDatabase.getOrCreateThreadIdFor(recipient);
          VisibleMessage message = new VisibleMessage();
          message.setSentTimestamp(System.currentTimeMillis());
          message.setText(responseText.toString());

          switch (replyMethod) {
            case GroupMessage: {
              OutgoingMediaMessage reply = OutgoingMediaMessage.from(message, recipient, Collections.emptyList(), null, null);
              try {
                DatabaseComponent.get(context).mmsDatabase().insertMessageOutbox(reply, threadId, false, null,true);
                MessageSender.send(message, address);
              } catch (MmsException e) {
                Log.w(TAG, e);
              }
              break;
            }
            case SecureMessage: {
              OutgoingTextMessage reply = OutgoingTextMessage.from(message, recipient);
              DatabaseComponent.get(context).smsDatabase().insertMessageOutbox(threadId, reply, false, System.currentTimeMillis(), null,true);
              MessageSender.send(message, address);
              break;
            }
            default:
              throw new AssertionError("Unknown Reply method");
          }

          List<MessagingDatabase.MarkedMessageInfo> messageIds = threadDatabase.setRead(threadId, true);

          ApplicationContext.getInstance(context).messageNotifier.updateNotification(context);
          MarkReadReceiver.process(context, messageIds);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }
}
