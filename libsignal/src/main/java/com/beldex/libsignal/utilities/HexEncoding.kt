package com.beldex.libsignal.utilities

import com.beldex.libsignal.crypto.IdentityKeyPair
import com.beldex.libsignal.crypto.ecc.ECKeyPair

fun ByteArray.toHexString(): String {
    return joinToString("") { String.format("%02x", it) }
}

val IdentityKeyPair.hexEncodedPublicKey: String
    get() = publicKey.serialize().toHexString()

val IdentityKeyPair.hexEncodedPrivateKey: String
    get() = privateKey.serialize().toHexString()

val ECKeyPair.hexEncodedPublicKey: String
    get() = publicKey.serialize().toHexString()

val ECKeyPair.hexEncodedPrivateKey: String
    get() = privateKey.serialize().toHexString()
