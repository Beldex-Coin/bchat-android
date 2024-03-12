package com.thoughtcrimes.securesms.groups

import com.beldex.libbchat.utilities.recipients.Recipient

sealed interface SecretGroupEvents {
    data class RecipientSelectionChanged(val recipient: Recipient, val isSelected: Boolean): SecretGroupEvents
    data class SearchQueryChanged(val query: String): SecretGroupEvents
}

sealed interface OpenGroupEvents {
    data class GroupUrlChanged(val url: String): OpenGroupEvents
}