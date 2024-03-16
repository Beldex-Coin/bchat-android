package com.thoughtcrimes.securesms.home

import android.view.View
import com.thoughtcrimes.securesms.database.model.ThreadRecord

interface ConversationClickListener {
    fun onConversationClick(thread: ThreadRecord)
    fun onLongConversationClick(thread: ThreadRecord, view: View)
    fun showMessageRequests()
    fun hideMessageRequests()
}