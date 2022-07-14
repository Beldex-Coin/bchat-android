package com.thoughtcrimes.securesms.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.beldex.libbchat.messaging.open_groups.OpenGroupV2
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.JsonUtil
import com.thoughtcrimes.securesms.database.helpers.SQLCipherOpenHelper
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent

class BeldexThreadDatabase(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper) {

    companion object {
        private val bchatResetTable = "beldex_thread_bchat_reset_database"
        val publicChatTable = "beldex_public_chat_database"
        val threadID = "thread_id"
        private val bchatResetStatus = "bchat_reset_status"
        val publicChat = "public_chat"
        @JvmStatic
        val createBchatResetTableCommand = "CREATE TABLE $bchatResetTable ($threadID INTEGER PRIMARY KEY, $bchatResetStatus INTEGER DEFAULT 0);"
        @JvmStatic
        val createPublicChatTableCommand = "CREATE TABLE $publicChatTable ($threadID INTEGER PRIMARY KEY, $publicChat TEXT);"
    }

    fun getThreadID(hexEncodedPublicKey: String): Long {
        val address = Address.fromSerialized(hexEncodedPublicKey)
        val recipient = Recipient.from(context, address, false)
        return DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(recipient)
    }

    fun getAllV2OpenGroups(): Map<Long, OpenGroupV2> {
        val database = databaseHelper.readableDatabase
        var cursor: Cursor? = null
        val result = mutableMapOf<Long, OpenGroupV2>()
        try {
            cursor = database.rawQuery("select * from $publicChatTable", null)
            while (cursor != null && cursor.moveToNext()) {
                val threadID = cursor.getLong(threadID)
                val string = cursor.getString(publicChat)
                val openGroup = OpenGroupV2.fromJSON(string)
                if (openGroup != null) result[threadID] = openGroup
            }
        } catch (e: Exception) {
            // do nothing
        } finally {
            cursor?.close()
        }
        return result
    }

    fun getOpenGroupChat(threadID: Long): OpenGroupV2? {
        if (threadID < 0) {
            return null
        }
        val database = databaseHelper.readableDatabase
        return database.get(publicChatTable, "${Companion.threadID} = ?", arrayOf(threadID.toString())) { cursor ->
            val json = cursor.getString(publicChat)
            OpenGroupV2.fromJSON(json)
        }
    }

    fun setOpenGroupChat(openGroupV2: OpenGroupV2, threadID: Long) {
        if (threadID < 0) {
            return
        }
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(2)
        contentValues.put(Companion.threadID, threadID)
        contentValues.put(publicChat, JsonUtil.toJson(openGroupV2.toJson()))
        database.insertOrUpdate(publicChatTable, contentValues, "${Companion.threadID} = ?", arrayOf(threadID.toString()))
    }

    fun removeOpenGroupChat(threadID: Long) {
        if (threadID < 0) return

        val database = databaseHelper.writableDatabase
        database.delete(publicChatTable,"${Companion.threadID} = ?", arrayOf(threadID.toString()))
    }

}