package com.beldex.libbchat.messaging.messages.visible

import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.Log
import com.google.protobuf.ByteString

class Profile() {
    var displayName: String? = null
    var profileKey: ByteArray? = null
    var profilePictureURL: String? = null

    companion object {
        const val TAG = "Profile"

        fun fromProto(proto: SignalServiceProtos.DataMessage): Profile? {
            val profileProto = proto.profile ?: return null
            val displayName = profileProto.displayName ?: return null
            val profileKey = proto.profileKey
            val profilePictureURL = profileProto.profilePicture
            if (profileKey != null && profilePictureURL != null) {
                return Profile(displayName, profileKey.toByteArray(), profilePictureURL)
            } else {
                return Profile(displayName)
            }
        }
    }

    internal constructor(displayName: String, profileKey: ByteArray? = null, profilePictureURL: String? = null) : this() {
        this.displayName = displayName
        this.profileKey = profileKey
        this.profilePictureURL = profilePictureURL
    }

    fun toProto(): SignalServiceProtos.DataMessage? {
        val displayName = displayName
        if (displayName == null) {
            Log.w(TAG, "Couldn't construct profile proto from: $this")
            return null
        }
        val dataMessageProto = SignalServiceProtos.DataMessage.newBuilder()
        val profileProto = SignalServiceProtos.DataMessage.BeldexProfile.newBuilder()
        profileProto.displayName = displayName
        profileKey?.let { dataMessageProto.profileKey = ByteString.copyFrom(it) }
        profilePictureURL?.let { profileProto.profilePicture = it }
        // Build
        try {
            dataMessageProto.profile = profileProto.build()
            return dataMessageProto.build()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't construct profile proto from: $this")
            return null
        }
    }
}