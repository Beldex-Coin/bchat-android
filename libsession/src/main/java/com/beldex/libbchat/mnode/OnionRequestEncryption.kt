package com.beldex.libbchat.mnode

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import com.beldex.libbchat.utilities.AESGCM
import com.beldex.libbchat.utilities.AESGCM.EncryptionResult
import com.beldex.libsignal.utilities.JsonUtil
import com.beldex.libsignal.utilities.toHexString
import com.beldex.libsignal.utilities.ThreadUtils
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

object OnionRequestEncryption {

    internal fun encode(ciphertext: ByteArray, json: Map<*, *>): ByteArray {
        // The encoding of V2 onion requests looks like: | 4 bytes: size N of ciphertext | N bytes: ciphertext | json as utf8 |
        val jsonAsData = JsonUtil.toJson(json).toByteArray()
        val ciphertextSize = ciphertext.size
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(ciphertextSize)
        val ciphertextSizeAsData = ByteArray(buffer.capacity())
        // Casting here avoids an issue where this gets compiled down to incorrect byte code. See
        // https://github.com/eclipse/jetty.project/issues/3244 for more info
        (buffer as Buffer).position(0)
        buffer.get(ciphertextSizeAsData)
        return ciphertextSizeAsData + ciphertext + jsonAsData
    }

    /**
     * Encrypts `payload` for `destination` and returns the result. Use this to build the core of an onion request.
     */
    internal fun encryptPayloadForDestination(payload: Map<*, *>, destination: OnionRequestAPI.Destination): Promise<EncryptionResult, Exception> {
        val deferred = deferred<EncryptionResult, Exception>()
        ThreadUtils.queue {
            try {
                // Wrapping isn't needed for file server or social group onion requests
                when (destination) {
                    is OnionRequestAPI.Destination.Mnode -> {
                        val mnodeX25519PublicKey = destination.mnode.publicKeySet!!.x25519Key
                        //-Log.d("Beldex","Mnode x25519 public key $mnodeX25519PublicKey")
                        //-Log.d("Beldex","payload in Encryption fun $payload")
                        val payloadAsData = JsonUtil.toJson(payload).toByteArray()
                        //-Log.d("Beldex","Pay load Data $payloadAsData")
                        val plaintext = encode(payloadAsData, mapOf( "headers" to "" ))
                        //-Log.d("Beldex","Plain text $plaintext")
                        val result = AESGCM.encrypt(plaintext, mnodeX25519PublicKey)
                        //-Log.d("Beldex","AEGCM encryption result $result")
                        deferred.resolve(result)
                    }
                    is OnionRequestAPI.Destination.Server -> {
                        val plaintext = JsonUtil.toJson(payload).toByteArray()
                        //-Log.d("Beldex","Plain text $plaintext")
                        val result = AESGCM.encrypt(plaintext, destination.x25519PublicKey)
                        //-Log.d("Beldex","AEGCM encryption result $result")
                        deferred.resolve(result)
                    }
                }
            } catch (exception: Exception) {
                deferred.reject(exception)
            }
        }
        return deferred.promise
    }

    /**
     * Encrypts the previous encryption result (i.e. that of the hop after this one) for this hop. Use this to build the layers of an onion request.
     */
    internal fun encryptHop(lhs: OnionRequestAPI.Destination, rhs: OnionRequestAPI.Destination, previousEncryptionResult: EncryptionResult): Promise<EncryptionResult, Exception> {
        val deferred = deferred<EncryptionResult, Exception>()
        ThreadUtils.queue {
            try {
                val payload: MutableMap<String, Any>
                when (rhs) {
                    is OnionRequestAPI.Destination.Mnode -> {
                        payload = mutableMapOf( "destination" to rhs.mnode.publicKeySet!!.ed25519Key )
                    }
                    is OnionRequestAPI.Destination.Server -> {
                        payload = mutableMapOf(
                            "host" to rhs.host,
                            "target" to rhs.target,
                            "method" to "POST",
                            "protocol" to rhs.scheme,
                            "port" to rhs.port
                        )
                    }
                }
                payload["ephemeral_key"] = previousEncryptionResult.ephemeralPublicKey.toHexString()
                val x25519PublicKey: String
                when (lhs) {
                    is OnionRequestAPI.Destination.Mnode -> {
                        x25519PublicKey = lhs.mnode.publicKeySet!!.x25519Key
                    }
                    is OnionRequestAPI.Destination.Server -> {
                        x25519PublicKey = lhs.x25519PublicKey
                    }
                }
                val plaintext = encode(previousEncryptionResult.ciphertext, payload)
                val result = AESGCM.encrypt(plaintext, x25519PublicKey)
                deferred.resolve(result)
            } catch (exception: Exception) {
                deferred.reject(exception)
            }
        }
        return deferred.promise
    }
}
