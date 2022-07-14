package com.beldex.libbchat.messaging.messages.control

import android.util.Log
import com.beldex.libsignal.protos.SignalServiceProtos


class MessageRequestResponse(val isApproved: Boolean) : ControlMessage() {
    /*Hales63*/

    override val isSelfSendValid: Boolean = true

    override fun toProto(): SignalServiceProtos.Content? {
        val messageRequestResponseProto = SignalServiceProtos.MessageRequestResponse.newBuilder()
            .setIsApproved(isApproved)
        return try {
            SignalServiceProtos.Content.newBuilder()
                .setMessageRequestResponse(messageRequestResponseProto.build())
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't construct message request response proto from: $this")
            null
        }
    }

    companion object {
        const val TAG = "MessageRequestResponse"

        fun fromProto(proto: SignalServiceProtos.Content): MessageRequestResponse? {
            val messageRequestResponseProto = if (proto.hasMessageRequestResponse()) proto.messageRequestResponse else return null
            val isApproved = messageRequestResponseProto.isApproved
            return MessageRequestResponse(isApproved)
        }
    }

}