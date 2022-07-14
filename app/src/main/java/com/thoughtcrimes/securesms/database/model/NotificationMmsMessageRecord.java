/*
 * Copyright (C) 2012 Moxie Marlinspike
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
package com.thoughtcrimes.securesms.database.model;

import android.content.Context;
import android.text.SpannableString;
import androidx.annotation.NonNull;

import com.thoughtcrimes.securesms.database.MmsDatabase;
import com.thoughtcrimes.securesms.database.SmsDatabase;
import com.thoughtcrimes.securesms.mms.SlideDeck;
import com.beldex.libbchat.utilities.IdentityKeyMismatch;
import com.beldex.libbchat.utilities.NetworkFailure;
import com.beldex.libbchat.utilities.recipients.Recipient;

import java.util.Collections;
import java.util.LinkedList;
import io.beldex.bchat.R;

/**
 * Represents the message record model for MMS messages that are
 * notifications (ie: they're pointers to undownloaded media).
 *
 * @author Moxie Marlinspike
 *
 */

public class NotificationMmsMessageRecord extends MmsMessageRecord {
  private final byte[] contentLocation;
  private final long   messageSize;
  private final long   expiry;
  private final int    status;
  private final byte[] transactionId;

  public NotificationMmsMessageRecord(long id, Recipient conversationRecipient,
                                      Recipient individualRecipient,
                                      long dateSent, long dateReceived, int deliveryReceiptCount,
                                      long threadId, byte[] contentLocation, long messageSize,
                                      long expiry, int status, byte[] transactionId, long mailbox,
                                      SlideDeck slideDeck, int readReceiptCount)
  {
    super(id, "", conversationRecipient, individualRecipient,
      dateSent, dateReceived, threadId, SmsDatabase.Status.STATUS_NONE, deliveryReceiptCount, mailbox,
      new LinkedList<IdentityKeyMismatch>(), new LinkedList<NetworkFailure>(),
      0, 0, slideDeck, readReceiptCount, null, Collections.emptyList(), Collections.emptyList(), false);

    this.contentLocation = contentLocation;
    this.messageSize     = messageSize;
    this.expiry          = expiry;
    this.status          = status;
    this.transactionId   = transactionId;
  }

  public byte[] getTransactionId() {
    return transactionId;
  }
  public int getStatus() {
    return this.status;
  }
  public byte[] getContentLocation() {
    return contentLocation;
  }
  public long getMessageSize() {
    return (messageSize + 1023) / 1024;
  }
  public long getExpiration() {
    return expiry * 1000;
  }

  @Override
  public boolean isOutgoing() {
    return false;
  }

  @Override
  public boolean isPending() {
    return false;
  }

  @Override
  public boolean isMmsNotification() {
    return true;
  }

  @Override
  public boolean isMediaPending() {
    return true;
  }

  @Override
  public SpannableString getDisplayBody(@NonNull Context context) {
    if (status == MmsDatabase.Status.DOWNLOAD_INITIALIZED) {
      return emphasisAdded(context.getString(R.string.NotificationMmsMessageRecord_multimedia_message));
    } else if (status == MmsDatabase.Status.DOWNLOAD_CONNECTING) {
      return emphasisAdded(context.getString(R.string.NotificationMmsMessageRecord_downloading_mms_message));
    } else {
      return emphasisAdded(context.getString(R.string.NotificationMmsMessageRecord_error_downloading_mms_message));
    }
  }
}
