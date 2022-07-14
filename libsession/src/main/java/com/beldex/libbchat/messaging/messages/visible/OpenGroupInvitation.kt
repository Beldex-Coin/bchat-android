package com.beldex.libbchat.messaging.messages.visible

import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.Log

class OpenGroupInvitation() {
    var url: String? = null
    var name: String? = null

    fun isValid(): Boolean {
        return (url != null && name != null)
    }

    companion object {
        const val TAG = "OpenGroupInvitation"

        fun fromProto(proto: SignalServiceProtos.DataMessage.OpenGroupInvitation): OpenGroupInvitation {
            return OpenGroupInvitation(proto.url, proto.name)
        }
    }

    constructor(url: String?, serverName: String?): this() {
        this.url = url
        this.name = serverName
    }

    fun toProto(): SignalServiceProtos.DataMessage.OpenGroupInvitation? {
        val openGroupInvitationProto = SignalServiceProtos.DataMessage.OpenGroupInvitation.newBuilder()
        openGroupInvitationProto.url = url
        openGroupInvitationProto.name = name
        return try {
            openGroupInvitationProto.build()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't construct social group invitation proto from: $this.")
            null
        }
    }
}