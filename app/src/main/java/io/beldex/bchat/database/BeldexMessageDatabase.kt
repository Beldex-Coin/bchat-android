package io.beldex.bchat.database

import android.content.ContentValues
import android.content.Context
import com.beldex.libsignal.database.BeldexMessageDatabaseProtocol
import net.zetetic.database.sqlcipher.SQLiteDatabase.CONFLICT_REPLACE
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper

class BeldexMessageDatabase(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper),
    BeldexMessageDatabaseProtocol {

    companion object {
        private val messageIDTable = "beldex_message_friend_request_database"
        private val messageThreadMappingTable = "beldex_message_thread_mapping_database"
        private val errorMessageTable = "beldex_error_message_database"
        private val messageHashTable = "beldex_message_hash_database"
        private val messageID = "message_id"
        private val serverID = "server_id"
        private val friendRequestStatus = "friend_request_status"
        private val threadID = "thread_id"
        private val errorMessage = "error_message"
        private val messageType = "message_type"
        private val serverHash = "server_hash"
        @JvmStatic
        val createMessageIDTableCommand = "CREATE TABLE $messageIDTable ($messageID INTEGER PRIMARY KEY, $serverID INTEGER DEFAULT 0, $friendRequestStatus INTEGER DEFAULT 0);"
        @JvmStatic
        val createMessageToThreadMappingTableCommand = "CREATE TABLE IF NOT EXISTS $messageThreadMappingTable ($messageID INTEGER PRIMARY KEY, $threadID INTEGER);"
        @JvmStatic
        val createErrorMessageTableCommand = "CREATE TABLE IF NOT EXISTS $errorMessageTable ($messageID INTEGER PRIMARY KEY, $errorMessage STRING);"
        @JvmStatic
        val updateMessageIDTableForType = "ALTER TABLE $messageIDTable ADD COLUMN $messageType INTEGER DEFAULT 0; ALTER TABLE $messageIDTable ADD CONSTRAINT PK_$messageIDTable PRIMARY KEY ($messageID, $serverID);"
        @JvmStatic
        val updateMessageMappingTable = "ALTER TABLE $messageThreadMappingTable ADD COLUMN $serverID INTEGER DEFAULT 0; ALTER TABLE $messageThreadMappingTable ADD CONSTRAINT PK_$messageThreadMappingTable PRIMARY KEY ($messageID, $serverID);"
        @JvmStatic
        val createMessageHashTableCommand = "CREATE TABLE IF NOT EXISTS $messageHashTable ($messageID INTEGER PRIMARY KEY, $serverHash STRING);"

        const val SMS_TYPE = 0
        const val MMS_TYPE = 1

    }

    fun getServerID(messageID: Long): Long? {
        val database = databaseHelper.readableDatabase
        return database.get(messageIDTable, "${Companion.messageID} = ?", arrayOf(messageID.toString())) { cursor ->
            cursor.getInt(serverID)
        }?.toLong()
    }

    fun getServerID(messageID: Long, isSms: Boolean): Long? {
        val database = databaseHelper.readableDatabase
        return database.get(messageIDTable, "${Companion.messageID} = ? AND $messageType = ?", arrayOf(messageID.toString(), if (isSms) SMS_TYPE.toString() else MMS_TYPE.toString())) { cursor ->
            cursor.getInt(serverID)
        }?.toLong()
    }

    fun getMessageID(serverID: Long): Long? {
        val database = databaseHelper.readableDatabase
        return database.get(messageIDTable, "${Companion.serverID} = ?", arrayOf(serverID.toString())) { cursor ->
            cursor.getInt(messageID)
        }?.toLong()
    }

    fun deleteMessage(messageID: Long, isSms: Boolean) {
        val database = databaseHelper.writableDatabase

        val serverID = database.get(messageIDTable,
                "${Companion.messageID} = ? AND $messageType = ?",
                arrayOf(messageID.toString(), (if (isSms) SMS_TYPE else MMS_TYPE).toString())) { cursor ->
            cursor.getInt(serverID).toLong()
        } ?: return

        database.beginTransaction()

        database.delete(messageIDTable, "${Companion.messageID} = ? AND ${Companion.serverID} = ?", arrayOf(messageID.toString(), serverID.toString()))
        database.delete(messageThreadMappingTable, "${Companion.messageID} = ? AND ${Companion.serverID} = ?", arrayOf(messageID.toString(), serverID.toString()))

        database.setTransactionSuccessful()
        database.endTransaction()
    }

    fun deleteMessages(messageIDs: List<Long>) {
        val database = databaseHelper.writableDatabase
        database.beginTransaction()

        database.delete(
            messageIDTable,
            "${Companion.messageID} IN (${messageIDs.map { "?" }.joinToString(",")})",
            messageIDs.map { "$it" }.toTypedArray()
        )
        database.delete(
            messageThreadMappingTable,
            "${Companion.messageID} IN (${messageIDs.map { "?" }.joinToString(",")})",
            messageIDs.map { "$it" }.toTypedArray()
        )

        database.setTransactionSuccessful()
        database.endTransaction()
    }

    fun getMessageID(serverID: Long, threadID: Long): Pair<Long, Boolean>? {
        val database = databaseHelper.readableDatabase
        val mappingResult = database.get(messageThreadMappingTable, "${Companion.serverID} = ? AND ${Companion.threadID} = ?",
                arrayOf(serverID.toString(), threadID.toString())) { cursor ->
            cursor.getInt(messageID) to cursor.getInt(Companion.serverID)
        } ?: return null

        val (mappedID, mappedServerID) = mappingResult

        return database.get(messageIDTable,
                "$messageID = ? AND ${Companion.serverID} = ?",
                arrayOf(mappedID.toString(), mappedServerID.toString())) { cursor ->
            cursor.getInt(messageID).toLong() to (cursor.getInt(messageType) == SMS_TYPE)
        }
    }

    fun getMessageIDs(serverIDs: List<Long>, threadID: Long): Pair<List<Long>, List<Long>> {
        val database = databaseHelper.readableDatabase

        // Retrieve the message ids
        val messageIdCursor = database
            .rawQuery(
                """
                    SELECT ${messageThreadMappingTable}.${messageID}, ${messageIDTable}.${messageType}
                    FROM ${messageThreadMappingTable}
                    JOIN ${messageIDTable} ON ${messageIDTable}.message_id = ${messageThreadMappingTable}.${messageID} 
                    WHERE (
                        ${messageThreadMappingTable}.${Companion.threadID} = $threadID AND
                        ${messageThreadMappingTable}.${Companion.serverID} IN (${serverIDs.joinToString(",")})
                    )
                """
            )

        val smsMessageIds: MutableList<Long> = mutableListOf()
        val mmsMessageIds: MutableList<Long> = mutableListOf()
        while (messageIdCursor.moveToNext()) {
            if (messageIdCursor.getInt(1) == SMS_TYPE) {
                smsMessageIds.add(messageIdCursor.getLong(0))
            }
            else {
                mmsMessageIds.add(messageIdCursor.getLong(0))
            }
        }

        return Pair(smsMessageIds, mmsMessageIds)
    }


    override fun setServerID(messageID: Long, serverID: Long, isSms: Boolean) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(3)
        contentValues.put(Companion.messageID, messageID)
        contentValues.put(Companion.serverID, serverID)
        contentValues.put(messageType, if (isSms) SMS_TYPE else MMS_TYPE)
        database.insertWithOnConflict(messageIDTable, null, contentValues, CONFLICT_REPLACE)
    }

    fun getOriginalThreadID(messageID: Long): Long {
        val database = databaseHelper.readableDatabase
        return database.get(messageThreadMappingTable, "${Companion.messageID} = ?", arrayOf(messageID.toString())) { cursor ->
            cursor.getInt(threadID)
        }?.toLong() ?: -1L
    }

    fun setOriginalThreadID(messageID: Long, serverID: Long, threadID: Long) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(3)
        contentValues.put(Companion.messageID, messageID)
        contentValues.put(Companion.serverID, serverID)
        contentValues.put(Companion.threadID, threadID)
        database.insertWithOnConflict(messageThreadMappingTable, null, contentValues, CONFLICT_REPLACE)
    }

    fun getErrorMessage(messageID: Long): String? {
        val database = databaseHelper.readableDatabase
        return database.get(errorMessageTable, "${Companion.messageID} = ?", arrayOf(messageID.toString())) { cursor ->
            cursor.getString(errorMessage)
        }
    }

    fun setErrorMessage(messageID: Long, errorMessage: String) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(2)
        contentValues.put(Companion.messageID, messageID)
        contentValues.put(Companion.errorMessage, errorMessage)
        database.insertOrUpdate(errorMessageTable, contentValues, "${Companion.messageID} = ?", arrayOf(messageID.toString()))
    }

    fun clearErrorMessage(messageID: Long) {
        val database = databaseHelper.writableDatabase
        database.delete(errorMessageTable, "${Companion.messageID} = ?", arrayOf(messageID.toString()))
    }

    fun deleteThread(threadId: Long) {
        val database = databaseHelper.writableDatabase
        try {
            val messages = mutableSetOf<Pair<Long,Long>>()
            database.get(messageThreadMappingTable,  "$threadID = ?", arrayOf(threadId.toString())) { cursor ->
                // for each add
                while (cursor.moveToNext()) {
                    messages.add(cursor.getLong(messageID) to cursor.getLong(serverID))
                }
            }
            var deletedCount = 0L
            database.beginTransaction()
            messages.forEach { (messageId, serverId) ->
                deletedCount += database.delete(messageIDTable, "$messageID = ? AND $serverID = ?", arrayOf(messageId.toString(), serverId.toString()))
            }
            val mappingDeleted = database.delete(messageThreadMappingTable, "$threadID = ?", arrayOf(threadId.toString()))
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    fun getMessageServerHash(messageID: Long): String? {
        val database = databaseHelper.readableDatabase
        return database.get(messageHashTable, "${Companion.messageID} = ?", arrayOf(messageID.toString())) { cursor ->
            cursor.getString(serverHash)
        }
    }

    fun setMessageServerHash(messageID: Long, serverHash: String) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(2)
        contentValues.put(Companion.messageID, messageID)
        contentValues.put(Companion.serverHash, serverHash)
        database.insertOrUpdate(messageHashTable, contentValues, "${Companion.messageID} = ?", arrayOf(messageID.toString()))
    }

    fun deleteMessageServerHash(messageID: Long) {
        val database = databaseHelper.writableDatabase
        database.delete(messageHashTable, "${Companion.messageID} = ?", arrayOf(messageID.toString()))
    }

    fun migrateThreadId(legacyThreadId: Long, newThreadId: Long) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(1)
        contentValues.put(threadID, newThreadId)
        database.update(messageThreadMappingTable, contentValues, "$threadID = ?", arrayOf(legacyThreadId.toString()))
    }

    fun deleteMessageServerHashes(messageIDs: List<Long>) {
        val database = databaseHelper.writableDatabase
        database.delete(
            messageHashTable,
            "${Companion.messageID} IN (${messageIDs.map { "?" }.joinToString(",")})",
            messageIDs.map { "$it" }.toTypedArray()
        )
    }
}