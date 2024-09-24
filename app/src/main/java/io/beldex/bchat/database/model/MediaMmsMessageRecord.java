/**
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
import androidx.annotation.Nullable;

import io.beldex.bchat.database.MmsDatabase;
import io.beldex.bchat.database.SmsDatabase;
import io.beldex.bchat.mms.SlideDeck;
import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview;
import com.beldex.libbchat.utilities.Contact;
import com.beldex.libbchat.utilities.IdentityKeyMismatch;
import com.beldex.libbchat.utilities.NetworkFailure;
import com.beldex.libbchat.utilities.recipients.Recipient;

import java.util.List;
import io.beldex.bchat.R;

/**
 * Represents the message record model for MMS messages that contain
 * media (ie: they've been downloaded).
 *
 * @author Moxie Marlinspike
 *
 */

public class MediaMmsMessageRecord extends MmsMessageRecord {
  private final int partCount;

  public MediaMmsMessageRecord(long id, Recipient conversationRecipient,
    Recipient individualRecipient, int recipientDeviceId,
    long dateSent, long dateReceived, int deliveryReceiptCount,
    long threadId, String body,
    @NonNull SlideDeck slideDeck,
    int partCount, long mailbox,
    List<IdentityKeyMismatch> mismatches,
    List<NetworkFailure> failures, int subscriptionId,
    long expiresIn, long expireStarted, int readReceiptCount,
    @Nullable Quote quote, @NonNull List<Contact> contacts,
    @NonNull List<LinkPreview> linkPreviews, boolean unidentified)
  {
    super(id, body, conversationRecipient, individualRecipient, dateSent,
      dateReceived, threadId, SmsDatabase.Status.STATUS_NONE, deliveryReceiptCount, mailbox, mismatches, failures,
      expiresIn, expireStarted, slideDeck, readReceiptCount, quote, contacts,
      linkPreviews, unidentified);
    this.partCount = partCount;
  }

  public int getPartCount() {
    return partCount;
  }

  @Override
  public boolean isMmsNotification() {
    return false;
  }

  @Override
  public SpannableString getDisplayBody(@NonNull Context context) {
    if (MmsDatabase.Types.isFailedDecryptType(type)) {
      return emphasisAdded(context.getString(R.string.MmsMessageRecord_bad_encrypted_mms_message));
    } else if (MmsDatabase.Types.isDuplicateMessageType(type)) {
      return emphasisAdded(context.getString(R.string.SmsMessageRecord_duplicate_message));
    } else if (MmsDatabase.Types.isNoRemoteBchatType(type)) {
      return emphasisAdded(context.getString(R.string.MmsMessageRecord_mms_message_encrypted_for_non_existing_bchat));
    }

    return super.getDisplayBody(context);
  }
}
