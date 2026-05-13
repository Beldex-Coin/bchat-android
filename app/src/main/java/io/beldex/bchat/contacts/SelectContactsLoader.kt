package io.beldex.bchat.contacts

import android.content.Context
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.dependencies.DatabaseComponent

class SelectContactsLoader(context: Context, private val usersToExclude: Set<String>) : io.beldex.bchat.util.AsyncLoader<List<String>>(context) {

    override fun loadInBackground(): List<String> {
        val threadDb = DatabaseComponent.get(context).threadDatabase()
        val recipients = mutableListOf<Recipient>()

        threadDb.conversationList.use { cursor ->
            val reader = threadDb.readerFor(cursor)
            while (true) {
                recipients += reader.next?.recipient ?: break
            }
        }

        threadDb.archivedConversationList.use { cursor ->
            val reader = threadDb.readerFor(cursor)
            while (true) {
                recipients += reader.next?.recipient ?: break
            }
        }

        return recipients
            .distinctBy { it.address.serialize() }
            .filter {
                !it.isGroupRecipient &&
                        !usersToExclude.contains(it.address.toString()) &&
                        it.hasApprovedMe() &&
                        !it.isBlocked &&
                        it.isApproved
            }
            .map {
                it.address.toString()
            }
    }
}