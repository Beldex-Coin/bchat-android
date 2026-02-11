package io.beldex.bchat.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ArchiveChatCountRepository {

    private val _archiveCount = MutableStateFlow(0)
    val archiveCount = _archiveCount.asStateFlow()

    fun updateArchiveCount(count: Int) {
        _archiveCount.value = count
    }
}