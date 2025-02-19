package com.beldex.libbchat.messaging.jobs

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import nl.komponents.kovenant.Promise
import okio.Buffer
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.file_server.FileServerAPIV2
import com.beldex.libbchat.messaging.messages.Message
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.utilities.Data
import com.beldex.libbchat.utilities.DecodedAudio
import com.beldex.libbchat.utilities.InputStreamMediaDataSource
import com.beldex.libbchat.utilities.UploadResult
import com.beldex.libbchat.utilities.Util
import com.beldex.libsignal.messages.SignalServiceAttachmentStream
import com.beldex.libsignal.streams.*
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.PushAttachmentData

class AttachmentUploadJob(val attachmentID: Long, val threadID: String, val message: Message, val messageSendJobID: String) : Job {
    override var delegate: JobDelegate? = null
    override var id: String? = null
    override var failureCount: Int = 0

    // Error
    internal sealed class Error(val description: String) : Exception(description) {
        object NoAttachment : Error("No such attachment.")
    }

    // Settings
    override val maxFailureCount: Int = 20

    companion object {
        val TAG = AttachmentUploadJob::class.simpleName
        val KEY: String = "AttachmentUploadJob"

        // Keys used for database storage
        private val ATTACHMENT_ID_KEY = "attachment_id"
        private val THREAD_ID_KEY = "thread_id"
        private val MESSAGE_KEY = "message"
        private val MESSAGE_SEND_JOB_ID_KEY = "message_send_job_id"
    }

    override fun execute() {
        try {
            val storage = MessagingModuleConfiguration.shared.storage
            val messageDataProvider = MessagingModuleConfiguration.shared.messageDataProvider
            val attachment = messageDataProvider.getScaledSignalAttachmentStream(attachmentID)
                ?: return handleFailure(Error.NoAttachment)
            val v2OpenGroup = storage.getV2OpenGroup(threadID.toLong())
            if (v2OpenGroup != null) {
                val keyAndResult = upload(attachment, v2OpenGroup.server, false) {
                    OpenGroupAPIV2.upload(it, v2OpenGroup.room, v2OpenGroup.server)
                }
                handleSuccess(attachment, keyAndResult.first, keyAndResult.second)
            } else {
                val keyAndResult = upload(attachment, FileServerAPIV2.server, true) {
                    FileServerAPIV2.upload(it)
                }
                handleSuccess(attachment, keyAndResult.first, keyAndResult.second)
            }
        } catch (e: java.lang.Exception) {
            if (e == Error.NoAttachment) {
                this.handlePermanentFailure(e)
            } else {
                this.handleFailure(e)
            }
        }
    }

    private fun upload(attachment: SignalServiceAttachmentStream, server: String, encrypt: Boolean, upload: (ByteArray) -> Promise<Long, Exception>): Pair<ByteArray, UploadResult> {
        // Key
        val key = if (encrypt) Util.getSecretBytes(64) else ByteArray(0)
        // Length
        val rawLength = attachment.length
        val length = if (encrypt) {
            val paddedLength = PaddingInputStream.getPaddedSize(rawLength)
            AttachmentCipherOutputStream.getCiphertextLength(paddedLength)
        } else {
            attachment.length
        }
        // In & out streams
        // PaddingInputStream adds padding as data is read out from it. AttachmentCipherOutputStream
        // encrypts as it writes data.
        val inputStream = if (encrypt) PaddingInputStream(
            attachment.inputStream,
            rawLength
        ) else attachment.inputStream
        val outputStreamFactory = if (encrypt) AttachmentCipherOutputStreamFactory(
            key
        ) else PlaintextOutputStreamFactory()
        // Create a digesting request body but immediately read it out to a buffer. Doing this makes
        // it easier to deal with inputStream and outputStreamFactory.
        val pad = PushAttachmentData(
            attachment.contentType,
            inputStream,
            length,
            outputStreamFactory,
            attachment.listener
        )
        val contentType = "application/octet-stream"
        val drb = DigestingRequestBody(
            pad.data,
            pad.outputStreamFactory,
            contentType,
            pad.dataSize,
            pad.listener
        )
        Log.d("Beldex", "File size: ${length.toDouble() / 1000} kb.")
        val b = Buffer()
        drb.writeTo(b)
        val data = b.readByteArray()
        // Upload the data
        val id = upload(data).get()
        val digest = drb.transmittedDigest
        // Return
        return Pair(key, UploadResult(id, "${server}/files/$id", digest))
    }

    private fun handleSuccess(attachment: SignalServiceAttachmentStream, attachmentKey: ByteArray, uploadResult: UploadResult) {
        Log.d(TAG, "Attachment uploaded successfully.")
        delegate?.handleJobSucceeded(this)
        val messageDataProvider = MessagingModuleConfiguration.shared.messageDataProvider
        messageDataProvider.handleSuccessfulAttachmentUpload(attachmentID, attachment, attachmentKey, uploadResult)
        if (attachment.contentType.startsWith("audio/")) {
            // process the duration
            try {
                val inputStream = messageDataProvider.getAttachmentStream(attachmentID)!!.inputStream!!
                InputStreamMediaDataSource(inputStream).use { mediaDataSource ->
                    val durationMs = (DecodedAudio.create(mediaDataSource).totalDuration / 1000.0).toLong()
                    messageDataProvider.getDatabaseAttachment(attachmentID)?.attachmentId?.let { attachmentId ->
                        messageDataProvider.updateAudioAttachmentDuration(attachmentId, durationMs, threadID.toLong())
                    }
                }
            } catch (e: Exception) {
                Log.e("Beldex", "Couldn't process audio attachment", e)
            }
        }
        MessagingModuleConfiguration.shared.storage.resumeMessageSendJobIfNeeded(messageSendJobID)
    }

    private fun handlePermanentFailure(e: Exception) {
        Log.w(TAG, "Attachment upload failed permanently due to error: $this.")
        delegate?.handleJobFailedPermanently(this, e)
        MessagingModuleConfiguration.shared.messageDataProvider.handleFailedAttachmentUpload(attachmentID)
        failAssociatedMessageSendJob(e)
    }

    private fun handleFailure(e: Exception) {
        Log.w(TAG, "Attachment upload failed due to error: $this.")
        delegate?.handleJobFailed(this, e)
        if (failureCount + 1 >= maxFailureCount) {
            failAssociatedMessageSendJob(e)
        }
    }

    private fun failAssociatedMessageSendJob(e: Exception) {
        val storage = MessagingModuleConfiguration.shared.storage
        val messageSendJob = storage.getMessageSendJob(messageSendJobID)
        MessageSender.handleFailedMessageSend(this.message, e)
        if (messageSendJob != null) {
            storage.markJobAsFailedPermanently(messageSendJobID)
        }
    }

    override fun serialize(): Data {
        val kryo = Kryo()
        kryo.isRegistrationRequired = false
        val serializedMessage = ByteArray(4096)
        val output = Output(serializedMessage, Job.MAX_BUFFER_SIZE_BYTES)
        kryo.writeClassAndObject(output, message)
        output.close()
        return Data.Builder()
            .putLong(ATTACHMENT_ID_KEY, attachmentID)
            .putString(THREAD_ID_KEY, threadID)
            .putByteArray(MESSAGE_KEY, output.toBytes())
            .putString(MESSAGE_SEND_JOB_ID_KEY, messageSendJobID)
            .build()
    }

    override fun getFactoryKey(): String {
        return KEY
    }

    class Factory: Job.Factory<AttachmentUploadJob> {

        override fun create(data: Data): AttachmentUploadJob? {
            val serializedMessage = data.getByteArray(MESSAGE_KEY)
            val kryo = Kryo()
            kryo.isRegistrationRequired = false
            val input = Input(serializedMessage)
            val message: Message
            try {
                message = kryo.readClassAndObject(input) as Message
            } catch (e: Exception) {
                Log.e("Beldex","Couldn't serialize the AttachmentUploadJob", e)
                return null
            }
            input.close()
            return AttachmentUploadJob(
                    data.getLong(ATTACHMENT_ID_KEY),
                    data.getString(THREAD_ID_KEY)!!,
                    message,
                    data.getString(MESSAGE_SEND_JOB_ID_KEY)!!
            )
        }
    }
}