package io.beldex.bchat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libsignal.utilities.Base64
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper

class BchatContactDatabase(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper) {

    companion object {
        private const val bchatContactTable = "bchat_contact_database"
        const val bchatID = "bchat_id"
        const val name = "name"
        const val nickname = "nickname"
        const val profilePictureURL = "profile_picture_url"
        const val profilePictureFileName = "profile_picture_file_name"
        const val profilePictureEncryptionKey = "profile_picture_encryption_key"
        const val threadID = "thread_id"
        const val isTrusted = "is_trusted"
        const val beldexAddress = "beldex_address"
        const val isBnsHolder = "is_bns_holder"
        @JvmStatic val createBchatContactTableCommand =
            "CREATE TABLE $bchatContactTable " +
                "($bchatID STRING PRIMARY KEY, " +
                "$name TEXT DEFAULT NULL, " +
                "$nickname TEXT DEFAULT NULL, " +
                "$profilePictureURL TEXT DEFAULT NULL, " +
                "$profilePictureFileName TEXT DEFAULT NULL, " +
                "$profilePictureEncryptionKey BLOB DEFAULT NULL, " +
                "$threadID INTEGER DEFAULT -1, " +
                "$isTrusted INTEGER DEFAULT 0, " +
                "$beldexAddress STRING DEFAULT NULL, " +
                "$isBnsHolder INTEGER DEFAULT 0);"

        @JvmStatic val createIsBnsHolderCommand = "ALTER TABLE " + bchatContactTable + " " +
                "ADD COLUMN " + isBnsHolder + " INTEGER DEFAULT 0;"
    }

    fun getContactWithBchatID(bchatID: String): Contact? {
        val database = databaseHelper.readableDatabase
        return database.get(bchatContactTable, "${Companion.bchatID} = ?", arrayOf( bchatID )) { cursor ->
            contactFromCursor(cursor)
        }
    }

    fun getBeldexAddress(bchatID: String): Contact? {
        val database = databaseHelper.readableDatabase
        return database.get(bchatContactTable, "${Companion.bchatID} = ?", arrayOf( bchatID )) { cursor ->
            contactFromCursor(cursor)
        }
    }

    fun getAllContacts(): Set<Contact> {
        val database = databaseHelper.readableDatabase
        return database.getAll(bchatContactTable, null, null) { cursor ->
            contactFromCursor(cursor)
        }.toSet()
    }

    fun setContactIsTrusted(contact: Contact, isTrusted: Boolean, threadID: Long) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(1)
        contentValues.put(Companion.isTrusted, if (isTrusted) 1 else 0)
        database.update(bchatContactTable, contentValues, "$bchatID = ?", arrayOf( contact.bchatID ))
        if (threadID >= 0) {
            notifyConversationListeners(threadID)
        }
        notifyConversationListListeners()
    }

    fun setBeldexAddres(contact: Contact, beldexAddress: String, threadID: Long) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(1)
        contentValues.put(Companion.beldexAddress, beldexAddress)
        Log.d("Beldex"," address is $beldexAddress")
        database.update(bchatContactTable, contentValues, "$bchatID = ?", arrayOf( contact.bchatID ))
        if (threadID >= 0) {
            notifyConversationListeners(threadID)
        }
        notifyConversationListListeners()
    }

    fun setIsBnsHolder(contact: Contact, isBnsHolder: Boolean, threadID: Long) {
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(1)
        contentValues.put(Companion.isBnsHolder, if (isBnsHolder) 1 else 0)
        database.update(bchatContactTable, contentValues, "$bchatID = ?", arrayOf( contact.bchatID ))
        if (threadID >= 0) {
            notifyConversationListeners(threadID)
        }
        notifyConversationListListeners()
    }

    fun setContact(contact: Contact) {
        Log.d("Beldex","Set contact fun called")
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(10)
        contentValues.put(bchatID, contact.bchatID)
        Log.d("Beldex","Set contact fun called contact.bchatID ${contact.bchatID}")
        contentValues.put(name, contact.name)
        Log.d("Beldex","Set contact fun called contact.name ${contact.name}")
        contentValues.put(nickname, contact.nickname)
        Log.d("Beldex","Set contact fun called nickname ${contact.nickname}")
        contentValues.put(profilePictureURL, contact.profilePictureURL)
        Log.d("Beldex","Set contact fun called")
        contentValues.put(profilePictureFileName, contact.profilePictureFileName)
        Log.d("Beldex","Set contact fun called profile pic file name ${contact.profilePictureFileName}")
        contact.profilePictureEncryptionKey?.let {
            contentValues.put(profilePictureEncryptionKey, Base64.encodeBytes(it))
        }
        contentValues.put(threadID, contact.threadID)
        contentValues.put(isTrusted, if (contact.isTrusted) 1 else 0)
        contentValues.put(beldexAddress,contact.beldexAddress)
        contentValues.put(isBnsHolder,if(contact.isBnsHolder) 1 else 0)
        database.insertOrUpdate(bchatContactTable, contentValues, "$bchatID = ?", arrayOf( contact.bchatID ))
        notifyConversationListListeners()
    }

    fun contactFromCursor(cursor: Cursor): Contact {
        val bchatID = cursor.getString(cursor.getColumnIndexOrThrow(bchatID))
        val contact = Contact(bchatID)
        contact.name = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(name))
        contact.nickname = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(nickname))
        contact.profilePictureURL = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(profilePictureURL))
        contact.profilePictureFileName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(profilePictureFileName))
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(profilePictureEncryptionKey))?.let {
            contact.profilePictureEncryptionKey = Base64.decode(it)
        }
        contact.threadID = cursor.getLong(cursor.getColumnIndexOrThrow(threadID))
        contact.isTrusted = cursor.getInt(cursor.getColumnIndexOrThrow(isTrusted)) != 0
        contact.beldexAddress   = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(beldexAddress))
        contact.isBnsHolder = cursor.getInt(cursor.getColumnIndexOrThrow(isBnsHolder)) != 0
        return contact
    }

    fun queryContactsByName(constraint: String): Cursor {
        return databaseHelper.readableDatabase.query(
            bchatContactTable, null, " $name LIKE ? OR $nickname LIKE ?", arrayOf(
                "%$constraint%",
                "%$constraint%"
            ),
            null, null, null
        )
    }
}