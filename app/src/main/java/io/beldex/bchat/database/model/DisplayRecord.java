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
package io.beldex.bchat.database.model;

import android.content.Context;
import android.text.SpannableString;

import androidx.annotation.NonNull;

import io.beldex.bchat.database.MmsSmsColumns;
import io.beldex.bchat.database.SmsDatabase;
import com.beldex.libbchat.utilities.recipients.Recipient;

/**
 * The base class for all message record models.  Encapsulates basic data
 * shared between ThreadRecord and MessageRecord.
 *
 * @author Moxie Marlinspike
 *
 */

public abstract class DisplayRecord {
  protected final long type;
  private final Recipient  recipient;
  private final long       dateSent;
  private final long       dateReceived;
  private final long       threadId;
  private final String     body;
  private final int        deliveryStatus;
  private final int        deliveryReceiptCount;
  private final int        readReceiptCount;

  DisplayRecord(String body, Recipient recipient, long dateSent,
    long dateReceived, long threadId, int deliveryStatus, int deliveryReceiptCount,
    long type, int readReceiptCount)
  {
    this.threadId             = threadId;
    this.recipient            = recipient;
    this.dateSent             = dateSent;
    this.dateReceived         = dateReceived;
    this.type                 = type;
    this.body                 = body;
    this.deliveryReceiptCount = deliveryReceiptCount;
    this.readReceiptCount     = readReceiptCount;
    this.deliveryStatus       = deliveryStatus;
  }

  public @NonNull String getBody() {
    return body == null ? "" : body;
  }
  public abstract SpannableString getDisplayBody(@NonNull Context context);
  public Recipient getRecipient() { return recipient; }
  public long getDateSent() { return dateSent; }
  public long getDateReceived() { return dateReceived; }
  public long getThreadId() { return threadId; }
  public int getDeliveryStatus() { return deliveryStatus; }
  public int getDeliveryReceiptCount() { return deliveryReceiptCount; }
  public int getReadReceiptCount() { return readReceiptCount; }

  public boolean isDelivered() {
    return (deliveryStatus >= SmsDatabase.Status.STATUS_COMPLETE
      && deliveryStatus < SmsDatabase.Status.STATUS_PENDING) || deliveryReceiptCount > 0;
  }

  public boolean isSent() {
    return !isFailed() && !isPending();
  }

  public boolean isFailed() {
    return MmsSmsColumns.Types.isFailedMessageType(type)
      || MmsSmsColumns.Types.isPendingSecureSmsFallbackType(type)
      || deliveryStatus >= SmsDatabase.Status.STATUS_FAILED;
  }

  public boolean isPending() {
    return MmsSmsColumns.Types.isPendingMessageType(type)
      && !MmsSmsColumns.Types.isIdentityVerified(type)
      && !MmsSmsColumns.Types.isIdentityDefault(type);
  }

  public boolean isRead() { return readReceiptCount > 0; }

  public boolean isOutgoing() {
    return MmsSmsColumns.Types.isOutgoingMessageType(type);
  }
  public boolean isGroupUpdateMessage() {
    return SmsDatabase.Types.isGroupUpdateMessage(type);
  }
  public boolean isExpirationTimerUpdate() { return SmsDatabase.Types.isExpirationTimerUpdate(type); }
  public boolean isMediaSavedNotification() { return MmsSmsColumns.Types.isMediaSavedExtraction(type); }
  public boolean isScreenshotNotification() { return MmsSmsColumns.Types.isScreenshotExtraction(type); }
  public boolean isDataExtractionNotification() { return isMediaSavedNotification() || isScreenshotNotification(); }
  public boolean isOpenGroupInvitation() { return MmsSmsColumns.Types.isOpenGroupInvitation(type); }
  public boolean isCallLog() {
    return SmsDatabase.Types.isCallLog(type);
  }
  public boolean isIncomingCall() {
    return SmsDatabase.Types.isIncomingCall(type);
  }
  public boolean isOutgoingCall() {
    return SmsDatabase.Types.isOutgoingCall(type);
  }
  public boolean isMissedCall() {
    return SmsDatabase.Types.isMissedCall(type);
  }
  public boolean isDeleted() { return  MmsSmsColumns.Types.isDeletedMessage(type); }
  /*Hales63*/
  public boolean isMessageRequestResponse() { return  MmsSmsColumns.Types.isMessageRequestResponse(type); }

  //Payment Tag
  public boolean isPayment() { return MmsSmsColumns.Types.isPayment(type); }


  //New Line
  public boolean isFirstMissedCall() {
    return SmsDatabase.Types.isFirstMissedCall(type);
  }

  public boolean isControlMessage() {
    return isGroupUpdateMessage() || isExpirationTimerUpdate() || isDataExtractionNotification()
            || isMessageRequestResponse() || isCallLog();
  }
}
