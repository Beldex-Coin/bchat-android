package com.beldex.libbchat.messaging.messages

import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.open_groups.OpenGroupV2
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libsignal.utilities.toHexString

sealed class Destination {

    class Contact(var publicKey: String) : Destination() {
        internal constructor(): this("")
    }
    class ClosedGroup(var groupPublicKey: String) : Destination() {
        internal constructor(): this("")
    }
    class OpenGroupV2(var room: String, var server: String) : Destination() {
        internal constructor(): this("", "")
    }

    companion object {

        fun from(address: Address): Destination {
            return when {
                address.isContact -> {
                    Contact(address.contactIdentifier())
                }
                address.isClosedGroup -> {
                    val groupID = address.toGroupString()
                    val groupPublicKey = GroupUtil.doubleDecodeGroupID(groupID).toHexString()
                    ClosedGroup(groupPublicKey)
                }
                address.isOpenGroup -> {
                    val storage = MessagingModuleConfiguration.shared.storage
                    val threadID = storage.getThreadId(address)!!
                    when (val openGroup = storage.getV2OpenGroup(threadID)) {
                        is com.beldex.libbchat.messaging.open_groups.OpenGroupV2
                            -> Destination.OpenGroupV2(openGroup.room, openGroup.server)
                        else -> throw Exception("Missing social group for thread with ID: $threadID.")
                    }
                }
                else -> {
                    throw Exception("TODO: Handle legacy secret groups.")
                }
            }
        }
    }
}