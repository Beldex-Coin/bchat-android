package com.beldex.libbchat.mnode

import com.beldex.libsignal.utilities.removing05PrefixIfNeeded

data class MnodeMessage(
    /**
     * The hex encoded public key of the recipient.
     * bchat id validation -- recipient address")
     */
    val recipient: String,
    /**
     * The base64 encoded content of the message.
     */
    val data: String,
    /**
     * The time to live for the message in milliseconds.
     * max time = 2 weeks
     * min time = 10 seconds
     */
    val ttl: Long,
    /**
     * When the proof of work was calculated.
     *
     * **Note:** Expressed as milliseconds since 00:00:00 UTC on 1 January 1970.
     */
    val timestamp: Long
) {

    internal fun toJSON(): Map<String, String> {
        return mapOf(
            "pubKey" to if (MnodeAPI.useTestnet) recipient.removing05PrefixIfNeeded() else recipient,
            "data" to data,
            "ttl" to ttl.toString(),
            "timestamp" to timestamp.toString(),
            "nonce" to ""
        )
    }
}
