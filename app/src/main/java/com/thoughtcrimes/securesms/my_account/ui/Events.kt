package com.thoughtcrimes.securesms.my_account.ui

import com.beldex.libbchat.utilities.recipients.Recipient

sealed interface BlockedContactEvents {
    data object MultiSelectClicked: BlockedContactEvents
    data class UnblockSingleContact(val contact: Recipient): BlockedContactEvents
    data object UnblockMultipleContact: BlockedContactEvents
    data class AddContactToUnBlockList(val contact: Recipient, val add: Boolean): BlockedContactEvents
}