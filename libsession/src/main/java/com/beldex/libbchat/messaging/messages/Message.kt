package com.beldex.libbchat.messaging.messages

import android.util.Log
import com.google.protobuf.ByteString
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libsignal.protos.SignalServiceProtos

abstract class Message {
    var id: Long? = null
    var threadID: Long? = null
    var sentTimestamp: Long? = null
    var receivedTimestamp: Long? = null
    var recipient: String? = null
    var sender: String? = null
    var groupPublicKey: String? = null
    var openGroupServerMessageID: Long? = null
    var serverHash: String? = null

    open val ttl: Long = 14 * 24 * 60 * 60 * 1000
    open val isSelfSendValid: Boolean = false

    open fun isValid(): Boolean {
        val sentTimestamp = sentTimestamp
        Log.d("DataMessage message body sendTimstamp->","${sentTimestamp != null}")
        if (sentTimestamp != null && sentTimestamp <= 0) {
            Log.d("DataMessage message body sendTimstamp <=0->","false")
            return false }
        val receivedTimestamp = receivedTimestamp
        Log.d("DataMessage message body receivedTimestamp->","${receivedTimestamp != null}")
        if (receivedTimestamp != null && receivedTimestamp <= 0) {
            Log.d("DataMessage message body receivedTimestamp <=0->","false")
            return false }
        if(sender != null && recipient != null) {
            Log.d(
                "DataMessage message body sender and receiver->",
                "${sender != null} and ${recipient != null}"
            )
        }
        return sender != null && recipient != null
    }

    abstract fun toProto(): SignalServiceProtos.Content?

    fun setGroupContext(dataMessage: SignalServiceProtos.DataMessage.Builder) {
        val groupProto = SignalServiceProtos.GroupContext.newBuilder()
        val groupID = GroupUtil.doubleEncodeGroupID(recipient!!)
        groupProto.id = ByteString.copyFrom(GroupUtil.getDecodedGroupIDAsData(groupID))
        groupProto.type = SignalServiceProtos.GroupContext.Type.DELIVER
        dataMessage.group = groupProto.build()
    }

}