package com.thoughtcrimes.securesms.home

import com.thoughtcrimes.securesms.database.model.ThreadRecord

interface ConversationClickListener {
    fun onConversationClick(thread: ThreadRecord)
    fun onLongConversationClick(thread: ThreadRecord)
    fun showMessageRequests()
    fun hideMessageRequests()
}