package com.thoughtcrimes.securesms.sskenvironment

import android.content.Context
import android.util.Log
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.jobs.RetrieveProfileAvatarJob

class ProfileManager : SSKEnvironment.ProfileManagerProtocol {

    override fun setNickname(context: Context, recipient: Recipient, nickname: String?) {
        val bchatID = recipient.address.serialize()
        val contactDatabase = DatabaseComponent.get(context).bchatContactDatabase()
        var contact = contactDatabase.getContactWithBchatID(bchatID)
        if (contact == null) contact = Contact(bchatID)
        contact.threadID = DatabaseComponent.get(context).storage().getThreadId(recipient.address)
        if (contact.nickname != nickname) {
            contact.nickname = nickname
            contactDatabase.setContact(contact)
        }
    }

    override fun setName(context: Context, recipient: Recipient, name: String) {
        // New API
        val bchatID = recipient.address.serialize()
        Log.d("Beldex","AM setName in profileManager.kt")
        val contactDatabase = DatabaseComponent.get(context).bchatContactDatabase()
        var contact = contactDatabase.getContactWithBchatID(bchatID)
        if (contact == null) contact = Contact(bchatID)
        contact.threadID = DatabaseComponent.get(context).storage().getThreadId(recipient.address)
        if (contact.name != name) {
            contact.name = name
            contactDatabase.setContact(contact)
        }
        // Old API
        val database = DatabaseComponent.get(context).recipientDatabase()
        database.setProfileName(recipient, name)
        recipient.notifyListeners()
    }

    override fun setBeldexAddress(context: Context, recipient: Recipient, address: String) {
        TODO("Not yet implemented")
    }

    override fun setProfilePictureURL(context: Context, recipient: Recipient, profilePictureURL: String) {
        val job = RetrieveProfileAvatarJob(
            recipient,
            profilePictureURL
        )
        ApplicationContext.getInstance(context).jobManager.add(job)
        val bchatID = recipient.address.serialize()
        val contactDatabase = DatabaseComponent.get(context).bchatContactDatabase()
        var contact = contactDatabase.getContactWithBchatID(bchatID)
        if (contact == null) contact = Contact(bchatID)
        contact.threadID = DatabaseComponent.get(context).storage().getThreadId(recipient.address)
        if (contact.profilePictureURL != profilePictureURL) {
            contact.profilePictureURL = profilePictureURL
            contactDatabase.setContact(contact)
        }
    }

    override fun setProfileKey(context: Context, recipient: Recipient, profileKey: ByteArray) {
        // New API
        val bchatID = recipient.address.serialize()
        val contactDatabase = DatabaseComponent.get(context).bchatContactDatabase()
        var contact = contactDatabase.getContactWithBchatID(bchatID)
        if (contact == null) contact = Contact(bchatID)
        contact.threadID = DatabaseComponent.get(context).storage().getThreadId(recipient.address)
        if (!contact.profilePictureEncryptionKey.contentEquals(profileKey)) {
            contact.profilePictureEncryptionKey = profileKey
            contactDatabase.setContact(contact)
        }
        // Old API
        val database = DatabaseComponent.get(context).recipientDatabase()
        database.setProfileKey(recipient, profileKey)
    }

    override fun setUnidentifiedAccessMode(context: Context, recipient: Recipient, unidentifiedAccessMode: Recipient.UnidentifiedAccessMode) {
        val database = DatabaseComponent.get(context).recipientDatabase()
        database.setUnidentifiedAccessMode(recipient, unidentifiedAccessMode)
    }
}