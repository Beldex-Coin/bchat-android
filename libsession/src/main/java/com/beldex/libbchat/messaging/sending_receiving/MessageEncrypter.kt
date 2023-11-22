package com.beldex.libbchat.messaging.sending_receiving

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.Sign
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libsignal.utilities.Hex
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.removingbdPrefixIfNeeded
import com.beldex.libbchat.messaging.sending_receiving.MessageSender.Error


object MessageEncrypter {

    private val sodium by lazy { LazySodiumAndroid(SodiumAndroid()) }

    /**
     * Encrypts `plaintext` using the Bchat protocol for `hexEncodedX25519PublicKey`.
     *
     * @param plaintext the plaintext to encrypt. Must already be padded.
     * @param recipientHexEncodedX25519PublicKey the X25519 public key to encrypt for. Could be the Bchat ID of a user, or the public key of a secret group.
     *
     * @return the encrypted message.
     */
    //Main Function
    /*internal fun encrypt(plaintext: ByteArray, recipientHexEncodedX25519PublicKey: String): ByteArray {
        Log.d("--> plainText ", plaintext.toHexString())
        val userED25519KeyPair = MessagingModuleConfiguration.shared.getUserED25519KeyPair() ?: throw Error.NoUserED25519KeyPair

        Log.d("--> userED25519KeyPair public key asHexString ",userED25519KeyPair.publicKey.asHexString)
        Log.d("--> userED25519KeyPair secret key asHexString ",userED25519KeyPair.secretKey.asHexString)

        val recipientX25519PublicKey = Hex.fromStringCondensed(recipientHexEncodedX25519PublicKey.removingbdPrefixIfNeeded())

        Log.d("--> recipientX25519PublicKey",recipientX25519PublicKey.toHexString())

        val verificationData = plaintext + userED25519KeyPair.publicKey.asBytes + recipientX25519PublicKey

        Log.d("--> userED25519KeyPair public key asBytes ",userED25519KeyPair.publicKey.asBytes.toHexString())
        Log.d("--> userED25519KeyPair secret key asBytes ",userED25519KeyPair.secretKey.asBytes.toHexString())
        Log.d("--> verificationData ", verificationData.toHexString())

        val signature = ByteArray(Sign.BYTES)

        Log.d("--> signature ", signature.toHexString())
        try {
            sodium.cryptoSignDetached(signature, verificationData, verificationData.size.toLong(), userED25519KeyPair.secretKey.asBytes)
            Log.d("--> signature ", signature.toHexString())
        } catch (exception: Exception) {
            Log.d("Beldex", "Couldn't sign message due to error: $exception.")
            throw Error.SigningFailed
        }
        val plaintextWithMetadata = plaintext + userED25519KeyPair.publicKey.asBytes + signature

        Log.d("--> plaintextWithMetadata ", plaintextWithMetadata.toHexString())

        val ciphertext = ByteArray(plaintextWithMetadata.size + Box.SEALBYTES)
        Log.d("E--> cipherText ",ciphertext.toHexString())
        try {
            sodium.cryptoBoxSeal(ciphertext, plaintextWithMetadata, plaintextWithMetadata.size.toLong(), recipientX25519PublicKey)
            Log.d("EE--> cipherText ",ciphertext.toHexString())
        } catch (exception: Exception) {
            Log.d("Beldex", "Couldn't encrypt message due to error: $exception.")
            throw Error.EncryptionFailed
        }

        val beldexAddress= "bxdCE36Hq9dZv3biveXck8FHiqbNmsLdyRDhgYPB8zhgRsGRSXnRiq9AmuuCGALMUjeV16PXuir4JHGNgtPKCWK22fzYJN3Ah"
        val beldexWalletAddress = beldexAddress.toByteArray()
        Log.d("@--> beldex wallet address bytearray size",beldexWalletAddress.size.toString())
        Log.d("@--> beldex wallet address",beldexWalletAddress.toHexString())
        Log.d("@--> beldex wallet address bytearray to string",
            String(beldexWalletAddress,StandardCharsets.UTF_8)
        )
        val cipherTextWithBeldexWalletAddress = beldexWalletAddress+ciphertext
        Log.d("@--> cipherText with wallet address",cipherTextWithBeldexWalletAddress.toHexString())
        Log.d("@--> cipherText with wallet address and UTF_8 ",String(ciphertext,StandardCharsets.UTF_8))
        Log.d("@--> cipherText ",ciphertext.toHexString())
        Log.d("@--> cipherText ",String(ciphertext,StandardCharsets.UTF_8))
        return cipherTextWithBeldexWalletAddress
    }*/

    //Sub Function
    internal fun encrypt(plaintext: ByteArray, recipientHexEncodedX25519PublicKey: String,senderBeldexAddress:String): ByteArray {
        //New Line
        val beldexAddress= senderBeldexAddress
        val beldexWalletAddress = beldexAddress.toByteArray()
        val plaintextWithBeldexAddress = beldexWalletAddress+plaintext;
        Log.d("messageEncryption beldexWalletAddress ", beldexAddress.toString())


        //-Log.d("messageEncryption plaintextWithBeldexAddress size", plaintextWithBeldexAddress.size.toString())
        //-Log.d("messageEncryption beldexWalletAddress size ", beldexWalletAddress.size.toString())
        //-Log.d("messageEncryption plainText ", plaintext.toHexString())

        //-Log.d("messageEncryption plainText ", plaintext.toHexString())
        //-Log.d("messageEncryption plainText size ", plaintext.size.toString())
        val userED25519KeyPair = MessagingModuleConfiguration.shared.getUserED25519KeyPair() ?: throw Error.NoUserED25519KeyPair

        //-Log.d("--> userED25519KeyPair public key asHexString ",userED25519KeyPair.publicKey.asHexString)
        //-Log.d("--> userED25519KeyPair secret key asHexString ",userED25519KeyPair.secretKey.asHexString)

        val recipientX25519PublicKey = Hex.fromStringCondensed(recipientHexEncodedX25519PublicKey.removingbdPrefixIfNeeded())

        //-Log.d("--> recipientX25519PublicKey",recipientX25519PublicKey.toHexString())

        val verificationData = plaintextWithBeldexAddress + userED25519KeyPair.publicKey.asBytes + recipientX25519PublicKey

        //-Log.d("--> userED25519KeyPair public key asBytes ",userED25519KeyPair.publicKey.asBytes.toHexString())
        //-Log.d("--> userED25519KeyPair secret key asBytes ",userED25519KeyPair.secretKey.asBytes.toHexString())
        //-Log.d("--> verificationData ", verificationData.toHexString())

        val signature = ByteArray(Sign.BYTES)

        //-Log.d("--> signature ", signature.toHexString())
        try {
            sodium.cryptoSignDetached(signature, verificationData, verificationData.size.toLong(), userED25519KeyPair.secretKey.asBytes)
            //-Log.d("--> signature ", signature.toHexString())
        } catch (exception: Exception) {
            Log.d("Beldex", "Couldn't sign message due to error: $exception.")
            throw Error.SigningFailed
        }
        val plaintextWithMetadata = plaintextWithBeldexAddress + userED25519KeyPair.publicKey.asBytes + signature

        //-Log.d("--> plaintextWithMetadata ", plaintextWithMetadata.toHexString())
        //-Log.d("messageEncryption plaintextWithMetadata ", plaintextWithMetadata.size.toString())
        val ciphertext = ByteArray(plaintextWithMetadata.size + Box.SEALBYTES)

        //-Log.d("E--> cipherText ",ciphertext.toHexString())
        try {
            sodium.cryptoBoxSeal(ciphertext, plaintextWithMetadata, plaintextWithMetadata.size.toLong(), recipientX25519PublicKey)
            //-Log.d("messageEncryption cipherText ",ciphertext.toHexString())
        } catch (exception: Exception) {
            Log.d("Beldex", "Couldn't encrypt message due to error: $exception.")
            throw Error.EncryptionFailed
        }

        /*  val beldexAddress= "bxdCE36Hq9dZv3biveXck8FHiqbNmsLdyRDhgYPB8zhgRsGRSXnRiq9AmuuCGALMUjeV16PXuir4JHGNgtPKCWK22fzYJN3Ah"
          val beldexWalletAddress = beldexAddress.toByteArray()
          Log.d("@--> beldex wallet address bytearray size",beldexWalletAddress.size.toString())
          Log.d("@--> beldex wallet address",beldexWalletAddress.toHexString())
          Log.d("@--> beldex wallet address bytearray to string",
              String(beldexWalletAddress,StandardCharsets.UTF_8)
          )
          val cipherTextWithBeldexWalletAddress = beldexWalletAddress+ciphertext
          Log.d("@--> cipherText with wallet address",cipherTextWithBeldexWalletAddress.toHexString())
          Log.d("@--> cipherText with wallet address and UTF_8 ",String(ciphertext,StandardCharsets.UTF_8))
          Log.d("@--> cipherText ",ciphertext.toHexString())
          Log.d("@--> cipherText ",String(ciphertext,StandardCharsets.UTF_8))*/
        //-Log.d("messageEncryption ciphertext size ", ciphertext.size.toString())
        return ciphertext
    }

}