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
package io.beldex.bchat.database;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.zetetic.database.sqlcipher.SQLiteDatabase;


import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.Util;
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper;
import io.beldex.bchat.database.model.MessageRecord;
import io.beldex.bchat.dependencies.DatabaseComponent;

import java.io.Closeable;

public class MmsSmsDatabase extends Database {

  @SuppressWarnings("unused")
  private static final String TAG = MmsSmsDatabase.class.getSimpleName();

  public static final String TRANSPORT     = "transport_type";
  public static final String MMS_TRANSPORT = "mms";
  public static final String SMS_TRANSPORT = "sms";

  private static final String[] PROJECTION = {MmsSmsColumns.ID, MmsSmsColumns.UNIQUE_ROW_ID,
          SmsDatabase.BODY, SmsDatabase.TYPE,
          MmsSmsColumns.THREAD_ID,
          SmsDatabase.ADDRESS, SmsDatabase.ADDRESS_DEVICE_ID, SmsDatabase.SUBJECT,
          MmsSmsColumns.NORMALIZED_DATE_SENT,
          MmsSmsColumns.NORMALIZED_DATE_RECEIVED,
          MmsDatabase.MESSAGE_TYPE, MmsDatabase.MESSAGE_BOX,
          SmsDatabase.STATUS,
          MmsSmsColumns.UNIDENTIFIED,
          MmsDatabase.PART_COUNT,
          MmsDatabase.CONTENT_LOCATION, MmsDatabase.TRANSACTION_ID,
          MmsDatabase.MESSAGE_SIZE, MmsDatabase.EXPIRY,
          MmsDatabase.STATUS,
          MmsSmsColumns.DELIVERY_RECEIPT_COUNT,
          MmsSmsColumns.READ_RECEIPT_COUNT,
          MmsSmsColumns.MISMATCHED_IDENTITIES,
          MmsDatabase.NETWORK_FAILURE,
          MmsSmsColumns.SUBSCRIPTION_ID,
          MmsSmsColumns.EXPIRES_IN,
          MmsSmsColumns.EXPIRE_STARTED,
          MmsSmsColumns.NOTIFIED,
          TRANSPORT,
          AttachmentDatabase.ATTACHMENT_JSON_ALIAS,
          MmsDatabase.QUOTE_ID,
          MmsDatabase.QUOTE_AUTHOR,
          MmsDatabase.QUOTE_BODY,
          MmsDatabase.QUOTE_MISSING,
          MmsDatabase.QUOTE_ATTACHMENT,
          MmsDatabase.SHARED_CONTACTS,
          MmsDatabase.LINK_PREVIEWS,
          ReactionDatabase.REACTION_JSON_ALIAS};


  public MmsSmsDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

  public @Nullable MessageRecord getMessageForTimestamp(long timestamp) {
    try (Cursor cursor = queryTables(PROJECTION, MmsSmsColumns.NORMALIZED_DATE_SENT + " = " + timestamp, true, null, null, null)) {
      MmsSmsDatabase.Reader reader = readerFor(cursor);
      return reader.getNext();
    }
  }

  public @Nullable MessageRecord getNonDeletedMessageForTimestamp(long timestamp) {
    String selection = MmsSmsColumns.NORMALIZED_DATE_SENT + " = " + timestamp;
    //includeReactions and additionalReactionSelection
    try (Cursor cursor = queryTables(PROJECTION, selection, true, null, null, null)) {
      MmsSmsDatabase.Reader reader = readerFor(cursor);
      return reader.getNext();
    }
  }

  public @Nullable MessageRecord getMessageFor(long timestamp, String serializedAuthor) {

    try (Cursor cursor = queryTables(PROJECTION, MmsSmsColumns.NORMALIZED_DATE_SENT + " = " + timestamp, true, null, null, null)) {
      MmsSmsDatabase.Reader reader = readerFor(cursor);

      MessageRecord messageRecord;

      while ((messageRecord = reader.getNext()) != null) {
        if ((Util.isOwnNumber(context, serializedAuthor) && messageRecord.isOutgoing()) ||
                (!Util.isOwnNumber(context, serializedAuthor)
                        && messageRecord.getIndividualRecipient().getAddress().serialize().equals(serializedAuthor)
                ))
        {
          return messageRecord;
        }
      }
    }

    return null;
  }

  public @Nullable MessageRecord getMessageFor(long timestamp, Address author) {
    return getMessageFor(timestamp, author.serialize());
  }
  public Cursor getConversation(long threadId, boolean reverse, long offset, long limit) {
    String order     = MmsSmsColumns.NORMALIZED_DATE_SENT + (reverse ? " DESC" : " ASC");
    String selection = MmsSmsColumns.THREAD_ID + " = " + threadId;
    String limitStr  = limit > 0 || offset > 0 ? offset + ", " + limit : null;

    Cursor cursor = queryTables(PROJECTION, selection, true, null, order, limitStr);
    setNotifyConverationListeners(cursor, threadId);

    return cursor;
  }

  public Cursor getConversation(long threadId, boolean reverse) {
    return getConversation(threadId, reverse, 0, 0);
  }

  public Cursor getConversationSnippet(long threadId) {
    String order     = MmsSmsColumns.NORMALIZED_DATE_RECEIVED + " DESC";
    String selection = MmsSmsColumns.THREAD_ID + " = " + threadId;

    return queryTables(PROJECTION, selection, true, null, order, null);
  }

  public long getLastMessageID(long threadId, boolean includeReactions, boolean getQuote) {
    String order     = MmsSmsColumns.NORMALIZED_DATE_SENT + " DESC";
    String selection = MmsSmsColumns.THREAD_ID + " = " + threadId;

    //includeReactions and additionalReactionSelection
    try (Cursor cursor = queryTables(PROJECTION, selection, includeReactions, null, order, "1")) {
      cursor.moveToFirst();
      return cursor.getLong(cursor.getColumnIndexOrThrow(MmsSmsColumns.ID));
    }
  }

  public long getLastMessageTimestamp(long threadId, boolean includeReactions, boolean getQuote) {
    String order     = MmsSmsColumns.NORMALIZED_DATE_SENT + " DESC";
    String selection = MmsSmsColumns.THREAD_ID + " = " + threadId;

    try (Cursor cursor = queryTables(PROJECTION, selection, includeReactions, null, order, "1")) {
      cursor.moveToFirst();
      return cursor.getLong(cursor.getColumnIndexOrThrow(MmsSmsColumns.NORMALIZED_DATE_SENT));
    }
  }

  public Cursor getUnread() {
    String order           = MmsSmsColumns.NORMALIZED_DATE_RECEIVED + " ASC";
    String selection       = "(" + MmsSmsColumns.READ + " = 0 OR " + MmsSmsColumns.REACTIONS_UNREAD + " = 1) AND " + MmsSmsColumns.NOTIFIED + " = 0";
    //includeReactions and additionalReactionSelection
    return queryTables(PROJECTION, selection,true, null, order, null);
  }

  public int getUnreadCount(long threadId) {
    String selection = MmsSmsColumns.READ + " = 0 AND " + MmsSmsColumns.NOTIFIED + " = 0 AND " + MmsSmsColumns.THREAD_ID + " = " + threadId;
    Cursor cursor    = queryTables(PROJECTION, selection, true, null, null, null);

    try {
      return cursor != null ? cursor.getCount() : 0;
    } finally {
      if (cursor != null) cursor.close();
    }
  }


  public int getConversationCount(long threadId) {
    int count = DatabaseComponent.get(context).smsDatabase().getMessageCountForThread(threadId);
    count    += DatabaseComponent.get(context).mmsDatabase().getMessageCountForThread(threadId);

    return count;
  }

  public void incrementDeliveryReceiptCount(MessagingDatabase.SyncMessageId syncMessageId, long timestamp) {
    DatabaseComponent.get(context).smsDatabase().incrementReceiptCount(syncMessageId, true, false);
    DatabaseComponent.get(context).mmsDatabase().incrementReceiptCount(syncMessageId, timestamp, true, false);
  }

  public void incrementReadReceiptCount(MessagingDatabase.SyncMessageId syncMessageId, long timestamp) {
    DatabaseComponent.get(context).smsDatabase().incrementReceiptCount(syncMessageId, false, true);
    DatabaseComponent.get(context).mmsDatabase().incrementReceiptCount(syncMessageId, timestamp, false, true);
  }

  public int getQuotedMessagePosition(long threadId, long quoteId, @NonNull Address address) {
    String order     = MmsSmsColumns.NORMALIZED_DATE_RECEIVED + " DESC";
    String selection = MmsSmsColumns.THREAD_ID + " = " + threadId;
    //includeReactions and additionalReactionSelection
    try (Cursor cursor = queryTables(new String[]{ MmsSmsColumns.NORMALIZED_DATE_SENT, MmsSmsColumns.ADDRESS }, selection, true, null, order, null)) {
      String  serializedAddress = address.serialize();
      boolean isOwnNumber       = Util.isOwnNumber(context, address.serialize());

      while (cursor != null && cursor.moveToNext()) {
        boolean quoteIdMatches = cursor.getLong(0) == quoteId;
        boolean addressMatches = serializedAddress.equals(cursor.getString(1));

        if (quoteIdMatches && (addressMatches || isOwnNumber)) {
          return cursor.getPosition();
        }
      }
    }
    return -1;
  }

  public int getMessagePositionInConversation(long threadId, long sentTimestamp, @NonNull Address address) {
    String order     = MmsSmsColumns.NORMALIZED_DATE_SENT + " DESC";
    String selection = MmsSmsColumns.THREAD_ID + " = " + threadId;

    try (Cursor cursor = queryTables(new String[]{ MmsSmsColumns.NORMALIZED_DATE_SENT, MmsSmsColumns.ADDRESS }, selection, true, null, order, null)) {
      String  serializedAddress = address.serialize();
      boolean isOwnNumber       = Util.isOwnNumber(context, address.serialize());

      while (cursor != null && cursor.moveToNext()) {
        boolean timestampMatches = cursor.getLong(0) == sentTimestamp;
        boolean addressMatches   = serializedAddress.equals(cursor.getString(1));

        if (timestampMatches && (addressMatches || isOwnNumber)) {
          return cursor.getPosition();
        }
      }
    }
    return -1;
  }

  private Cursor queryTables(
          @NonNull String[] projection,
          @Nullable String selection,
          boolean includeReactions,
          @Nullable String additionalReactionSelection,
          @Nullable String order,
          @Nullable String limit) {
    SQLiteDatabase db = databaseHelper.getReadableDatabase();
    String query = MmsSmsDatabaseSQLKt.buildMmsSmsCombinedQuery(projection,
            selection,
            includeReactions,
            additionalReactionSelection,
            order,
            limit);
    return db.rawQuery(query, null);
  }

  public Reader readerFor(@NonNull Cursor cursor) {
    return new Reader(cursor);
  }

  public class Reader implements Closeable {

    private final Cursor                 cursor;
    private       SmsDatabase.Reader     smsReader;
    private       MmsDatabase.Reader     mmsReader;

    public Reader(Cursor cursor) {
      this.cursor = cursor;
    }

    private SmsDatabase.Reader getSmsReader() {
      if (smsReader == null) {
        smsReader = DatabaseComponent.get(context).smsDatabase().readerFor(cursor);
      }

      return smsReader;
    }

    private MmsDatabase.Reader getMmsReader() {
      if (mmsReader == null) {
        mmsReader = DatabaseComponent.get(context).mmsDatabase().readerFor(cursor);
      }

      return mmsReader;
    }

    public MessageRecord getNext() {
      if (cursor == null || !cursor.moveToNext())
        return null;

      return getCurrent();
    }

    public MessageRecord getCurrent() {
      String type = cursor.getString(cursor.getColumnIndexOrThrow(TRANSPORT));

      if      (MmsSmsDatabase.MMS_TRANSPORT.equals(type)) return getMmsReader().getCurrent();
      else if (MmsSmsDatabase.SMS_TRANSPORT.equals(type)) return getSmsReader().getCurrent();
      else                                                throw new AssertionError("Bad type: " + type);
    }

    public void close() {
      if (cursor != null) {
        cursor.close();
      }
    }
  }
}
