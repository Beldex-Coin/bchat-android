package io.beldex.bchat.conversation.v2

import android.content.Context
import android.database.Cursor
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.AbstractCursorLoader

class ConversationLoader(private val threadID: Long, private val reverse: Boolean, context: Context) : AbstractCursorLoader(context) {

    override fun getCursor(): Cursor {
        return DatabaseComponent.get(context).mmsSmsDatabase().getConversation(threadID, reverse)
    }
}