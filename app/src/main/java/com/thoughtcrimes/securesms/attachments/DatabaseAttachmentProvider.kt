package com.thoughtcrimes.securesms.attachments

import android.content.Context
import android.text.TextUtils
import com.google.protobuf.ByteString
import org.greenrobot.eventbus.EventBus
import com.beldex.libbchat.database.MessageDataProvider
import com.beldex.libbchat.messaging.sending_receiving.attachments.*
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.UploadResult
import com.beldex.libbchat.utilities.Util
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.messages.SignalServiceAttachment
import com.beldex.libsignal.messages.SignalServiceAttachmentPointer
import com.beldex.libsignal.messages.SignalServiceAttachmentStream
import com.beldex.libsignal.utilities.Base64
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.guava.Optional
import com.thoughtcrimes.securesms.database.AttachmentDatabase
import com.thoughtcrimes.securesms.database.Database
import com.thoughtcrimes.securesms.database.MessagingDatabase
import com.thoughtcrimes.securesms.database.helpers.SQLCipherOpenHelper
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.events.PartProgressEvent
import com.thoughtcrimes.securesms.mms.MediaConstraints
import com.thoughtcrimes.securesms.mms.PartAuthority
import com.thoughtcrimes.securesms.util.MediaUtil
import java.io.IOException
import java.io.InputStream

class DatabaseAttachmentProvider(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper), MessageDataProvider {

    override fun getAttachmentStream(attachmentId: Long): BchatServiceAttachmentStream? {
        val attachmentDatabase = DatabaseComponent.get(context).attachmentDatabase()
        val databaseAttachment = attachmentDatabase.getAttachment(
            AttachmentId(
                attachmentId,
                0
            )
        ) ?: return null
        return databaseAttachment.toAttachmentStream(context)
    }

    override fun getAttachmentPointer(attachmentId: Long): BchatServiceAttachmentPointer? {
        val attachmentDatabase = DatabaseComponent.get(context).attachmentDatabase()
        val databaseAttachment = attachmentDatabase.getAttachment(
            AttachmentId(
                attachmentId,
                0
            )
        ) ?: return null
        return databaseAttachment.toAttachmentPointer()
    }

    override fun getSignalAttachmentStream(attachmentId: Long): SignalServiceAttachmentStream? {
        val attachmentDatabase = DatabaseComponent.get(context).attachmentDatabase()
        val databaseAttachment = attachmentDatabase.getAttachment(
            AttachmentId(
                attachmentId,
                0
            )
        ) ?: return null
        return databaseAttachment.toSignalAttachmentStream(context)
    }

    override fun getScaledSignalAttachmentStream(attachmentId: Long): SignalServiceAttachmentStream? {
        val database = DatabaseComponent.get(context).attachmentDatabase()
        val databaseAttachment = database.getAttachment(
            AttachmentId(
                attachmentId,
                0
            )
        ) ?: return null
        val mediaConstraints = MediaConstraints.getPushMediaConstraints()
        val scaledAttachment = scaleAndStripExif(database, mediaConstraints, databaseAttachment) ?: return null
        return getAttachmentFor(scaledAttachment)
    }

    override fun getSignalAttachmentPointer(attachmentId: Long): SignalServiceAttachmentPointer? {
        val attachmentDatabase = DatabaseComponent.get(context).attachmentDatabase()
        val databaseAttachment = attachmentDatabase.getAttachment(
            AttachmentId(
                attachmentId,
                0
            )
        ) ?: return null
        return databaseAttachment.toSignalAttachmentPointer()
    }

    override fun setAttachmentState(attachmentState: AttachmentState, attachmentId: AttachmentId, messageID: Long) {
        val attachmentDatabase = DatabaseComponent.get(context).attachmentDatabase()
        attachmentDatabase.setTransferState(messageID, attachmentId, attachmentState.value)
    }

    override fun getMessageForQuote(timestamp: Long, author: Address): Pair<Long, Boolean>? {
        val messagingDatabase = DatabaseComponent.get(context).mmsSmsDatabase()
        val message = messagingDatabase.getMessageFor(timestamp, author)
        return if (message != null) Pair(message.id, message.isMms) else null
    }

    override fun getAttachmentsAndLinkPreviewFor(mmsId: Long): List<Attachment> {
        return DatabaseComponent.get(context).attachmentDatabase().getAttachmentsForMessage(mmsId)
    }

    override fun getMessageBodyFor(timestamp: Long, author: String): String {
        val messagingDatabase = DatabaseComponent.get(context).mmsSmsDatabase()
        return messagingDatabase.getMessageFor(timestamp, author)!!.body
    }

    override fun getAttachmentIDsFor(messageID: Long): List<Long> {
        return DatabaseComponent.get(context)
            .attachmentDatabase()
            .getAttachmentsForMessage(messageID).mapNotNull {
            if (it.isQuote) return@mapNotNull null
            it.attachmentId.rowId
        }
    }

    override fun getLinkPreviewAttachmentIDFor(messageID: Long): Long? {
        val message = DatabaseComponent.get(context).mmsDatabase().getOutgoingMessage(messageID)
        return message.linkPreviews.firstOrNull()?.attachmentId?.rowId
    }

    override fun getIndividualRecipientForMms(mmsId: Long): Recipient? {
        val mmsDb = DatabaseComponent.get(context).mmsDatabase()
        val message = mmsDb.getMessage(mmsId).use {
            mmsDb.readerFor(it).next
        }
        return message?.individualRecipient
    }

    override fun insertAttachment(messageId: Long, attachmentId: AttachmentId, stream: InputStream) {
        val attachmentDatabase = DatabaseComponent.get(context).attachmentDatabase()
        attachmentDatabase.insertAttachmentsForPlaceholder(messageId, attachmentId, stream)
    }

    override fun updateAudioAttachmentDuration(
        attachmentId: AttachmentId,
        durationMs: Long,
        threadId: Long
    ) {
        val attachmentDb = DatabaseComponent.get(context).attachmentDatabase()
        attachmentDb.setAttachmentAudioExtras(DatabaseAttachmentAudioExtras(
            attachmentId = attachmentId,
            visualSamples = byteArrayOf(),
            durationMs = durationMs
        ), threadId)
    }

    override fun isMmsOutgoing(mmsMessageId: Long): Boolean {
        val mmsDb = DatabaseComponent.get(context).mmsDatabase()
        return mmsDb.getMessage(mmsMessageId).use { cursor ->
            mmsDb.readerFor(cursor).next
        }?.isOutgoing ?: false
    }

    override fun isOutgoingMessage(timestamp: Long): Boolean {
        val smsDatabase = DatabaseComponent.get(context).smsDatabase()
        val mmsDatabase = DatabaseComponent.get(context).mmsDatabase()
        return smsDatabase.isOutgoingMessage(timestamp) || mmsDatabase.isOutgoingMessage(timestamp)
    }

    override fun handleSuccessfulAttachmentUpload(attachmentId: Long, attachmentStream: SignalServiceAttachmentStream, attachmentKey: ByteArray, uploadResult: UploadResult) {
        val database = DatabaseComponent.get(context).attachmentDatabase()
        val databaseAttachment = getDatabaseAttachment(attachmentId) ?: return
        val attachmentPointer =
            SignalServiceAttachmentPointer(
                uploadResult.id,
                attachmentStream.contentType,
                attachmentKey,
                Optional.of(
                    Util.toIntExact(
                        attachmentStream.length
                    )
                ),
                attachmentStream.preview,
                attachmentStream.width, attachmentStream.height,
                Optional.fromNullable(
                    uploadResult.digest
                ),
                attachmentStream.fileName,
                attachmentStream.voiceNote,
                attachmentStream.caption,
                uploadResult.url
            );
        val attachment = PointerAttachment.forPointer(
            Optional.of(attachmentPointer), databaseAttachment.fastPreflightId).get()
        database.updateAttachmentAfterUploadSucceeded(databaseAttachment.attachmentId, attachment)
    }

    override fun handleFailedAttachmentUpload(attachmentId: Long) {
        val database = DatabaseComponent.get(context).attachmentDatabase()
        val databaseAttachment = getDatabaseAttachment(attachmentId) ?: return
        database.handleFailedAttachmentUpload(databaseAttachment.attachmentId)
    }

    override fun getMessageID(serverID: Long): Long? {
        val openGroupMessagingDatabase = DatabaseComponent.get(context).beldexMessageDatabase()
        return openGroupMessagingDatabase.getMessageID(serverID)
    }

    override fun getMessageID(serverId: Long, threadId: Long): Pair<Long, Boolean>? {
        val messageDB = DatabaseComponent.get(context).beldexMessageDatabase()
        return messageDB.getMessageID(serverId, threadId)
    }

    override fun getMessageIDs(serverIds: List<Long>, threadId: Long): Pair<List<Long>, List<Long>> {
        val messageDB = DatabaseComponent.get(context).beldexMessageDatabase()
        return messageDB.getMessageIDs(serverIds, threadId)
    }

    override fun deleteMessage(messageID: Long, isSms: Boolean) {
        val messagingDatabase: MessagingDatabase = if (isSms)  DatabaseComponent.get(context).smsDatabase()
                                                   else DatabaseComponent.get(context).mmsDatabase()
        messagingDatabase.deleteMessage(messageID)
        DatabaseComponent.get(context).beldexMessageDatabase().deleteMessage(messageID, isSms)
        DatabaseComponent.get(context).beldexMessageDatabase().deleteMessageServerHash(messageID)
    }

    override fun deleteMessages(messageIDs: List<Long>, threadId: Long, isSms: Boolean) {
        val messagingDatabase: MessagingDatabase = if (isSms)  DatabaseComponent.get(context).smsDatabase()
        else DatabaseComponent.get(context).mmsDatabase()

        messagingDatabase.deleteMessages(messageIDs.toLongArray(), threadId)
        DatabaseComponent.get(context).beldexMessageDatabase().deleteMessages(messageIDs)
        DatabaseComponent.get(context).beldexMessageDatabase().deleteMessageServerHashes(messageIDs)
    }

    override fun updateMessageAsDeleted(timestamp: Long, author: String) {
        val database = DatabaseComponent.get(context).mmsSmsDatabase()
        val address = Address.fromSerialized(author)
        val message = database.getMessageFor(timestamp, address) ?: return
        val messagingDatabase: MessagingDatabase = if (message.isMms)  DatabaseComponent.get(context).mmsDatabase()
                                                   else DatabaseComponent.get(context).smsDatabase()
        messagingDatabase.markAsDeleted(message.id, message.isRead)
        if (message.isOutgoing) {
            messagingDatabase.deleteMessage(message.id)
        }
    }

    override fun getServerHashForMessage(messageID: Long): String? {
        val messageDB = DatabaseComponent.get(context).beldexMessageDatabase()
        return messageDB.getMessageServerHash(messageID)
    }

    override fun getDatabaseAttachment(attachmentId: Long): DatabaseAttachment? {
        val attachmentDatabase = DatabaseComponent.get(context).attachmentDatabase()
        return attachmentDatabase.getAttachment(
            AttachmentId(
                attachmentId,
                0
            )
        )
    }

    private fun scaleAndStripExif(attachmentDatabase: AttachmentDatabase, constraints: MediaConstraints, attachment: Attachment): Attachment? {
        return try {
            if (constraints.isSatisfied(context, attachment)) {
                if (MediaUtil.isJpeg(attachment)) {
                    val stripped = constraints.getResizedMedia(context, attachment)
                    attachmentDatabase.updateAttachmentData(attachment, stripped)
                } else {
                    attachment
                }
            } else if (constraints.canResize(attachment)) {
                val resized = constraints.getResizedMedia(context, attachment)
                attachmentDatabase.updateAttachmentData(attachment, resized)
            } else {
                throw Exception("Size constraints could not be met!")
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun getAttachmentFor(attachment: Attachment): SignalServiceAttachmentStream? {
        try {
            if (attachment.dataUri == null || attachment.size == 0L) throw IOException("Assertion failed, outgoing attachment has no data!")
            val `is` = PartAuthority.getAttachmentStream(context, attachment.dataUri!!)
            return SignalServiceAttachment.newStreamBuilder()
                    .withStream(`is`)
                    .withContentType(attachment.contentType)
                    .withLength(attachment.size)
                    .withFileName(attachment.fileName)
                    .withVoiceNote(attachment.isVoiceNote)
                    .withWidth(attachment.width)
                    .withHeight(attachment.height)
                    .withCaption(attachment.caption)
                    .withListener { total: Long, progress: Long -> EventBus.getDefault().postSticky(
                        PartProgressEvent(
                            attachment,
                            total,
                            progress
                        )
                    ) }
                    .build()
        } catch (ioe: IOException) {
            Log.w("Beldex", "Couldn't open attachment", ioe)
        }
        return null
    }

}

fun DatabaseAttachment.toAttachmentPointer(): BchatServiceAttachmentPointer {
    return BchatServiceAttachmentPointer(attachmentId.rowId, contentType, key?.toByteArray(), Optional.fromNullable(size.toInt()), Optional.absent(), width, height, Optional.fromNullable(digest), Optional.fromNullable(fileName), isVoiceNote, Optional.fromNullable(caption), url)
}

fun BchatServiceAttachmentPointer.toSignalPointer(): SignalServiceAttachmentPointer {
    return SignalServiceAttachmentPointer(
        id,
        contentType,
        key?.toByteArray() ?: byteArrayOf(),
        size,
        preview,
        width,
        height,
        digest,
        fileName,
        voiceNote,
        caption,
        url
    )
}

fun DatabaseAttachment.toAttachmentStream(context: Context): BchatServiceAttachmentStream {
    val stream = PartAuthority.getAttachmentStream(context, this.dataUri!!)
    val listener = SignalServiceAttachment.ProgressListener { total: Long, progress: Long -> EventBus.getDefault().postSticky(
        PartProgressEvent(
            this,
            total,
            progress
        )
    )}

    var attachmentStream = BchatServiceAttachmentStream(stream, this.contentType, this.size, Optional.fromNullable(this.fileName), this.isVoiceNote, Optional.absent(), this.width, this.height, Optional.fromNullable(this.caption), listener)
    attachmentStream.attachmentId = this.attachmentId.rowId
    attachmentStream.isAudio = MediaUtil.isAudio(this)
    attachmentStream.isGif = MediaUtil.isGif(this)
    attachmentStream.isVideo = MediaUtil.isVideo(this)
    attachmentStream.isImage = MediaUtil.isImage(this)

    attachmentStream.key = ByteString.copyFrom(this.key?.toByteArray())
    attachmentStream.digest = Optional.fromNullable(this.digest)

    attachmentStream.url = this.url

    return attachmentStream
}

fun DatabaseAttachment.toSignalAttachmentPointer(): SignalServiceAttachmentPointer? {
    if (TextUtils.isEmpty(location)) { return null }
    // `key` can be empty in an social group context (no encryption means no encryption key)
    return try {
        val id = location!!.toLong()
        val key = Base64.decode(key!!)
        SignalServiceAttachmentPointer(
            id,
            contentType,
            key,
            Optional.of(
                Util.toIntExact(
                    size
                )
            ),
            Optional.absent(),
            width,
            height,
            Optional.fromNullable(digest),
            Optional.fromNullable(fileName),
            isVoiceNote,
            Optional.fromNullable(caption),
            url
        )
    } catch (e: Exception) {
        null
    }
}

fun DatabaseAttachment.toSignalAttachmentStream(context: Context): SignalServiceAttachmentStream {
    val stream = PartAuthority.getAttachmentStream(context, this.dataUri!!)
    val listener = SignalServiceAttachment.ProgressListener { total: Long, progress: Long -> EventBus.getDefault().postSticky(
        PartProgressEvent(
            this,
            total,
            progress
        )
    )}

    return SignalServiceAttachmentStream(
        stream,
        this.contentType,
        this.size,
        Optional.fromNullable(this.fileName),
        this.isVoiceNote,
        Optional.absent(),
        this.width,
        this.height,
        Optional.fromNullable(this.caption),
        listener
    )
}

fun DatabaseAttachment.shouldHaveImageSize(): Boolean {
    return (MediaUtil.isVideo(this) || MediaUtil.isImage(this) || MediaUtil.isGif(this));
}