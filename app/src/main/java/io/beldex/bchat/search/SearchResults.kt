package io.beldex.bchat.search

import android.os.Parcelable
import androidx.annotation.StringRes
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupRecord
import io.beldex.bchat.search.model.MessageResult
import kotlinx.parcelize.Parcelize

sealed interface SearchResults {
    data class Header(@StringRes val title: Int) : SearchResults
    data class SavedMessages(val currentUserPublicKey: String): SearchResults
    data class Contact(val contact: com.beldex.libbchat.messaging.contacts.Contact) : SearchResults
    data class GroupConversation(val groupRecord: GroupRecord) : SearchResults
    data class Message(val messageResult: MessageResult, val unread: Int) : SearchResults
}

@Parcelize
sealed interface SearchActivityResults: Parcelable {
    @Parcelize
    data class Message(val threadId: Long, val timeStamp: Long, val author: Address): SearchActivityResults, Parcelable
    @Parcelize
    data class SavedMessage(val address: Address): SearchActivityResults, Parcelable
    @Parcelize
    data class Contact(val address: Address): SearchActivityResults, Parcelable
    @Parcelize
    data class GroupConversation(val groupEncodedId: String): SearchActivityResults, Parcelable
}