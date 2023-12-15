package com.thoughtcrimes.securesms.database.helpers;


import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.thoughtcrimes.securesms.crypto.DatabaseSecret;
import com.thoughtcrimes.securesms.database.AttachmentDatabase;
import com.thoughtcrimes.securesms.database.BchatContactDatabase;
import com.thoughtcrimes.securesms.database.BchatJobDatabase;
import com.thoughtcrimes.securesms.database.BchatRecipientAddressDatabase;
import com.thoughtcrimes.securesms.database.BeldexAPIDatabase;
import com.thoughtcrimes.securesms.database.BeldexBackupFilesDatabase;
import com.thoughtcrimes.securesms.database.BeldexMessageDatabase;
import com.thoughtcrimes.securesms.database.BeldexThreadDatabase;
import com.thoughtcrimes.securesms.database.BeldexUserDatabase;
import com.thoughtcrimes.securesms.database.DraftDatabase;
import com.thoughtcrimes.securesms.database.GroupDatabase;
import com.thoughtcrimes.securesms.database.GroupReceiptDatabase;
import com.thoughtcrimes.securesms.database.JobDatabase;
import com.thoughtcrimes.securesms.database.MmsDatabase;
import com.thoughtcrimes.securesms.database.PushDatabase;
import com.thoughtcrimes.securesms.database.RecipientDatabase;
import com.thoughtcrimes.securesms.database.SearchDatabase;
import com.thoughtcrimes.securesms.database.SmsDatabase;
import com.thoughtcrimes.securesms.database.ThreadDatabase;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;
import net.zetetic.database.sqlcipher.SQLiteConnection;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;

import java.io.File;

import com.beldex.libsignal.utilities.Log;

public class SQLCipherOpenHelper extends SQLiteOpenHelper {

  @SuppressWarnings("unused")
  private static final String TAG = SQLCipherOpenHelper.class.getSimpleName();

  // First public release (1.0.0) DB version was 27.
  // So we have to keep the migrations onwards.
  private static final int beldexV7                           = 28;
  private static final int beldexV8                           = 29;
  private static final int beldexV9                           = 30;
  private static final int beldexV10                          = 31;
  private static final int beldexV11                          = 32;
  private static final int beldexV12                          = 33;
  private static final int beldexV13                          = 34;
  private static final int beldexV14_BACKUP_FILES             = 35;
  private static final int beldexV15                          = 36;
  private static final int beldexV16                          = 37;
  private static final int beldexV17                          = 38;
  private static final int beldexV18_CLEAR_BG_POLL_JOBS       = 39;
  private static final int beldexV19                          = 40;
  private static final int beldexV20                          = 41;
  private static final int beldexV21                          = 42;
  private static final int beldexV22                          = 43;
  private static final int beldexV23                          = 44;
  private static final int beldexV24                          = 45;
  private static final int beldexV25                          = 46;
  private static final int beldexV26                          = 47;
  private static final int beldexV27                          = 48;
  private static final int beldexV28                          = 49;
  private static final int beldexV29                          = 50;
  private static final int beldexV30                          = 51;
  private static final int beldexV31                          = 52;
  //New
  private static final int beldexV32                          = 53;

  private static final int beldexV33                          = 54;

  // beldex - onUpgrade(...) must be updated to use beldex version numbers if Signal makes any database changes
  private static final int    DATABASE_VERSION = beldexV33;
  private static final int    MIN_DATABASE_VERSION     = beldexV7;
  public static final String DATABASE_NAME    = "bchat_v4.db";
  private static final String CIPHER3_DATABASE_NAME    = "bchat.db";

  private final Context        context;
  private final DatabaseSecret databaseSecret;

  public SQLCipherOpenHelper(@NonNull Context context, @NonNull DatabaseSecret databaseSecret) {
    super(context, DATABASE_NAME, databaseSecret.asString(), null, DATABASE_VERSION, MIN_DATABASE_VERSION, null, new SQLiteDatabaseHook() {
      @Override
      public void preKey(SQLiteConnection connection) {
        connection.execute("PRAGMA cipher_default_kdf_iter = 256000;", null, null);
        connection.execute("PRAGMA cipher_default_page_size = 4096;", null, null);
      }

      @Override
      public void postKey(SQLiteConnection connection) {
        connection.execute("PRAGMA kdf_iter = '256000';", null, null);
        connection.execute("PRAGMA cipher_page_size = 4096;", null, null);
        // if not vacuumed in a while, perform that operation
        long currentTime = System.currentTimeMillis();
        // 7 days
        if (currentTime - TextSecurePreferences.getLastVacuumTime(context) > 604_800_000) {
          connection.execute("VACUUM;", null, null);
          TextSecurePreferences.setLastVacuumNow(context);
        }
      }
    }, true);

    this.context        = context.getApplicationContext();
    this.databaseSecret = databaseSecret;
  }

  public static void migrateSqlCipher3To4IfNeeded(@NonNull Context context, @NonNull DatabaseSecret databaseSecret) {
    String oldDbPath = context.getDatabasePath(CIPHER3_DATABASE_NAME).getPath();
    File oldDbFile = new File(oldDbPath);

    // If the old SQLCipher3 database file doesn't exist then just return early
    if (!oldDbFile.exists()) { return; }

    // If the new database file already exists then we probably had a failed migration and it's likely in
    // an invalid state so should delete it
    String newDbPath = context.getDatabasePath(DATABASE_NAME).getPath();
    File newDbFile = new File(newDbPath);

    if (newDbFile.exists()) { newDbFile.delete(); }

    try {
      newDbFile.createNewFile();
    }
    catch (Exception e) {
      // TODO: Communicate the error somehow???
      return;
    }

    try {
      // Open the old database
      SQLiteDatabase oldDb = SQLiteDatabase.openOrCreateDatabase(oldDbPath, databaseSecret.asString(), null, null, new SQLiteDatabaseHook() {
        @Override
        public void preKey(SQLiteConnection connection) {
          connection.execute("PRAGMA cipher_compatibility = 3;", null, null);
          connection.execute("PRAGMA kdf_iter = '1';", null, null);
          connection.execute("PRAGMA cipher_page_size = 4096;", null, null);
        }

        @Override
        public void postKey(SQLiteConnection connection) {
          connection.execute("PRAGMA cipher_compatibility = 3;", null, null);
          connection.execute("PRAGMA kdf_iter = '1';", null, null);
          connection.execute("PRAGMA cipher_page_size = 4096;", null, null);
        }
      });

      // Export the old database to the new one (will have the default 'kdf_iter' and 'page_size' settings)
      int oldDbVersion = oldDb.getVersion();
      oldDb.rawExecSQL(
              String.format("ATTACH DATABASE '%s' AS sqlcipher4 KEY '%s'", newDbPath, databaseSecret.asString())
      );
      Cursor cursor = oldDb.rawQuery("SELECT sqlcipher_export('sqlcipher4')");
      cursor.moveToLast();
      cursor.close();
      oldDb.rawExecSQL("DETACH DATABASE sqlcipher4");
      oldDb.close();

      // TODO: Performance testing

      SQLiteDatabase newDb = SQLiteDatabase.openDatabase(newDbPath, databaseSecret.asString(), null, SQLiteDatabase.OPEN_READWRITE, new SQLiteDatabaseHook() {
        @Override
        public void preKey(SQLiteConnection connection) {
          connection.execute("PRAGMA cipher_default_kdf_iter = 256000;", null, null);
          connection.execute("PRAGMA cipher_default_page_size = 4096;", null, null);
        }

        @Override
        public void postKey(SQLiteConnection connection) {
          connection.execute("PRAGMA cipher_default_kdf_iter = 256000;", null, null);
          connection.execute("PRAGMA cipher_default_page_size = 4096;", null, null);
        }
      });
      newDb.setVersion(oldDbVersion);
      newDb.close();

      // TODO: Delete 'CIPHER3_DATABASE_NAME'
      // TODO: What do we do if the deletion fails??? (The current logic will end up re-migrating...)
//      oldDbFile.delete();
    }
    catch (Exception e) {
      // TODO: Communicate the error somehow???
    }
  }


  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SmsDatabase.CREATE_TABLE);
    db.execSQL(MmsDatabase.CREATE_TABLE);
    db.execSQL(AttachmentDatabase.CREATE_TABLE);
    db.execSQL(ThreadDatabase.CREATE_TABLE);
    db.execSQL(DraftDatabase.CREATE_TABLE);
    db.execSQL(PushDatabase.CREATE_TABLE);
    db.execSQL(GroupDatabase.CREATE_TABLE);
    db.execSQL(RecipientDatabase.CREATE_TABLE);
    db.execSQL(GroupReceiptDatabase.CREATE_TABLE);
    for (String sql : SearchDatabase.CREATE_TABLE) {
      db.execSQL(sql);
    }
    for (String sql : JobDatabase.CREATE_TABLE) {
      db.execSQL(sql);
    }
    db.execSQL(BeldexAPIDatabase.getCreateMnodePoolTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateOnionRequestPathTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateSwarmTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateLastMessageHashValueTable2Command());
    db.execSQL(BeldexAPIDatabase.getCreateReceivedMessageHashValuesTable3Command());
    db.execSQL(BeldexAPIDatabase.getCreateOpenGroupAuthTokenTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateLastMessageServerIDTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateLastDeletionServerIDTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateDeviceLinkCacheCommand());
    db.execSQL(BeldexAPIDatabase.getCreateUserCountTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateBchatRequestTimestampCacheCommand());
    db.execSQL(BeldexAPIDatabase.getCreateBchatRequestSentTimestampTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateBchatRequestProcessedTimestampTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateOpenGroupPublicKeyTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateOpenGroupProfilePictureTableCommand());
    db.execSQL(BeldexAPIDatabase.getCreateClosedGroupEncryptionKeyPairsTable());
    db.execSQL(BeldexAPIDatabase.getCreateClosedGroupPublicKeysTable());
    db.execSQL(BeldexMessageDatabase.getCreateMessageIDTableCommand());
    db.execSQL(BeldexMessageDatabase.getCreateMessageToThreadMappingTableCommand());
    db.execSQL(BeldexMessageDatabase.getCreateErrorMessageTableCommand());
    db.execSQL(BeldexMessageDatabase.getCreateMessageHashTableCommand());
    db.execSQL(BeldexThreadDatabase.getCreateBchatResetTableCommand());
    db.execSQL(BeldexThreadDatabase.getCreatePublicChatTableCommand());
    db.execSQL(BeldexUserDatabase.getCreateDisplayNameTableCommand());
    db.execSQL(BeldexBackupFilesDatabase.getCreateTableCommand());
    db.execSQL(BchatJobDatabase.getCreateBchatJobTableCommand());
    db.execSQL(BeldexMessageDatabase.getUpdateMessageIDTableForType());
    db.execSQL(BeldexMessageDatabase.getUpdateMessageMappingTable());
    db.execSQL(BchatContactDatabase.getCreateBchatContactTableCommand());
    db.execSQL(RecipientDatabase.getCreateNotificationTypeCommand());
    db.execSQL(ThreadDatabase.getCreatePinnedCommand());
    db.execSQL(GroupDatabase.getCreateUpdatedTimestampCommand());
    db.execSQL(RecipientDatabase.getCreateApprovedCommand());
    db.execSQL(RecipientDatabase.getCreateApprovedMeCommand());
    db.execSQL(MmsDatabase.getCreateMessageRequestResponseCommand);
    //New
    db.execSQL(BchatRecipientAddressDatabase.getCreateBchatRecipientAddressTableCommand());

    db.execSQL(BeldexAPIDatabase.CREATE_FORK_INFO_TABLE_COMMAND);
    db.execSQL(BeldexAPIDatabase.CREATE_DEFAULT_FORK_INFO_COMMAND);
    db.execSQL(BeldexAPIDatabase.UPDATE_HASHES_INCLUDE_NAMESPACE_COMMAND);
    db.execSQL(BeldexAPIDatabase.UPDATE_RECEIVED_INCLUDE_NAMESPACE_COMMAND);


    executeStatements(db, SmsDatabase.CREATE_INDEXS);
    executeStatements(db, MmsDatabase.CREATE_INDEXS);
    executeStatements(db, AttachmentDatabase.CREATE_INDEXS);
    executeStatements(db, ThreadDatabase.CREATE_INDEXS);
    executeStatements(db, DraftDatabase.CREATE_INDEXS);
    executeStatements(db, GroupDatabase.CREATE_INDEXS);
    executeStatements(db, GroupReceiptDatabase.CREATE_INDEXES);
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    super.onConfigure(db);

    db.execSQL("PRAGMA cache_size = 10000");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.i(TAG, "Upgrading database: " + oldVersion + ", " + newVersion);

    db.beginTransaction();

    try {

      if (oldVersion < beldexV7) {
        db.execSQL(BeldexMessageDatabase.getCreateErrorMessageTableCommand());
      }

      if (oldVersion < beldexV8) {
        db.execSQL(BeldexAPIDatabase.getCreateBchatRequestTimestampCacheCommand());
      }

      if (oldVersion < beldexV9) {
        db.execSQL(BeldexAPIDatabase.getCreateMnodePoolTableCommand());
        db.execSQL(BeldexAPIDatabase.getCreateOnionRequestPathTableCommand());
      }

      if (oldVersion < beldexV10) {
        db.execSQL(BeldexAPIDatabase.getCreateBchatRequestSentTimestampTableCommand());
        db.execSQL(BeldexAPIDatabase.getCreateBchatRequestProcessedTimestampTableCommand());
      }

      if (oldVersion < beldexV11) {
        db.execSQL(BeldexAPIDatabase.getCreateOpenGroupPublicKeyTableCommand());
      }

      if (oldVersion < beldexV12) {
        db.execSQL(BeldexAPIDatabase.getCreateLastMessageHashValueTable2Command());
      }

      if (oldVersion < beldexV13) {
        db.execSQL(BeldexAPIDatabase.getCreateReceivedMessageHashValuesTable3Command());
      }

      if (oldVersion < beldexV14_BACKUP_FILES) {
        db.execSQL(BeldexBackupFilesDatabase.getCreateTableCommand());
      }

      if (oldVersion < beldexV16) {
        db.execSQL(BeldexAPIDatabase.getCreateOpenGroupProfilePictureTableCommand());
      }

      if (oldVersion < beldexV17) {
        db.execSQL("ALTER TABLE part ADD COLUMN audio_visual_samples BLOB");
        db.execSQL("ALTER TABLE part ADD COLUMN audio_duration INTEGER");
      }

      if (oldVersion < beldexV18_CLEAR_BG_POLL_JOBS) {
        // BackgroundPollJob was replaced with BackgroundPollWorker. Clear all the scheduled job records.
        db.execSQL("DELETE FROM job_spec WHERE factory_key = 'BackgroundPollJob'");
        db.execSQL("DELETE FROM constraint_spec WHERE factory_key = 'BackgroundPollJob'");
      }

      // Many classes were removed. We need to update DB structure and data to match the code changes.
      if (oldVersion < beldexV19) {
        db.execSQL(BeldexAPIDatabase.getCreateClosedGroupEncryptionKeyPairsTable());
        db.execSQL(BeldexAPIDatabase.getCreateClosedGroupPublicKeysTable());
        db.execSQL("DROP TABLE identities");
        deleteJobRecords(db, "RetrieveProfileJob");
        deleteJobRecords(db,
                "RefreshAttributesJob",
                "RotateProfileKeyJob",
                "RefreshUnidentifiedDeliveryAbilityJob",
                "RotateCertificateJob"
        );
      }

      if (oldVersion < beldexV20) {
        deleteJobRecords(db,
                "CleanPreKeysJob",
                "RefreshPreKeysJob",
                "CreateSignedPreKeyJob",
                "RotateSignedPreKeyJob",
                "MultiDeviceBlockedUpdateJob",
                "MultiDeviceConfigurationUpdateJob",
                "MultiDeviceContactUpdateJob",
                "MultiDeviceGroupUpdateJob",
                "MultiDeviceOpenGroupUpdateJob",
                "MultiDeviceProfileKeyUpdateJob",
                "MultiDeviceReadUpdateJob",
                "MultiDeviceStickerPackOperationJob",
                "MultiDeviceStickerPackSyncJob",
                "MultiDeviceVerifiedUpdateJob",
                "ServiceOutageDetectionJob",
                "BchatRequestMessageSendJob"
        );
      }

      if (oldVersion < beldexV21) {
        deleteJobRecords(db,
                "ClosedGroupUpdateMessageSendJob",
                "NullMessageSendJob",
                "StickerDownloadJob",
                "StickerPackDownloadJob",
                "MmsSendJob",
                "MmsReceiveJob",
                "MmsDownloadJob",
                "SmsSendJob",
                "SmsSentJob",
                "SmsReceiveJob",
                "PushGroupUpdateJob",
                "ResetThreadBchatJob");
      }

      if (oldVersion < beldexV22) {
        db.execSQL(BchatJobDatabase.getCreateBchatJobTableCommand());
        deleteJobRecords(db,
                "PushGroupSendJob",
                "PushMediaSendJob",
                "PushTextSendJob",
                "SendReadReceiptJob",
                "TypingSendJob",
                "AttachmentUploadJob",
                "RequestGroupInfoJob",
                "ClosedGroupUpdateMessageSendJobV2",
                "SendDeliveryReceiptJob");
      }

      if (oldVersion < beldexV23) {
        db.execSQL("ALTER TABLE groups ADD COLUMN zombie_members TEXT");
        db.execSQL(BeldexMessageDatabase.getUpdateMessageIDTableForType());
        db.execSQL(BeldexMessageDatabase.getUpdateMessageMappingTable());
      }

      if (oldVersion < beldexV24) {
        String swarmTable = BeldexAPIDatabase.Companion.getSwarmTable();
        String mnodePoolTable = BeldexAPIDatabase.Companion.getMnodePoolTable();
        db.execSQL("DROP TABLE " + swarmTable);
        db.execSQL("DROP TABLE " + mnodePoolTable);
        db.execSQL(BeldexAPIDatabase.getCreateMnodePoolTableCommand());
        db.execSQL(BeldexAPIDatabase.getCreateSwarmTableCommand());
      }

      if (oldVersion < beldexV25) {
        String jobTable = BchatJobDatabase.bchatJobTable;
        db.execSQL("DROP TABLE " + jobTable);
        db.execSQL(BchatJobDatabase.getCreateBchatJobTableCommand());
      }

      if (oldVersion < beldexV26) {
        db.execSQL(BchatContactDatabase.getCreateBchatContactTableCommand());
      }

      if (oldVersion < beldexV27) {
        db.execSQL(RecipientDatabase.getCreateNotificationTypeCommand());
      }

      if (oldVersion < beldexV28) {
        db.execSQL(BeldexMessageDatabase.getCreateMessageHashTableCommand());
      }

      if (oldVersion < beldexV29) {
        db.execSQL(ThreadDatabase.getCreatePinnedCommand());
      }

      if (oldVersion < beldexV30) {
        db.execSQL(GroupDatabase.getCreateUpdatedTimestampCommand());
      }
      if (oldVersion < beldexV31) {
        db.execSQL(RecipientDatabase.getCreateApprovedCommand());
        db.execSQL(RecipientDatabase.getCreateApprovedMeCommand());
        db.execSQL(RecipientDatabase.getUpdateApprovedCommand());
        db.execSQL(RecipientDatabase.getUpdateApprovedSelectConversations());
        db.execSQL(MmsDatabase.getCreateMessageRequestResponseCommand);
      }

      if(oldVersion < beldexV32) {
        db.execSQL(BchatRecipientAddressDatabase.getCreateBchatRecipientAddressTableCommand());
      }

      if (oldVersion < beldexV33) {
        db.execSQL(BeldexAPIDatabase.CREATE_FORK_INFO_TABLE_COMMAND);
        db.execSQL(BeldexAPIDatabase.CREATE_DEFAULT_FORK_INFO_COMMAND);
        db.execSQL(BeldexAPIDatabase.UPDATE_HASHES_INCLUDE_NAMESPACE_COMMAND);
        db.execSQL(BeldexAPIDatabase.UPDATE_RECEIVED_INCLUDE_NAMESPACE_COMMAND);
      }

      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }


  public void markCurrent(SQLiteDatabase db) {
    db.setVersion(DATABASE_VERSION);
  }

  private void executeStatements(SQLiteDatabase db, String[] statements) {
    for (String statement : statements)
      db.execSQL(statement);
  }

  private static boolean columnExists(@NonNull SQLiteDatabase db, @NonNull String table, @NonNull String column) {
    try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
      int nameColumnIndex = cursor.getColumnIndexOrThrow("name");

      while (cursor.moveToNext()) {
        String name = cursor.getString(nameColumnIndex);

        if (name.equals(column)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Cleans up all the records related to the job keys specified.
   * This method should be called once the Signal job class is deleted from the project.
   */
  private static void deleteJobRecords(SQLiteDatabase db, String... jobKeys) {
    for (String jobKey : jobKeys) {
      db.execSQL("DELETE FROM job_spec WHERE factory_key = ?", new String[]{jobKey});
      db.execSQL("DELETE FROM constraint_spec WHERE factory_key = ?", new String[]{jobKey});
    }
  }
}
