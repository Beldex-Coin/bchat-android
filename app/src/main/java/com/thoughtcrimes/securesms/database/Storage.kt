package com.thoughtcrimes.securesms.database

import android.content.Context
import android.net.Uri
import com.beldex.libbchat.database.StorageProtocol
import com.beldex.libbchat.messaging.calls.CallMessageType
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.jobs.*
import com.beldex.libbchat.messaging.messages.control.ConfigurationMessage
import com.beldex.libbchat.messaging.messages.control.MessageRequestResponse
import com.beldex.libbchat.messaging.messages.signal.*
import com.beldex.libbchat.messaging.messages.visible.Attachment
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage
import com.beldex.libbchat.messaging.open_groups.OpenGroupV2
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentId
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import com.beldex.libbchat.messaging.sending_receiving.data_extraction.DataExtractionNotificationInfoMessage
import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview
import com.beldex.libbchat.messaging.sending_receiving.quotes.QuoteModel
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.*
import com.beldex.libbchat.utilities.Address.Companion.fromSerialized
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.beldex.libsignal.messages.SignalServiceAttachmentPointer
import com.beldex.libsignal.messages.SignalServiceGroup
import com.beldex.libsignal.utilities.KeyHelper
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.guava.Optional
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.database.helpers.SQLCipherOpenHelper
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.groups.OpenGroupManager
import com.thoughtcrimes.securesms.jobs.RetrieveProfileAvatarJob
import com.thoughtcrimes.securesms.mms.PartAuthority
import com.thoughtcrimes.securesms.util.BchatMetaProtocol

class Storage(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper), StorageProtocol {

    override fun getUserPublicKey(): String? {
        return TextSecurePreferences.getLocalNumber(context)
    }

    override fun getUserX25519KeyPair(): ECKeyPair {
        return DatabaseComponent.get(context).beldexAPIDatabase().getUserX25519KeyPair()
    }
    override fun getUserDisplayName(): String? {
        return TextSecurePreferences.getProfileName(context)
    }

    override fun getUserProfileKey(): ByteArray? {
        return ProfileKeyUtil.getProfileKey(context)
    }

    override fun getUserProfilePictureURL(): String? {
        return TextSecurePreferences.getProfilePictureURL(context)
    }

    override fun setUserProfilePictureURL(newValue: String) {
        val ourRecipient = Address.fromSerialized(getUserPublicKey()!!).let {
            Recipient.from(context, it, false)
        }
        TextSecurePreferences.setProfilePictureURL(context, newValue)
        RetrieveProfileAvatarJob(
            ourRecipient,
            newValue
        )
        ApplicationContext.getInstance(context).jobManager.add(
            RetrieveProfileAvatarJob(
                ourRecipient,
                newValue
            )
        )
    }

    override fun getOrGenerateRegistrationID(): Int {
        var registrationID = TextSecurePreferences.getLocalRegistrationId(context)
        if (registrationID == 0) {
            registrationID = KeyHelper.generateRegistrationId(false)
            TextSecurePreferences.setLocalRegistrationId(context, registrationID)
        }
        return registrationID
    }

    override fun getSenderBeldexAddress(): String? {
       return TextSecurePreferences.getSenderAddress(context)
    }

    override fun persistAttachments(messageID: Long, attachments: List<Attachment>): List<Long> {
        val database = DatabaseComponent.get(context).attachmentDatabase()
        val databaseAttachments = attachments.mapNotNull { it.toSignalAttachment() }
        return database.insertAttachments(messageID, databaseAttachments)
    }

    override fun getAttachmentsForMessage(messageID: Long): List<DatabaseAttachment> {
        val database = DatabaseComponent.get(context).attachmentDatabase()
        return database.getAttachmentsForMessage(messageID)
    }

    override fun markConversationAsRead(threadId: Long, updateLastSeen: Boolean) {
        val threadDb = DatabaseComponent.get(context).threadDatabase()
        threadDb.setRead(threadId, updateLastSeen)
    }

    override fun incrementUnread(threadId: Long, amount: Int) {
        val threadDb = DatabaseComponent.get(context).threadDatabase()
        threadDb.incrementUnread(threadId, amount)
    }

    override fun updateThread(threadId: Long, unarchive: Boolean) {
        val threadDb = DatabaseComponent.get(context).threadDatabase()
        threadDb.update(threadId, unarchive)
    }

    override fun persist(message: VisibleMessage, quotes: QuoteModel?, linkPreview: List<LinkPreview?>, groupPublicKey: String?, openGroupID: String?, attachments: List<Attachment>,runIncrement:Boolean,runThreadUpdate:Boolean): Long? {
        var messageID: Long? = null
        val senderAddress = Address.fromSerialized(message.sender!!)
        val isUserSender = (message.sender!! == getUserPublicKey())
        val group: Optional<SignalServiceGroup> = when {
            openGroupID != null -> Optional.of(
                SignalServiceGroup(
                    openGroupID.toByteArray(),
                    SignalServiceGroup.GroupType.PUBLIC_CHAT
                )
            )
            groupPublicKey != null -> {
                val doubleEncoded = GroupUtil.doubleEncodeGroupID(groupPublicKey)
                Optional.of(
                    SignalServiceGroup(
                        GroupUtil.getDecodedGroupIDAsData(doubleEncoded),
                        SignalServiceGroup.GroupType.SIGNAL
                    )
                )
            }
            else -> Optional.absent()
        }
        val pointers = attachments.mapNotNull {
            it.toSignalAttachment()
        }
        val targetAddress = if (isUserSender && !message.syncTarget.isNullOrEmpty()) {
            Address.fromSerialized(message.syncTarget!!)
        } else if (group.isPresent) {
            Address.fromSerialized(GroupUtil.getEncodedId(group.get()))
        } else {
            senderAddress
        }
        val targetRecipient = Recipient.from(context, targetAddress, false)
        //New Line v32
        if (!targetRecipient.isGroupRecipient) {
            val recipientDb = DatabaseComponent.get(context).recipientDatabase()
            if (isUserSender) {
                recipientDb.setApproved(targetRecipient, true)
            } else {
                recipientDb.setApprovedMe(targetRecipient, true)
            }
        }

        if (message.isMediaMessage() || attachments.isNotEmpty()) {
            val quote: Optional<QuoteModel> = if (quotes != null) Optional.of(quotes) else Optional.absent()
            val linkPreviews: Optional<List<LinkPreview>> = if (linkPreview.isEmpty()) Optional.absent() else Optional.of(linkPreview.mapNotNull { it!! })
            val mmsDatabase = DatabaseComponent.get(context).mmsDatabase()
            val insertResult = if (message.sender == getUserPublicKey()) {
                val mediaMessage = OutgoingMediaMessage.from(message, targetRecipient, pointers, quote.orNull(), linkPreviews.orNull()?.firstOrNull())
                mmsDatabase.insertSecureDecryptedMessageOutbox(mediaMessage, message.threadID ?: -1, message.sentTimestamp!!,runThreadUpdate)
            } else {
                // It seems like we have replaced SignalServiceAttachment with BchatServiceAttachment
                val signalServiceAttachments = attachments.mapNotNull {
                    it.toSignalPointer()
                }
                val mediaMessage = IncomingMediaMessage.from(message, senderAddress, targetRecipient.expireMessages * 1000L, group, signalServiceAttachments, quote, linkPreviews)
                mmsDatabase.insertSecureDecryptedMessageInbox(mediaMessage, message.threadID ?: -1, message.receivedTimestamp ?: 0,runIncrement,runThreadUpdate)
            }
            if (insertResult.isPresent) {
                messageID = insertResult.get().messageId
            }
        } else {
            val smsDatabase = DatabaseComponent.get(context).smsDatabase()
            val contactDB   = DatabaseComponent.get(context).bchatContactDatabase()
            val isOpenGroupInvitation = (message.openGroupInvitation != null)
            //Payment Tag
            val isPayment = (message.payment != null)

            val insertResult = if (message.sender == getUserPublicKey()) {
                val textMessage = if (isOpenGroupInvitation) OutgoingTextMessage.fromOpenGroupInvitation(message.openGroupInvitation, targetRecipient, message.sentTimestamp)
                else if(isPayment) OutgoingTextMessage.fromPayment(message.payment,targetRecipient,message.sentTimestamp) //Payment Tag
                else OutgoingTextMessage.from(message, targetRecipient)
                smsDatabase.insertMessageOutboxNew(message.threadID ?: -1, textMessage, message.sentTimestamp!!,runThreadUpdate)

            } else {
                val textMessage = if (isOpenGroupInvitation) IncomingTextMessage.fromOpenGroupInvitation(message.openGroupInvitation, senderAddress, message.sentTimestamp,targetRecipient.expireMessages * 1000L)
                else if(isPayment) IncomingTextMessage.fromPayment(message.payment,senderAddress,message.sentTimestamp,targetRecipient.expireMessages * 1000L) //Payment Tag
                else IncomingTextMessage.from(message, senderAddress, group, targetRecipient.expireMessages * 1000L)
                val encrypted =
                    IncomingEncryptedMessage(
                        textMessage,
                        textMessage.messageBody
                    )
                val contact = contactDB.getContactWithBchatID(message.sender.toString())
                // insert beldex address to Contact database
                message.beldexAddress?.let {
                    if (contact != null) {
                        contactDB.setBeldexAddres(contact,
                            it,1)
                    }
                }
                smsDatabase.insertMessageInboxNew(encrypted, message.receivedTimestamp ?: 0,runIncrement,runThreadUpdate)

            }
            insertResult.orNull()?.let { result ->
                messageID = result.messageId
            }
        }
        val threadID = message.threadID
        // social group trim thread job is scheduled after processing in OpenGroupPollerV2
        if (openGroupID.isNullOrEmpty() && threadID != null && threadID >= 0 && TextSecurePreferences.isThreadLengthTrimmingEnabled(context)) {
            JobQueue.shared.queueThreadForTrim(threadID)
        }
        message.serverHash?.let { serverHash ->
            messageID?.let { id ->
                DatabaseComponent.get(context).beldexMessageDatabase().setMessageServerHash(id, serverHash)
            }
        }
        return messageID
    }

    override fun persistJob(job: Job) {
        DatabaseComponent.get(context).bchatJobDatabase().persistJob(job)
    }

    override fun markJobAsSucceeded(jobId: String) {
        DatabaseComponent.get(context).bchatJobDatabase().markJobAsSucceeded(jobId)
    }

    override fun markJobAsFailedPermanently(jobId: String) {
        DatabaseComponent.get(context).bchatJobDatabase().markJobAsFailedPermanently(jobId)
    }

    override fun getAllPendingJobs(type: String): Map<String, Job?> {
        return DatabaseComponent.get(context).bchatJobDatabase().getAllPendingJobs(type)
    }

    override fun getAttachmentUploadJob(attachmentID: Long): AttachmentUploadJob? {
        return DatabaseComponent.get(context).bchatJobDatabase().getAttachmentUploadJob(attachmentID)
    }

    override fun getMessageSendJob(messageSendJobID: String): MessageSendJob? {
        return DatabaseComponent.get(context).bchatJobDatabase().getMessageSendJob(messageSendJobID)
    }

    override fun getMessageReceiveJob(messageReceiveJobID: String): MessageReceiveJob? {
        return DatabaseComponent.get(context).bchatJobDatabase().getMessageReceiveJob(messageReceiveJobID)
    }

    override fun getGroupAvatarDownloadJob(server: String, room: String): GroupAvatarDownloadJob? {
        return DatabaseComponent.get(context).bchatJobDatabase().getGroupAvatarDownloadJob(server, room)
    }

    override fun resumeMessageSendJobIfNeeded(messageSendJobID: String) {
        val job = DatabaseComponent.get(context).bchatJobDatabase().getMessageSendJob(messageSendJobID) ?: return
        JobQueue.shared.resumePendingSendMessage(job)
    }

    override fun isJobCanceled(job: Job): Boolean {
        return DatabaseComponent.get(context).bchatJobDatabase().isJobCanceled(job)
    }

    override fun getAuthToken(room: String, server: String): String? {
        val id = "$server.$room"
        return DatabaseComponent.get(context).beldexAPIDatabase().getAuthToken(id)
    }

    override fun setAuthToken(room: String, server: String, newValue: String) {
        val id = "$server.$room"
        DatabaseComponent.get(context).beldexAPIDatabase().setAuthToken(id, newValue)
    }

    override fun removeAuthToken(room: String, server: String) {
        val id = "$server.$room"
        DatabaseComponent.get(context).beldexAPIDatabase().setAuthToken(id, null)
    }

    override fun getV2OpenGroup(threadId: Long): OpenGroupV2? {
        if (threadId.toInt() < 0) { return null }
        val database = databaseHelper.readableDatabase
        return database.get(BeldexThreadDatabase.publicChatTable, "${BeldexThreadDatabase.threadID} = ?", arrayOf( threadId.toString() )) { cursor ->
            val publicChatAsJson = cursor.getString(BeldexThreadDatabase.publicChat)
            OpenGroupV2.fromJSON(publicChatAsJson)
        }
    }

    override fun getOpenGroupPublicKey(server: String): String? {
        return DatabaseComponent.get(context).beldexAPIDatabase().getOpenGroupPublicKey(server)
    }

    override fun setOpenGroupPublicKey(server: String, newValue: String) {
        DatabaseComponent.get(context).beldexAPIDatabase().setOpenGroupPublicKey(server, newValue)
    }

    override fun getLastMessageServerID(room: String, server: String): Long? {
        return DatabaseComponent.get(context).beldexAPIDatabase().getLastMessageServerID(room, server)
    }

    override fun setLastMessageServerID(room: String, server: String, newValue: Long) {
        DatabaseComponent.get(context).beldexAPIDatabase().setLastMessageServerID(room, server, newValue)
    }

    override fun removeLastMessageServerID(room: String, server: String) {
        DatabaseComponent.get(context).beldexAPIDatabase().removeLastMessageServerID(room, server)
    }

    override fun getLastDeletionServerID(room: String, server: String): Long? {
        return DatabaseComponent.get(context).beldexAPIDatabase().getLastDeletionServerID(room, server)
    }

    override fun setLastDeletionServerID(room: String, server: String, newValue: Long) {
        DatabaseComponent.get(context).beldexAPIDatabase().setLastDeletionServerID(room, server, newValue)
    }

    override fun removeLastDeletionServerID(room: String, server: String) {
        DatabaseComponent.get(context).beldexAPIDatabase().removeLastDeletionServerID(room, server)
    }

    override fun setUserCount(room: String, server: String, newValue: Int) {
        DatabaseComponent.get(context).beldexAPIDatabase().setUserCount(room, server, newValue)
    }

    override fun setOpenGroupServerMessageID(messageID: Long, serverID: Long, threadID: Long, isSms: Boolean) {
        DatabaseComponent.get(context).beldexMessageDatabase().setServerID(messageID, serverID, isSms)
        DatabaseComponent.get(context).beldexMessageDatabase().setOriginalThreadID(messageID, serverID, threadID)
    }

    override fun isDuplicateMessage(timestamp: Long): Boolean {
        return getReceivedMessageTimestamps().contains(timestamp)
    }

    override fun updateTitle(groupID: String, newValue: String) {
        DatabaseComponent.get(context).groupDatabase().updateTitle(groupID, newValue)
    }

    override fun updateProfilePicture(groupID: String, newValue: ByteArray) {
        DatabaseComponent.get(context).groupDatabase().updateProfilePicture(groupID, newValue)
    }

    override fun getReceivedMessageTimestamps(): Set<Long> {
        return BchatMetaProtocol.getTimestamps()
    }

    override fun addReceivedMessageTimestamp(timestamp: Long) {
        BchatMetaProtocol.addTimestamp(timestamp)
    }

    override fun removeReceivedMessageTimestamps(timestamps: Set<Long>) {
        BchatMetaProtocol.removeTimestamps(timestamps)
    }

    override fun getMessageIdInDatabase(timestamp: Long, author: String): Long? {
        val database = DatabaseComponent.get(context).mmsSmsDatabase()
        val address = Address.fromSerialized(author)
        return database.getMessageFor(timestamp, address)?.getId()
    }

    override fun updateSentTimestamp(
        messageID: Long,
        isMms: Boolean,
        openGroupSentTimestamp: Long,
        threadId: Long
    ) {
        if (isMms) {
            val mmsDb = DatabaseComponent.get(context).mmsDatabase()
            mmsDb.updateSentTimestamp(messageID, openGroupSentTimestamp, threadId)
        } else {
            val smsDb = DatabaseComponent.get(context).smsDatabase()
            smsDb.updateSentTimestamp(messageID, openGroupSentTimestamp, threadId)
        }
    }

    override fun markAsSent(timestamp: Long, author: String) {
        val database = DatabaseComponent.get(context).mmsSmsDatabase()
        val messageRecord = database.getMessageFor(timestamp, author) ?: return
        if (messageRecord.isMms) {
            val mmsDatabase = DatabaseComponent.get(context).mmsDatabase()
            mmsDatabase.markAsSent(messageRecord.getId(), true)
        } else {
            val smsDatabase = DatabaseComponent.get(context).smsDatabase()
            smsDatabase.markAsSent(messageRecord.getId(), true)
        }
    }

    override fun markAsSending(timestamp: Long, author: String) {
        val database = DatabaseComponent.get(context).mmsSmsDatabase()
        val messageRecord = database.getMessageFor(timestamp, author) ?: return
        if (messageRecord.isMms) {
            val mmsDatabase = DatabaseComponent.get(context).mmsDatabase()
            mmsDatabase.markAsSending(messageRecord.getId())
        } else {
            val smsDatabase = DatabaseComponent.get(context).smsDatabase()
            smsDatabase.markAsSending(messageRecord.getId())
            messageRecord.isPending
        }
    }

    override fun markUnidentified(timestamp: Long, author: String) {
        val database = DatabaseComponent.get(context).mmsSmsDatabase()
        val messageRecord = database.getMessageFor(timestamp, author) ?: return
        if (messageRecord.isMms) {
            val mmsDatabase = DatabaseComponent.get(context).mmsDatabase()
            mmsDatabase.markUnidentified(messageRecord.getId(), true)
        } else {
            val smsDatabase = DatabaseComponent.get(context).smsDatabase()
            smsDatabase.markUnidentified(messageRecord.getId(), true)
        }
    }

    override fun setErrorMessage(timestamp: Long, author: String, error: Exception) {
        val database = DatabaseComponent.get(context).mmsSmsDatabase()
        val messageRecord = database.getMessageFor(timestamp, author) ?: return
        if (messageRecord.isMms) {
            val mmsDatabase = DatabaseComponent.get(context).mmsDatabase()
            mmsDatabase.markAsSentFailed(messageRecord.getId())
        } else {
            val smsDatabase = DatabaseComponent.get(context).smsDatabase()
            smsDatabase.markAsSentFailed(messageRecord.getId())
        }
        if (error.localizedMessage != null) {
            val message: String
            if (error is OnionRequestAPI.HTTPRequestFailedAtDestinationException && error.statusCode == 429) {
                message = "429: Rate limited."
            } else {
                message = error.localizedMessage!!
            }
            DatabaseComponent.get(context).beldexMessageDatabase().setErrorMessage(messageRecord.getId(), message)
        } else {
            DatabaseComponent.get(context).beldexMessageDatabase().setErrorMessage(messageRecord.getId(), error.javaClass.simpleName)
        }
    }

    override fun setMessageServerHash(messageID: Long, serverHash: String) {
        DatabaseComponent.get(context).beldexMessageDatabase().setMessageServerHash(messageID, serverHash)
    }

    override fun getGroup(groupID: String): GroupRecord? {
        val group = DatabaseComponent.get(context).groupDatabase().getGroup(groupID)
        return if (group.isPresent) { group.get() } else null
    }

    override fun createGroup(groupId: String, title: String?, members: List<Address>, avatar: SignalServiceAttachmentPointer?, relay: String?, admins: List<Address>, formationTimestamp: Long) {
        DatabaseComponent.get(context).groupDatabase().create(groupId, title, members, avatar, relay, admins, formationTimestamp)
    }

    override fun isGroupActive(groupPublicKey: String): Boolean {
        return DatabaseComponent.get(context).groupDatabase().getGroup(GroupUtil.doubleEncodeGroupID(groupPublicKey)).orNull()?.isActive == true
    }

    override fun setActive(groupID: String, value: Boolean) {
        DatabaseComponent.get(context).groupDatabase().setActive(groupID, value)
    }

    override fun getZombieMembers(groupID: String): Set<String> {
        return DatabaseComponent.get(context).groupDatabase().getGroupZombieMembers(groupID).map { it.address.serialize() }.toHashSet()
    }

    override fun removeMember(groupID: String, member: Address) {
        DatabaseComponent.get(context).groupDatabase().removeMember(groupID, member)
    }

    override fun updateMembers(groupID: String, members: List<Address>) {
        DatabaseComponent.get(context).groupDatabase().updateMembers(groupID, members)
    }

    override fun setZombieMembers(groupID: String, members: List<Address>) {
        DatabaseComponent.get(context).groupDatabase().updateZombieMembers(groupID, members)
    }

    override fun insertIncomingInfoMessage(context: Context, senderPublicKey: String, groupID: String, type: SignalServiceGroup.Type, name: String, members: Collection<String>, admins: Collection<String>, sentTimestamp: Long) {
        val group = SignalServiceGroup(
            type,
            GroupUtil.getDecodedGroupIDAsData(groupID),
            SignalServiceGroup.GroupType.SIGNAL,
            name,
            members.toList(),
            null,
            admins.toList()
        )
        val m =
            IncomingTextMessage(
                Address.fromSerialized(senderPublicKey),
                1,
                sentTimestamp,
                "",
                Optional.of(group),
                0,
                true
            )
        val updateData = UpdateMessageData.buildGroupUpdate(type, name, members)?.toJSON()
        val infoMessage =
            IncomingGroupMessage(
                m,
                groupID,
                updateData,
                true
            )
        val smsDB = DatabaseComponent.get(context).smsDatabase()
        smsDB.insertMessageInbox(infoMessage,true,true)
    }

    override fun insertOutgoingInfoMessage(context: Context, groupID: String, type: SignalServiceGroup.Type, name: String, members: Collection<String>, admins: Collection<String>, threadID: Long, sentTimestamp: Long) {
        val userPublicKey = getUserPublicKey()
        val recipient = Recipient.from(context, Address.fromSerialized(groupID), false)

        val updateData = UpdateMessageData.buildGroupUpdate(type, name, members)?.toJSON() ?: ""
        val infoMessage =
            OutgoingGroupMediaMessage(
                recipient,
                updateData,
                groupID,
                null,
                sentTimestamp,
                0,
                true,
                null,
                listOf(),
                listOf()
            )
        val mmsDB = DatabaseComponent.get(context).mmsDatabase()
        val mmsSmsDB = DatabaseComponent.get(context).mmsSmsDatabase()
        if (mmsSmsDB.getMessageFor(sentTimestamp, userPublicKey) != null) return
        val infoMessageID = mmsDB.insertMessageOutbox(infoMessage, threadID, false, null,runThreadUpdate = true)
        mmsDB.markAsSent(infoMessageID, true)
    }

    override fun isClosedGroup(publicKey: String): Boolean {
        val isClosedGroup = DatabaseComponent.get(context).beldexAPIDatabase().isClosedGroup(publicKey)
        val address = Address.fromSerialized(publicKey)
        return address.isClosedGroup || isClosedGroup
    }

    override fun getClosedGroupEncryptionKeyPairs(groupPublicKey: String): MutableList<ECKeyPair> {
        return DatabaseComponent.get(context).beldexAPIDatabase().getClosedGroupEncryptionKeyPairs(groupPublicKey).toMutableList()
    }

    override fun getLatestClosedGroupEncryptionKeyPair(groupPublicKey: String): ECKeyPair? {
        return DatabaseComponent.get(context).beldexAPIDatabase().getLatestClosedGroupEncryptionKeyPair(groupPublicKey)
    }

    override fun getAllClosedGroupPublicKeys(): Set<String> {
        return DatabaseComponent.get(context).beldexAPIDatabase().getAllClosedGroupPublicKeys()
    }

    override fun getAllActiveClosedGroupPublicKeys(): Set<String> {
        return DatabaseComponent.get(context).beldexAPIDatabase().getAllClosedGroupPublicKeys().filter {
            getGroup(GroupUtil.doubleEncodeGroupID(it))?.isActive == true
        }.toSet()
    }

    override fun addClosedGroupPublicKey(groupPublicKey: String) {
        DatabaseComponent.get(context).beldexAPIDatabase().addClosedGroupPublicKey(groupPublicKey)
    }

    override fun removeClosedGroupPublicKey(groupPublicKey: String) {
        DatabaseComponent.get(context).beldexAPIDatabase().removeClosedGroupPublicKey(groupPublicKey)
    }

    override fun addClosedGroupEncryptionKeyPair(encryptionKeyPair: ECKeyPair, groupPublicKey: String) {
        DatabaseComponent.get(context).beldexAPIDatabase().addClosedGroupEncryptionKeyPair(encryptionKeyPair, groupPublicKey)
    }

    override fun removeAllClosedGroupEncryptionKeyPairs(groupPublicKey: String) {
        DatabaseComponent.get(context).beldexAPIDatabase().removeAllClosedGroupEncryptionKeyPairs(groupPublicKey)
    }

    override fun updateFormationTimestamp(groupID: String, formationTimestamp: Long) {
        DatabaseComponent.get(context).groupDatabase()
            .updateFormationTimestamp(groupID, formationTimestamp)
    }

    override fun updateTimestampUpdated(groupID: String, updatedTimestamp: Long) {
        DatabaseComponent.get(context).groupDatabase()
            .updateTimestampUpdated(groupID, updatedTimestamp)
    }

    override fun setExpirationTimer(groupID: String, duration: Int) {
        val recipient = Recipient.from(context, fromSerialized(groupID), false)
        DatabaseComponent.get(context).recipientDatabase().setExpireMessages(recipient, duration);
    }

    override fun getAllV2OpenGroups(): Map<Long, OpenGroupV2> {
        return DatabaseComponent.get(context).beldexThreadDatabase().getAllV2OpenGroups()
    }

    override fun getAllGroups(): List<GroupRecord> {
        return DatabaseComponent.get(context).groupDatabase().allGroups
    }

    override fun addOpenGroup(urlAsString: String) {
        OpenGroupManager.addOpenGroup(urlAsString, context)
    }

    override fun onOpenGroupAdded(urlAsString: String) {
        val server = OpenGroupV2.getServer(urlAsString)
        OpenGroupManager.restartPollerForServer(server.toString().removeSuffix("/"))
    }

    override fun hasBackgroundGroupAddJob(groupJoinUrl: String): Boolean {
        val jobDb = DatabaseComponent.get(context).bchatJobDatabase()
        return jobDb.hasBackgroundGroupAddJob(groupJoinUrl)
    }

    override fun setProfileSharing(address: Address, value: Boolean) {
        val recipient = Recipient.from(context, address, false)
        DatabaseComponent.get(context).recipientDatabase().setProfileSharing(recipient, value)
    }

    override fun getOrCreateThreadIdFor(address: Address): Long {
        val recipient = Recipient.from(context, address, false)
        return DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(recipient)
    }

    override fun getOrCreateThreadIdFor(publicKey: String, groupPublicKey: String?, openGroupID: String?): Long {
        val database = DatabaseComponent.get(context).threadDatabase()
        if (!openGroupID.isNullOrEmpty()) {
            val recipient = Recipient.from(context, Address.fromSerialized(GroupUtil.getEncodedOpenGroupID(openGroupID.toByteArray())), false)
            return database.getThreadIdIfExistsFor(recipient)
        } else if (!groupPublicKey.isNullOrEmpty()) {
            val recipient = Recipient.from(context, Address.fromSerialized(GroupUtil.doubleEncodeGroupID(groupPublicKey)), false)
            return database.getOrCreateThreadIdFor(recipient)
        } else {
            val recipient = Recipient.from(context, Address.fromSerialized(publicKey), false)
            return database.getOrCreateThreadIdFor(recipient)
        }
    }

    override fun getThreadId(publicKeyOrOpenGroupID: String): Long? {
        val address = Address.fromSerialized(publicKeyOrOpenGroupID)
        return getThreadId(address)
    }

    override fun getThreadId(address: Address): Long? {
        val recipient = Recipient.from(context, address, false)
        return getThreadId(recipient)
    }

    override fun getThreadId(recipient: Recipient): Long? {
        val threadID = DatabaseComponent.get(context).threadDatabase().getThreadIdIfExistsFor(recipient)
        return if (threadID < 0) null else threadID
    }

    override fun getThreadIdForMms(mmsId: Long): Long {
        val mmsDb = DatabaseComponent.get(context).mmsDatabase()
        val cursor = mmsDb.getMessage(mmsId)
        val reader = mmsDb.readerFor(cursor)
        val threadId = reader.next?.threadId
        cursor.close()
        return threadId ?: -1
    }

    override fun getContactWithBchatID(bchatID: String): Contact? {
        return DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(bchatID)
    }

    override fun getAllContacts(): Set<Contact> {
        return DatabaseComponent.get(context).bchatContactDatabase().getAllContacts()
    }

    override fun setContact(contact: Contact) {
        Log.d("beldex","Hi am SetContact in Storage.kt")
        DatabaseComponent.get(context).bchatContactDatabase().setContact(contact)
    }

    override fun insertRecipientAddress(transactionId: String, recipientAddress: String) {
        DatabaseComponent.get(context).bchatRecipientAddressDatabase().insertRecipientAddress(transactionId,recipientAddress)
    }

    override fun getRecipientForThread(threadId: Long): Recipient? {
        return DatabaseComponent.get(context).threadDatabase().getRecipientForThreadId(threadId)
    }

    override fun getRecipientSettings(address: Address): Recipient.RecipientSettings? {
        val recipientSettings = DatabaseComponent.get(context).recipientDatabase().getRecipientSettings(address)
        return if (recipientSettings.isPresent) { recipientSettings.get() } else null
    }

    override fun addContacts(contacts: List<ConfigurationMessage.Contact>) {
        val recipientDatabase = DatabaseComponent.get(context).recipientDatabase()
        val threadDatabase = DatabaseComponent.get(context).threadDatabase()
        for (contact in contacts) {
            val address = Address.fromSerialized(contact.publicKey)
            val recipient = Recipient.from(context, address, true)
            if (!contact.profilePicture.isNullOrEmpty()) {
                recipientDatabase.setProfileAvatar(recipient, contact.profilePicture)
            }
            if (contact.profileKey?.isNotEmpty() == true) {
                recipientDatabase.setProfileKey(recipient, contact.profileKey)
            }
            if (contact.name.isNotEmpty()) {
                recipientDatabase.setProfileName(recipient, contact.name)
            }
            recipientDatabase.setProfileSharing(recipient, true)
            recipientDatabase.setRegistered(recipient, Recipient.RegisteredState.REGISTERED)
            // create Thread if needed
            val threadId = threadDatabase.getOrCreateThreadIdFor(recipient)
            if (contact.didApproveMe == true) {
                recipientDatabase.setApprovedMe(recipient, true)
                threadDatabase.setHasSent(threadId, true)
            }
            if (contact.isApproved == true) {
                recipientDatabase.setApproved(recipient, true)
                threadDatabase.setHasSent(threadId, true)
            }
            if (contact.isBlocked == true) {
                recipientDatabase.setBlocked(recipient, true)
                threadDatabase.deleteConversation(threadId)
            }
        }
        if (contacts.isNotEmpty()) {
            threadDatabase.notifyConversationListListeners()
        }
    }

    override fun getLastUpdated(threadID: Long): Long {
        val threadDB = DatabaseComponent.get(context).threadDatabase()
        return threadDB.getLastUpdated(threadID)
    }

    override fun trimThread(threadID: Long, threadLimit: Int) {
        val threadDB = DatabaseComponent.get(context).threadDatabase()
        threadDB.trimThread(threadID, threadLimit)
    }

    override fun getAttachmentDataUri(attachmentId: AttachmentId): Uri {
        return PartAuthority.getAttachmentDataUri(attachmentId)
    }

    override fun getAttachmentThumbnailUri(attachmentId: AttachmentId): Uri {
        return PartAuthority.getAttachmentThumbnailUri(attachmentId)
    }

    override fun insertDataExtractionNotificationMessage(senderPublicKey: String, message: DataExtractionNotificationInfoMessage, sentTimestamp: Long) {
        val database = DatabaseComponent.get(context).mmsDatabase()
        val address = fromSerialized(senderPublicKey)
        val recipient = Recipient.from(context, address, false)

        if (recipient.isBlocked) return

        val mediaMessage = IncomingMediaMessage(
            address,
            sentTimestamp,
            -1,
            0,
            false,
            false,
            false,
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.of(message)
        )


        database.insertSecureDecryptedMessageInbox(mediaMessage, -1,runIncrement = true, runThreadUpdate = true)
    }

    override fun insertMessageRequestResponse(response: MessageRequestResponse) {
        val userPublicKey = getUserPublicKey()
        val senderPublicKey = response.sender!!
        val recipientPublicKey = response.recipient!!
        if (userPublicKey == null || (userPublicKey != recipientPublicKey && userPublicKey != senderPublicKey)) return
        val recipientDb = DatabaseComponent.get(context).recipientDatabase()
        val threadDB = DatabaseComponent.get(context).threadDatabase()
        if (userPublicKey == senderPublicKey) {
            val requestRecipient = Recipient.from(context, fromSerialized(recipientPublicKey), false)
            recipientDb.setApproved(requestRecipient, true)
            val threadId = threadDB.getOrCreateThreadIdFor(requestRecipient)
            threadDB.setHasSent(threadId, true)
        } else {
            val mmsDb = DatabaseComponent.get(context).mmsDatabase()
            val senderAddress = fromSerialized(senderPublicKey)
            val requestSender = Recipient.from(context, senderAddress, false)
            /* Msg Req Hales63*/
            recipientDb.setApprovedMe(requestSender, true)

            val message = IncomingMediaMessage(
                senderAddress,
                response.sentTimestamp!!,
                -1,
                0,
                false,
                false,
                true,
                Optional.absent(),
                Optional.absent(),
                Optional.absent(),
                Optional.absent(),
                Optional.absent(),
                Optional.absent(),
                Optional.absent()
            )
            val threadId = getOrCreateThreadIdFor(senderAddress)
            mmsDb.insertSecureDecryptedMessageInbox(message, threadId,runIncrement = true, runThreadUpdate = true)
        }
    }

    /*Msg Req Hales63*/
    override fun setRecipientApproved(recipient: Recipient, approved: Boolean) {
        DatabaseComponent.get(context).recipientDatabase().setApproved(recipient, approved)
    }
    /*Msg Req Hales63*/
    override fun setRecipientApprovedMe(recipient: Recipient, approvedMe: Boolean) {
        DatabaseComponent.get(context).recipientDatabase().setApprovedMe(recipient, approvedMe)
    }

    override fun insertCallMessage(senderPublicKey: String, callMessageType: CallMessageType, sentTimestamp: Long) {
        val database = DatabaseComponent.get(context).smsDatabase()
        val address = fromSerialized(senderPublicKey)
        val callMessage = IncomingTextMessage.fromCallInfo(callMessageType, address, Optional.absent(), sentTimestamp)
        database.insertCallMessage(callMessage)
    }

    //SteveJosephh21
    override fun unblock(toUnblock: List<Recipient>) {
        val recipientDb = DatabaseComponent.get(context).recipientDatabase()
        recipientDb.setBlocked(toUnblock, false)
    }

    override fun unblockSingleUser(toUnblock: Recipient) {
        val recipientDb = DatabaseComponent.get(context).recipientDatabase()
        recipientDb.setBlocked(toUnblock, false)
    }

    override fun blockedContacts(): List<Recipient> {
        val recipientDb = DatabaseComponent.get(context).recipientDatabase()
        return recipientDb.blockedContacts
    }
}