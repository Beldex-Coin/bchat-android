/*
 * Copyright (C) 2011 Whisper Systems
 * Copyright (C) 2013 - 2017 Open Whisper Systems
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Pair;


import com.annimon.stream.Stream;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteStatement;


import com.beldex.libbchat.messaging.MessagingModuleConfiguration;
import com.beldex.libbchat.messaging.calls.CallMessageType;
import com.beldex.libbchat.messaging.messages.signal.IncomingGroupMessage;
import com.beldex.libbchat.messaging.messages.signal.IncomingTextMessage;
import com.beldex.libbchat.messaging.messages.signal.OutgoingTextMessage;
import com.beldex.libbchat.messaging.sending_receiving.MessageDecrypter;

import com.beldex.libbchat.mnode.MnodeAPI;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.IdentityKeyMismatch;
import com.beldex.libbchat.utilities.IdentityKeyMismatchList;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libsignal.utilities.JsonUtil;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.utilities.guava.Optional;
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper;
import io.beldex.bchat.database.model.MessageRecord;
import io.beldex.bchat.database.model.SmsMessageRecord;
import io.beldex.bchat.dependencies.DatabaseComponent;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Database for storage of SMS messages.
 *
 * @author Moxie Marlinspike
 */
public class SmsDatabase extends MessagingDatabase {


  byte[]        ciphertext;
  private static final String TAG = SmsDatabase.class.getSimpleName();

  public  static final String TABLE_NAME         = "sms";
  public  static final String PERSON             = "person";
  static final String DATE_RECEIVED      = "date";
  static final String DATE_SENT          = "date_sent";
  public  static final String PROTOCOL           = "protocol";
  public  static final String STATUS             = "status";
  public  static final String TYPE               = "type";
  public  static final String REPLY_PATH_PRESENT = "reply_path_present";
  public  static final String SUBJECT            = "subject";
  public  static final String SERVICE_CENTER     = "service_center";

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " integer PRIMARY KEY, "                +
          THREAD_ID + " INTEGER, " + ADDRESS + " TEXT, " + ADDRESS_DEVICE_ID + " INTEGER DEFAULT 1, " + PERSON + " INTEGER, " +
          DATE_RECEIVED  + " INTEGER, " + DATE_SENT + " INTEGER, " + PROTOCOL + " INTEGER, " + READ + " INTEGER DEFAULT 0, " +
          STATUS + " INTEGER DEFAULT -1," + TYPE + " INTEGER, " + REPLY_PATH_PRESENT + " INTEGER, " +
          DELIVERY_RECEIPT_COUNT + " INTEGER DEFAULT 0," + SUBJECT + " TEXT, " + BODY + " TEXT, " +
          MISMATCHED_IDENTITIES + " TEXT DEFAULT NULL, " + SERVICE_CENTER + " TEXT, " + SUBSCRIPTION_ID + " INTEGER DEFAULT -1, " +
          EXPIRES_IN + " INTEGER DEFAULT 0, " + EXPIRE_STARTED + " INTEGER DEFAULT 0, " + NOTIFIED + " DEFAULT 0, " +
          READ_RECEIPT_COUNT + " INTEGER DEFAULT 0, " + UNIDENTIFIED + " INTEGER DEFAULT 0);";

  public static final String[] CREATE_INDEXS = {
          "CREATE INDEX IF NOT EXISTS sms_thread_id_index ON " + TABLE_NAME + " (" + THREAD_ID + ");",
          "CREATE INDEX IF NOT EXISTS sms_read_index ON " + TABLE_NAME + " (" + READ + ");",
          "CREATE INDEX IF NOT EXISTS sms_read_and_notified_and_thread_id_index ON " + TABLE_NAME + "(" + READ + "," + NOTIFIED + ","  + THREAD_ID + ");",
          "CREATE INDEX IF NOT EXISTS sms_type_index ON " + TABLE_NAME + " (" + TYPE + ");",
          "CREATE INDEX IF NOT EXISTS sms_date_sent_index ON " + TABLE_NAME + " (" + DATE_SENT + ");",
          "CREATE INDEX IF NOT EXISTS sms_thread_date_index ON " + TABLE_NAME + " (" + THREAD_ID + ", " + DATE_RECEIVED + ");"
  };

  private static final String[] MESSAGE_PROJECTION = new String[] {
          ID, THREAD_ID, ADDRESS, ADDRESS_DEVICE_ID, PERSON,
          DATE_RECEIVED + " AS " + NORMALIZED_DATE_RECEIVED,
          DATE_SENT + " AS " + NORMALIZED_DATE_SENT,
          PROTOCOL, READ, STATUS, TYPE,
          REPLY_PATH_PRESENT, SUBJECT, BODY, SERVICE_CENTER, DELIVERY_RECEIPT_COUNT,
          MISMATCHED_IDENTITIES, SUBSCRIPTION_ID, EXPIRES_IN, EXPIRE_STARTED,
          NOTIFIED, READ_RECEIPT_COUNT, UNIDENTIFIED
  };

  private static final EarlyReceiptCache earlyDeliveryReceiptCache = new EarlyReceiptCache();
  private static final EarlyReceiptCache earlyReadReceiptCache     = new EarlyReceiptCache();

  public SmsDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

  protected String getTableName() {
    return TABLE_NAME;
  }

  private void updateTypeBitmask(long id, long maskOff, long maskOn) {
    Log.i("MessageDatabase", "Updating ID: " + id + " to base type: " + maskOn);

    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.execSQL("UPDATE " + TABLE_NAME +
            " SET " + TYPE + " = (" + TYPE + " & " + (Types.TOTAL_MASK - maskOff) + " | " + maskOn + " )" +
            " WHERE " + ID + " = ?", new String[] {id+""});

    long threadId = getThreadIdForMessage(id);

    DatabaseComponent.get(context).threadDatabase().update(threadId, false);
    notifyConversationListeners(threadId);
  }

  public long getThreadIdForMessage(long id) {
    String sql        = "SELECT " + THREAD_ID + " FROM " + TABLE_NAME + " WHERE " + ID + " = ?";
    String[] sqlArgs  = new String[] {id+""};
    SQLiteDatabase db = databaseHelper.getReadableDatabase();

    Cursor cursor = null;

    try {
      cursor = db.rawQuery(sql, sqlArgs);
      if (cursor != null && cursor.moveToFirst())
        return cursor.getLong(0);
      else
        return -1;
    } finally {
      if (cursor != null)
        cursor.close();
    }
  }
  public int getMessageCountForThread(long threadId) {
    SQLiteDatabase db = databaseHelper.getReadableDatabase();
    Cursor cursor     = null;

    try {
      cursor = db.query(TABLE_NAME, new String[] {"COUNT(*)"}, THREAD_ID + " = ?",
              new String[] {threadId+""}, null, null, null);

      if (cursor != null && cursor.moveToFirst())
        return cursor.getInt(0);
    } finally {
      if (cursor != null)
        cursor.close();
    }

    return 0;
  }

  public void markAsDecryptFailed(long id) {
    updateTypeBitmask(id, Types.ENCRYPTION_MASK, Types.ENCRYPTION_REMOTE_FAILED_BIT);
  }

  @Override
  public void markAsSent(long id, boolean isSecure) {
    updateTypeBitmask(id, Types.BASE_TYPE_MASK, Types.BASE_SENT_TYPE | (isSecure ? Types.PUSH_MESSAGE_BIT | Types.SECURE_MESSAGE_BIT : 0));
  }

  public void markAsSending(long id) {
    updateTypeBitmask(id, Types.BASE_TYPE_MASK, Types.BASE_SENDING_TYPE);
  }

  @Override
  public void markUnidentified(long id, boolean unidentified) {
    ContentValues contentValues = new ContentValues(1);
    contentValues.put(UNIDENTIFIED, unidentified ? 1 : 0);

    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.update(TABLE_NAME, contentValues, ID_WHERE, new String[] {String.valueOf(id)});
  }

  @Override
  public void markAsDeleted(long messageId, boolean read) {
    SQLiteDatabase database     = databaseHelper.getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    contentValues.put(READ, 1);
    contentValues.put(BODY, "");
    contentValues.put(STATUS, Status.STATUS_NONE);
    database.update(TABLE_NAME, contentValues, ID_WHERE, new String[] {String.valueOf(messageId)});
    long threadId = getThreadIdForMessage(messageId);
    if (!read) { DatabaseComponent.get(context).threadDatabase().decrementUnread(threadId, 1); }
    updateTypeBitmask(messageId, Types.BASE_TYPE_MASK, Types.BASE_DELETED_TYPE);
  }

  @Override
  public void markExpireStarted(long id) {
    markExpireStarted(id, MnodeAPI.getNowWithOffset());
  }

  @Override
  public void markExpireStarted(long id, long startedAtTimestamp) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(EXPIRE_STARTED, startedAtTimestamp);

    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.update(TABLE_NAME, contentValues, ID_WHERE, new String[] {String.valueOf(id)});

    long threadId = getThreadIdForMessage(id);

    DatabaseComponent.get(context).threadDatabase().update(threadId, false);
    notifyConversationListeners(threadId);
  }

  public void markAsSentFailed(long id) {
    updateTypeBitmask(id, Types.BASE_TYPE_MASK, Types.BASE_SENT_FAILED_TYPE);
  }

  public void markAsNotified(long id) {
    SQLiteDatabase database      = databaseHelper.getWritableDatabase();
    ContentValues  contentValues = new ContentValues();

    contentValues.put(NOTIFIED, 1);

    database.update(TABLE_NAME, contentValues, ID_WHERE, new String[] {String.valueOf(id)});
  }

  public boolean isOutgoingMessage(long timestamp) {
    SQLiteDatabase database     = databaseHelper.getWritableDatabase();
    Cursor         cursor       = null;
    boolean        isOutgoing   = false;

    try {
      cursor = database.query(TABLE_NAME, new String[] { ID, THREAD_ID, ADDRESS, TYPE },
              DATE_SENT + " = ?", new String[] { String.valueOf(timestamp) },
              null, null, null, null);

      while (cursor.moveToNext()) {
        if (Types.isOutgoingMessageType(cursor.getLong(cursor.getColumnIndexOrThrow(TYPE)))) {
          isOutgoing = true;
        }
      }
    } finally {
      if (cursor != null) cursor.close();
    }

    return isOutgoing;
  }

  public boolean isDeletedMessage(long timestamp) {
    SQLiteDatabase database     = databaseHelper.getWritableDatabase();
    Cursor         cursor       = null;
    boolean        isDeleted   = false;
    try {
      cursor = database.query(TABLE_NAME, new String[] { ID, THREAD_ID, ADDRESS, TYPE },
              DATE_SENT + " = ?", new String[] { String.valueOf(timestamp) },
              null, null, null, null);
      while (cursor.moveToNext()) {
        if (Types.isDeletedMessage(cursor.getLong(cursor.getColumnIndexOrThrow(TYPE)))) {
          isDeleted = true;
        }
      }
    } finally {
      if (cursor != null) cursor.close();
    }
    return isDeleted;
  }

  public void incrementReceiptCount(SyncMessageId messageId, boolean deliveryReceipt, boolean readReceipt) {
    SQLiteDatabase database     = databaseHelper.getWritableDatabase();
    Cursor         cursor       = null;
    boolean        foundMessage = false;

    try {
      cursor = database.query(TABLE_NAME, new String[] {ID, THREAD_ID, ADDRESS, TYPE},
              DATE_SENT + " = ?", new String[] {String.valueOf(messageId.getTimetamp())},
              null, null, null, null);

      while (cursor.moveToNext()) {
        if (Types.isOutgoingMessageType(cursor.getLong(cursor.getColumnIndexOrThrow(TYPE)))) {
          Address theirAddress = messageId.getAddress();
          Address ourAddress   = Address.fromSerialized(cursor.getString(cursor.getColumnIndexOrThrow(ADDRESS)));
          String  columnName   = deliveryReceipt ? DELIVERY_RECEIPT_COUNT : READ_RECEIPT_COUNT;

          if (ourAddress.equals(theirAddress)) {
            long threadId = cursor.getLong(cursor.getColumnIndexOrThrow(THREAD_ID));

            database.execSQL("UPDATE " + TABLE_NAME +
                            " SET " + columnName + " = " + columnName + " + 1 WHERE " +
                            ID + " = ?",
                    new String[] {String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(ID)))});

            DatabaseComponent.get(context).threadDatabase().update(threadId, false);
            notifyConversationListeners(threadId);
            foundMessage = true;
          }
        }
      }

      if (!foundMessage) {
        if (deliveryReceipt) earlyDeliveryReceiptCache.increment(messageId.getTimetamp(), messageId.getAddress());
        if (readReceipt)     earlyReadReceiptCache.increment(messageId.getTimetamp(), messageId.getAddress());
      }

    } finally {
      if (cursor != null)
        cursor.close();
    }
  }

  public List<MarkedMessageInfo> setMessagesRead(long threadId) {
    return setMessagesRead(THREAD_ID + " = ? AND " + READ + " = 0", new String[] {String.valueOf(threadId)});
  }

  public List<MarkedMessageInfo> setAllMessagesRead() {
    return setMessagesRead(READ + " = 0", null);
  }

  private List<MarkedMessageInfo> setMessagesRead(String where, String[] arguments) {
    SQLiteDatabase          database  = databaseHelper.getWritableDatabase();
    List<MarkedMessageInfo> results   = new LinkedList<>();
    Cursor                  cursor    = null;

    database.beginTransaction();
    try {
      cursor = database.query(TABLE_NAME, new String[] {ID, ADDRESS, DATE_SENT, TYPE, EXPIRES_IN, EXPIRE_STARTED}, where, arguments, null, null, null);

      while (cursor != null && cursor.moveToNext()) {
        if (Types.isSecureType(cursor.getLong(3))) {
          SyncMessageId  syncMessageId  = new SyncMessageId(Address.fromSerialized(cursor.getString(1)), cursor.getLong(2));
          ExpirationInfo expirationInfo = new ExpirationInfo(cursor.getLong(0), cursor.getLong(4), cursor.getLong(5), false);

          results.add(new MarkedMessageInfo(syncMessageId, expirationInfo));
        }
      }

      ContentValues contentValues = new ContentValues();
      contentValues.put(READ, 1);

      database.update(TABLE_NAME, contentValues, where, arguments);
      database.setTransactionSuccessful();
    } finally {
      if (cursor != null) cursor.close();
      database.endTransaction();
    }

    return results;
  }

  public void updateSentTimestamp(long messageId, long newTimestamp, long threadId) {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.execSQL("UPDATE " + TABLE_NAME + " SET " + DATE_SENT + " = ? " +
                    "WHERE " + ID + " = ?",
            new String[] {newTimestamp + "", messageId + ""});
    notifyConversationListeners(threadId);
    notifyConversationListListeners();
  }

  public Pair<Long, Long> updateBundleMessageBody(long messageId, String body) {
    long type = Types.BASE_INBOX_TYPE | Types.SECURE_MESSAGE_BIT | Types.PUSH_MESSAGE_BIT;
    return updateMessageBodyAndType(messageId, body, Types.TOTAL_MASK, type);
  }

  private Pair<Long, Long> updateMessageBodyAndType(long messageId, String body, long maskOff, long maskOn) {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.execSQL("UPDATE " + TABLE_NAME + " SET " + BODY + " = ?, " +
                    TYPE + " = (" + TYPE + " & " + (Types.TOTAL_MASK - maskOff) + " | " + maskOn + ") " +
                    "WHERE " + ID + " = ?",
            new String[] {body, messageId + ""});

    long threadId = getThreadIdForMessage(messageId);

    DatabaseComponent.get(context).threadDatabase().update(threadId, true);
    notifyConversationListeners(threadId);
    notifyConversationListListeners();

    return new Pair<>(messageId, threadId);
  }

  protected Optional<InsertResult> insertMessageInbox(IncomingTextMessage message, long type, long serverTimestamp,boolean runIncrement,boolean runThreadUpdate) {
    if (message.isSecureMessage()) {
      type |= Types.SECURE_MESSAGE_BIT;
    } else if (message.isGroup()) {
      type |= Types.SECURE_MESSAGE_BIT;
      if (((IncomingGroupMessage)message).isUpdateMessage()) type |= Types.GROUP_UPDATE_MESSAGE_BIT;
    }

    if (message.isPush()) type |= Types.PUSH_MESSAGE_BIT;

    if (message.isOpenGroupInvitation()) type |= Types.OPEN_GROUP_INVITATION_BIT;

    //Payment Tag
    if (message.isPayment())             type |= Types.PAYMENT_BIT;

    CallMessageType callMessageType = message.getCallType();
    if (callMessageType != null) {
      switch (callMessageType) {
        case CALL_OUTGOING:
          type |= Types.OUTGOING_CALL_TYPE;
          break;
        case CALL_INCOMING:
          type |= Types.INCOMING_CALL_TYPE;
          break;
        case CALL_MISSED:
          type |= Types.MISSED_CALL_TYPE;
          break;
        case CALL_FIRST_MISSED:
          type |= Types.FIRST_MISSED_CALL_TYPE;
          break;
      }
    }

    Recipient recipient = Recipient.from(context, message.getSender(), true);

    Recipient groupRecipient;

    if (message.getGroupId() == null) {
      groupRecipient = null;
    } else {
      groupRecipient = Recipient.from(context, message.getGroupId(), true);
    }

    boolean    unread     = (Util.isDefaultSmsProvider(context) ||
            message.isSecureMessage() || message.isGroup() || message.isCallInfo());

    long       threadId;

    if (groupRecipient == null) threadId = DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(recipient);
    else                        threadId = DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(groupRecipient);

    ContentValues values = new ContentValues(6);
    values.put(ADDRESS, message.getSender().serialize());
    Log.d("Beldex","insertMessageInbox message.getSender().serialize() " + message.getSender().serialize() );
    values.put(ADDRESS_DEVICE_ID,  message.getSenderDeviceId());
    // In social groups messages should be sorted by their server timestamp
    long receivedTimestamp = serverTimestamp;
    if (serverTimestamp == 0) { receivedTimestamp = message.getSentTimestampMillis(); }
    values.put(DATE_RECEIVED, receivedTimestamp); // Beldex - This is important due to how we handle GIFs
    values.put(DATE_SENT, message.getSentTimestampMillis());
    values.put(PROTOCOL, message.getProtocol());
    values.put(READ, unread ? 0 : 1);
    values.put(SUBSCRIPTION_ID, message.getSubscriptionId());
    values.put(EXPIRES_IN, message.getExpiresIn());
    values.put(UNIDENTIFIED, message.isUnidentified());

    if (!TextUtils.isEmpty(message.getPseudoSubject()))
      values.put(SUBJECT, message.getPseudoSubject());

    values.put(REPLY_PATH_PRESENT, message.isReplyPathPresent());
    values.put(SERVICE_CENTER, message.getServiceCenterAddress());
    values.put(BODY, message.getMessageBody());
    Log.d("Beldex","insertMessageInbox message.getMessageBody() " + message.getMessageBody() );
    values.put(TYPE, type);
    values.put(THREAD_ID, threadId);



    if (message.isPush() && isDuplicate(message, threadId)) {
      Log.w(TAG, "Duplicate message (" + message.getSentTimestampMillis() + "), ignoring...");
      return Optional.absent();
    } else {
      SQLiteDatabase db        = databaseHelper.getWritableDatabase();
      long           messageId = db.insert(TABLE_NAME, null, values);

      if (unread && runIncrement) {
        DatabaseComponent.get(context).threadDatabase().incrementUnread(threadId, 1);
      }

      if (runThreadUpdate) {
        DatabaseComponent.get(context).threadDatabase().update(threadId, true);
      }

      if (message.getSubscriptionId() != -1) {
        DatabaseComponent.get(context).recipientDatabase().setDefaultSubscriptionId(recipient, message.getSubscriptionId());
      }

      notifyConversationListeners(threadId);

      return Optional.of(new InsertResult(messageId, threadId));
    }
  }

  public Optional<InsertResult> insertMessageInbox(IncomingTextMessage message,boolean runIncrement, boolean runThreadUpdate) {
    //Important For Group Message
    return insertMessageInbox(message, Types.BASE_INBOX_TYPE, 0,runIncrement,runThreadUpdate);
  }

  //New Line
  public Optional<InsertResult> insertCallMessage(IncomingTextMessage message) {
    return insertMessageInbox(message, 0, 0, true, true);
  }

  public Optional<InsertResult> insertMessageInboxNew(IncomingTextMessage message, long serverTimestamp,boolean runIncrement, boolean runThreadUpdate) {
    return insertMessageInbox(message, Types.BASE_INBOX_TYPE, serverTimestamp,runIncrement,runThreadUpdate);
  }

  public Optional<InsertResult> insertMessageOutboxNew(long threadId, OutgoingTextMessage message, long serverTimestamp,boolean runThreadUpdate) {
    if (threadId == -1) {
      threadId = DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(message.getRecipient());
    }
    long messageId = insertMessageOutbox(threadId, message, false, serverTimestamp, null,runThreadUpdate);
    if (messageId == -1) {
      return Optional.absent();
    }
    markAsSent(messageId, true);
    return Optional.fromNullable(new InsertResult(messageId, threadId));
  }

  public long insertMessageOutbox(long threadId, OutgoingTextMessage message,
                                  boolean forceSms, long date, InsertListener insertListener,boolean runThreadUpdate)
  {
    long type = Types.BASE_SENDING_TYPE;

    if (message.isSecureMessage())       type |= (Types.SECURE_MESSAGE_BIT | Types.PUSH_MESSAGE_BIT);
    if (forceSms)                        type |= Types.MESSAGE_FORCE_SMS_BIT;
    if (message.isOpenGroupInvitation()) type |= Types.OPEN_GROUP_INVITATION_BIT;
    //Payment Tag
    if (message.isPayment())             type |= Types.PAYMENT_BIT;

    Address            address               = message.getRecipient().getAddress();
    Map<Address, Long> earlyDeliveryReceipts = earlyDeliveryReceiptCache.remove(date);
    Map<Address, Long> earlyReadReceipts     = earlyReadReceiptCache.remove(date);

    ContentValues contentValues = new ContentValues(6);
    contentValues.put(ADDRESS, address.serialize());
    Log.d("Beldex","insertMessageOutbox message.getRecipient().getAddress() " + address.serialize());
    contentValues.put(THREAD_ID, threadId);
    contentValues.put(BODY, message.getMessageBody());
    Log.d("Beldex","insertMessageOutbox message.getMessageBody() " + message.getMessageBody());
    contentValues.put(DATE_RECEIVED, MnodeAPI.getNowWithOffset());
    contentValues.put(DATE_SENT, message.getSentTimestampMillis());
    contentValues.put(READ, 1);
    contentValues.put(TYPE, type);
    contentValues.put(SUBSCRIPTION_ID, message.getSubscriptionId());
    contentValues.put(EXPIRES_IN, message.getExpiresIn());
    contentValues.put(DELIVERY_RECEIPT_COUNT, Stream.of(earlyDeliveryReceipts.values()).mapToLong(Long::longValue).sum());
    contentValues.put(READ_RECEIPT_COUNT, Stream.of(earlyReadReceipts.values()).mapToLong(Long::longValue).sum());
    //New Line
    //contentValues.put(BELDEX_ADDRESS,beldexAddress);

    if (isDuplicate(message, threadId)) {
      Log.w(TAG, "Duplicate message (" + message.getSentTimestampMillis() + "), ignoring...");
      return -1;
    }

    SQLiteDatabase db        = databaseHelper.getWritableDatabase();
    long           messageId = db.insert(TABLE_NAME, ADDRESS, contentValues);
    if (insertListener != null) {
      insertListener.onComplete();
    }
    /*if (runThreadUpdate) {
      DatabaseComponent.get(context).threadDatabase().update(threadId, true);
    }*/
    DatabaseComponent.get(context).threadDatabase().setLastSeen(threadId);

    DatabaseComponent.get(context).threadDatabase().setHasSent(threadId, true);

    notifyConversationListeners(threadId);


    return messageId;
  }

  public Cursor getExpirationStartedMessages() {
    String         where = EXPIRE_STARTED + " > 0";
    SQLiteDatabase db    = databaseHelper.getReadableDatabase();
    return db.query(TABLE_NAME, MESSAGE_PROJECTION, where, null, null, null, null);
  }

  public SmsMessageRecord getMessage(long messageId) throws NoSuchMessageException {
    SQLiteDatabase db     = databaseHelper.getReadableDatabase();
    Cursor         cursor = db.query(TABLE_NAME, MESSAGE_PROJECTION, ID_WHERE, new String[]{messageId + ""}, null, null, null);
    Reader         reader = new Reader(cursor);
    SmsMessageRecord record = reader.getNext();

    reader.close();

    if (record == null) throw new NoSuchMessageException("No message for ID: " + messageId);
    else                return record;
  }

  public Cursor getMessageCursor(long messageId) {
    SQLiteDatabase db = databaseHelper.getReadableDatabase();
    Cursor cursor = db.query(TABLE_NAME, MESSAGE_PROJECTION, ID_WHERE, new String[] {messageId + ""}, null, null, null);
    setNotifyConverationListeners(cursor, getThreadIdForMessage(messageId));
    return cursor;
  }

  @Override
  public boolean deleteMessage(long messageId) {
    Log.i("MessageDatabase", "Deleting: " + messageId);
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    long threadId = getThreadIdForMessage(messageId);
    db.delete(TABLE_NAME, ID_WHERE, new String[] {messageId+""});
    boolean threadDeleted = DatabaseComponent.get(context).threadDatabase().update(threadId, false);
    notifyConversationListeners(threadId);
    return threadDeleted;
  }

  @Override
  public boolean deleteMessages(long[] messageIds, long threadId) {
    String[] argsArray = new String[messageIds.length];
    String[] argValues = new String[messageIds.length];
    Arrays.fill(argsArray, "?");

    for (int i = 0; i < messageIds.length; i++) {
      argValues[i] = (messageIds[i] + "");
    }

    String combinedMessageIdArgss = StringUtils.join(messageIds, ',');
    String combinedMessageIds = StringUtils.join(messageIds, ',');
    Log.i("MessageDatabase", "Deleting: " + combinedMessageIds);
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.delete(
            TABLE_NAME,
            ID + " IN (" + StringUtils.join(argsArray, ',') + ")",
            argValues
    );
    boolean threadDeleted = DatabaseComponent.get(context).threadDatabase().update(threadId, false);
    notifyConversationListeners(threadId);
    return threadDeleted;
  }


  private boolean isDuplicate(IncomingTextMessage message, long threadId) {
    SQLiteDatabase database = databaseHelper.getReadableDatabase();
    Cursor         cursor   = database.query(TABLE_NAME, null, DATE_SENT + " = ? AND " + ADDRESS + " = ? AND " + THREAD_ID + " = ?",
            new String[]{String.valueOf(message.getSentTimestampMillis()), message.getSender().serialize(), String.valueOf(threadId)},
            null, null, null, "1");

    try {
      return cursor != null && cursor.moveToFirst();
    } finally {
      if (cursor != null) cursor.close();
    }
  }

  private boolean isDuplicate(OutgoingTextMessage message, long threadId) {
    SQLiteDatabase database = databaseHelper.getReadableDatabase();
    Cursor         cursor   = database.query(TABLE_NAME, null, DATE_SENT + " = ? AND " + ADDRESS + " = ? AND " + THREAD_ID + " = ?",
            new String[]{String.valueOf(message.getSentTimestampMillis()), message.getRecipient().getAddress().serialize(), String.valueOf(threadId)},
            null, null, null, "1");

    try {
      return cursor != null && cursor.moveToFirst();
    } finally {
      if (cursor != null) cursor.close();
    }
  }

  /*package */void deleteThread(long threadId) {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.delete(TABLE_NAME, THREAD_ID + " = ?", new String[] {threadId+""});
  }

  /*package*/void deleteMessagesInThreadBeforeDate(long threadId, long date) {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    String where      = THREAD_ID + " = ? AND (CASE " + TYPE;

    for (long outgoingType : Types.OUTGOING_MESSAGE_TYPES) {
      where += " WHEN " + outgoingType + " THEN " + DATE_SENT + " < " + date;
    }

    where += (" ELSE " + DATE_RECEIVED + " < " + date + " END)");

    db.delete(TABLE_NAME, where, new String[] {threadId + ""});
  }

  /*package*/ void deleteThreads(Set<Long> threadIds) {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    String where      = "";

    for (long threadId : threadIds) {
      where += THREAD_ID + " = '" + threadId + "' OR ";
    }

    where = where.substring(0, where.length() - 4);

    db.delete(TABLE_NAME, where, null);
  }

  /*package */ void deleteAllThreads() {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.delete(TABLE_NAME, null, null);
  }

  /*package*/ SQLiteDatabase beginTransaction() {
    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    database.beginTransaction();
    return database;
  }

  /*package*/ void endTransaction(SQLiteDatabase database) {
    database.setTransactionSuccessful();
    database.endTransaction();
  }

  /*package*/ SQLiteStatement createInsertStatement(SQLiteDatabase database) {
    return database.compileStatement("INSERT INTO " + TABLE_NAME + " (" + ADDRESS + ", " +
            PERSON + ", " +
            DATE_SENT + ", " +
            DATE_RECEIVED  + ", " +
            PROTOCOL + ", " +
            READ + ", " +
            STATUS + ", " +
            TYPE + ", " +
            REPLY_PATH_PRESENT + ", " +
            SUBJECT + ", " +
            BODY + ", " +
            SERVICE_CENTER +
            ", " + THREAD_ID + ") " +
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

  }

  public static class Status {
    public static final int STATUS_NONE     = -1;
    public static final int STATUS_COMPLETE  = 0;
    public static final int STATUS_PENDING   = 0x20;
    public static final int STATUS_FAILED    = 0x40;
  }

  public Reader readerFor(Cursor cursor) {
    return new Reader(cursor);
  }

  public OutgoingMessageReader readerFor(OutgoingTextMessage message, long threadId) {
    return new OutgoingMessageReader(message, threadId);
  }

  public class OutgoingMessageReader {

    private final OutgoingTextMessage message;
    private final long                id;
    private final long                threadId;

    private  String                   beldexAddress;

    public OutgoingMessageReader(OutgoingTextMessage message, long threadId) {
      this.message  = message;
      this.threadId = threadId;
      this.id       = new SecureRandom().nextLong();
    }

    public MessageRecord getCurrent() {
      return new SmsMessageRecord(id, message.getMessageBody(),

              message.getRecipient(), message.getRecipient(),
              MnodeAPI.getNowWithOffset(), MnodeAPI.getNowWithOffset(),
              0, message.isSecureMessage() ? MmsSmsColumns.Types.getOutgoingEncryptedMessageType() : MmsSmsColumns.Types.getOutgoingSmsMessageType(),
              threadId, 0, new LinkedList<IdentityKeyMismatch>(),
              message.getExpiresIn(),
              MnodeAPI.getNowWithOffset(), 0, false);
    }
  }

  public class Reader {

    private final Cursor cursor;

    public Reader(Cursor cursor) {
      this.cursor = cursor;
    }

    public SmsMessageRecord getNext() {
      if (cursor == null || !cursor.moveToNext())
        return null;

      return getCurrent();
    }

    public int getCount() {
      if (cursor == null) return 0;
      else                return cursor.getCount();
    }

    public SmsMessageRecord getCurrent() {
      long    messageId            = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
      Address address              = Address.fromSerialized(cursor.getString(cursor.getColumnIndexOrThrow(ADDRESS)));
      int     addressDeviceId      = cursor.getInt(cursor.getColumnIndexOrThrow(ADDRESS_DEVICE_ID));
      long    type                 = cursor.getLong(cursor.getColumnIndexOrThrow(SmsDatabase.TYPE));
      long    dateReceived         = cursor.getLong(cursor.getColumnIndexOrThrow(NORMALIZED_DATE_RECEIVED));
      long    dateSent             = cursor.getLong(cursor.getColumnIndexOrThrow(NORMALIZED_DATE_SENT));
      long    threadId             = cursor.getLong(cursor.getColumnIndexOrThrow(THREAD_ID));
      int     status               = cursor.getInt(cursor.getColumnIndexOrThrow(SmsDatabase.STATUS));
      int     deliveryReceiptCount = cursor.getInt(cursor.getColumnIndexOrThrow(DELIVERY_RECEIPT_COUNT));
      int     readReceiptCount     = cursor.getInt(cursor.getColumnIndexOrThrow(READ_RECEIPT_COUNT));
      String  mismatchDocument     = cursor.getString(cursor.getColumnIndexOrThrow(MISMATCHED_IDENTITIES));
      int     subscriptionId       = cursor.getInt(cursor.getColumnIndexOrThrow(SUBSCRIPTION_ID));
      long    expiresIn            = cursor.getLong(cursor.getColumnIndexOrThrow(EXPIRES_IN));
      long    expireStarted        = cursor.getLong(cursor.getColumnIndexOrThrow(EXPIRE_STARTED));
      String  body                 = cursor.getString(cursor.getColumnIndexOrThrow(BODY));
      boolean unidentified         = cursor.getInt(cursor.getColumnIndexOrThrow(UNIDENTIFIED)) == 1;

      if (!TextSecurePreferences.isReadReceiptsEnabled(context)) {
        readReceiptCount = 0;
      }

      List<IdentityKeyMismatch> mismatches = getMismatches(mismatchDocument);
      Recipient                 recipient  = Recipient.from(context, address, true);

      return new SmsMessageRecord(messageId, body, recipient,
              recipient,
              dateSent, dateReceived, deliveryReceiptCount, type,
              threadId, status, mismatches,
              expiresIn, expireStarted, readReceiptCount, unidentified);

    }

    private List<IdentityKeyMismatch> getMismatches(String document) {
      try {
        if (!TextUtils.isEmpty(document)) {
          return JsonUtil.fromJson(document, IdentityKeyMismatchList.class).getList();
        }
      } catch (IOException e) {
        Log.w(TAG, e);
      }

      return new LinkedList<>();
    }

    public void close() {
      cursor.close();
    }
  }

  public interface InsertListener {
    public void onComplete();
  }

}
