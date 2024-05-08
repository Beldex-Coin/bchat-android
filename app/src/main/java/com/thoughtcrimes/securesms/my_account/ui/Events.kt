package com.thoughtcrimes.securesms.my_account.ui

import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.database.model.ThreadRecord

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
