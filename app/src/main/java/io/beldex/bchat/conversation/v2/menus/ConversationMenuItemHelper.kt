package io.beldex.bchat.conversation.v2.menus

import android.content.Context
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.messaging.open_groups.OpenGroupV2
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.groups.OpenGroupManager
object ConversationMenuItemHelper {

    @JvmStatic
    fun userCanDeleteSelectItems(messageRecord: MessageRecord, openGroup: OpenGroupV2?, userPublicKey: String): Boolean {
        val allSentByCurrentUser = messageRecord.isOutgoing
        val allReceivedByCurrentUser =  !messageRecord.isOutgoing
        if (openGroup == null) { return allSentByCurrentUser || allReceivedByCurrentUser }
        if (allSentByCurrentUser) { return true }
        return OpenGroupAPIV2.isUserModerator(userPublicKey, openGroup.room, openGroup.server)
    }
    @JvmStatic
    fun userCanBanSelectUsers( message: MessageRecord, openGroup: OpenGroupV2?, userPublicKey: String): Boolean {
        if (openGroup == null) { return false }
        val anySentByCurrentUser =   message.isOutgoing
        if (anySentByCurrentUser) { return false } // Users can't ban themselves
        val selectedUsers =   message.recipient.address.toString().toSet()
        if (selectedUsers.size > 1) { return false }
        return OpenGroupAPIV2.isUserModerator(userPublicKey, openGroup.room, openGroup.server)
    }
}