package com.beldex.libbchat.messaging.sending_receiving

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.messaging.jobs.MessageSendJob
import com.beldex.libbchat.messaging.jobs.NotifyPNServerJob
import com.beldex.libbchat.messaging.messages.Destination
import com.beldex.libbchat.messaging.messages.Message
import com.beldex.libbchat.messaging.messages.control.*
import com.beldex.libbchat.messaging.messages.visible.*
import com.beldex.libbchat.messaging.open_groups.*
import com.beldex.libbchat.messaging.utilities.MessageWrapper
import com.beldex.libbchat.mnode.RawResponsePromise
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.mnode.MnodeMessage
import com.beldex.libbchat.mnode.MnodeModule
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libsignal.crypto.PushTransportDetails
import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.Base64
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.hexEncodedPublicKey
import java.util.concurrent.atomic.AtomicInteger
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment as SignalAttachment
import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview as SignalLinkPreview
import com.beldex.libbchat.messaging.sending_receiving.quotes.QuoteModel as SignalQuote

object MessageSender {

    // Error
    sealed class Error(val description: String) : Exception(description) {
        object InvalidMessage : Error("Invalid message.")
        object ProtoConversionFailed : Error("Couldn't convert message to proto.")
        object NoUserED25519KeyPair : Error("Couldn't find user ED25519 key pair.")
        object SigningFailed : Error("Couldn't sign message.")
        object EncryptionFailed : Error("Couldn't encrypt message.")

        // Secret groups
        object NoThread : Error("Couldn't find a thread associated with the given group public key.")
        object NoKeyPair: Error("Couldn't find a private key associated with the given group public key.")
        object InvalidClosedGroupUpdate : Error("Invalid group update.")

        internal val isRetryable: Boolean = when (this) {
            is InvalidMessage, ProtoConversionFailed, InvalidClosedGroupUpdate -> false
            else -> true
        }
    }

    // Convenience
    fun send(message: Message, destination: Destination): Promise<Unit, Exception> {
        if (destination is Destination.OpenGroupV2) {
            return sendToOpenGroupDestination(destination, message)
        } else {
            return sendToMnodeDestination(destination, message)
        }
    }

    // One-on-One Chats & Secret Groups
    private fun sendToMnodeDestination(destination: Destination, message: Message, isSyncMessage: Boolean = false): Promise<Unit, Exception> {
        val deferred = deferred<Unit, Exception>()
        val promise = deferred.promise
        val storage = MessagingModuleConfiguration.shared.storage
        val userPublicKey = storage.getUserPublicKey()
        Log.d("Beldex","bchat id validation -- get user public key")
        // Set the timestamp, sender and recipient
        if (message.sentTimestamp == null) {
            message.sentTimestamp = MnodeAPI.nowWithOffset // Visible messages will already have their sent timestamp set
        }
        Log.d("Beldex","bchat id validation -- check send id and user public key")
        message.sender = userPublicKey

        val isSelfSend = (message.recipient == userPublicKey)
        Log.d("Beldex","bchat id validation -- check send id and user public key for msg send to self")
        // Set the failure handler (need it here already for precondition failure handling)
        fun handleFailure(error: Exception) {
            handleFailedMessageSend(message, error)
            if (destination is Destination.Contact && message is VisibleMessage && !isSelfSend) {
                MnodeModule.shared.broadcaster.broadcast("messageFailed", message.sentTimestamp!!)
            }
            deferred.reject(error)
        }
        try {
            when (destination) {
                is Destination.Contact -> message.recipient = destination.publicKey
                is Destination.ClosedGroup -> message.recipient = destination.groupPublicKey
                is Destination.OpenGroupV2 -> throw IllegalStateException("Destination should not be an social group.")
            }
            // Validate the message
            if (!message.isValid()) { throw Error.InvalidMessage }
            // Stop here if this is a self-send, unless it's:
            // • a configuration message
            // • a sync message
            // • a secret group control message of type `new`
            var isNewClosedGroupControlMessage = false
            if (message is ClosedGroupControlMessage && message.kind is ClosedGroupControlMessage.Kind.New) isNewClosedGroupControlMessage = true
            if (isSelfSend && message !is ConfigurationMessage && !isSyncMessage && !isNewClosedGroupControlMessage && message !is UnsendRequest) {
                handleSuccessfulMessageSend(message, destination)
                deferred.resolve(Unit)
                return promise
            }
            // Attach the user's profile if needed
            if (message is VisibleMessage) {
                message.profile = storage.getUserProfile()
            }
            /*if (message is MessageRequestResponse) {
                message.profile = storage.getUserProfile()
            }*/
            // Convert it to protobuf
            val proto = message.toProto() ?: throw Error.ProtoConversionFailed
            // Serialize the protobuf
            val plaintext = PushTransportDetails.getPaddedMessageBody(proto.toByteArray())
            // Encrypt the serialized protobuf
            val ciphertext: ByteArray
            val senderBeldexAddress = storage.getSenderBeldexAddress()!!
            when (destination) {
                is Destination.Contact -> ciphertext = MessageEncrypter.encrypt(plaintext, destination.publicKey,senderBeldexAddress)
                is Destination.ClosedGroup -> {
                    val encryptionKeyPair = MessagingModuleConfiguration.shared.storage.getLatestClosedGroupEncryptionKeyPair(destination.groupPublicKey)!!
                    ciphertext = MessageEncrypter.encrypt(
                        plaintext,
                        encryptionKeyPair.hexEncodedPublicKey,senderBeldexAddress
                    )
                }
                is Destination.OpenGroupV2 -> throw IllegalStateException("Destination should not be social group.")
            }
            // Wrap the result
            val kind: SignalServiceProtos.Envelope.Type
            val senderPublicKey: String
            when (destination) {
                is Destination.Contact -> {
                    kind = SignalServiceProtos.Envelope.Type.BCHAT_MESSAGE
                    senderPublicKey = ""
                }
                is Destination.ClosedGroup -> {
                    kind = SignalServiceProtos.Envelope.Type.CLOSED_GROUP_MESSAGE
                    senderPublicKey = destination.groupPublicKey
                }
                is Destination.OpenGroupV2 -> throw IllegalStateException("Destination should not be social group.")
            }
            val wrappedMessage = MessageWrapper.wrap(kind, message.sentTimestamp!!, senderPublicKey, ciphertext)
            // Send the result
            if (destination is Destination.Contact && message is VisibleMessage && !isSelfSend) {
                MnodeModule.shared.broadcaster.broadcast("calculatingPoW", message.sentTimestamp!!)
            }
            val base64EncodedData = Base64.encodeBytes(wrappedMessage)
            // Send the result
            val timestamp = message.sentTimestamp!! + MnodeAPI.clockOffset
            val mnodeMessage = MnodeMessage(message.recipient!!, base64EncodedData, message.ttl, timestamp)
            Log.d("Beldex","bchat id validation --  send msg to mnode")
            if (destination is Destination.Contact && message is VisibleMessage && !isSelfSend) {
                MnodeModule.shared.broadcaster.broadcast("sendingMessage", message.sentTimestamp!!)
            }
            MnodeAPI.sendMessage(mnodeMessage).success { promises: Set<RawResponsePromise> ->
                var isSuccess = false
                val promiseCount = promises.size
                var errorCount =  AtomicInteger(0)
                promises.iterator().forEach { promise: RawResponsePromise ->
                    promise.success {
                        if (isSuccess) { return@success } // Succeed as soon as the first promise succeeds
                        isSuccess = true
                        if (destination is Destination.Contact && message is VisibleMessage && !isSelfSend) {
                            MnodeModule.shared.broadcaster.broadcast("messageSent", message.sentTimestamp!!)
                        }
                        val hash = it["hash"] as? String
                        message.serverHash = hash
                        handleSuccessfulMessageSend(message, destination, isSyncMessage)
                        var shouldNotify = ((message is VisibleMessage || message is UnsendRequest || message is CallMessage) && !isSyncMessage)
                        /*
                        if (message is ClosedGroupControlMessage && message.kind is ClosedGroupControlMessage.Kind.New) {
                            shouldNotify = true
                        }
                         */
                        if (shouldNotify) {
                            val notifyPNServerJob = NotifyPNServerJob(mnodeMessage)
                            JobQueue.shared.add(notifyPNServerJob)
                        }
                        deferred.resolve(Unit)
                    }
                    promise.fail {
                        errorCount.getAndIncrement()
                        if (errorCount.get() != promiseCount) { return@fail } // Only error out if all promises failed
                        handleFailure(it)
                    }
                }
            }.fail {
                Log.d("Beldex", "Couldn't send message due to error: $it.")
                handleFailure(it)
            }
        } catch (exception: Exception) {
            handleFailure(exception)
        }
        return promise
    }

    // Social Groups
    private fun sendToOpenGroupDestination(destination: Destination, message: Message): Promise<Unit, Exception> {
        val deferred = deferred<Unit, Exception>()
        val storage = MessagingModuleConfiguration.shared.storage
        if (message.sentTimestamp == null) {
            message.sentTimestamp = MnodeAPI.nowWithOffset
        }
        message.sender = storage.getUserPublicKey()
        // Set the failure handler (need it here already for precondition failure handling)
        fun handleFailure(error: Exception) {
            handleFailedMessageSend(message, error)
            deferred.reject(error)
        }
        try {
            when (destination) {
                is Destination.Contact, is Destination.ClosedGroup -> throw IllegalStateException("Invalid destination.")
                is Destination.OpenGroupV2 -> {
                    message.recipient = "${destination.server}.${destination.room}"
                    val server = destination.server
                    val room = destination.room
                    // Attach the user's profile if needed
                    if (message is VisibleMessage) {
                        message.profile = storage.getUserProfile()
                    }
                    // Validate the message
                    if (message !is VisibleMessage || !message.isValid()) {
                        throw Error.InvalidMessage
                    }
                    val proto = message.toProto()!!
                    val plaintext = PushTransportDetails.getPaddedMessageBody(proto.toByteArray())
                    val openGroupMessage = OpenGroupMessageV2(
                        sender = message.sender,
                        sentTimestamp = message.sentTimestamp!!,
                        base64EncodedData = Base64.encodeBytes(plaintext),
                    )
                    OpenGroupAPIV2.send(openGroupMessage,room,server).success {
                        message.openGroupServerMessageID = it.serverID
                        handleSuccessfulMessageSend(message, destination, openGroupSentTimestamp = it.sentTimestamp)
                        deferred.resolve(Unit)
                    }.fail {
                        handleFailure(it)
                    }
                }
            }
        } catch (exception: Exception) {
            handleFailure(exception)
        }
        return deferred.promise
    }

    // Result Handling
    fun handleSuccessfulMessageSend(message: Message, destination: Destination, isSyncMessage: Boolean = false, openGroupSentTimestamp: Long = -1) {
        val storage = MessagingModuleConfiguration.shared.storage
        val userPublicKey = storage.getUserPublicKey()!!
        // Ignore future self-sends
        storage.addReceivedMessageTimestamp(message.sentTimestamp!!)
        storage.getMessageIdInDatabase(message.sentTimestamp!!, message.sender?:userPublicKey)?.let { messageID ->
            if (openGroupSentTimestamp != -1L && message is VisibleMessage) {
                storage.addReceivedMessageTimestamp(openGroupSentTimestamp)
                storage.updateSentTimestamp(messageID, message.isMediaMessage(), openGroupSentTimestamp, message.threadID!!)
                message.sentTimestamp = openGroupSentTimestamp
            }
            // When the sync message is successfully sent, the hash value of this TSOutgoingMessage
            // will be replaced by the hash value of the sync message. Since the hash value of the
            // real message has no use when we delete a message. It is OK to let it be.
            message.serverHash?.let {
                storage.setMessageServerHash(messageID, it)
            }
            // in case any errors from previous sends
            storage.clearErrorMessage(messageID)
            // Track the social group server message ID
            if (message.openGroupServerMessageID != null && destination is Destination.OpenGroupV2) {
                val encoded = GroupUtil.getEncodedOpenGroupID("${destination.server}.${destination.room}".toByteArray())
                val threadID = storage.getThreadId(Address.fromSerialized(encoded))
                if (threadID != null && threadID >= 0) {
                    storage.setOpenGroupServerMessageID(messageID, message.openGroupServerMessageID!!, threadID, !(message as VisibleMessage).isMediaMessage())
                }
            }
            // Mark the message as sent
            storage.markAsSent(message.sentTimestamp!!, message.sender?:userPublicKey)
            storage.markUnidentified(message.sentTimestamp!!, message.sender?:userPublicKey)
            // Start the disappearing messages timer if needed
            if (message is VisibleMessage && !isSyncMessage) {
                SSKEnvironment.shared.messageExpirationManager.startAnyExpiration(message.sentTimestamp!!, message.sender?:userPublicKey)
            }
        }
        // Sync the message if:
        // • it's a visible message
        // • the destination was a contact
        // • we didn't sync it already
        if (destination is Destination.Contact && !isSyncMessage) {
            if (message is VisibleMessage) { message.syncTarget = destination.publicKey }
            if (message is ExpirationTimerUpdate) { message.syncTarget = destination.publicKey }
            sendToMnodeDestination(Destination.Contact(userPublicKey), message, true)
        }
    }

    fun handleFailedMessageSend(message: Message, error: Exception) {
        val storage = MessagingModuleConfiguration.shared.storage
        val userPublicKey = storage.getUserPublicKey()!!
        storage.setErrorMessage(message.sentTimestamp!!, message.sender?:userPublicKey, error)
    }

    // Convenience
    @JvmStatic
    fun send(message: VisibleMessage, address: Address, attachments: List<SignalAttachment>, quote: SignalQuote?, linkPreview: SignalLinkPreview?) {
        val messageDataProvider = MessagingModuleConfiguration.shared.messageDataProvider
        val attachmentIDs = messageDataProvider.getAttachmentIDsFor(message.id!!)
        message.attachmentIDs.addAll(attachmentIDs)
        message.quote = Quote.from(quote)
        message.linkPreview = LinkPreview.from(linkPreview)
        message.linkPreview?.let { linkPreview ->
            if (linkPreview.attachmentID == null) {
                messageDataProvider.getLinkPreviewAttachmentIDFor(message.id!!)?.let { attachmentID ->
                    message.linkPreview!!.attachmentID = attachmentID
                    message.attachmentIDs.remove(attachmentID)
                }
            }
        }
        send(message, address)
    }

    @JvmStatic
    fun send(message: Message, address: Address) {
        val threadID = MessagingModuleConfiguration.shared.storage.getOrCreateThreadIdFor(address)
        message.threadID = threadID
        val destination = Destination.from(address)
        val job = MessageSendJob(message, destination)
        JobQueue.shared.add(job)
    }

    fun sendNonDurably(message: VisibleMessage, attachments: List<SignalAttachment>, address: Address): Promise<Unit, Exception> {
        val attachmentIDs = MessagingModuleConfiguration.shared.messageDataProvider.getAttachmentIDsFor(message.id!!)
        message.attachmentIDs.addAll(attachmentIDs)
        return sendNonDurably(message, address)
    }

    fun sendNonDurably(message: Message, address: Address): Promise<Unit, Exception> {
        val threadID = MessagingModuleConfiguration.shared.storage.getOrCreateThreadIdFor(address)
        message.threadID = threadID
        val destination = Destination.from(address)
        return send(message, destination)
    }

    // Secret groups
    fun createClosedGroup(name: String, members: Collection<String>): Promise<String, Exception> {
        return create(name, members)
    }

    fun explicitNameChange(groupPublicKey: String, newName: String) {
        return setName(groupPublicKey, newName)
    }

    fun explicitAddMembers(groupPublicKey: String, membersToAdd: List<String>) {
        return addMembers(groupPublicKey, membersToAdd)
    }

    fun explicitRemoveMembers(groupPublicKey: String, membersToRemove: List<String>) {
        return removeMembers(groupPublicKey, membersToRemove)
    }

    @JvmStatic
    fun explicitLeave(groupPublicKey: String, notifyUser: Boolean): Promise<Unit, Exception> {
        return leave(groupPublicKey, notifyUser)
    }
}