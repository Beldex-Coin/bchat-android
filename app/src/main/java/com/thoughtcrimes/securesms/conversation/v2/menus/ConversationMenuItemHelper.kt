package com.thoughtcrimes.securesms.conversation.v2.menus

import android.content.Context
import com.beldex.libbchat.messaging.open_groups.OpenGroupV2
import com.thoughtcrimes.securesms.database.model.MessageRecord
import com.thoughtcrimes.securesms.groups.OpenGroupManager


object ConversationMenuItemHelper {

    /*@JvmStatic
    fun userCanDeleteSelectedItems(context: Context, message: MessageRecord, openGroup: OpenGroupV2?, userPublicKey: String, blindedPublicKey: String?): Boolean {
        if (openGroup  == null) return message.isOutgoing || !message.isOutgoing
        if (message.isOutgoing) return true
        return OpenGroupManager.isUserModerator(context, openGroup.groupId, userPublicKey, blindedPublicKey)
    }*/

  /*  @JvmStatic
    fun userCanBanSelectedUsers(context: Context, message: MessageRecord, openGroup: OpenGroupV2?, userPublicKey: String, blindedPublicKey: String?): Boolean {
        if (openGroup == null)  return false
        if (message.isOutgoing) return false // Users can't ban themselves
        return OpenGroupManager.isUserModerator(context, openGroup.groupId, userPublicKey, blindedPublicKey)
    }*/

}
