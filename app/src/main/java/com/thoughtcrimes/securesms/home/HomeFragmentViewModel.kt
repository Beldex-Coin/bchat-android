package com.thoughtcrimes.securesms.home

import android.database.Cursor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeFragmentViewModel @Inject constructor(
    private val threadDb: ThreadDatabase,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val _conversationList = MutableStateFlow<Cursor?>(null)
    val conversationList: StateFlow<Cursor?> = _conversationList

    init {
        loadConversations()
    }

    private fun loadConversations() {
        _conversationList.value = threadDb.approvedConversationList
    }

}