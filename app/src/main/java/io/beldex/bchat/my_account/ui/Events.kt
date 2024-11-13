package io.beldex.bchat.my_account.ui

import android.content.Context
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.database.model.ThreadRecord

sealed interface BlockedContactEvents {
    data object MultiSelectClicked: BlockedContactEvents
    data class UnblockSingleContact(val contact: Recipient): BlockedContactEvents
    data object UnblockMultipleContact: BlockedContactEvents
    data class AddContactToUnBlockList(val contact: Recipient, val add: Boolean): BlockedContactEvents
}

sealed interface MessageRequestEvents {
    data class DeleteRequest(val request: ThreadRecord): MessageRequestEvents
    data class BlockRequest(val request: ThreadRecord): MessageRequestEvents
    data class RequestSelected(val request: ThreadRecord): MessageRequestEvents
    data object AcceptAllRequest: MessageRequestEvents
    data object ClearAllRequest: MessageRequestEvents
}

sealed interface AddressBookEvents {
    data class SearchQueryChanged(val query: String): AddressBookEvents

}

sealed interface ArchiveChatsEvents {
    data class UnArchiveChats(val thread : ThreadRecord) : ArchiveChatsEvents

    data class BlockConversation(val thread : ThreadRecord) : ArchiveChatsEvents

    data class UnBlockConversation(val thread : ThreadRecord) : ArchiveChatsEvents

    data class MuteNotification(val thread : ThreadRecord, val index : Int, val context : Context) :
        ArchiveChatsEvents

    data class NotificationSettings(
        val thread : ThreadRecord, val index : Int, val context : Context
    ) : ArchiveChatsEvents

    data class MarkAsRead(val thread : ThreadRecord) : ArchiveChatsEvents

    data class DeleteConversation(val thread : ThreadRecord, val context : Context) :
        ArchiveChatsEvents
}
