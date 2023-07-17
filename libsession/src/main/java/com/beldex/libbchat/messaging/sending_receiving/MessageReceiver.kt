package com.beldex.libbchat.messaging.sending_receiving


import android.util.Log
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.messages.Message
import com.beldex.libbchat.messaging.messages.control.*
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage
import com.beldex.libsignal.crypto.PushTransportDetails
import com.beldex.libsignal.protos.SignalServiceProtos

object MessageReceiver {

    internal sealed class Error(message: String) : Exception(message) {
        object DuplicateMessage: Error("Duplicate message.")
        object InvalidMessage: Error("Invalid message.")
        object UnknownMessage: Error("Unknown message type.")
        object UnknownEnvelopeType: Error("Unknown envelope type.")
        object DecryptionFailed : Exception("Couldn't decrypt message.")
        object InvalidSignature: Error("Invalid message signature.")
        object NoData: Error("Received an empty envelope.")
        object SenderBlocked: Error("Received a message from a blocked user.")
        object NoThread: Error("Couldn't find thread for message.")
        object SelfSend: Error("Message addressed at self.")
        object InvalidGroupPublicKey: Error("Invalid group public key.")
        object NoGroupKeyPair: Error("Missing group key pair.")

        internal val isRetryable: Boolean = when (this) {
            is DuplicateMessage, is InvalidMessage, is UnknownMessage,
            is UnknownEnvelopeType, is InvalidSignature, is NoData,
            is SenderBlocked, is SelfSend -> false
            else -> true
        }
    }

    internal fun parse(data: ByteArray, openGroupServerID: Long?): Pair<Message, SignalServiceProtos.Content> {
        val storage = MessagingModuleConfiguration.shared.storage
        val userPublicKey = storage.getUserPublicKey()
        val isOpenGroupMessage = (openGroupServerID != null)
        // Parse the envelope
        val envelope = SignalServiceProtos.Envelope.parseFrom(data)
        // Decrypt the contents
        val ciphertext = envelope.content ?: run {
            throw Error.NoData
        }
        var plaintext: ByteArray? = null
        var sender: String? = null
        var groupPublicKey: String? = null
        var address:String =""
        if (isOpenGroupMessage) {
            plaintext = envelope.content.toByteArray()
            sender = envelope.source
        } else {
            when (envelope.type) {
                SignalServiceProtos.Envelope.Type.BCHAT_MESSAGE -> {
                    val userX25519KeyPair = MessagingModuleConfiguration.shared.storage.getUserX25519KeyPair()
                    val decryptionResult = MessageDecrypter.decrypt(ciphertext.toByteArray(), userX25519KeyPair)
                    plaintext = decryptionResult.first
                    sender = decryptionResult.second
                    address = decryptionResult.third

                    //-Log.d("beldex", "receiver add 1 $address")
                }
                SignalServiceProtos.Envelope.Type.CLOSED_GROUP_MESSAGE -> {
                    val hexEncodedGroupPublicKey = envelope.source
                    if (hexEncodedGroupPublicKey == null || !MessagingModuleConfiguration.shared.storage.isClosedGroup(hexEncodedGroupPublicKey)) {
                        throw Error.InvalidGroupPublicKey
                    }
                    val encryptionKeyPairs = MessagingModuleConfiguration.shared.storage.getClosedGroupEncryptionKeyPairs(hexEncodedGroupPublicKey)
                    if (encryptionKeyPairs.isEmpty()) { throw Error.NoGroupKeyPair }
                    // Loop through all known group key pairs in reverse order (i.e. try the latest key pair first (which'll more than
                    // likely be the one we want) but try older ones in case that didn't work)
                    var encryptionKeyPair = encryptionKeyPairs.removeLast()
                    fun decrypt() {
                        try {
                            val decryptionResult = MessageDecrypter.decrypt(ciphertext.toByteArray(), encryptionKeyPair)
                            plaintext = decryptionResult.first
                            sender = decryptionResult.second
                            address = decryptionResult.third
                            //-Log.d("beldex", "receiver add 2 $address")
                        } catch (e: Exception) {
                            if (encryptionKeyPairs.isNotEmpty()) {
                                encryptionKeyPair = encryptionKeyPairs.removeLast()
                                decrypt()
                            } else {
                                throw e
                            }
                        }
                    }
                    groupPublicKey = envelope.source
                    decrypt()
                }
                else -> {
                    throw Error.UnknownEnvelopeType
                }
            }
        }
        // Don't process the envelope any further if the sender is blocked
        if (isBlocked(sender!!)) {
            throw Error.SenderBlocked
        }
        // Parse the proto
        val proto = SignalServiceProtos.Content.parseFrom(
            PushTransportDetails.getStrippedPaddingMessageBody(plaintext))
        // Parse the message
        val message: Message = ReadReceipt.fromProto(proto) ?:
        TypingIndicator.fromProto(proto) ?:
        ClosedGroupControlMessage.fromProto(proto) ?:
        DataExtractionNotification.fromProto(proto) ?:
        ExpirationTimerUpdate.fromProto(proto) ?:
        ConfigurationMessage.fromProto(proto) ?:
        UnsendRequest.fromProto(proto) ?:
        CallMessage.fromProto(proto)?:
        /*Hales63*/
        MessageRequestResponse.fromProto(proto) ?:
        VisibleMessage.fromProto(proto,address!!) ?: run {
            throw Error.UnknownMessage
        }
        // Ignore self send if needed
        if (!message.isSelfSendValid && sender == userPublicKey) throw Error.SelfSend
        // Guard against control messages in social groups
        if (isOpenGroupMessage && message !is VisibleMessage) {
            throw Error.InvalidMessage
        }
        // Finish parsing
        message.sender = sender
        message.recipient = userPublicKey
        message.sentTimestamp = envelope.timestamp
        message.receivedTimestamp = if (envelope.hasServerTimestamp()) envelope.serverTimestamp else System.currentTimeMillis()
        message.groupPublicKey = groupPublicKey
        message.openGroupServerMessageID = openGroupServerID
        // Validate
        var isValid = message.isValid()
        Log.d("DataMessage message body-> ",message.sentTimestamp.toString())
        Log.d("DataMessage message body1-> ",message.receivedTimestamp.toString())
        Log.d("DataMessage isValid-> ",isValid.toString())
        Log.d("DataMessage message-> ",(message is VisibleMessage).toString())
        Log.d("DataMessage attachmentCount-> ",proto.dataMessage.attachmentsCount.toString())
        if (message is VisibleMessage && !isValid && proto.dataMessage.attachmentsCount != 0) { isValid = true }
        if (!isValid) {
            throw Error.InvalidMessage
        }
        // If the message failed to process the first time around we retry it later (if the error is retryable). In this case the timestamp
        // will already be in the database but we don't want to treat the message as a duplicate. The isRetry flag is a simple workaround
        // for this issue.
        if (message is ClosedGroupControlMessage && message.kind is ClosedGroupControlMessage.Kind.New) {
            // Allow duplicates in this case to avoid the following situation:
            // • The app performed a background poll or received a push notification
            // • This method was invoked and the received message timestamps table was updated
            // • Processing wasn't finished
            // • The user doesn't see the new secret group
        } else {
            if (storage.isDuplicateMessage(envelope.timestamp)) { throw Error.DuplicateMessage }
            storage.addReceivedMessageTimestamp(envelope.timestamp)
        }
        // Return
        return Pair(message, proto)
    }
}