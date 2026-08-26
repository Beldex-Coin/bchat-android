package io.beldex.bchat.home

import android.view.View
import io.beldex.bchat.database.model.ThreadRecord

interface ConversationClickListener {
    fun onConversationClick(thread: ThreadRecord)
    fun onLongConversationClick(thread : ThreadRecord, view : View, position : Int, touchX : Float, touchY : Float)
    fun showMessageRequests()
    fun hideMessageRequests()
}