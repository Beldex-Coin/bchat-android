package io.beldex.bchat.messagerequests

import android.content.Context
import android.database.Cursor
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.AbstractCursorLoader

class MessageRequestsLoader(context: Context) : AbstractCursorLoader(context) {

    override fun getCursor(): Cursor {
        return DatabaseComponent.get(context).threadDatabase().unapprovedConversationList
    }
}