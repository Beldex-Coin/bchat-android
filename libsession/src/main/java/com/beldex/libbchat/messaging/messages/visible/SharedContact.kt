package com.beldex.libbchat.messaging.messages.visible

import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.Log

class SharedContact() {

    var threadId: String? = null
    var address: String? = null
    var name: String? = null

    fun isValid(): Boolean {
        return (threadId != null && address != null && name != null)
    }

    companion object {
        const val TAG = "SharedContact"

        fun fromProto(proto: SignalServiceProtos.DataMessage.SharedContact): SharedContact {
            return SharedContact(proto.threadId, proto.address, proto.name)
        }
    }

    constructor(threadId: String?, address: String?, name: String?): this() {
        this.threadId = threadId
        this.address = address
        this.name = name
    }

    fun toProto(): SignalServiceProtos.DataMessage.SharedContact? {
        val contactProto = SignalServiceProtos.DataMessage.SharedContact.newBuilder()
        contactProto.threadId = threadId.toString()
        contactProto.address = address
        contactProto.name = name
        return try {
            contactProto.build()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't construct contact proto from: $this.")
            null
        }
    }

}