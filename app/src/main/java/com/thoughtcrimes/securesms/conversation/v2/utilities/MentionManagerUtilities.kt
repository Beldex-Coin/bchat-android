package com.thoughtcrimes.securesms.conversation.v2.utilities

import android.content.Context
import com.beldex.libbchat.messaging.mentions.MentionsManager
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.database.model.MessageRecord
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent

object MentionManagerUtilities {

    fun populateUserPublicKeyCacheIfNeeded(threadID: Long, context: Context) {

        val result = mutableSetOf<String>()
        val recipient = DatabaseComponent.get(context).threadDatabase().getRecipientForThreadId(threadID) ?: return
        if (recipient.address.isClosedGroup) {
            val members = DatabaseComponent.get(context).groupDatabase().getGroupMembers(recipient.address.toGroupString(), false).map { it.address.serialize() }
            result.addAll(members)
        } else {
            val messageDatabase = DatabaseComponent.get(context).mmsSmsDatabase()
            /*Hales63*/
            val reader = messageDatabase.readerFor(messageDatabase.getConversation(threadID,true, 0, 200))
            var record: MessageRecord? = reader.next
            while (record != null) {
                result.add(record.individualRecipient.address.serialize())
                try {
                    record = reader.next
                } catch (exception: Exception) {
                    record = null
                }
            }
            reader.close()
            result.add(TextSecurePreferences.getLocalNumber(context)!!)
        }
        MentionsManager.userPublicKeyCache[threadID] = result
    }
}