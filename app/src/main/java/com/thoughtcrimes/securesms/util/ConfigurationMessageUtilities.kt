package com.thoughtcrimes.securesms.util

import android.content.Context
import nl.komponents.kovenant.Promise
import com.beldex.libbchat.messaging.messages.Destination
import com.beldex.libbchat.messaging.messages.control.ConfigurationMessage
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil

object ConfigurationMessageUtilities {

    @JvmStatic
    fun syncConfigurationIfNeeded(context: Context) {
        val userPublicKey = TextSecurePreferences.getLocalNumber(context) ?: return
        val lastSyncTime = TextSecurePreferences.getLastConfigurationSyncTime(context)
        val now = System.currentTimeMillis()
        if (now - lastSyncTime < 7 * 24 * 60 * 60 * 1000) return
        /*Hales63*/
        val contacts = ContactUtilities.getAllContacts(context).filter { recipient ->
            !recipient.name.isNullOrEmpty()  && !recipient.isLocalNumber && recipient.address.serialize().isNotEmpty()
        }.map { recipient ->
            ConfigurationMessage.Contact(
                publicKey = recipient.address.serialize(),
                name = recipient.name!!,
                profilePicture = recipient.profileAvatar,
                profileKey = recipient.profileKey,
                isApproved = recipient.isApproved,
                isBlocked = recipient.isBlocked,
                didApproveMe = recipient.hasApprovedMe()
            )        }
        val configurationMessage = ConfigurationMessage.getCurrent(contacts) ?: return
        MessageSender.send(configurationMessage, Address.fromSerialized(userPublicKey))
        TextSecurePreferences.setLastConfigurationSyncTime(context, now)
    }

   /* Hales63*/
    fun forceSyncConfigurationNowIfNeeded(context: Context): Promise<Unit, Exception> {
        val userPublicKey = TextSecurePreferences.getLocalNumber(context) ?: return Promise.ofSuccess(Unit)
        val contacts = ContactUtilities.getAllContacts(context).filter { recipient ->
            !recipient.isGroupRecipient && !recipient.name.isNullOrEmpty() && !recipient.isLocalNumber && recipient.address.serialize().isNotEmpty()
        }.map { recipient ->
            ConfigurationMessage.Contact(
                publicKey = recipient.address.serialize(),
                name = recipient.name!!,
                profilePicture = recipient.profileAvatar,
                profileKey = recipient.profileKey,
                isApproved = recipient.isApproved,
                isBlocked = recipient.isBlocked,
                didApproveMe = recipient.hasApprovedMe()
            )        }
        val configurationMessage = ConfigurationMessage.getCurrent(contacts) ?: return Promise.ofSuccess(Unit)
        val promise = MessageSender.send(configurationMessage, Destination.from(Address.fromSerialized(userPublicKey)))
        TextSecurePreferences.setLastConfigurationSyncTime(context, System.currentTimeMillis())
        return promise
    }

}