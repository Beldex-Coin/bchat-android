package io.beldex.bchat.home

import io.beldex.bchat.database.model.ThreadRecord

interface ConversationClickListener {
    fun onConversationClick(thread: ThreadRecord)
    fun onLongConversationClick(thread: ThreadRecord)
    fun showMessageRequests()
    fun hideMessageRequests()
}