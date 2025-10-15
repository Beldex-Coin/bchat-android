package io.beldex.bchat.conversation.v2.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.beldex.libbchat.utilities.Debouncer
import dagger.hilt.android.lifecycle.HiltViewModel
import com.beldex.libbchat.utilities.Util.runOnMain
import io.beldex.bchat.database.CursorList
import io.beldex.bchat.search.SearchRepository
import io.beldex.bchat.search.model.MessageResult
import io.beldex.bchat.util.CloseableLiveData
import java.io.Closeable
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val result: CloseableLiveData<SearchResult> = CloseableLiveData()
    private val debouncer: Debouncer = Debouncer(500)
    private var firstSearch = false
    private var searchOpen = false
    private var activeQuery: String? = null
    private var activeThreadId: Long = 0
    private val _hasSearchResults = MutableLiveData<Boolean>()
    var hasSearchResults: LiveData<Boolean> = _hasSearchResults

    fun updateSearchResult(hasResult: Boolean){
        _hasSearchResults.postValue(hasResult)
    }

    val searchResults: LiveData<SearchResult>
        get() = result

    fun onQueryUpdated(query: String, threadId: Long) {
        if (query == activeQuery) {
            return
        }
        updateQuery(query, threadId)
    }

    fun onMissingResult() {
        if (activeQuery != null) {
            updateQuery(activeQuery!!, activeThreadId)
        }
    }

    fun onMoveUp() {
        debouncer.clear()
        val messages = result.value!!.getResults() as CursorList<MessageResult?>
        val position = Math.min(result.value!!.position + 1, messages.size - 1)
        result.setValue(SearchResult(messages, position), false)
    }

    fun onMoveDown() {
        debouncer.clear()
        val messages = result.value!!.getResults() as CursorList<MessageResult?>
        val position = Math.max(result.value!!.position - 1, 0)
        result.setValue(SearchResult(messages, position), false)
    }

    fun onSearchOpened() {
        searchOpen = true
        firstSearch = true
    }

    fun onSearchClosed() {
        searchOpen = false
        activeQuery = null
        debouncer.clear()
        result.close()
    }

    override fun onCleared() {
        super.onCleared()
        result.close()
    }

    private fun updateQuery(query: String, threadId: Long) {
        activeQuery = query
        activeThreadId = threadId
        debouncer.publish {
            firstSearch = false
            searchRepository.query(query, threadId) { messages: CursorList<MessageResult?> ->
                runOnMain {
                    if (searchOpen && query == activeQuery) {
                        result.setValue(SearchResult(messages, 0))
                    } else {
                        messages.close()
                    }
                }
            }
        }
    }

    class SearchResult(private val results: CursorList<MessageResult?>, val position: Int) : Closeable {

        fun getResults(): List<MessageResult?> {
            return results
        }

        override fun close() {
            results.close()
        }
    }

}