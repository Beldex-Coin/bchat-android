package com.beldex.libbchat.database

import android.content.Context
import android.net.Uri
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.messaging.jobs.AttachmentUploadJob
import com.beldex.libbchat.messaging.jobs.Job
import com.beldex.libbchat.messaging.jobs.MessageSendJob
import com.beldex.libbchat.messaging.messages.control.ConfigurationMessage
import com.beldex.libbchat.messaging.messages.control.MessageRequestResponse
import com.beldex.libbchat.messaging.messages.visible.Attachment
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage
import com.beldex.libbchat.messaging.open_groups.OpenGroupV2
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentId
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import com.beldex.libbchat.messaging.sending_receiving.data_extraction.DataExtractionNotificationInfoMessage
import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview
import com.beldex.libbchat.messaging.sending_receiving.quotes.QuoteModel
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupRecord
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libbchat.utilities.recipients.Recipient.RecipientSettings
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.beldex.libsignal.messages.SignalServiceAttachmentPointer
import com.beldex.libsignal.messages.SignalServiceGroup

interface StorageProtocol {

    // General
    fun getUserPublicKey(): String?
    fun getUserX25519KeyPair(): ECKeyPair
    fun getUserDisplayName(): String?
    fun getUserProfileKey(): ByteArray?
    fun getUserProfilePictureURL(): String?
    fun setUserProfilePictureURL(newProfilePicture: String)
    // Signal
    fun getOrGenerateRegistrationID(): Int
    // New line
    fun getSenderBeldexAddress(): String?

    // Jobs
    fun persistJob(job: Job)
    fun markJobAsSucceeded(jobId: String)
    fun markJobAsFailedPermanently(jobId: String)
    fun getAllPendingJobs(type: String): Map<String,Job?>
    fun getAttachmentUploadJob(attachmentID: Long): AttachmentUploadJob?
    fun getMessageSendJob(messageSendJobID: String): MessageSendJob?
    fun getMessageReceiveJob(messageReceiveJobID: String): Job?
    fun getGroupAvatarDownloadJob(server: String, room: String): Job?
    fun resumeMessageSendJobIfNeeded(messageSendJobID: String)
    fun isJobCanceled(job: Job): Boolean

    // Authorization
    fun getAuthToken(room: String, server: String): String?
    fun setAuthToken(room: String, server: String, newValue: String)
    fun removeAuthToken(room: String, server: String)

    // Social Groups
    fun getAllV2OpenGroups(): Map<Long, OpenGroupV2>
    fun getV2OpenGroup(threadId: Long): OpenGroupV2?
    fun addOpenGroup(urlAsString: String)
    fun setOpenGroupServerMessageID(messageID: Long, serverID: Long, threadID: Long, isSms: Boolean)

    // Social Group Public Keys
    fun getOpenGroupPublicKey(server: String): String?
    fun setOpenGroupPublicKey(server: String, newValue: String)

    // Social Group Metadata
    fun updateTitle(groupID: String, newValue: String)
    fun updateProfilePicture(groupID: String, newValue: ByteArray)
    fun setUserCount(room: String, server: String, newValue: Int)

    // Last Message Server ID
    fun getLastMessageServerID(room: String, server: String): Long?
    fun setLastMessageServerID(room: String, server: String, newValue: Long)
    fun removeLastMessageServerID(room: String, server: String)

    // Last Deletion Server ID
    fun getLastDeletionServerID(room: String, server: String): Long?
    fun setLastDeletionServerID(room: String, server: String, newValue: Long)
    fun removeLastDeletionServerID(room: String, server: String)

    // Message Handling
    fun isDuplicateMessage(timestamp: Long): Boolean
    fun getReceivedMessageTimestamps(): Set<Long>
    fun addReceivedMessageTimestamp(timestamp: Long)
    fun removeReceivedMessageTimestamps(timestamps: Set<Long>)
    /**
     * Returns the IDs of the saved attachments.
     */
    fun persistAttachments(messageID: Long, attachments: List<Attachment>): List<Long>
    fun getAttachmentsForMessage(messageID: Long): List<DatabaseAttachment>
    fun getMessageIdInDatabase(timestamp: Long, author: String): Long? // TODO: This is a weird name
    fun updateSentTimestamp(messageID: Long, isMms: Boolean, openGroupSentTimestamp: Long, threadId: Long)
    fun markAsSending(timestamp: Long, author: String)
    fun markAsSent(timestamp: Long, author: String)
    fun markUnidentified(timestamp: Long, author: String)
    fun setErrorMessage(timestamp: Long, author: String, error: Exception)
    fun setMessageServerHash(messageID: Long, serverHash: String)

    // Secret Groups
    fun getGroup(groupID: String): GroupRecord?
    fun createGroup(groupID: String, title: String?, members: List<Address>, avatar: SignalServiceAttachmentPointer?, relay: String?, admins: List<Address>, formationTimestamp: Long)
    fun isGroupActive(groupPublicKey: String): Boolean
    fun setActive(groupID: String, value: Boolean)
    fun getZombieMembers(groupID: String): Set<String>
    fun removeMember(groupID: String, member: Address)
    fun updateMembers(groupID: String, members: List<Address>)
    fun setZombieMembers(groupID: String, members: List<Address>)
    fun getAllClosedGroupPublicKeys(): Set<String>
    fun getAllActiveClosedGroupPublicKeys(): Set<String>
    fun addClosedGroupPublicKey(groupPublicKey: String)
    fun removeClosedGroupPublicKey(groupPublicKey: String)
    fun addClosedGroupEncryptionKeyPair(encryptionKeyPair: ECKeyPair, groupPublicKey: String)
    fun removeAllClosedGroupEncryptionKeyPairs(groupPublicKey: String)
    fun insertIncomingInfoMessage(context: Context, senderPublicKey: String, groupID: String, type: SignalServiceGroup.Type,
                                  name: String, members: Collection<String>, admins: Collection<String>, sentTimestamp: Long)
    fun insertOutgoingInfoMessage(context: Context, groupID: String, type: SignalServiceGroup.Type, name: String,
                                  members: Collection<String>, admins: Collection<String>, threadID: Long, sentTimestamp: Long)
    fun isClosedGroup(publicKey: String): Boolean
    fun getClosedGroupEncryptionKeyPairs(groupPublicKey: String): MutableList<ECKeyPair>
    fun getLatestClosedGroupEncryptionKeyPair(groupPublicKey: String): ECKeyPair?
    fun updateFormationTimestamp(groupID: String, formationTimestamp: Long)
    fun updateTimestampUpdated(groupID: String, updatedTimestamp: Long)
    fun setExpirationTimer(groupID: String, duration: Int)

    // Groups
    fun getAllGroups(): List<GroupRecord>

    // Settings
    fun setProfileSharing(address: Address, value: Boolean)

    // Thread
    fun getOrCreateThreadIdFor(address: Address): Long
    fun getOrCreateThreadIdFor(publicKey: String, groupPublicKey: String?, openGroupID: String?): Long
    fun getThreadId(publicKeyOrOpenGroupID: String): Long?
    fun getThreadId(address: Address): Long?
    fun getThreadId(recipient: Recipient): Long?
    fun getThreadIdForMms(mmsId: Long): Long
    fun getLastUpdated(threadID: Long): Long
    fun trimThread(threadID: Long, threadLimit: Int)

    // Contacts
    fun getContactWithBchatID(bchatID: String): Contact?
    fun getAllContacts(): Set<Contact>
    fun setContact(contact: Contact)
   // fun setBeldexAddress(contect: Contact,beldexAddress : String, threadID: Long)
    fun getRecipientForThread(threadId: Long): Recipient?
    fun getRecipientSettings(address: Address): RecipientSettings?
    fun addContacts(contacts: List<ConfigurationMessage.Contact>)

    // Attachments
    fun getAttachmentDataUri(attachmentId: AttachmentId): Uri
    fun getAttachmentThumbnailUri(attachmentId: AttachmentId): Uri

    // Message Handling
    /**
     * Returns the ID of the `TSIncomingMessage` that was constructed.
     */
    fun persist(message: VisibleMessage, quotes: QuoteModel?, linkPreview: List<LinkPreview?>, groupPublicKey: String?, openGroupID: String?, attachments: List<Attachment>): Long?
    fun insertDataExtractionNotificationMessage(senderPublicKey: String, message: DataExtractionNotificationInfoMessage, sentTimestamp: Long)
    /*Hales63*/
    fun insertMessageRequestResponse(response: MessageRequestResponse)
    //New Line v32
    fun setRecipientApproved(recipient: Recipient, approved: Boolean)
    fun setRecipientApprovedMe(recipient: Recipient, approvedMe: Boolean)
}
