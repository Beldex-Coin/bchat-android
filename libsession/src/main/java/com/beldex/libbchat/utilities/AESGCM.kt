package com.beldex.libbchat.utilities

import androidx.annotation.WorkerThread
import com.beldex.libsignal.utilities.ByteUtil
import com.beldex.libsignal.utilities.Hex
import org.whispersystems.curve25519.Curve25519
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@WorkerThread
internal object AESGCM {
    internal val gcmTagSize = 128
    internal val ivSize = 12

    internal data class EncryptionResult(
        internal val ciphertext: ByteArray,
        internal val symmetricKey: ByteArray,
        internal val ephemeralPublicKey: ByteArray
    )

    /**
     * Sync. Don't call from the main thread.
     */
    internal fun decrypt(
        ivAndCiphertext: ByteArray,
        offset: Int = 0,
        len: Int = ivAndCiphertext.size,
        symmetricKey: ByteArray
    ): ByteArray {
        val iv = ivAndCiphertext.sliceArray(offset until (offset + ivSize))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(symmetricKey, "AES"), GCMParameterSpec(gcmTagSize, iv))
        return cipher.doFinal(ivAndCiphertext, offset + ivSize, len - ivSize)
    }

    /**
     * Sync. Don't call from the main thread.
     */
    internal fun generateSymmetricKey(x25519PublicKey: ByteArray, x25519PrivateKey: ByteArray): ByteArray {
        val ephemeralSharedSecret = Curve25519.getInstance(Curve25519.BEST).calculateAgreement(x25519PublicKey, x25519PrivateKey)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec("BELDEX".toByteArray(), "HmacSHA256"))
        return mac.doFinal(ephemeralSharedSecret)
    }

    /**
     * Sync. Don't call from the main thread.
     */
    internal fun encrypt(plaintext: ByteArray, symmetricKey: ByteArray): ByteArray {
        val iv = Util.getSecretBytes(ivSize)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(symmetricKey, "AES"), GCMParameterSpec(gcmTagSize, iv))
        return ByteUtil.combine(iv, cipher.doFinal(plaintext))
    }

    /**
     * Sync. Don't call from the main thread.
     */
    internal fun encrypt(plaintext: ByteArray, hexEncodedX25519PublicKey: String): EncryptionResult {
        val x25519PublicKey = Hex.fromStringCondensed(hexEncodedX25519PublicKey)
        val ephemeralKeyPair = Curve25519.getInstance(Curve25519.BEST).generateKeyPair()
        val symmetricKey = generateSymmetricKey(x25519PublicKey, ephemeralKeyPair.privateKey)
        val ciphertext = encrypt(plaintext, symmetricKey)
        return EncryptionResult(ciphertext, symmetricKey, ephemeralKeyPair.publicKey)
    }

}