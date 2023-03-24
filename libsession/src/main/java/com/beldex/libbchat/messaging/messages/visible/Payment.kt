package com.beldex.libbchat.messaging.messages.visible

import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.Log

class Payment() {
    var amount: String? = null
    var txnId: String? = null

    fun isValid(): Boolean {
        return (amount != null && txnId != null)
    }

    companion object {
        const val TAG = "Payment"

        fun fromProto(proto: SignalServiceProtos.DataMessage.Payment): Payment {
            return Payment(proto.amount, proto.txnId)
        }
    }

    constructor(amount: String?, txnId: String?): this() {
        this.amount = amount
        this.txnId = txnId
    }

    fun toProto(): SignalServiceProtos.DataMessage.Payment? {
        val paymentProto = SignalServiceProtos.DataMessage.Payment.newBuilder()
        paymentProto.amount = amount
        paymentProto.txnId = txnId
        return try {
            paymentProto.build()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't construct payment proto from: $this.")
            null
        }
    }
}