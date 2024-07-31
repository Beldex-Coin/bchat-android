package io.beldex.bchat.home.search

import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.GroupRecord
import io.beldex.bchat.search.model.MessageResult
import io.beldex.bchat.search.model.SearchResult

data class GlobalSearchResult(
        val query: String,
        val contacts: List<Contact>,
        val threads: List<GroupRecord>,
        val messages: List<MessageResult>
) {

    val isEmpty: Boolean
        get() = contacts.isEmpty() && threads.isEmpty() && messages.isEmpty()

    companion object {

        val EMPTY = GlobalSearchResult("", emptyList(), emptyList(), emptyList())
        const val SEARCH_LIMIT = 5

        fun from(searchResult: SearchResult): GlobalSearchResult {
            val query = searchResult.query
            val contactList = searchResult.contacts.toList()
            val threads = searchResult.conversations.toList()
            val messages = searchResult.messages.toList()
            searchResult.close()
            return GlobalSearchResult(query, contactList, threads, messages)
        }

    }
}
