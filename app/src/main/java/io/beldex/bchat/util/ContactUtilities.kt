package io.beldex.bchat.util

import android.content.Context
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.dependencies.DatabaseComponent

object ContactUtilities {

    @JvmStatic
    fun getAllContacts(context: Context): Set<Recipient> {
        val threadDatabase = DatabaseComponent.get(context).threadDatabase()
        val cursor = threadDatabase.conversationList
        val result = mutableSetOf<Recipient>()
        threadDatabase.readerFor(cursor).use { reader ->
            while (reader.next != null) {
                val thread = reader.current
                val recipient = thread.recipient
                result.add(recipient)
            }
        }
        return result
    }
}