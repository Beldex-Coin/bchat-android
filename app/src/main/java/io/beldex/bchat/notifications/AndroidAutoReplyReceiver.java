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

package io.beldex.bchat.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.core.app.RemoteInput;

import com.beldex.libbchat.mnode.MnodeAPI;
import io.beldex.bchat.ApplicationContext;
import io.beldex.bchat.database.MessagingDatabase;
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
 * Get the response text from the Android Auto and sends an message as a reply
 */
public class AndroidAutoReplyReceiver extends BroadcastReceiver {

  public static final String TAG             = AndroidAutoReplyReceiver.class.getSimpleName();
  public static final String REPLY_ACTION    = "io.beldex.securesms.notifications.ANDROID_AUTO_REPLY";
  public static final String ADDRESS_EXTRA   = "car_address";
  public static final String VOICE_REPLY_KEY = "car_voice_reply_key";
  public static final String THREAD_ID_EXTRA = "car_reply_thread_id";

  @SuppressLint("StaticFieldLeak")
  @Override
  public void onReceive(final Context context, Intent intent)
  {
    if (!REPLY_ACTION.equals(intent.getAction())) return;

    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

    if (remoteInput == null) return;

    final Address      address      = intent.getParcelableExtra(ADDRESS_EXTRA);
    final long         threadId     = intent.getLongExtra(THREAD_ID_EXTRA, -1);
    final CharSequence responseText = getMessageText(intent);
    final Recipient    recipient    = Recipient.from(context, address, false);

    if (responseText != null) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {

          long replyThreadId;

          if (threadId == -1) {
            replyThreadId = DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(recipient);
          } else {
            replyThreadId = threadId;
          }

          VisibleMessage message = new VisibleMessage();
          message.setText(responseText.toString());
          message.setSentTimestamp(MnodeAPI.getNowWithOffset());
          MessageSender.send(message, recipient.getAddress());

          if (recipient.isGroupRecipient()) {
            Log.w("AndroidAutoReplyReceiver", "GroupRecipient, Sending media message");
            OutgoingMediaMessage reply = OutgoingMediaMessage.from(message, recipient, Collections.emptyList(), null, null);
            try {
              DatabaseComponent.get(context).mmsDatabase().insertMessageOutbox(reply, replyThreadId, false, null,true);
            } catch (MmsException e) {
              Log.w(TAG, e);
            }
          } else {
            Log.w("AndroidAutoReplyReceiver", "Sending regular message ");
            OutgoingTextMessage reply = OutgoingTextMessage.from(message, recipient);
            DatabaseComponent.get(context).smsDatabase().insertMessageOutbox(replyThreadId, reply, false, MnodeAPI.getNowWithOffset(), null,true);
          }

          List<MessagingDatabase.MarkedMessageInfo> messageIds = DatabaseComponent.get(context).threadDatabase().setRead(replyThreadId, true);

          ApplicationContext.getInstance(context).messageNotifier.updateNotification(context);
          MarkReadReceiver.process(context, messageIds);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private CharSequence getMessageText(Intent intent) {
    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
    if (remoteInput != null) {
      return remoteInput.getCharSequence(VOICE_REPLY_KEY);
    }
    return null;
  }

}
