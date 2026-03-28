package io.beldex.bchat.home

import io.beldex.bchat.database.ThreadDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ArchiveChatCountRepository {

    private val _archiveCount = MutableStateFlow(0)
    val archiveCount = _archiveCount.asStateFlow()

    fun updateArchiveCount(count: Int) {
        _archiveCount.value = count
    }

    fun refreshArchiveCount(threadDb: ThreadDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            val count = threadDb.archivedConversationList.count
            withContext(Dispatchers.Main) {
                _archiveCount.value = count
            }
        }
    }
}