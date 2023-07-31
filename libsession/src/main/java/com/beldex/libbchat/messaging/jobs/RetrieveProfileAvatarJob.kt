package com.beldex.libbchat.messaging.jobs

import com.beldex.libbchat.avatars.AvatarHelper
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.utilities.Data
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.DownloadUtilities
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setProfileAvatarId
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setProfilePictureURL
import com.beldex.libbchat.utilities.Util
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.streams.ProfileCipherInputStream
import com.beldex.libsignal.utilities.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom

class RetrieveProfileAvatarJob(private val profileAvatar: String?, private val recipientAddress: Address): Job {
    override var delegate: JobDelegate? = null
    override var id: String? = null
    override var failureCount: Int = 0
    override val maxFailureCount: Int = 0

    companion object {
        val TAG = RetrieveProfileAvatarJob::class.simpleName
        val KEY: String = "RetrieveProfileAvatarJob"

        // Keys used for database storage
        private const val PROFILE_AVATAR_KEY = "profileAvatar"
        private const val RECEIPIENT_ADDRESS_KEY = "recipient"
    }

    override fun execute() {
        val context = MessagingModuleConfiguration.shared.context
        val storage = MessagingModuleConfiguration.shared.storage
        val recipient = Recipient.from(context, recipientAddress, true)
        val profileKey = recipient.resolve().profileKey

        if (profileKey == null || (profileKey.size != 32 && profileKey.size != 16)) {
            Log.w(TAG, "Recipient profile key is gone!")
            return
        }

        if (AvatarHelper.avatarFileExists(context, recipient.resolve().address) && Util.equals(
                profileAvatar,
                recipient.resolve().profileAvatar
            )
        ) {
            Log.w(TAG, "Already retrieved profile avatar: $profileAvatar")
            return
        }

        if (profileAvatar.isNullOrEmpty()) {
            Log.w(TAG, "Removing profile avatar for: " + recipient.address.serialize())
            if (recipient.isLocalNumber) {
                setProfileAvatarId(context, SecureRandom().nextInt())
                setProfilePictureURL(context, null)
            }
            AvatarHelper.delete(context, recipient.address)
            storage.setProfileAvatar(recipient, null)
            return
        }

        val downloadDestination = File.createTempFile("avatar", ".jpg", context.cacheDir)

        try {
            DownloadUtilities.downloadFile(downloadDestination, profileAvatar)
            val avatarStream: InputStream =
                ProfileCipherInputStream(FileInputStream(downloadDestination), profileKey)
            val decryptDestination = File.createTempFile("avatar", ".jpg", context.cacheDir)
            Util.copy(avatarStream, FileOutputStream(decryptDestination))
            decryptDestination.renameTo(AvatarHelper.getAvatarFile(context, recipient.address))
        } finally {
            downloadDestination.delete()
        }

        if (recipient.isLocalNumber) {
            setProfileAvatarId(context, SecureRandom().nextInt())
            setProfilePictureURL(context, profileAvatar)
        }
        storage.setProfileAvatar(recipient, profileAvatar)
    }

    override fun serialize(): Data {
        return Data.Builder()
            .putString(PROFILE_AVATAR_KEY, profileAvatar)
            .putString(RECEIPIENT_ADDRESS_KEY, recipientAddress.serialize())
            .build()
    }

    override fun getFactoryKey(): String {
        return KEY
    }

    class Factory: Job.Factory<RetrieveProfileAvatarJob> {
        override fun create(data: Data): RetrieveProfileAvatarJob {
            val profileAvatar = if (data.hasString(PROFILE_AVATAR_KEY)) { data.getString(PROFILE_AVATAR_KEY) } else { null }
            val recipientAddress = Address.fromSerialized(data.getString(RECEIPIENT_ADDRESS_KEY))
            return RetrieveProfileAvatarJob(profileAvatar, recipientAddress)
        }
    }
}