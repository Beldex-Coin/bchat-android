package io.beldex.bchat.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper

class BchatRecipientAddressDatabase(context:Context,helper: SQLCipherOpenHelper): Database(context,helper) {
    companion object{
        private const val bchatRecipientAddressTable = "bchat_recipient_address_database"
        const val transactionId = "transaction_id"
        const val recipientAddress = "recipient_address"
        @JvmStatic val createBchatRecipientAddressTableCommand =
            "CREATE TABLE $bchatRecipientAddressTable " +
                    "($transactionId STRING DEFAULT NULL, " +
                    "$recipientAddress STRING DEFAULT NULL);"
    }

    fun getRecipientAddress(transactionId:String): String? {
       val database = databaseHelper.readableDatabase
       return database.get(bchatRecipientAddressTable,"${BchatRecipientAddressDatabase.transactionId} = ?", arrayOf(transactionId)){cursor ->
           recipientFromCursor(cursor)
       }
    }

    private fun recipientFromCursor(cursor: Cursor): String? {
        return cursor.getStringOrNull(recipientAddress)
    }

    fun insertRecipientAddress(transactionId:String, recipientAddress:String) {
        Log.d("Beldex","Set recipient address fun called")
        val database = databaseHelper.writableDatabase
        val contentValues = ContentValues(2)
        contentValues.put(BchatRecipientAddressDatabase.transactionId, transactionId)
        Log.d("Beldex","Set contact fun called transaction id $transactionId")
        contentValues.put(BchatRecipientAddressDatabase.recipientAddress, recipientAddress)
        Log.d("Beldex","Set contact fun called recipientAddress $recipientAddress")
        database.insertOrUpdate(bchatRecipientAddressTable, contentValues, "$transactionId = ?", arrayOf( transactionId ))
        notifyConversationListListeners()
    }
}