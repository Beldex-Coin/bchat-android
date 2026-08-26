package com.beldex.libbchat.messaging.sending_receiving

import com.beldex.libbchat.database.StorageProtocol
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.DiffieHellman
import com.goterl.lazysodium.interfaces.SecretBox
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters
import java.security.SecureRandom

/**
 * Version 0x02 hybrid X25519 + ML-KEM-768 envelope. This intentionally uses
 * Bouncy Castle's lightweight API and never installs a global JCE provider.
 */
internal object PostQuantumCrypto {
    const val VERSION: Byte = 0x02
    private const val X25519_BYTES = 32
    private const val ML_KEM_768_PUBLIC_KEY_BYTES = 1184
    private const val ML_KEM_768_CIPHERTEXT_BYTES = 1088
    private const val XSALSA20_NONCE_BYTES = 24
    private const val DOMAIN = "bchat-pq-v1"

    private val sodium by lazy { LazySodiumAndroid(SodiumAndroid()) }
    private val secureRandom = SecureRandom()

    data class LocalKeyPair(val publicKey: ByteArray, val privateKey: ByteArray)

    fun localKeyPair(storage: StorageProtocol): LocalKeyPair {
        val privateKey = storage.getPostQuantumPrivateKey()
        if (privateKey != null) {
            val key = MLKEMPrivateKeyParameters(MLKEMParameters.ml_kem_768, privateKey)
            return LocalKeyPair(key.publicKey, privateKey)
        }
        val generator = MLKEMKeyPairGenerator()
        generator.init(MLKEMKeyGenerationParameters(secureRandom, MLKEMParameters.ml_kem_768))
        val generated: AsymmetricCipherKeyPair = generator.generateKeyPair()
        val generatedPrivate = generated.private as MLKEMPrivateKeyParameters
        val encoded = generatedPrivate.encoded
        storage.setPostQuantumPrivateKey(encoded)
        return LocalKeyPair(generatedPrivate.publicKey, encoded)
    }

    fun encrypt(plaintext: ByteArray, recipientX25519PublicKey: ByteArray, recipientMlKemPublicKey: ByteArray): ByteArray {
        require(recipientX25519PublicKey.size == X25519_BYTES)
        require(recipientMlKemPublicKey.size == ML_KEM_768_PUBLIC_KEY_BYTES)

        val ephemeralPrivate = sodium.randomBytesBuf(X25519_BYTES)
        val ephemeralPublic = ByteArray(X25519_BYTES)
        val x25519Secret = ByteArray(X25519_BYTES)
        check(sodium.cryptoScalarMultBase(ephemeralPublic, ephemeralPrivate))
        check(sodium.cryptoScalarMult(x25519Secret, ephemeralPrivate, recipientX25519PublicKey))

        val encapsulated = MLKEMGenerator(secureRandom).generateEncapsulated(
            MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, recipientMlKemPublicKey)
        )
        val kemCiphertext = encapsulated.encapsulation
        val key = deriveKey(x25519Secret, encapsulated.secret)
        val nonce = ephemeralPublic.copyOfRange(0, XSALSA20_NONCE_BYTES)
        val payload = ByteArray(plaintext.size + SecretBox.MACBYTES)
        try {
            check(sodium.cryptoSecretBoxEasy(payload, plaintext, plaintext.size.toLong(), nonce, key))
            return byteArrayOf(VERSION) + ephemeralPublic + kemCiphertext + payload
        } finally {
            ephemeralPrivate.fill(0)
            x25519Secret.fill(0)
            key.fill(0)
            encapsulated.secret.fill(0)
        }
    }

    fun decrypt(ciphertext: ByteArray, recipientX25519PrivateKey: ByteArray, recipientMlKemPrivateKey: ByteArray): ByteArray {
        val headerSize = 1 + X25519_BYTES + ML_KEM_768_CIPHERTEXT_BYTES
        require(ciphertext.size > headerSize + SecretBox.MACBYTES) { "PQ ciphertext is too short" }
        require(ciphertext[0] == VERSION) { "Unsupported PQ version" }
        val ephemeralPublic = ciphertext.copyOfRange(1, 1 + X25519_BYTES)
        val kemCiphertext = ciphertext.copyOfRange(1 + X25519_BYTES, headerSize)
        val payload = ciphertext.copyOfRange(headerSize, ciphertext.size)
        val x25519Secret = ByteArray(X25519_BYTES)
        check(sodium.cryptoScalarMult(x25519Secret, recipientX25519PrivateKey, ephemeralPublic))
        val kemSecret = MLKEMExtractor(
            MLKEMPrivateKeyParameters(MLKEMParameters.ml_kem_768, recipientMlKemPrivateKey)
        ).extractSecret(kemCiphertext)
        val key = deriveKey(x25519Secret, kemSecret)
        val plaintext = ByteArray(payload.size - SecretBox.MACBYTES)
        try {
            // ML-KEM implicit rejection returns a pseudorandom secret. Authentication is the integrity gate.
            check(sodium.cryptoSecretBoxOpenEasy(plaintext, payload, payload.size.toLong(), ephemeralPublic.copyOfRange(0, XSALSA20_NONCE_BYTES), key))
            return plaintext
        } finally {
            x25519Secret.fill(0)
            kemSecret.fill(0)
            key.fill(0)
        }
    }

    private fun deriveKey(x25519Secret: ByteArray, kemSecret: ByteArray): ByteArray {
        val key = ByteArray(SecretBox.KEYBYTES)
        HKDFBytesGenerator(org.bouncycastle.crypto.digests.SHA256Digest()).apply {
            init(HKDFParameters(x25519Secret + kemSecret, null, DOMAIN.toByteArray(Charsets.US_ASCII)))
            generateBytes(key, 0, key.size)
        }
        return key
    }
}
