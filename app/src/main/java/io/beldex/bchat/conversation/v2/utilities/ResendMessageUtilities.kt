package io.beldex.bchat.conversation.v2.utilities

import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.messages.visible.LinkPreview
import com.beldex.libbchat.messaging.messages.visible.OpenGroupInvitation
import com.beldex.libbchat.messaging.messages.visible.Quote
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.database.model.MmsMessageRecord

object ResendMessageUtilities {

    fun resend(messageRecord: MessageRecord) {
        val recipient: Recipient = messageRecord.recipient
        val message = VisibleMessage()
        message.id = messageRecord.getId()
        if (messageRecord.isOpenGroupInvitation) {
            val openGroupInvitation = OpenGroupInvitation()
            UpdateMessageData.fromJSON(messageRecord.body)?.let { updateMessageData ->
                val kind = updateMessageData.kind
                if (kind is UpdateMessageData.Kind.OpenGroupInvitation) {
                    openGroupInvitation.name = kind.groupName
                    openGroupInvitation.url = kind.groupUrl
                }
            }
            message.openGroupInvitation = openGroupInvitation
        } else {
            message.text = messageRecord.body
        }
        message.sentTimestamp = messageRecord.timestamp
        if (recipient.isGroupRecipient) {
            message.groupPublicKey = recipient.address.toGroupString()
        } else {
            message.recipient = messageRecord.recipient.address.serialize()
        }
        message.threadID = messageRecord.threadId
        if (messageRecord.isMms) {
            val mmsMessageRecord = messageRecord as MmsMessageRecord
            if (mmsMessageRecord.linkPreviews.isNotEmpty()) {
                message.linkPreview = LinkPreview.from(mmsMessageRecord.linkPreviews[0])
            }
            if (mmsMessageRecord.quote != null) {
                message.quote = Quote.from(mmsMessageRecord.quote!!.quoteModel)
            }
            message.addSignalAttachments(mmsMessageRecord.slideDeck.asAttachments())
        }
        val sentTimestamp = message.sentTimestamp
        val sender = MessagingModuleConfiguration.shared.storage.getUserPublicKey()
        if (sentTimestamp != null && sender != null) {
            MessagingModuleConfiguration.shared.storage.markAsSending(sentTimestamp, sender)
        }
        MessageSender.send(message, recipient.address)
    }
}