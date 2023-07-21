package com.beldex.libbchat.database

import com.beldex.libbchat.messaging.sending_receiving.attachments.*
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.UploadResult
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.messages.SignalServiceAttachmentPointer
import com.beldex.libsignal.messages.SignalServiceAttachmentStream
import java.io.InputStream

interface MessageDataProvider {

    fun getMessageID(serverID: Long): Long?
    fun getMessageID(serverId: Long, threadId: Long): Pair<Long, Boolean>?
    fun getMessageIDs(serverIDs: List<Long>, threadID: Long): Pair<List<Long>, List<Long>>
    fun deleteMessage(messageID: Long, isSms: Boolean)
    fun deleteMessages(messageIDs: List<Long>, threadId: Long, isSms: Boolean)
    fun updateMessageAsDeleted(timestamp: Long, author: String)
    fun getServerHashForMessage(messageID: Long): String?
    fun getDatabaseAttachment(attachmentId: Long): DatabaseAttachment?
    fun getAttachmentStream(attachmentId: Long): BchatServiceAttachmentStream?
    fun getAttachmentPointer(attachmentId: Long): BchatServiceAttachmentPointer?
    fun getSignalAttachmentStream(attachmentId: Long): SignalServiceAttachmentStream?
    fun getScaledSignalAttachmentStream(attachmentId: Long): SignalServiceAttachmentStream?
    fun getSignalAttachmentPointer(attachmentId: Long): SignalServiceAttachmentPointer?
    fun setAttachmentState(attachmentState: AttachmentState, attachmentId: AttachmentId, messageID: Long)
    fun insertAttachment(messageId: Long, attachmentId: AttachmentId, stream : InputStream)
    fun updateAudioAttachmentDuration(attachmentId: AttachmentId, durationMs: Long, threadId: Long)
    fun isMmsOutgoing(mmsMessageId: Long): Boolean
    fun isOutgoingMessage(mmsId: Long): Boolean
    fun handleSuccessfulAttachmentUpload(attachmentId: Long, attachmentStream: SignalServiceAttachmentStream, attachmentKey: ByteArray, uploadResult: UploadResult)
    fun handleFailedAttachmentUpload(attachmentId: Long)
    fun getMessageForQuote(timestamp: Long, author: Address): Pair<Long, Boolean>?
    fun getAttachmentsAndLinkPreviewFor(mmsId: Long): List<Attachment>
    fun getMessageBodyFor(timestamp: Long, author: String): String
    fun getAttachmentIDsFor(messageID: Long): List<Long>
    fun getLinkPreviewAttachmentIDFor(messageID: Long): Long?
    fun getIndividualRecipientForMms(mmsId: Long): Recipient?
}