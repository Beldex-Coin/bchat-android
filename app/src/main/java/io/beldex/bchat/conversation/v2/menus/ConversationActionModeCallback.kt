package io.beldex.bchat.conversation.v2.menus

import android.content.Context
import android.view.ActionMode
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.ConversationAdapter
import io.beldex.bchat.database.model.MediaMmsMessageRecord
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import androidx.core.view.size
import androidx.core.view.get
import com.beldex.libbchat.utilities.getColorFromAttr

class ConversationActionModeCallback(private val adapter: ConversationAdapter, private val threadID: Long,
    private val context: Context) : ActionMode.Callback {
    var delegate: ConversationActionModeCallbackDelegate? = null

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val themedContext = ContextThemeWrapper(context, context.theme)
        val inflater = MenuInflater(themedContext)
        inflater.inflate(R.menu.menu_conversation_item_action, menu)
        updateActionModeMenu(menu)


        // tint icons manually as it seems the xml color is ignored, in spite of the context theme wrapper
        val tintColor = context.getColorFromAttr(android.R.attr.textColorPrimary)

        for (i in 0 until menu.size) {
            val menuItem = menu[i]
            menuItem.icon?.setTint(tintColor)
        }

        return true
    }

    fun updateActionModeMenu(menu: Menu) {
        // Prepare
        val selectedItems = adapter.selectedItems
        val containsControlMessage = selectedItems.any { it.isUpdate }
        val containsContacts = selectedItems.any { it.isSharedContact }
        val hasText = selectedItems.any { it.body.isNotEmpty() }
        if (selectedItems.isEmpty()) { return }
        val firstMessage = selectedItems.iterator().next()
        val openGroup = DatabaseComponent.get(context).beldexThreadDatabase().getOpenGroupChat(threadID)
        val thread = DatabaseComponent.get(context).threadDatabase().getRecipientForThreadId(threadID)!!
        val userPublicKey = TextSecurePreferences.getLocalNumber(context)!!
        fun userCanBanSelectedUsers(): Boolean {
            if (openGroup == null) { return false }
            val anySentByCurrentUser = selectedItems.any { it.isOutgoing }
            if (anySentByCurrentUser) { return false } // Users can't ban themselves
            val selectedUsers = selectedItems.map { it.recipient.address.toString() }.toSet()
            if (selectedUsers.size > 1) { return false }
            return OpenGroupAPIV2.isUserModerator(userPublicKey, openGroup.room, openGroup.server)
        }
        // Delete message
        menu.findItem(R.id.menu_context_delete_message).isVisible = true
        // Ban user
        menu.findItem(R.id.menu_context_ban_user).isVisible = userCanBanSelectedUsers()
        // Ban and delete all
        menu.findItem(R.id.menu_context_ban_and_delete_all).isVisible = userCanBanSelectedUsers()
        // Copy message text
        menu.findItem(R.id.menu_context_copy).isVisible = !containsControlMessage && !containsContacts && hasText
        // Copy Bchat ID
        menu.findItem(R.id.menu_context_copy_public_key).isVisible =
            (thread.isGroupRecipient && !thread.isOpenGroupRecipient && selectedItems.size == 1 && firstMessage.individualRecipient.address.toString() != userPublicKey)
        // Message detail
        menu.findItem(R.id.menu_message_details).isVisible = (selectedItems.size == 1 && firstMessage.isOutgoing && !firstMessage.isPending)
        // Resend
        menu.findItem(R.id.menu_context_resend).isVisible = (selectedItems.size == 1 && firstMessage.isFailed)
        // Save media
        menu.findItem(R.id.menu_context_save_attachment).isVisible = (selectedItems.size == 1
            && firstMessage.isMms && (firstMessage as MediaMmsMessageRecord).containsMediaSlide())
        // Reply
        menu.findItem(R.id.menu_context_reply).isVisible =
            (selectedItems.size == 1 && !firstMessage.isPending && !firstMessage.isFailed)
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selectedItems = adapter.selectedItems.toSet()
        when (item.itemId) {
            R.id.menu_context_delete_message -> delegate?.deleteMessages(selectedItems)
            R.id.menu_context_ban_user -> delegate?.banUser(selectedItems)
            R.id.menu_context_ban_and_delete_all -> delegate?.banAndDeleteAll(selectedItems)
            R.id.menu_context_copy -> delegate?.copyMessages(selectedItems)
            R.id.menu_context_copy_public_key -> delegate?.copyBchatID(selectedItems)
            R.id.menu_context_resend -> delegate?.resendMessage(selectedItems)
            R.id.menu_message_details -> delegate?.showMessageDetail(selectedItems)
            R.id.menu_context_save_attachment -> delegate?.saveAttachment(selectedItems)
            R.id.menu_context_reply -> delegate?.reply(selectedItems)
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        adapter.selectedItems.clear()
        adapter.notifyDataSetChanged()
        delegate?.destroyActionMode()
    }
}

interface ConversationActionModeCallbackDelegate {
    fun selectMessages(messages : Set<MessageRecord>, position : Int)

    fun deleteMessages(messages: Set<MessageRecord>)
    fun banUser(messages: Set<MessageRecord>)
    fun banAndDeleteAll(messages: Set<MessageRecord>)
    fun copyMessages(messages: Set<MessageRecord>)
    fun copyBchatID(messages: Set<MessageRecord>)
    fun resendMessage(messages: Set<MessageRecord>)
    fun showMessageDetail(messages: Set<MessageRecord>)
    fun saveAttachment(messages: Set<MessageRecord>)
    fun reply(messages: Set<MessageRecord>)
    fun destroyActionMode()
}