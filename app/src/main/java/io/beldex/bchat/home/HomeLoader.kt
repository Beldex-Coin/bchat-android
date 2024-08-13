package io.beldex.bchat.home

import android.content.Context
import android.database.Cursor
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.AbstractCursorLoader

class HomeLoader(context: Context, val onNewCursor: (Cursor?) -> Unit) : AbstractCursorLoader(context) {

    override fun getCursor(): Cursor {
        /*Hales63*/
        return DatabaseComponent.get(context).threadDatabase().approvedConversationList
    }

    override fun deliverResult(newCursor: Cursor?) {
        super.deliverResult(newCursor)
        onNewCursor(newCursor)
    }
}