package io.beldex.bchat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.json.JSONArray
import org.json.JSONException
import com.beldex.libsignal.utilities.JsonUtil.SaneJSONObject
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper
import io.beldex.bchat.database.model.MessageId
import io.beldex.bchat.database.model.ReactionRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.CursorUtil

/**
 * Store reactions on messages.
 */
class ReactionDatabase(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper) {

    companion object {
        const val TABLE_NAME = "reaction"
        const val REACTION_JSON_ALIAS = "reaction_json"
        const val ROW_ID = "reaction_id"
        const val MESSAGE_ID = "message_id"
        const val IS_MMS = "is_mms"
        const val AUTHOR_ID = "author_id"
        const val SERVER_ID = "server_id"
        const val COUNT = "count"
        const val SORT_ID = "sort_id"
        const val EMOJI = "emoji"
        const val DATE_SENT = "reaction_date_sent"
        const val DATE_RECEIVED = "reaction_date_received"

        @JvmField
        val CREATE_REACTION_TABLE_COMMAND = """
      CREATE TABLE $TABLE_NAME (
        $ROW_ID INTEGER PRIMARY KEY,
        $MESSAGE_ID INTEGER NOT NULL,
        $IS_MMS INTEGER NOT NULL,
        $AUTHOR_ID INTEGER NOT NULL REFERENCES ${RecipientDatabase.TABLE_NAME} (${RecipientDatabase.ID}) ON DELETE CASCADE,
        $EMOJI TEXT NOT NULL,
        $SERVER_ID TEXT NOT NULL,
        $COUNT INTEGER NOT NULL,
        $SORT_ID INTEGER NOT NULL,
        $DATE_SENT INTEGER NOT NULL,
        $DATE_RECEIVED INTEGER NOT NULL,
        UNIQUE($MESSAGE_ID, $IS_MMS, $EMOJI, $AUTHOR_ID) ON CONFLICT REPLACE
      )
    """.trimIndent()

        @JvmField
        val CREATE_REACTION_TRIGGERS = arrayOf(
            """
        CREATE TRIGGER reactions_sms_delete AFTER DELETE ON ${SmsDatabase.TABLE_NAME} 
        BEGIN 
        	DELETE FROM $TABLE_NAME WHERE $MESSAGE_ID = old.${MmsSmsColumns.ID} AND $IS_MMS = 0;
        END
      """,
            """
        CREATE TRIGGER reactions_mms_delete AFTER DELETE ON ${MmsDatabase.TABLE_NAME} 
        BEGIN 
        	DELETE FROM $TABLE_NAME WHERE $MESSAGE_ID = old.${MmsSmsColumns.ID} AND $IS_MMS = 1;
        END
      """
        )

        private fun readReaction(cursor: Cursor): ReactionRecord {
            return ReactionRecord(
                messageId = CursorUtil.requireLong(cursor, MESSAGE_ID),
                isMms = CursorUtil.requireInt(cursor, IS_MMS) == 1,
                emoji = CursorUtil.requireString(cursor, EMOJI),
                author = CursorUtil.requireString(cursor, AUTHOR_ID),
                serverId = CursorUtil.requireString(cursor, SERVER_ID),
                count = CursorUtil.requireLong(cursor, COUNT),
                sortId = CursorUtil.requireLong(cursor, SORT_ID),
                dateSent = CursorUtil.requireLong(cursor, DATE_SENT),
                dateReceived = CursorUtil.requireLong(cursor, DATE_RECEIVED)
            )
        }
    }

    fun getReactions(messageId: MessageId): List<ReactionRecord> {
        val query = "$MESSAGE_ID = ? AND $IS_MMS = ? ORDER BY $SORT_ID"
        val args = arrayOf("${messageId.id}", "${if (messageId.mms) 1 else 0}")

        val reactions: MutableList<ReactionRecord> = mutableListOf()

        databaseHelper.readableDatabase.query(TABLE_NAME, null, query, args, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                reactions += readReaction(cursor)
            }
        }

        return reactions
    }

    fun addReaction(messageId: MessageId, reaction: ReactionRecord) {

        databaseHelper.writableDatabase.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(MESSAGE_ID, messageId.id)
                put(IS_MMS, if (messageId.mms) 1 else 0)
                put(EMOJI, reaction.emoji)
                put(AUTHOR_ID, reaction.author)
                put(SERVER_ID, reaction.serverId)
                put(COUNT, reaction.count)
                put(SORT_ID, reaction.sortId)
                put(DATE_SENT, reaction.dateSent)
                put(DATE_RECEIVED, reaction.dateReceived)
            }

            databaseHelper.writableDatabase.insert(TABLE_NAME, null, values)

            if (messageId.mms) {
                DatabaseComponent.get(context).mmsDatabase().updateReactionsUnread( databaseHelper.writableDatabase, messageId.id, hasReactions(messageId), false)
            } else {
                DatabaseComponent.get(context).smsDatabase().updateReactionsUnread( databaseHelper.writableDatabase, messageId.id, hasReactions(messageId), false)
            }

            databaseHelper.writableDatabase.setTransactionSuccessful()
        } finally {
            databaseHelper.writableDatabase.endTransaction()
        }
    }

    fun deleteReaction(emoji: String, messageId: MessageId, author: String) {
        deleteReactions(
            messageId = messageId,
            query = "$MESSAGE_ID = ? AND $IS_MMS = ? AND $EMOJI = ? AND $AUTHOR_ID = ?",
            args = arrayOf("${messageId.id}", "${if (messageId.mms)  1 else 0}", emoji, author)
        )
    }

    fun deleteEmojiReactions(emoji: String, messageId: MessageId) {
        deleteReactions(
            messageId = messageId,
            query = "$MESSAGE_ID = ? AND $IS_MMS = ? AND $EMOJI = ?",
            args = arrayOf("${messageId.id}", "${if (messageId.mms)  1 else 0}", emoji)
        )
    }

    fun deleteMessageReactions(messageId: MessageId) {
        deleteReactions(
            messageId = messageId,
            query = "$MESSAGE_ID = ? AND $IS_MMS = ?",
            args = arrayOf("${messageId.id}", "${if (messageId.mms)  1 else 0}")
        )
    }

    private fun deleteReactions(messageId: MessageId, query: String, args: Array<String>) {
        databaseHelper.writableDatabase.beginTransaction()
        try {
            databaseHelper.writableDatabase.delete(TABLE_NAME, query, args)

            if (messageId.mms) {
                DatabaseComponent.get(context).mmsDatabase().updateReactionsUnread( databaseHelper.writableDatabase, messageId.id, hasReactions(messageId), true)
            } else {
                DatabaseComponent.get(context).smsDatabase().updateReactionsUnread( databaseHelper.writableDatabase, messageId.id, hasReactions(messageId), true)
            }

            databaseHelper.writableDatabase.setTransactionSuccessful()
        } finally {
            databaseHelper.writableDatabase.endTransaction()
        }
    }

    private fun hasReactions(messageId: MessageId): Boolean {
        val query = "$MESSAGE_ID = ? AND $IS_MMS = ?"
        val args = arrayOf("${messageId.id}", "${if (messageId.mms) 1 else 0}")

        databaseHelper.readableDatabase.query(TABLE_NAME, arrayOf(MESSAGE_ID), query, args, null, null, null).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    fun getReactions(cursor: Cursor): List<ReactionRecord> {
        return try {
            if (cursor.getColumnIndex(REACTION_JSON_ALIAS) != -1) {
                if (cursor.isNull(cursor.getColumnIndexOrThrow(REACTION_JSON_ALIAS))) {
                    return listOf()
                }
                val result = mutableSetOf<ReactionRecord>()
                val array = JSONArray(cursor.getString(cursor.getColumnIndexOrThrow(REACTION_JSON_ALIAS)))
                for (i in 0 until array.length()) {
                    val `object` = SaneJSONObject(array.getJSONObject(i))
                    if (!`object`.isNull(ROW_ID)) {
                        result.add(
                            ReactionRecord(
                                `object`.getLong(ROW_ID),
                                `object`.getLong(MESSAGE_ID),
                                `object`.getInt(IS_MMS) == 1,
                                `object`.getString(AUTHOR_ID),
                                `object`.getString(EMOJI),
                                `object`.getString(SERVER_ID),
                                `object`.getLong(COUNT),
                                `object`.getLong(SORT_ID),
                                `object`.getLong(DATE_SENT),
                                `object`.getLong(DATE_RECEIVED)
                            )
                        )
                    }
                }
                result.sortedBy { it.dateSent }
            } else {
                listOf(
                    ReactionRecord(
                        cursor.getLong(cursor.getColumnIndexOrThrow(ROW_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(MESSAGE_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(IS_MMS)) == 1,
                        cursor.getString(cursor.getColumnIndexOrThrow(AUTHOR_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(EMOJI)),
                        cursor.getString(cursor.getColumnIndexOrThrow(SERVER_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COUNT)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(SORT_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DATE_SENT)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DATE_RECEIVED))
                    )
                )
            }
        } catch (e: JSONException) {
            throw AssertionError(e)
        }
    }

    fun getReactionFor(timestamp: Long, sender: String): ReactionRecord? {
        val query = "$DATE_SENT = ? AND $AUTHOR_ID = ?"
        val args = arrayOf("$timestamp", sender)

        databaseHelper.readableDatabase.query(TABLE_NAME, null, query, args, null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) readReaction(cursor) else null
        }
    }

    fun updateReaction(reaction: ReactionRecord) {
        databaseHelper.writableDatabase.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(EMOJI, reaction.emoji)
                put(AUTHOR_ID, reaction.author)
                put(SERVER_ID, reaction.serverId)
                put(COUNT, reaction.count)
                put(SORT_ID, reaction.sortId)
                put(DATE_SENT, reaction.dateSent)
                put(DATE_RECEIVED, reaction.dateReceived)
            }

            val query = "$ROW_ID = ?"
            val args = arrayOf("${reaction.id}")
            databaseHelper.writableDatabase.update(TABLE_NAME, values, query, args)

            databaseHelper.writableDatabase.setTransactionSuccessful()
        } finally {
            databaseHelper.writableDatabase.endTransaction()
        }
    }

}