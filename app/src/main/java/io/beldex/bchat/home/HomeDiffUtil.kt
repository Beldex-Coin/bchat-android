package io.beldex.bchat.home

import android.content.Context
import androidx.recyclerview.widget.DiffUtil
import io.beldex.bchat.database.model.ThreadRecord

class HomeDiffUtil(
        private val old: List<ThreadRecord>,
        private val new: List<ThreadRecord>,
        private val context: Context,
        private val hiddenRequestCounts: Boolean
): DiffUtil.Callback() {

    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].threadId == new[newItemPosition].threadId

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = old[oldItemPosition]
        val newItem = new[newItemPosition]

        val sameMessageRequestCount = oldItem.messageRequestCount == newItem.messageRequestCount
        if(hiddenRequestCounts && !sameMessageRequestCount) return false

//        if (oldItem.threadId == -1000L || newItem.threadId == -1000L)
//            return true

        if (oldItem.recipient == null || newItem.recipient == null)
            return true

        // return early to save getDisplayBody or expensive calls
        val sameCount = oldItem.count == newItem.count
        if (!sameCount) return false
        val sameUnreads = oldItem.unreadCount == newItem.unreadCount
        if (!sameUnreads) return false
        val samePinned = oldItem.isPinned == newItem.isPinned
        if (!samePinned) return false
        val sameAvatar = oldItem.recipient.profileAvatar == newItem.recipient.profileAvatar
        if (!sameAvatar) return false
//        val sameUsername = oldItem.recipient.name == newItem.recipient.name
        /*recipient.name is replaced by new variable as recipient.name was causing home screen to
        * not update the list after nickname was updated from sheet*/
        val sameUsername = oldItem.nickName == newItem.nickName
        if (!sameUsername) return false
        val sameSnippet = oldItem.getDisplayBody(context) == newItem.getDisplayBody(context)
        if (!sameSnippet) return false
        val sameSendStatus = oldItem.isFailed == newItem.isFailed && oldItem.isDelivered == newItem.isDelivered
                && oldItem.isSent == newItem.isSent && oldItem.isPending == newItem.isPending
        if (!sameSendStatus) return false

        // all same
        return true
    }

}