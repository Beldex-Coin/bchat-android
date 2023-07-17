package com.thoughtcrimes.securesms.home

import android.content.Context
import android.database.Cursor
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.util.AbstractCursorLoader

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