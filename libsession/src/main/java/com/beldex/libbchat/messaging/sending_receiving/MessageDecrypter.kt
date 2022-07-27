package com.beldex.libbchat.messaging.sending_receiving

import android.content.SharedPreferences
import android.util.Log
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.beldex.libsignal.utilities.Hex
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.Sign
import com.beldex.libsignal.utilities.hexEncodedPublicKey
import com.beldex.libsignal.utilities.removing05PrefixIfNeeded
import com.beldex.libsignal.utilities.toHexString
import java.nio.charset.StandardCharsets

object MessageDecrypter {

    private val sodium by lazy { LazySodiumAndroid(SodiumAndroid()) }
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    /**
     * Decrypts `ciphertext` using the Bchat protocol and `x25519KeyPair`.
     *
     * @param ciphertext the data to decrypt.
     * @param x25519KeyPair the key pair to use for decryption. This could be the current user's key pair, or the key pair of a secret group.
     *
     * @return the padded plaintext.
     */
    //Main Function
    /*public fun decrypt(newCiphertext: ByteArray, x25519KeyPair: ECKeyPair): Pair<ByteArray, String> {
        //New Line
        val beldexWalletAddress = newCiphertext.sliceArray(0 until 97)
        val ciphertext = newCiphertext.sliceArray(97 until newCiphertext.size)

        Log.d("@--> beldexWalletaddress ",String(beldexWalletAddress, StandardCharsets.UTF_8))
        Log.d("@-->cipherText value ", ciphertext.toHexString())

        val recipientX25519PrivateKey = x25519KeyPair.privateKey.serialize()
        Log.d("D--> recipientX25519PrivateKey ",recipientX25519PrivateKey.toHexString())

        val recipientX25519PublicKey = Hex.fromStringCondensed(x25519KeyPair.hexEncodedPublicKey.removing05PrefixIfNeeded())
        Log.d("D--> recipientX25519PublicKey ",recipientX25519PublicKey.toHexString())

        val signatureSize = Sign.BYTES
        Log.d("D--> signatureSize ",signatureSize.toString())

        val ed25519PublicKeySize = Sign.PUBLICKEYBYTES
        Log.d("D--> ed25519PublicKeySize ",ed25519PublicKeySize.toString())

        // 1. ) Decrypt the message
        Log.d("D--> cipherText ",ciphertext.toHexString())
        val plaintextWithMetadata = ByteArray(ciphertext.size - Box.SEALBYTES)
        Log.d("D--> plaintextWithMetadata ",plaintextWithMetadata.toHexString())

        try {
            sodium.cryptoBoxSealOpen(plaintextWithMetadata, ciphertext, ciphertext.size.toLong(), recipientX25519PublicKey, recipientX25519PrivateKey)
            Log.d("D--> plaintextWithMetadata decrypt",plaintextWithMetadata.toHexString())
        } catch (exception: Exception) {
            Log.d("Beldex", "Couldn't decrypt message due to error: $exception.")
            throw MessageReceiver.Error.DecryptionFailed
        }
        if (plaintextWithMetadata.size <= (signatureSize + ed25519PublicKeySize)) { throw MessageReceiver.Error.DecryptionFailed }
        // 2. ) Get the message parts
        val signature = plaintextWithMetadata.sliceArray(plaintextWithMetadata.size - signatureSize until plaintextWithMetadata.size)
        val senderED25519PublicKey = plaintextWithMetadata.sliceArray(plaintextWithMetadata.size - (signatureSize + ed25519PublicKeySize) until plaintextWithMetadata.size - signatureSize)
        val plaintext = plaintextWithMetadata.sliceArray(0 until plaintextWithMetadata.size - (signatureSize + ed25519PublicKeySize))
        // 3. ) Verify the signature
        val verificationData = (plaintext + senderED25519PublicKey + recipientX25519PublicKey)
        try {
            val isValid = sodium.cryptoSignVerifyDetached(signature, verificationData, verificationData.size, senderED25519PublicKey)
            if (!isValid) { throw MessageReceiver.Error.InvalidSignature }
        } catch (exception: Exception) {
            Log.d("Beldex", "Couldn't verify message signature due to error: $exception.")
            throw MessageReceiver.Error.InvalidSignature
        }
        // 4. ) Get the sender's X25519 public key
        val senderX25519PublicKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
        sodium.convertPublicKeyEd25519ToCurve25519(senderX25519PublicKey, senderED25519PublicKey)

        return Pair(plaintext, "bd" + senderX25519PublicKey.toHexString())
    }*/

    //Sub Function
    public fun decrypt(ciphertext: ByteArray, x25519KeyPair: ECKeyPair): Triple<ByteArray, String,String> {

        //Log.d("@--> beldexWalletaddress ",String(beldexWalletAddress, StandardCharsets.UTF_8))
        //-Log.d("messageDecryption ", ciphertext.toHexString())

        val recipientX25519PrivateKey = x25519KeyPair.privateKey.serialize()
        //-Log.d("D--> recipientX25519PrivateKey ",recipientX25519PrivateKey.toHexString())

        val recipientX25519PublicKey = Hex.fromStringCondensed(x25519KeyPair.hexEncodedPublicKey.removing05PrefixIfNeeded())
        //-Log.d("D--> recipientX25519PublicKey ",recipientX25519PublicKey.toHexString())

        val signatureSize = Sign.BYTES
        //-Log.d("D--> signatureSize ",signatureSize.toString())

        val ed25519PublicKeySize = Sign.PUBLICKEYBYTES
        //-Log.d("D--> ed25519PublicKeySize ",ed25519PublicKeySize.toString())

        // 1. ) Decrypt the message
        //-Log.d("D--> cipherText ",ciphertext.toHexString())
        val plaintextWithMetadata = ByteArray(ciphertext.size - Box.SEALBYTES)
        //-Log.d("D--> plaintextWithMetadata ",plaintextWithMetadata.toHexString())

        try {
            sodium.cryptoBoxSealOpen(plaintextWithMetadata, ciphertext, ciphertext.size.toLong(), recipientX25519PublicKey, recipientX25519PrivateKey)
            //-Log.d("D--> plaintextWithMetadata decrypt",plaintextWithMetadata.toHexString())
        } catch (exception: Exception) {
            Log.d("Beldex", "Couldn't decrypt message due to error: $exception.")
            throw MessageReceiver.Error.DecryptionFailed
        }
        if (plaintextWithMetadata.size <= (signatureSize + ed25519PublicKeySize)) { throw MessageReceiver.Error.DecryptionFailed }
        // 2. ) Get the message parts
        val signature = plaintextWithMetadata.sliceArray(plaintextWithMetadata.size - signatureSize until plaintextWithMetadata.size)
        val senderED25519PublicKey = plaintextWithMetadata.sliceArray(plaintextWithMetadata.size - (signatureSize + ed25519PublicKeySize) until plaintextWithMetadata.size - signatureSize)
        val plaintext = plaintextWithMetadata.sliceArray(0 until plaintextWithMetadata.size - (signatureSize + ed25519PublicKeySize))
        // 3. ) Verify the signature
        val verificationData = (plaintext + senderED25519PublicKey + recipientX25519PublicKey)
        try {
            val isValid = sodium.cryptoSignVerifyDetached(signature, verificationData, verificationData.size, senderED25519PublicKey)
            if (!isValid) { throw MessageReceiver.Error.InvalidSignature }
        } catch (exception: Exception) {
            Log.d("Beldex", "Couldn't verify message signature due to error: $exception.")
            throw MessageReceiver.Error.InvalidSignature
        }
        // 4. ) Get the sender's X25519 public key
        val senderX25519PublicKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
        sodium.convertPublicKeyEd25519ToCurve25519(senderX25519PublicKey, senderED25519PublicKey)

        //New Line
        /* val beldexWalletAddress = plaintext.sliceArray(0 until 97)
         val newPlainText = plaintext.sliceArray(97 until plaintext.size)
         val receiveradd = String(beldexWalletAddress, StandardCharsets.UTF_8)

         Log.d("@--> beldexWalletaddress ",String(beldexWalletAddress, StandardCharsets.UTF_8))
         Log.d("@-->cipherText value ", newPlainText.toHexString())

         return Triple(newPlainText, "bd" + senderX25519PublicKey.toHexString(),receiveradd)*/

        var beldexWalletAddress = plaintext.sliceArray(0 until 1)
        var receiveradd = String(beldexWalletAddress, StandardCharsets.UTF_8)
        var newPlainText = plaintext.sliceArray(97 until plaintext.size)

        //Log.d("messageDecryption beldexWalletAddress ", receiveradd.toString())
        //-Log.d("@--> beldexWalletaddress before if ",String(beldexWalletAddress, StandardCharsets.UTF_8))
        if(receiveradd != "b"){
            beldexWalletAddress = plaintext.sliceArray(0 until 95)
            newPlainText = plaintext.sliceArray(95 until plaintext.size)
            receiveradd = String(beldexWalletAddress, StandardCharsets.UTF_8)
            //-Log.d("@--> beldexWalletaddress if ",String(beldexWalletAddress, StandardCharsets.UTF_8))
            //-Log.d("@-->cipherText value ", newPlainText.toHexString())
            //-Log.d("Beldex","MessageDecryption senhderx255Pub ${senderX25519PublicKey.toHexString()}")
            return Triple(newPlainText, "bd" + senderX25519PublicKey.toHexString(),receiveradd)
        }
         else{
            beldexWalletAddress = plaintext.sliceArray(0 until 97)
            newPlainText = plaintext.sliceArray( 97 until plaintext.size)
            receiveradd = String(beldexWalletAddress, StandardCharsets.UTF_8)
            //-Log.d("@--> beldexWalletaddress else ",String(beldexWalletAddress, StandardCharsets.UTF_8))
            //-Log.d("@-->cipherText value ", newPlainText.toHexString())
            //-Log.d("Beldex","MessageDecryption senhderx255Pub ${senderX25519PublicKey.toHexString()}")
            return Triple(newPlainText, "bd" + senderX25519PublicKey.toHexString(),receiveradd)
        }
    }

}