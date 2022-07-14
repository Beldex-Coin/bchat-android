package com.beldex.libbchat.messaging.utilities

import com.google.protobuf.ByteString
import com.beldex.libsignal.protos.SignalServiceProtos.Envelope
import com.beldex.libsignal.protos.WebSocketProtos.WebSocketMessage
import com.beldex.libsignal.protos.WebSocketProtos.WebSocketRequestMessage
import com.beldex.libsignal.utilities.Log
import java.security.SecureRandom

object MessageWrapper {

    // region Types
    sealed class Error(val description: String) : Exception(description) {
        object FailedToWrapData : Error("Failed to wrap data.")
        object FailedToWrapMessageInEnvelope : Error("Failed to wrap message in envelope.")
        object FailedToWrapEnvelopeInWebSocketMessage : Error("Failed to wrap envelope in web socket message.")
        object FailedToUnwrapData : Error("Failed to unwrap data.")
    }
    // endregion

    // region Wrapping
    /**
     * Wraps `message` in a `SignalServiceProtos.Envelope` and then a `WebSocketProtos.WebSocketMessage` to match the desktop application.
     */
    fun wrap(type: Envelope.Type, timestamp: Long, senderPublicKey: String, content: ByteArray): ByteArray {
        try {
            val envelope = createEnvelope(type, timestamp, senderPublicKey, content)
            Log.d("Beldex","bchat id validation -- sender public key ")
            val webSocketMessage = createWebSocketMessage(envelope)
            return webSocketMessage.toByteArray()
        } catch (e: Exception) {
            throw if (e is Error) { e } else { Error.FailedToWrapData }
        }
    }

    private fun createEnvelope(type: Envelope.Type, timestamp: Long, senderPublicKey: String, content: ByteArray): Envelope {
        try {
            val builder = Envelope.newBuilder()
            builder.type = type
            builder.timestamp = timestamp
            builder.source = senderPublicKey
            builder.sourceDevice = 1
            builder.content = ByteString.copyFrom(content)
            return builder.build()
        } catch (e: Exception) {
            Log.d("Beldex", "Failed to wrap message in envelope: ${e.message}.")
            throw Error.FailedToWrapMessageInEnvelope
        }
    }

    private fun createWebSocketMessage(envelope: Envelope): WebSocketMessage {
        try {
            val requestBuilder = WebSocketRequestMessage.newBuilder()
            requestBuilder.verb = "PUT"
            requestBuilder.path = "/api/v1/message"
            requestBuilder.id = SecureRandom.getInstance("SHA1PRNG").nextLong()
            requestBuilder.body = envelope.toByteString()
            val messageBuilder = WebSocketMessage.newBuilder()
            messageBuilder.request = requestBuilder.build()
            messageBuilder.type = WebSocketMessage.Type.REQUEST
            return messageBuilder.build()
        } catch (e: Exception) {
            Log.d("Beldex", "Failed to wrap envelope in web socket message: ${e.message}.")
            throw Error.FailedToWrapEnvelopeInWebSocketMessage
        }
    }
    // endregion

    // region Unwrapping
    /**
     * `data` shouldn't be base 64 encoded.
     */
    fun unwrap(data: ByteArray): Envelope {
        try {
            val webSocketMessage = WebSocketMessage.parseFrom(data)
            val envelopeAsData = webSocketMessage.request.body
            return Envelope.parseFrom(envelopeAsData);
        } catch (e: Exception) {
            Log.d("Beldex", "Failed to unwrap data: ${e.message}.")
            throw Error.FailedToUnwrapData
        }
    }
    // endregion
}
