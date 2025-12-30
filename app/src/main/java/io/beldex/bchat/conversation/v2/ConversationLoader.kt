package io.beldex.bchat.conversation.v2

import android.content.Context
import android.database.Cursor
import io.beldex.bchat.database.MmsSmsDatabase
import io.beldex.bchat.util.AbstractCursorLoader

class ConversationLoader(private val threadID: Long, private val reverse: Boolean, context: Context, val mmsSmsDatabase: MmsSmsDatabase) : AbstractCursorLoader(context) {

    override fun getCursor(): Cursor? {
        return mmsSmsDatabase.getConversation(threadID, reverse)
    }
}