package com.beldex.libbchat.messaging.messages.visible

import com.goterl.lazysodium.BuildConfig
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.messages.Message
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.Log
import com.fasterxml.jackson.annotation.JsonFormat
import com.google.protobuf.TextFormat.printToString
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment as SignalAttachment

class VisibleMessage : Message()  {
    /** In the case of a sync message, the public key of the person the message was targeted at.
     *
     * **Note:** `nil` if this isn't a sync message.
     */
    var syncTarget: String? = null
    var text: String? = null
    val attachmentIDs: MutableList<Long> = mutableListOf()
    var quote: Quote? = null
    var linkPreview: LinkPreview? = null
    var profile: Profile? = null
    var openGroupInvitation: OpenGroupInvitation? = null
    var beldexAddress:String?= null
    //Payment Tag
    var payment: Payment? = null
    var reaction: Reaction? = null
    override val isSelfSendValid: Boolean = true

    // region Validation
    override fun isValid(): Boolean {
        Log.d("DataMessage body VisibleMessage->","yes")
        if (!super.isValid()) return false
        if (attachmentIDs.isNotEmpty()) return true
        if (openGroupInvitation != null) return true
        if (payment !=null) return true //Payment Tag
        if (reaction != null) return true
        val text = text?.trim() ?: return false
        return text.isNotEmpty()
    }
    // endregion

    // region Proto Conversion
    companion object {
        const val TAG = "VisibleMessage"

        fun fromProto(proto: SignalServiceProtos.Content, bAddress: String): VisibleMessage? {
            val dataMessage = proto.dataMessage ?: return null
            val result = VisibleMessage()
            //New Line
            result.beldexAddress=bAddress

            if (dataMessage.hasSyncTarget()) { result.syncTarget = dataMessage.syncTarget }
            result.text = dataMessage.body
            // Attachments are handled in MessageReceiver
            val quoteProto = if (dataMessage.hasQuote()) dataMessage.quote else null
            if (quoteProto != null) {
                val quote = Quote.fromProto(quoteProto)
                result.quote = quote
            }
            val linkPreviewProto = dataMessage.previewList.firstOrNull()
            if (linkPreviewProto != null) {
                val linkPreview = LinkPreview.fromProto(linkPreviewProto)
                result.linkPreview = linkPreview
            }
            Log.d("DataMessage opengroup-> ",dataMessage.hasOpenGroupInvitation().toString())
            val openGroupInvitationProto = if (dataMessage.hasOpenGroupInvitation()) dataMessage.openGroupInvitation else null
            if (openGroupInvitationProto != null) {
                val openGroupInvitation = OpenGroupInvitation.fromProto(openGroupInvitationProto)
                result.openGroupInvitation = openGroupInvitation
            }
            Log.d("DataMessage payment-> ",dataMessage.hasPayment().toString())
            //Payment Tag
            val paymentProto = if (dataMessage.hasPayment()) dataMessage.payment else null
            if (paymentProto != null) {
                val payment = Payment.fromProto(paymentProto)
                result.payment = payment
            }
            // TODO Contact
            val profile = Profile.fromProto(dataMessage)
            if (profile != null) { result.profile = profile }
            val reactionProto = if (dataMessage.hasReaction()) dataMessage.reaction else null
            if (reactionProto != null) {
                val reaction = Reaction.fromProto(reactionProto)
                result.reaction = reaction
            }
            return  result
        }
    }

    override fun toProto(): SignalServiceProtos.Content? {
        val proto = SignalServiceProtos.Content.newBuilder()
        val dataMessage: SignalServiceProtos.DataMessage.Builder
        // Profile
        val profileProto = profile?.toProto()
        dataMessage = if (profileProto != null) {
            profileProto.toBuilder()
        } else {
            SignalServiceProtos.DataMessage.newBuilder()
        }
        // Text
        if (text != null) { dataMessage.body = text }
        // Quote
        val quoteProto = quote?.toProto()
        if (quoteProto != null) {
            dataMessage.quote = quoteProto
        }
        // Reaction
        val reactionProto = reaction?.toProto()
        if (reactionProto != null) {
            dataMessage.reaction = reactionProto
        }

        // Link preview
        val linkPreviewProto = linkPreview?.toProto()
        if (linkPreviewProto != null) {
            dataMessage.addAllPreview(listOf(linkPreviewProto))
        }
        // Social group invitation
        val openGroupInvitationProto = openGroupInvitation?.toProto()
        if (openGroupInvitationProto != null) {
            dataMessage.openGroupInvitation = openGroupInvitationProto
        }
        // Payment Tag
        val paymentProto = payment?.toProto()
        if (paymentProto != null) {
            dataMessage.payment = paymentProto
        }
        // Attachments
        val database = MessagingModuleConfiguration.shared.messageDataProvider
        val attachments = attachmentIDs.mapNotNull { database.getSignalAttachmentPointer(it) }
        if (attachments.any { it.url.isNullOrEmpty() }) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Sending a message before all associated attachments have been uploaded.")
            }
        }
        val pointers = attachments.mapNotNull { Attachment.createAttachmentPointer(it) }
        dataMessage.addAllAttachments(pointers)
        // TODO: Contact
        // Expiration timer
        // TODO: We * want * expiration timer updates to be explicit. But currently Android will disable the expiration timer for a conversation
        //       if it receives a message without the current expiration timer value attached to it...
        val storage = MessagingModuleConfiguration.shared.storage
        val context = MessagingModuleConfiguration.shared.context
        val expiration = if (storage.isClosedGroup(recipient!!)) {
            Recipient.from(context, Address.fromSerialized(GroupUtil.doubleEncodeGroupID(recipient!!)), false).expireMessages
        } else {
            Recipient.from(context, Address.fromSerialized(recipient!!), false).expireMessages
        }
        dataMessage.expireTimer = expiration
        // Group context
        if (storage.isClosedGroup(recipient!!)) {
            try {
                setGroupContext(dataMessage)
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't construct visible message proto from: $this")
                return null
            }
        }
        // Sync target
        if (syncTarget != null) {
            dataMessage.syncTarget = syncTarget
        }
        // Build
        return try {
            proto.dataMessage = dataMessage.build()
            proto.build()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't construct visible message proto from: $this")
            null
        }
    }
    // endregion

    fun addSignalAttachments(signalAttachments: List<SignalAttachment>) {
        val attachmentIDs = signalAttachments.map {
            val databaseAttachment = it as DatabaseAttachment
            databaseAttachment.attachmentId.rowId
        }
        this.attachmentIDs.addAll(attachmentIDs)
    }

    fun isMediaMessage(): Boolean {
        return attachmentIDs.isNotEmpty() || quote != null || linkPreview != null || reaction != null
    }
}