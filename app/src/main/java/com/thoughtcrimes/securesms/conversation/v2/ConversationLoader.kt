package com.thoughtcrimes.securesms.conversation.v2

import android.content.Context
import android.database.Cursor
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.util.AbstractCursorLoader

class ConversationLoader(private val threadID: Long, private val reverse: Boolean, context: Context) : AbstractCursorLoader(context) {

    override fun getCursor(): Cursor {
        return DatabaseComponent.get(context).mmsSmsDatabase().getConversation(threadID, reverse)
    }
}