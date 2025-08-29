package com.beldex.libbchat.messaging.open_groups

import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libsignal.crypto.PushTransportDetails
import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.Base64
import com.beldex.libsignal.utilities.Base64.decode
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.toHexString
import org.whispersystems.curve25519.Curve25519

data class OpenGroupMessageV2(
    val serverID: Long? = null,
    val sender: String?,
    val sentTimestamp: Long,
    /**
     * The serialized protobuf in base64 encoding.
     */
    val base64EncodedData: String,
    /**
     * When sending a message, the sender signs the serialized protobuf with their private key so that
     * a receiving user can verify that the message wasn't tampered with.
     */
    val base64EncodedSignature: String? = null,
    val reactions: Map<String, OpenGroupAPIV2.Reaction>? = null
) {

    companion object {
        private val curve = Curve25519.getInstance(Curve25519.BEST)

        fun fromJSON(json: Map<String, Any>): OpenGroupMessageV2? {
            val base64EncodedData = json["data"] as? String ?: return null
            val sentTimestamp = json["timestamp"] as? Long ?: return null
            val serverID = json["server_id"] as? Int
            val sender = json["public_key"] as? String
            val base64EncodedSignature = json["signature"] as? String
            return OpenGroupMessageV2(
                serverID = serverID?.toLong(),
                sender = sender,
                sentTimestamp = sentTimestamp,
                base64EncodedData = base64EncodedData,
                base64EncodedSignature = base64EncodedSignature
            )
        }

    }

    fun sign(): OpenGroupMessageV2? {
        if (base64EncodedData.isEmpty()) return null
        val (publicKey, privateKey) = MessagingModuleConfiguration.shared.storage.getUserX25519KeyPair().let { it.publicKey to it.privateKey }
        if (sender != publicKey.serialize().toHexString()) return null
        val signature = try {
            curve.calculateSignature(privateKey.serialize(), decode(base64EncodedData))
        } catch (e: Exception) {
            Log.w("Beldex", "Couldn't sign social group message.", e)
            return null
        }
        return copy(base64EncodedSignature = Base64.encodeBytes(signature))
    }

    fun toJSON(): Map<String, Any> {
        val json = mutableMapOf( "data" to base64EncodedData, "timestamp" to sentTimestamp )
        serverID?.let { json["server_id"] = it }
        sender?.let { json["public_key"] = it }
        base64EncodedSignature?.let { json["signature"] = it }
        return json
    }

    fun toProto(): SignalServiceProtos.Content {
        val data = decode(base64EncodedData).let(PushTransportDetails::getStrippedPaddingMessageBody)
        return SignalServiceProtos.Content.parseFrom(data)
    }
}