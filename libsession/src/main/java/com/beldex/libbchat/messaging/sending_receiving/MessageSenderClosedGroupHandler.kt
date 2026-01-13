@file:Suppress("NAME_SHADOWING")

package com.beldex.libbchat.messaging.sending_receiving

import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.messages.control.ClosedGroupControlMessage
import com.beldex.libbchat.messaging.sending_receiving.MessageSender.Error
import com.beldex.libbchat.messaging.sending_receiving.notifications.PushRegistryV1
import com.beldex.libbchat.messaging.sending_receiving.pollers.ClosedGroupPollerV2
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.Address.Companion.fromSerialized
import com.beldex.libbchat.utilities.Device
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.crypto.ecc.Curve
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.beldex.libsignal.messages.SignalServiceGroup
import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.Hex
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.ThreadUtils
import com.beldex.libsignal.utilities.guava.Optional
import com.beldex.libsignal.utilities.hexEncodedPublicKey
import com.beldex.libsignal.utilities.removingbdPrefixIfNeeded
import com.google.protobuf.ByteString
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.Collection
import kotlin.collections.List
import kotlin.collections.any
import kotlin.collections.isNotEmpty
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.minus
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.setOf
import kotlin.collections.toSet

const val groupSizeLimit = 100

val pendingKeyPairs = ConcurrentHashMap<String, Optional<ECKeyPair>>()

fun MessageSender.create(device: Device, name: String, members: Collection<String>): Promise<String, Exception> {
    val deferred = deferred<String, Exception>()
    ThreadUtils.queue {
        // Prepare
        val context = MessagingModuleConfiguration.shared.context
        val storage = MessagingModuleConfiguration.shared.storage
        val userPublicKey = storage.getUserPublicKey()!!
        val membersAsData = members.map { ByteString.copyFrom(Hex.fromStringCondensed(it)) }
        // Generate the group's public key
        val groupPublicKey = Curve.generateKeyPair().hexEncodedPublicKey // Includes the "05" prefix
        // Generate the key pair that'll be used for encryption and decryption
        val encryptionKeyPair = Curve.generateKeyPair()
        // Create the group
        val groupID = GroupUtil.doubleEncodeGroupID(groupPublicKey)
        val admins = setOf( userPublicKey )
        val adminsAsData = admins.map { ByteString.copyFrom(Hex.fromStringCondensed(it)) }
        storage.createGroup(groupID, name, LinkedList(members.map { Address.fromSerialized(it) }),
            null, null, LinkedList(admins.map { Address.fromSerialized(it) }), MnodeAPI.nowWithOffset)
        storage.setProfileSharing(Address.fromSerialized(groupID), true)
        // Send a secret group update message to all members individually
        val closedGroupUpdateKind = ClosedGroupControlMessage.Kind.New(ByteString.copyFrom(
            Hex.fromStringCondensed(groupPublicKey)), name, encryptionKeyPair, membersAsData, adminsAsData, 0)
        val sentTime = MnodeAPI.nowWithOffset

        // Add the group to the user's set of public keys to poll for
        storage.addClosedGroupPublicKey(groupPublicKey)
        // Store the encryption key pair
        storage.addClosedGroupEncryptionKeyPair(encryptionKeyPair, groupPublicKey)
        // Notify the user
        val threadID = storage.getOrCreateThreadIdFor(Address.fromSerialized(groupID))
        storage.insertOutgoingInfoMessage(context, groupID, SignalServiceGroup.Type.CREATION, name, members, admins, threadID, sentTime)

        for (member in members) {
            val closedGroupControlMessage = ClosedGroupControlMessage(closedGroupUpdateKind)
            closedGroupControlMessage.sentTimestamp = sentTime
            try {
                sendNonDurably(closedGroupControlMessage, Address.fromSerialized(member)).get()
            } catch (e: Exception) {
                // We failed to properly create the group so delete it's associated data (in the past
                // we didn't create this data until the messages successfully sent but this resulted
                // in race conditions due to the `NEW` message sent to our own swarm)
                storage.removeClosedGroupPublicKey(groupPublicKey)
                storage.removeAllClosedGroupEncryptionKeyPairs(groupPublicKey)
                storage.deleteConversation(threadID)
                deferred.reject(e)
                return@queue
            }
        }

        // Notify the PN server
        PushRegistryV1.register(device = device, publicKey = userPublicKey)
        // Start polling
        ClosedGroupPollerV2.shared.startPolling(groupPublicKey)
        // Fulfill the promise
        deferred.resolve(groupID)
    }
    // Return
    return deferred.promise
}

fun MessageSender.update(groupPublicKey: String, members: List<String>, name: String) {
    val context = MessagingModuleConfiguration.shared.context
    val storage = MessagingModuleConfiguration.shared.storage
    val groupID = GroupUtil.doubleEncodeGroupID(groupPublicKey)
    val group = storage.getGroup(groupID) ?: run {
        Log.d("Beldex", "Can't update nonexistent secret group.")
        throw Error.NoThread
    }
    // Update name if needed
    if (name != group.title) { setName(groupPublicKey, name) }
    // Add members if needed
    val addedMembers = members - group.members.map { it.serialize() }
    if (!addedMembers.isEmpty()) { addMembers(groupPublicKey, addedMembers) }
    // Remove members if needed
    val removedMembers = group.members.map { it.serialize() } - members
    if (removedMembers.isEmpty()) { removeMembers(groupPublicKey, removedMembers) }
}

fun MessageSender.setName(groupPublicKey: String, newName: String) {
    val context = MessagingModuleConfiguration.shared.context
    val storage = MessagingModuleConfiguration.shared.storage
    val groupID = GroupUtil.doubleEncodeGroupID(groupPublicKey)
    val group = storage.getGroup(groupID) ?: run {
        Log.d("Beldex", "Can't change name for nonexistent secret group.")
        throw Error.NoThread
    }
    val members = group.members.map { it.serialize() }.toSet()
    val admins = group.admins.map { it.serialize() }
    // Send the update to the group
    val kind = ClosedGroupControlMessage.Kind.NameChange(newName)
    val sentTime = MnodeAPI.nowWithOffset
    val closedGroupControlMessage = ClosedGroupControlMessage(kind)
    closedGroupControlMessage.sentTimestamp = sentTime
    send(closedGroupControlMessage, Address.fromSerialized(groupID))
    // Update the group
    storage.updateTitle(groupID, newName)
    // Notify the user
    val infoType = SignalServiceGroup.Type.NAME_CHANGE
    val threadID = storage.getOrCreateThreadIdFor(Address.fromSerialized(groupID))
    storage.insertOutgoingInfoMessage(context, groupID, infoType, newName, members, admins, threadID, sentTime)
}

fun MessageSender.addMembers(groupPublicKey: String, membersToAdd: List<String>) {
    val context = MessagingModuleConfiguration.shared.context
    val storage = MessagingModuleConfiguration.shared.storage
    val groupID = GroupUtil.doubleEncodeGroupID(groupPublicKey)
    val group = storage.getGroup(groupID) ?: run {
        Log.d("Beldex", "Can't add members to nonexistent secret group.")
        throw Error.NoThread
    }
    val recipient = Recipient.from(context, fromSerialized(groupID), false)
    val expireTimer = recipient.expireMessages
    if (membersToAdd.isEmpty()) {
        Log.d("Beldex", "Invalid secret group update.")
        throw Error.InvalidClosedGroupUpdate
    }
    val updatedMembers = group.members.map { it.serialize() }.toSet() + membersToAdd
    // Save the new group members
    storage.updateMembers(groupID, updatedMembers.map { Address.fromSerialized(it) })
    val membersAsData = updatedMembers.map { ByteString.copyFrom(Hex.fromStringCondensed(it)) }
    val newMembersAsData = membersToAdd.map { ByteString.copyFrom(Hex.fromStringCondensed(it)) }
    val admins = group.admins.map { it.serialize() }
    val adminsAsData = admins.map { ByteString.copyFrom(Hex.fromStringCondensed(it)) }
    val encryptionKeyPair = storage.getLatestClosedGroupEncryptionKeyPair(groupPublicKey) ?: run {
        Log.d("Beldex", "Couldn't get encryption key pair for secret group.")
        throw Error.NoKeyPair
    }
    val name = group.title
    // Send the update to the group
    val memberUpdateKind = ClosedGroupControlMessage.Kind.MembersAdded(newMembersAsData)
    val sentTime = MnodeAPI.nowWithOffset
    val closedGroupControlMessage = ClosedGroupControlMessage(memberUpdateKind)
    closedGroupControlMessage.sentTimestamp = sentTime
    send(closedGroupControlMessage, Address.fromSerialized(groupID))
    // Send secret group update messages to any new members individually
    for (member in membersToAdd) {
        val closedGroupNewKind = ClosedGroupControlMessage.Kind.New(ByteString.copyFrom(
            Hex.fromStringCondensed(groupPublicKey)), name, encryptionKeyPair, membersAsData, adminsAsData, expireTimer)
        val closedGroupControlMessage = ClosedGroupControlMessage(closedGroupNewKind)
        // It's important that the sent timestamp of this message is greater than the sent timestamp
        // of the `MembersAdded` message above. The reason is that upon receiving this `New` message,
        // the recipient will update the secret group formation timestamp and ignore any secret group
        // updates from before that timestamp. By setting the timestamp of the message below to a value
        // greater than that of the `MembersAdded` message, we ensure that newly added members ignore
        // the `MembersAdded` message.
        closedGroupControlMessage.sentTimestamp = MnodeAPI.nowWithOffset
        send(closedGroupControlMessage, Address.fromSerialized(member))
    }
    // Notify the user
    val infoType = SignalServiceGroup.Type.MEMBER_ADDED
    val threadID = storage.getOrCreateThreadIdFor(Address.fromSerialized(groupID))
    storage.insertOutgoingInfoMessage(context, groupID, infoType, name, membersToAdd, admins, threadID, sentTime)
}

fun MessageSender.removeMembers(groupPublicKey: String, membersToRemove: List<String>) {
    val context = MessagingModuleConfiguration.shared.context
    val storage = MessagingModuleConfiguration.shared.storage
    val userPublicKey = storage.getUserPublicKey()!!
    val groupID = GroupUtil.doubleEncodeGroupID(groupPublicKey)
    val group = storage.getGroup(groupID) ?: run {
        Log.d("Beldex", "Can't remove members from nonexistent secret group.")
        throw Error.NoThread
    }
    if (membersToRemove.isEmpty() || membersToRemove.contains(userPublicKey)) {
        Log.d("Beldex", "Invalid secret group update.")
        throw Error.InvalidClosedGroupUpdate
    }
    val admins = group.admins.map { it.serialize() }
    if (!admins.contains(userPublicKey)) {
        Log.d("Beldex", "Only an admin can remove members from a group.")
        throw Error.InvalidClosedGroupUpdate
    }
    val updatedMembers = group.members.map { it.serialize() }.toSet() - membersToRemove
    if (membersToRemove.any { it in admins } && updatedMembers.isNotEmpty()) {
        Log.d("Beldex", "Can't remove admin from secret group unless the group is destroyed entirely.")
        throw Error.InvalidClosedGroupUpdate
    }
    // Save the new group members
    storage.updateMembers(groupID, updatedMembers.map { Address.fromSerialized(it) })
    // Update the zombie list
    val oldZombies = storage.getZombieMembers(groupID)
    storage.setZombieMembers(groupID, oldZombies.minus(membersToRemove).map { Address.fromSerialized(it) })
    val removeMembersAsData = membersToRemove.map { ByteString.copyFrom(Hex.fromStringCondensed(it)) }
    val name = group.title
    // Send the update to the group
    val memberUpdateKind = ClosedGroupControlMessage.Kind.MembersRemoved(removeMembersAsData)
    val sentTime = MnodeAPI.nowWithOffset
    val closedGroupControlMessage = ClosedGroupControlMessage(memberUpdateKind)
    closedGroupControlMessage.sentTimestamp = sentTime
    send(closedGroupControlMessage, Address.fromSerialized(groupID))
    // Send the new encryption key pair to the remaining group members.
    // At this stage we know the user is admin, no need to test.
    generateAndSendNewEncryptionKeyPair(groupPublicKey, updatedMembers)
    // Notify the user
    // We don't display zombie members in the notification as users have already been notified when those members left
    val notificationMembers = membersToRemove.minus(oldZombies)
    if (notificationMembers.isNotEmpty()) {
        // No notification to display when only zombies have been removed
        val infoType = SignalServiceGroup.Type.MEMBER_REMOVED
        val threadID = storage.getOrCreateThreadIdFor(Address.fromSerialized(groupID))
        storage.insertOutgoingInfoMessage(context, groupID, infoType, name, notificationMembers, admins, threadID, sentTime)
    }
}

fun MessageSender.leave(groupPublicKey: String, notifyUser: Boolean = true): Promise<Unit, Exception> {
    val deferred = deferred<Unit, Exception>()
    ThreadUtils.queue {
        val context = MessagingModuleConfiguration.shared.context
        val storage = MessagingModuleConfiguration.shared.storage
        val userPublicKey = TextSecurePreferences.getLocalNumber(context)!!
        val groupID = GroupUtil.doubleEncodeGroupID(groupPublicKey)
        val group = storage.getGroup(groupID) ?: return@queue deferred.reject(Error.NoThread)
        val updatedMembers = group.members.map { it.serialize() }.toSet() - userPublicKey
        val admins = group.admins.map { it.serialize() }
        val name = group.title
        // Send the update to the group
        val closedGroupControlMessage = ClosedGroupControlMessage(ClosedGroupControlMessage.Kind.MemberLeft())
        val sentTime = MnodeAPI.nowWithOffset
        closedGroupControlMessage.sentTimestamp = sentTime
        storage.setActive(groupID, false)
        sendNonDurably(closedGroupControlMessage, Address.fromSerialized(groupID)).success {
            // Notify the user
            val infoType = SignalServiceGroup.Type.QUIT
            val threadID = storage.getOrCreateThreadIdFor(Address.fromSerialized(groupID))
            if (notifyUser) {
                storage.insertOutgoingInfoMessage(context, groupID, infoType, name, updatedMembers, admins, threadID, sentTime)
            }
            // Remove the group private key and unsubscribe from PNs
            MessageReceiver.disableLocalGroupAndUnsubscribe(groupPublicKey, groupID, userPublicKey, true)
            deferred.resolve(Unit)
        }.fail {
            storage.setActive(groupID, true)
        }
    }
    return deferred.promise
}

fun MessageSender.generateAndSendNewEncryptionKeyPair(groupPublicKey: String, targetMembers: Collection<String>) {
    // Prepare
    val storage = MessagingModuleConfiguration.shared.storage
    val userPublicKey = storage.getUserPublicKey()!!
    val groupID = GroupUtil.doubleEncodeGroupID(groupPublicKey)
    val group = storage.getGroup(groupID) ?: run {
        Log.d("Beldex", "Can't update nonexistent secret group.")
        throw Error.NoThread
    }
    if (!group.admins.map { it.toString() }.contains(userPublicKey)) {
        Log.d("Beldex", "Can't distribute new encryption key pair as non-admin.")
        throw Error.InvalidClosedGroupUpdate
    }
    // Generate the new encryption key pair
    val newKeyPair = Curve.generateKeyPair()
    // Replace call will not succeed if no value already set
    pendingKeyPairs.putIfAbsent(groupPublicKey,
        Optional.absent())
    do {
        // Make sure we set the pending key pair or wait until it is not null
    } while (!pendingKeyPairs.replace(groupPublicKey,
            Optional.absent(),
           Optional.fromNullable(newKeyPair)))
    // Distribute it
    sendEncryptionKeyPair(groupPublicKey, newKeyPair, targetMembers)?.success {
        // Store it * after * having sent out the message to the group
        storage.addClosedGroupEncryptionKeyPair(newKeyPair, groupPublicKey)
        pendingKeyPairs[groupPublicKey] = Optional.absent()
    }
}

fun MessageSender.sendEncryptionKeyPair(groupPublicKey: String, newKeyPair: ECKeyPair, targetMembers: Collection<String>, targetUser: String? = null, force: Boolean = true): Promise<Unit, Exception>? {
    val destination = targetUser ?: GroupUtil.doubleEncodeGroupID(groupPublicKey)
    val proto = SignalServiceProtos.KeyPair.newBuilder()
    proto.publicKey = ByteString.copyFrom(newKeyPair.publicKey.serialize().removingbdPrefixIfNeeded())
    proto.privateKey = ByteString.copyFrom(newKeyPair.privateKey.serialize())
    val plaintext = proto.build().toByteArray()
    val storage = MessagingModuleConfiguration.shared.storage

    val wrappers = targetMembers.map { publicKey ->
        val ciphertext = MessageEncrypter.encrypt(plaintext, publicKey,storage.getSenderBeldexAddress()!!)
        ClosedGroupControlMessage.KeyPairWrapper(publicKey, ByteString.copyFrom(ciphertext))
    }
    val kind = ClosedGroupControlMessage.Kind.EncryptionKeyPair(ByteString.copyFrom(
        Hex.fromStringCondensed(groupPublicKey)), wrappers)
    val sentTime = MnodeAPI.nowWithOffset
    val closedGroupControlMessage = ClosedGroupControlMessage(kind)
    closedGroupControlMessage.sentTimestamp = sentTime
    return if (force) {
        MessageSender.sendNonDurably(closedGroupControlMessage, Address.fromSerialized(destination))
    } else {
        MessageSender.send(closedGroupControlMessage, Address.fromSerialized(destination))
        null
    }
}

fun MessageSender.sendLatestEncryptionKeyPair(publicKey: String, groupPublicKey: String) {
    val storage = MessagingModuleConfiguration.shared.storage
    val groupID = GroupUtil.doubleEncodeGroupID(groupPublicKey)
    val group = storage.getGroup(groupID) ?: run {
        Log.d("Beldex", "Can't send encryption key pair for nonexistent secret group.")
        throw Error.NoThread
    }
    val members = group.members.map { it.serialize() }
    if (!members.contains(publicKey)) {
        Log.d("Beldex", "Refusing to send latest encryption key pair to non-member.")
        return
    }
    // Get the latest encryption key pair
    val encryptionKeyPair = pendingKeyPairs[groupPublicKey]?.orNull()
        ?: storage.getLatestClosedGroupEncryptionKeyPair(groupPublicKey) ?: return
    // Send it
    val proto = SignalServiceProtos.KeyPair.newBuilder()
    proto.publicKey = ByteString.copyFrom(encryptionKeyPair.publicKey.serialize().removingbdPrefixIfNeeded())
    proto.privateKey = ByteString.copyFrom(encryptionKeyPair.privateKey.serialize())
    val plaintext = proto.build().toByteArray()
    val ciphertext = MessageEncrypter.encrypt(plaintext, publicKey,storage.getSenderBeldexAddress()!!)
    val wrapper = ClosedGroupControlMessage.KeyPairWrapper(publicKey, ByteString.copyFrom(ciphertext))
    val kind = ClosedGroupControlMessage.Kind.EncryptionKeyPair(ByteString.copyFrom(
        Hex.fromStringCondensed(groupPublicKey)), listOf(wrapper))
    val closedGroupControlMessage = ClosedGroupControlMessage(kind)
    MessageSender.send(closedGroupControlMessage, Address.fromSerialized(publicKey))
}