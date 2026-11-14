package io.beldex.bchat.conversation.v2.forward

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.beldex.libbchat.messaging.messages.signal.OutgoingMediaMessage
import com.beldex.libbchat.messaging.messages.signal.OutgoingTextMessage
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentTransferProgress
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import com.beldex.libbchat.messaging.sending_receiving.attachments.UriAttachment
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.database.model.MmsMessageRecord
import io.beldex.bchat.dependencies.DatabaseComponent
import kotlinx.parcelize.Parcelize
import com.beldex.libbchat.utilities.TextSecurePreferences

/**
 * One attachment to forward, carrying every value the chat's own media-send flow sets on an
 * attachment (content type, size, dimensions, file name, voice-note flag, caption), so a
 * forwarded photo / video / document / voice note / GIF / sticker arrives exactly like the original.
 */
@Parcelize
data class ForwardAttachment(
    val uri: Uri,
    val thumbnailUri: Uri?,
    val contentType: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val fileName: String?,
    val voiceNote: Boolean,
    val caption: String?
) : Parcelable {

    /** Fresh outgoing attachment (new row on insert) backed by the original's stored data. */
    fun toAttachment(): Attachment = UriAttachment(
        uri,
        thumbnailUri,
        contentType,
        AttachmentTransferProgress.TRANSFER_PROGRESS_STARTED,
        size,
        width,
        height,
        fileName,
        null,
        voiceNote,
        false,
        caption
    )

    companion object {
        /** Null when the attachment has no local data yet (not downloaded). */
        fun from(attachment: Attachment): ForwardAttachment? {
            val dataUri = attachment.dataUri ?: return null
            if (attachment is DatabaseAttachment &&
                attachment.transferState != AttachmentTransferProgress.TRANSFER_PROGRESS_DONE) return null
            return ForwardAttachment(
                uri = dataUri,
                thumbnailUri = attachment.thumbnailUri,
                contentType = attachment.contentType,
                size = attachment.size,
                width = attachment.width,
                height = attachment.height,
                fileName = attachment.fileName,
                voiceNote = attachment.isVoiceNote,
                caption = attachment.caption
            )
        }
    }
}

/** One message to forward: its text (body / caption) and all of its media attachments. */
@Parcelize
data class ForwardPayload(
    val text: String?,
    val attachments: List<ForwardAttachment>
) : Parcelable {

    val isEmpty: Boolean get() = text.isNullOrBlank() && attachments.isEmpty()

    companion object {
        sealed class Result {
            data class Ok(val payload: ForwardPayload) : Result()
            object NotDownloaded : Result()
            object NotForwardable : Result()
        }

        fun from(message: MessageRecord): Result {
            if (message.isDeleted || message.isPayment || message.isOpenGroupInvitation ||
                message.isSharedContact) return Result.NotForwardable
            val mediaAttachments = (message as? MmsMessageRecord)?.slideDeck?.asAttachments().orEmpty()
            val attachments = mediaAttachments.map { ForwardAttachment.from(it) ?: return Result.NotDownloaded }
            val payload = ForwardPayload(message.body.takeIf { it.isNotBlank() }, attachments)
            return if (payload.isEmpty) Result.NotForwardable else Result.Ok(payload)
        }
    }
}

/**
 * Sends forwarded content to one or more chats using the same steps as
 * ConversationActivityV2.sendTextOnlyMessage / sendAttachments: build a VisibleMessage, store it in
 * the outbox of the recipient's thread (SMS table for text, MMS table with attachments for media),
 * then queue it with MessageSender. Quotes are not carried over (a forward is a new message).
 */
object MessageForwarder {

    private var lastTimestamp = 0L

    /** Unique, increasing sent timestamps so several forwarded messages never collide. */
    @Synchronized
    private fun nextTimestamp(): Long {
        val now = MnodeAPI.nowWithOffset
        lastTimestamp = if (now > lastTimestamp) now else lastTimestamp + 1
        return lastTimestamp
    }

    /**
     * Must be called off the main thread. Returns address -> thread id for every chat that was
     * sent to (the thread is created on demand, e.g. Note to Self opened for the first time).
     */
    fun forward(context: Context, recipients: List<Recipient>, payloads: List<ForwardPayload>): Map<String, Long> {
        val db = DatabaseComponent.get(context)
        val threadDb = db.threadDatabase()
        val self = TextSecurePreferences.getLocalNumber(context)
        val sentTo = LinkedHashMap<String, Long>()
        for (recipient in recipients) {
            try {
                val threadId = threadDb.getOrCreateThreadIdFor(recipient)
                val isNoteToSelf = recipient.address.serialize() == self
                if (!isNoteToSelf && recipient.isContactRecipient && !recipient.isApproved) {
                    db.recipientDatabase().setApproved(recipient, true)
                }
                for (payload in payloads) {
                    if (payload.attachments.isEmpty()) sendText(context, recipient, threadId, payload.text.orEmpty())
                    else sendMedia(context, recipient, threadId, payload)
                }
                sentTo[recipient.address.serialize()] = threadId
            } catch (e: Exception) {
                Log.e("MessageForwarder", "Failed to forward to ${recipient.address}", e)
            }
        }
        return sentTo
    }

    private fun sendText(context: Context, recipient: Recipient, threadId: Long, text: String) {
        val message = VisibleMessage().apply {
            sentTimestamp = nextTimestamp()
            this.text = text
        }
        val outgoing = OutgoingTextMessage.from(message, recipient)
        message.id = DatabaseComponent.get(context).smsDatabase()
            .insertMessageOutbox(threadId, outgoing, false, message.sentTimestamp!!, null, true)
        MessageSender.send(message, recipient.address)
    }

    private fun sendMedia(context: Context, recipient: Recipient, threadId: Long, payload: ForwardPayload) {
        val attachments = payload.attachments.map { it.toAttachment() }
        val message = VisibleMessage().apply {
            sentTimestamp = nextTimestamp()
            text = payload.text
        }
        val outgoing = OutgoingMediaMessage.from(message, recipient, attachments, null, null)
        message.id = DatabaseComponent.get(context).mmsDatabase()
            .insertMessageOutbox(outgoing, threadId, false, null, runThreadUpdate = true)
        MessageSender.send(message, recipient.address, attachments, null, null)
    }
}
