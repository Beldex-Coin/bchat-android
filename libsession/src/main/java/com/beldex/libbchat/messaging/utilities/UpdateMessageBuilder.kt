package com.beldex.libbchat.messaging.utilities

import android.content.Context
import com.beldex.libbchat.R
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.calls.CallMessageType
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.sending_receiving.data_extraction.DataExtractionNotificationInfoMessage
import com.beldex.libbchat.utilities.ExpirationUtil
import com.beldex.libbchat.utilities.truncateIdForDisplay

object UpdateMessageBuilder {

    fun buildGroupUpdateMessage(context: Context, updateMessageData: UpdateMessageData, senderId: String? = null, isOutgoing: Boolean = false): String {
        var message = ""
        val updateData = updateMessageData.kind ?: return message
        if (!isOutgoing && senderId == null) return message
        val storage = MessagingModuleConfiguration.shared.storage
        val senderName: String = if (!isOutgoing) {
            storage.getContactWithBchatID(senderId!!)?.displayName(Contact.ContactContext.REGULAR) ?: truncateIdForDisplay(senderId)
        } else { context.getString(R.string.MessageRecord_you) }

        when (updateData) {
            is UpdateMessageData.Kind.GroupCreation -> {
                message = if (isOutgoing) {
                    context.getString(R.string.MessageRecord_you_created_a_new_group)
                } else {
                    context.getString(R.string.MessageRecord_s_added_you_to_the_group, senderName)
                }
            }
            is UpdateMessageData.Kind.GroupNameChange -> {
                message = if (isOutgoing) {
                    context.getString(R.string.MessageRecord_you_renamed_the_group_to_s, updateData.name)
                } else {
                    context.getString(R.string.MessageRecord_s_renamed_the_group_to_s, senderName, updateData.name)
                }
            }
            is UpdateMessageData.Kind.GroupMemberAdded -> {
                val members = updateData.updatedMembers.joinToString(", ") {
                    storage.getContactWithBchatID(it)?.displayName(Contact.ContactContext.REGULAR) ?: it
                }
                message = if (isOutgoing) {
                    context.getString(R.string.MessageRecord_you_added_s_to_the_group, members)
                } else {
                    context.getString(R.string.MessageRecord_s_added_s_to_the_group, senderName, members)
                }
            }
            is UpdateMessageData.Kind.GroupMemberRemoved -> {
                val storage = MessagingModuleConfiguration.shared.storage
                val userPublicKey = storage.getUserPublicKey()!!
                // 1st case: you are part of the removed members
                message = if (userPublicKey in updateData.updatedMembers) {
                    if (isOutgoing) {
                        context.getString(R.string.MessageRecord_left_group)
                    } else {
                        context.getString(R.string.MessageRecord_you_were_removed_from_the_group)
                    }
                } else {
                    // 2nd case: you are not part of the removed members
                    val members = updateData.updatedMembers.joinToString(", ") {
                        storage.getContactWithBchatID(it)?.displayName(Contact.ContactContext.REGULAR) ?: it
                    }
                    if (isOutgoing) {
                        context.getString(R.string.MessageRecord_you_removed_s_from_the_group, members)
                    } else {
                        context.getString(R.string.MessageRecord_s_removed_s_from_the_group, senderName, members)
                    }
                }
            }
            is UpdateMessageData.Kind.GroupMemberLeft -> {
                message = if (isOutgoing) {
                    context.getString(R.string.MessageRecord_left_group)
                } else {
                    context.getString(R.string.ConversationItem_group_action_left, senderName)
                }
            }
            else -> Unit
        }
        return message
    }

    fun buildExpirationTimerMessage(context: Context, duration: Long, senderId: String? = null, isOutgoing: Boolean = false): String {
        if (!isOutgoing && senderId == null) return ""
        val storage = MessagingModuleConfiguration.shared.storage
        val senderName: String? = if (!isOutgoing) {
            storage.getContactWithBchatID(senderId!!)?.displayName(Contact.ContactContext.REGULAR) ?: truncateIdForDisplay(senderId)
        } else { context.getString(R.string.MessageRecord_you) }
        return if (duration <= 0) {
            if (isOutgoing) context.getString(R.string.MessageRecord_you_disabled_disappearing_messages)
            else context.getString(R.string.MessageRecord_s_disabled_disappearing_messages, senderName)
        } else {
            val time = ExpirationUtil.getExpirationDisplayValue(context, duration.toInt())
            if (isOutgoing)context.getString(R.string.MessageRecord_you_set_disappearing_message_time_to_s, time)
            else context.getString(R.string.MessageRecord_s_set_disappearing_message_time_to_s, senderName, time)
        }
    }

    fun buildDataExtractionMessage(context: Context, kind: DataExtractionNotificationInfoMessage.Kind, senderId: String? = null): String {
        val storage = MessagingModuleConfiguration.shared.storage
        val senderName = storage.getContactWithBchatID(senderId!!)?.displayName(Contact.ContactContext.REGULAR) ?: truncateIdForDisplay(senderId)
        return when (kind) {
            DataExtractionNotificationInfoMessage.Kind.SCREENSHOT ->
                context.getString(R.string.MessageRecord_s_took_a_screenshot, senderName)
            DataExtractionNotificationInfoMessage.Kind.MEDIA_SAVED ->
                context.getString(R.string.MessageRecord_media_saved_by_s, senderName)
        }
    }

    //New Line
    fun buildCallMessage(context: Context, type: CallMessageType, senderId: String): String {
        val storage = MessagingModuleConfiguration.shared.storage
        val senderName = storage.getContactWithBchatID(senderId)?.displayName(Contact.ContactContext.REGULAR) ?: truncateIdForDisplay(senderId)
        return when (type) {
            CallMessageType.CALL_MISSED ->
                context.getString(R.string.MessageRecord_missed_call_from, senderName)
            CallMessageType.CALL_INCOMING ->
                context.getString(R.string.MessageRecord_received_call_from, senderName)
            CallMessageType.CALL_OUTGOING ->
                context.getString(R.string.MessageRecord_called_s, senderName)
            CallMessageType.CALL_FIRST_MISSED ->
                context.getString(R.string.MessageRecord_missed_call_from, senderName)
        }
    }
}
