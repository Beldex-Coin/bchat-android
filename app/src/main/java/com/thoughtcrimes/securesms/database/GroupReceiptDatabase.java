package com.thoughtcrimes.securesms.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import androidx.annotation.NonNull;

import net.sqlcipher.database.SQLiteDatabase;

import com.thoughtcrimes.securesms.database.helpers.SQLCipherOpenHelper;

import com.beldex.libbchat.utilities.Address;

import java.util.LinkedList;
import java.util.List;

public class GroupReceiptDatabase extends Database {

  public  static final String TABLE_NAME = "group_receipts";

  private static final String ID           = "_id";
  public  static final String MMS_ID       = "mms_id";
  private static final String ADDRESS      = "address";
  private static final String STATUS       = "status";
  private static final String TIMESTAMP    = "timestamp";
  private static final String UNIDENTIFIED = "unidentified";

  public static final int STATUS_UNKNOWN     = -1;
  public static final int STATUS_UNDELIVERED = 0;
  public static final int STATUS_DELIVERED   = 1;
  public static final int STATUS_READ        = 2;

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY, "                          +
      MMS_ID + " INTEGER, " + ADDRESS + " TEXT, " + STATUS + " INTEGER, " + TIMESTAMP + " INTEGER, " + UNIDENTIFIED + " INTEGER DEFAULT 0);";

  public static final String[] CREATE_INDEXES = {
      "CREATE INDEX IF NOT EXISTS group_receipt_mms_id_index ON " + TABLE_NAME + " (" + MMS_ID + ");",
  };

  public GroupReceiptDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }

  public void insert(List<Address> addresses, long mmsId, int status, long timestamp) {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();

    for (Address address : addresses) {
      ContentValues values = new ContentValues(4);
      values.put(MMS_ID, mmsId);
      values.put(ADDRESS, address.serialize());
      values.put(STATUS, status);
      values.put(TIMESTAMP, timestamp);

      db.insert(TABLE_NAME, null, values);
    }
  }

  public void update(Address address, long mmsId, int status, long timestamp) {
    SQLiteDatabase db     = databaseHelper.getWritableDatabase();
    ContentValues  values = new ContentValues(2);
    values.put(STATUS, status);
    values.put(TIMESTAMP, timestamp);

    db.update(TABLE_NAME, values, MMS_ID + " = ? AND " + ADDRESS + " = ? AND " + STATUS + " < ?",
              new String[] {String.valueOf(mmsId), address.serialize(), String.valueOf(status)});
  }

  public void setUnidentified(Address address, long mmsId, boolean unidentified) {
    SQLiteDatabase db     = databaseHelper.getWritableDatabase();
    ContentValues  values = new ContentValues(1);
    values.put(UNIDENTIFIED, unidentified ? 1 : 0);

    db.update(TABLE_NAME, values, MMS_ID + " = ? AND " + ADDRESS + " = ?",
              new String[] {String.valueOf(mmsId), address.serialize()});

  }

  public @NonNull List<GroupReceiptInfo> getGroupReceiptInfo(long mmsId) {
    SQLiteDatabase         db      = databaseHelper.getReadableDatabase();
    List<GroupReceiptInfo> results = new LinkedList<>();

    try (Cursor cursor = db.query(TABLE_NAME, null, MMS_ID + " = ?", new String[] {String.valueOf(mmsId)}, null, null, null)) {
      while (cursor != null && cursor.moveToNext()) {
        results.add(new GroupReceiptInfo(Address.fromSerialized(cursor.getString(cursor.getColumnIndexOrThrow(ADDRESS))),
                                         cursor.getInt(cursor.getColumnIndexOrThrow(STATUS)),
                                         cursor.getLong(cursor.getColumnIndexOrThrow(TIMESTAMP)),
                                         cursor.getInt(cursor.getColumnIndexOrThrow(UNIDENTIFIED)) == 1));
      }
    }

    return results;
  }

  void deleteRowsForMessage(long mmsId) {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.delete(TABLE_NAME, MMS_ID + " = ?", new String[] {String.valueOf(mmsId)});
  }

  void deleteAllRows() {
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    db.delete(TABLE_NAME, null, null);
  }

  public static class GroupReceiptInfo {
    private final Address address;
    private final int     status;
    private final long    timestamp;
    private final boolean unidentified;

    GroupReceiptInfo(Address address, int status, long timestamp, boolean unidentified) {
      this.address      = address;
      this.status       = status;
      this.timestamp    = timestamp;
      this.unidentified = unidentified;
    }

    public Address getAddress() {
      return address;
    }

    public int getStatus() {
      return status;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public boolean isUnidentified() {
      return unidentified;
    }
  }
}
