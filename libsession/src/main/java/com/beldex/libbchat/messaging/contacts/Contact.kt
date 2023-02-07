package com.beldex.libbchat.messaging.contacts

import com.beldex.libbchat.utilities.recipients.Recipient

class Contact(val bchatID: String) {
    /**
     * The URL from which to fetch the contact's profile picture.
     */
    var profilePictureURL: String? = null
    /**
     * The file name of the contact's profile picture on local storage.
     */
    var profilePictureFileName: String? = null
    /**
     * The key with which the profile picture is encrypted.
     */
    var profilePictureEncryptionKey: ByteArray? = null
    /**
     * The ID of the thread associated with this contact.
     */
    var threadID: Long? = null
    /**
     * This flag is used to determine whether we should auto-download files sent by this contact.
     */
    var isTrusted = false

    // region Name
    /**
     * The name of the contact. Use this whenever you need the "real", underlying name of a user (e.g. when sending a message).
     */
    var name: String? = null
    /**
     * The contact's nickname, if the user set one.
     */
    var nickname: String? = null
    /**
     * The Beldex address is get from decryption.
     */
    var beldexAddress: String? = null
    /**
     * The report issue's BChat ID, set by default.
     */
    private val reportIssueBChatID = "bdb890a974a25ef50c64cc4e3270c4c49c7096c433b8eecaf011c1ad000e426813"
    /**
     * The name to display in the UI. For local use only.
     */
    fun displayName(context: ContactContext): String? {
        if (bchatID == reportIssueBChatID) {
            name = "Report Issue"
            return name
        }
        nickname?.let { return it }
        return when (context) {
            ContactContext.REGULAR -> name
            ContactContext.OPEN_GROUP -> {
                // In social groups, where it's more likely that multiple users have the same name,
                // we display a bit of the BChat ID after a user's display name for added context.
                name?.let {
                    return "$name (...${bchatID.takeLast(8)})"
                }
                return null
            }
        }
    }
    // endregion
    fun displayBeldexAddress(context: ContactContext): String? {
        /*Hales*/
           /*nickname?.let { return it }*/
        return when (context) {
            ContactContext.REGULAR -> beldexAddress
            ContactContext.OPEN_GROUP -> {
                // In social groups, where it's more likely that multiple users have the same name,
                // we display a bit of the BChat ID after a user's display name for added context.
                beldexAddress?.let {
                    return "$beldexAddress (...${bchatID.takeLast(8)})"
                }
                return null
            }
        }
    }

    enum class ContactContext {
        REGULAR, OPEN_GROUP
    }

    fun isValid(): Boolean {
        if (profilePictureURL != null) { return profilePictureEncryptionKey != null }
        if (profilePictureEncryptionKey != null) { return profilePictureURL != null}
        return true
    }

    override fun equals(other: Any?): Boolean {
        return this.bchatID == (other as? Contact)?.bchatID
    }

    override fun hashCode(): Int {
        return bchatID.hashCode()
    }

    override fun toString(): String {
        return nickname ?: name ?: bchatID
    }

    companion object {

        fun contextForRecipient(recipient: Recipient): ContactContext {
            return if (recipient.isOpenGroupRecipient) ContactContext.OPEN_GROUP else ContactContext.REGULAR
        }
    }
}