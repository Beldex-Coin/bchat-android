package com.thoughtcrimes.securesms.home.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beldex.libsignal.utilities.SettableFuture
import com.thoughtcrimes.securesms.database.MmsSmsDatabase
import com.thoughtcrimes.securesms.search.SearchRepository
import com.thoughtcrimes.securesms.search.SearchResults
import com.thoughtcrimes.securesms.search.model.SearchResult
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.beldex.bchat.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val mmsSmsDatabase: MmsSmsDatabase,
    private val sharedPreferenceUtil: SharedPreferenceUtil
) : ViewModel() {

    private val executor = viewModelScope + SupervisorJob()

    private val _result: MutableStateFlow<GlobalSearchResult> =
            MutableStateFlow(GlobalSearchResult.EMPTY)

    val result: StateFlow<GlobalSearchResult> = _result

    private val _queryText: MutableStateFlow<CharSequence> = MutableStateFlow("")
    val queryText: StateFlow<CharSequence> = _queryText.asStateFlow()

    private val _searchResults: MutableStateFlow<List<SearchResults>> = MutableStateFlow(emptyList())
    val searchResults: StateFlow<List<SearchResults>> = _searchResults.asStateFlow()

    private val publicKey by lazy {
        sharedPreferenceUtil.getPublicKey()
    }

    fun postQuery(charSequence: CharSequence?) {
        println("")
        charSequence ?: return
        _queryText.value = charSequence
    }

    init {
        _queryText
                .buffer(onBufferOverflow = BufferOverflow.DROP_OLDEST)
                .mapLatest { query ->
                    if (query.trim().length < 2) {
                        SearchResult.EMPTY
                    } else {
                        // user input delay here in case we get a new query within a few hundred ms
                        // this coroutine will be cancelled and expensive query will not be run if typing quickly
                        // first query of 2 characters will be instant however
                        delay(300)
                        val settableFuture =
                            SettableFuture<SearchResult>()
                        searchRepository.query(query.toString(), settableFuture::set)
                        try {
                            // search repository doesn't play nicely with suspend functions (yet)
                            settableFuture.get(10_000, TimeUnit.MILLISECONDS)
                        } catch (e: Exception) {
                            SearchResult.EMPTY
                        }
                    }
                }
                .onEach { result ->
                    // update the latest _result value
                    _result.value = GlobalSearchResult.from(result)
                    prepareData(this.result.value)
                }
                .launchIn(executor)
    }

    private fun prepareData(result: GlobalSearchResult) {
        val contactAndGroupList =
            result.contacts.map { SearchResults.Contact(it) } +
                    result.threads.map { SearchResults.GroupConversation(it) }

        val contactResults = contactAndGroupList.toMutableList()

        if (contactResults.isEmpty()) {
            contactResults.add(
                SearchResults.SavedMessages(
                    publicKey
                )
            )
        }

        val userIndex =
            contactResults.indexOfFirst { it is SearchResults.Contact && it.contact.bchatID == publicKey }
        if (userIndex >= 0) {
            contactResults[userIndex] =
                SearchResults.SavedMessages(publicKey)
        }

        if (contactResults.isNotEmpty()) {
            contactResults.add(
                0,
                SearchResults.Header(R.string.global_search_contacts_groups)
            )
        }

        val unreadThreadMap = result.messages
            .groupBy { it.threadId }.keys.associateWith {
                mmsSmsDatabase.getUnreadCount(
                    it
                )
            }

        val messageResults: MutableList<SearchResults> = result.messages
            .map { messageResult ->
                SearchResults.Message(
                    messageResult,
                    unreadThreadMap[messageResult.threadId] ?: 0
                )
            }.toMutableList()

        if (messageResults.isNotEmpty()) {
            messageResults.add(
                0,
                SearchResults.Header(R.string.global_search_messages)
            )
        }
        _searchResults.update { contactResults + messageResults }
    }


}