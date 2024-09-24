package io.beldex.bchat.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import com.beldex.libbchat.utilities.Document;
import com.beldex.libbchat.utilities.IdentityKeyMismatch;
import com.beldex.libbchat.utilities.IdentityKeyMismatchList;
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.crypto.IdentityKey;

import com.beldex.libbchat.utilities.Address;
import com.beldex.libsignal.utilities.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class MessagingDatabase extends Database implements MmsSmsColumns {

  private static final String TAG = MessagingDatabase.class.getSimpleName();

  public MessagingDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

  protected abstract String getTableName();

  public abstract void markExpireStarted(long messageId);
  public abstract void markExpireStarted(long messageId, long startTime);

  public abstract void markAsSent(long messageId, boolean secure);
  public abstract void markUnidentified(long messageId, boolean unidentified);

  public abstract void markAsDeleted(long messageId, boolean read);

  public abstract boolean deleteMessage(long messageId);
  public abstract boolean deleteMessages(long[] messageId, long threadId);

  public void addMismatchedIdentity(long messageId, Address address, IdentityKey identityKey) {
    try {
      addToDocument(messageId, MISMATCHED_IDENTITIES,
                    new IdentityKeyMismatch(address, identityKey),
                    IdentityKeyMismatchList.class);
    } catch (IOException e) {
      Log.w(TAG, e);
    }
  }

  public void removeMismatchedIdentity(long messageId, Address address, IdentityKey identityKey) {
    try {
      removeFromDocument(messageId, MISMATCHED_IDENTITIES,
                         new IdentityKeyMismatch(address, identityKey),
                         IdentityKeyMismatchList.class);
    } catch (IOException e) {
      Log.w(TAG, e);
    }
  }

  protected <D extends Document<I>, I> void removeFromDocument(long messageId, String column, I object, Class<D> clazz) throws IOException {
    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    database.beginTransaction();

    try {
      D           document = getDocument(database, messageId, column, clazz);
      Iterator<I> iterator = document.getList().iterator();

      while (iterator.hasNext()) {
        I item = iterator.next();

        if (item.equals(object)) {
          iterator.remove();
          break;
        }
      }

      setDocument(database, messageId, column, document);
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
  }

  protected <T extends Document<I>, I> void addToDocument(long messageId, String column, final I object, Class<T> clazz) throws IOException {
    List<I> list = new ArrayList<I>() {{
      add(object);
    }};

    addToDocument(messageId, column, list, clazz);
  }

  protected <T extends Document<I>, I> void addToDocument(long messageId, String column, List<I> objects, Class<T> clazz) throws IOException {
    SQLiteDatabase database = databaseHelper.getWritableDatabase();
    database.beginTransaction();

    try {
      T document = getDocument(database, messageId, column, clazz);
      document.getList().addAll(objects);
      setDocument(database, messageId, column, document);

      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
  }

  private void setDocument(SQLiteDatabase database, long messageId, String column, Document document) throws IOException {
    ContentValues contentValues = new ContentValues();

    if (document == null || document.size() == 0) {
      contentValues.put(column, (String)null);
    } else {
      contentValues.put(column, JsonUtil.toJsonThrows(document));
    }

    database.update(getTableName(), contentValues, ID_WHERE, new String[] {String.valueOf(messageId)});
  }

  private <D extends Document> D getDocument(SQLiteDatabase database, long messageId,
                                             String column, Class<D> clazz)
  {
    Cursor cursor = null;

    try {
      cursor = database.query(getTableName(), new String[] {column},
                              ID_WHERE, new String[] {String.valueOf(messageId)},
                              null, null, null);

      if (cursor != null && cursor.moveToNext()) {
        String document = cursor.getString(cursor.getColumnIndexOrThrow(column));

        try {
          if (!TextUtils.isEmpty(document)) {
            return JsonUtil.fromJson(document, clazz);
          }
        } catch (IOException e) {
          Log.w(TAG, e);
        }
      }

      try {
        return clazz.newInstance();
      } catch (InstantiationException e) {
        throw new AssertionError(e);
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      }

    } finally {
      if (cursor != null)
        cursor.close();
    }
  }

  public static class SyncMessageId {

    private final Address address;
    private final long   timetamp;

    public SyncMessageId(Address address, long timetamp) {
      this.address  = address;
      this.timetamp = timetamp;
    }

    public Address getAddress() {
      return address;
    }

    public long getTimetamp() {
      return timetamp;
    }
  }

  public static class ExpirationInfo {

    private final long    id;
    private final long    expiresIn;
    private final long    expireStarted;
    private final boolean mms;

    public ExpirationInfo(long id, long expiresIn, long expireStarted, boolean mms) {
      this.id            = id;
      this.expiresIn     = expiresIn;
      this.expireStarted = expireStarted;
      this.mms           = mms;
    }

    public long getId() {
      return id;
    }

    public long getExpiresIn() {
      return expiresIn;
    }

    public long getExpireStarted() {
      return expireStarted;
    }

    public boolean isMms() {
      return mms;
    }
  }

  public static class MarkedMessageInfo {

    private final SyncMessageId  syncMessageId;
    private final ExpirationInfo expirationInfo;

    public MarkedMessageInfo(SyncMessageId syncMessageId, ExpirationInfo expirationInfo) {
      this.syncMessageId  = syncMessageId;
      this.expirationInfo = expirationInfo;
    }

    public SyncMessageId getSyncMessageId() {
      return syncMessageId;
    }

    public ExpirationInfo getExpirationInfo() {
      return expirationInfo;
    }
  }

  public static class InsertResult {
    private final long messageId;
    private final long threadId;

    public InsertResult(long messageId, long threadId) {
      this.messageId = messageId;
      this.threadId = threadId;
    }

    public long getMessageId() {
      return messageId;
    }

    public long getThreadId() {
      return threadId;
    }
  }
}
