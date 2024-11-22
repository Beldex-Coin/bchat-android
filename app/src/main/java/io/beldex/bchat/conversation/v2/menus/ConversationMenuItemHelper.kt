package io.beldex.bchat.conversation.v2.menus

import android.content.Context
import com.beldex.libbchat.messaging.open_groups.OpenGroupV2
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.groups.OpenGroupManager
object ConversationMenuItemHelper {
    //need to check
   /* @JvmStatic
    fun userCanDeleteSelectedItems(context: Context, message: MessageRecord, openGroup: OpenGroupV2?, userPublicKey: String): Boolean {
        if (openGroup  == null) return message.isOutgoing || !message.isOutgoing
        if (message.isOutgoing) return true
        return OpenGroupManager.isUserModerator(context, openGroup.groupId, userPublicKey)
    }
    @JvmStatic
    fun userCanBanSelectedUsers(context: Context, message: MessageRecord, openGroup: OpenGroupV2?, userPublicKey: String): Boolean {
        if (openGroup == null)  return false
        if (message.isOutgoing) return false // Users can't ban themselves
        return OpenGroupManager.isUserModerator(context, openGroup.groupId, userPublicKey)
    }*/
}