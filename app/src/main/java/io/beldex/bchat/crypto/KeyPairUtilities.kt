package io.beldex.bchat.crypto

import android.content.Context
import com.beldex.libsignal.crypto.ecc.DjbECPrivateKey
import com.beldex.libsignal.crypto.ecc.DjbECPublicKey
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.beldex.libsignal.utilities.Base64
import com.beldex.libsignal.utilities.Hex
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair

object KeyPairUtilities {

    private val sodium by lazy { LazySodiumAndroid(SodiumAndroid()) }

    fun generate(): KeyPairGenerationResult {
        val seed = sodium.randomBytesBuf(32)
        return try {
            generate(seed)
        } catch (exception: Exception) {
            generate()
        }
    }

    fun generate(seed: ByteArray): KeyPairGenerationResult {
        val padding = ByteArray(16) { 0 }
        val ed25519KeyPair = sodium.cryptoSignSeedKeypair(seed)
        val sodiumX25519KeyPair = sodium.convertKeyPairEd25519ToCurve25519(ed25519KeyPair)
        val x25519KeyPair = ECKeyPair(
            DjbECPublicKey(sodiumX25519KeyPair.publicKey.asBytes),
            DjbECPrivateKey(sodiumX25519KeyPair.secretKey.asBytes)
        )
        return KeyPairGenerationResult(seed, ed25519KeyPair, x25519KeyPair)
    }

    /*fun generate(seed: ByteArray): KeyPairGenerationResult {

        val publicKey = BigInteger(
            "026e853ca93c3a6fdf00b6a08ec0e0184dcae202af0b613e139100653b01c2c6",
            16
        ).toByteArray()
        val secretKey = BigInteger(
            "5658cfa8657c1d42494a07fb2951e65b40657f89eb25559037699dcb62929f05026e853ca93c3a6fdf00b6a08ec0e0184dcae202af0b613e139100653b01c2c6",
            32
        ).toByteArray()

        val curvePkBytes1 = ByteArray(32)
        val curveSkBytes1 = ByteArray(32)

        Log.d("--> seed", Hex.toStringCondensed(seed))
        val padding = ByteArray(16) { 0 }
        Log.d("--> padding", Hex.toStringCondensed(padding))
        val ed25519KeyPairpublic =
            sodium.convertPublicKeyEd25519ToCurve25519(curvePkBytes1, publicKey)
        val ed25519KeyPairsecret =
            sodium.convertSecretKeyEd25519ToCurve25519(curveSkBytes1, secretKey)

        Log.d("Beldex", "keys publicKey public key ${publicKey.toHexString()}")
        Log.d("Beldex", "keys secret key ${secretKey.toHexString()}")

        Log.d("Beldex", "keys curvePkBytes public key ${curvePkBytes1.toHexString()}")
        Log.d("Beldex", "keys curveSkBytes secret key ${curveSkBytes1.toHexString()}")
        val ed25519KeyPair = sodium.cryptoSignSeedKeypair(seed + padding)
        val sodiumX25519KeyPair = sodium.convertKeyPairEd25519ToCurve25519(ed25519KeyPair)
        Log.d("Beldex", "keys sodiumX25519KeyPair public key $sodiumX25519KeyPair.viewpublicKey.key.toString()");
        Log.d("Beldex","keys sodiumX25519KeyPair secret key  $sodiumX25519KeyPair.viewsecretKey.key.toString()")
        val x25519KeyPair = ECKeyPair(DjbECPublicKey(curvePkBytes1), DjbECPrivateKey(curveSkBytes1))
        Log.d("-->keys x25519KeyPair public key ",x25519KeyPair.publicKey.toString())
        Log.d("-->keys x25519KeyPair secret key ",x25519KeyPair.privateKey.toString())
//            return KeyPairGenerationResult(seed, ed25519KeyPair, x25519KeyPair)
//        }
//

//        Log.d("Beldex", "sodiumX25519KeyPair public key $sodiumX25519KeyPair.viewpublicKey.key.toString()");
//        Log.d("Beldex","sodiumX25519KeyPair secret key  $sodiumX25519KeyPair.viewsecretKey.key.toString()")
//        val x25519KeyPair = ECKeyPair(DjbECPublicKey(sodiumX25519KeyPair.viewpublicKey.key), DjbECPrivateKey(sodiumX25519KeyPair.viewsecretKey.key))
//        Log.d("--> x25519KeyPair public key ",x25519KeyPair.publicKey.toString())
//        Log.d("--> x25519KeyPair secret key ",x25519KeyPair.privateKey.toString())
        return KeyPairGenerationResult(seed, ed25519KeyPair, x25519KeyPair)

    }*/

    fun store(context: Context, seed: ByteArray, ed25519KeyPair: KeyPair, x25519KeyPair: ECKeyPair) {
        IdentityKeyUtil.save(context, IdentityKeyUtil.BELDEX_SEED, Hex.toStringCondensed(seed))
        IdentityKeyUtil.save(context, IdentityKeyUtil.IDENTITY_PUBLIC_KEY_PREF, Base64.encodeBytes(x25519KeyPair.publicKey.serialize()))
        IdentityKeyUtil.save(context, IdentityKeyUtil.IDENTITY_PRIVATE_KEY_PREF, Base64.encodeBytes(x25519KeyPair.privateKey.serialize()))
        IdentityKeyUtil.save(context, IdentityKeyUtil.ED25519_PUBLIC_KEY, Base64.encodeBytes(ed25519KeyPair.publicKey.asBytes))
        IdentityKeyUtil.save(context, IdentityKeyUtil.ED25519_SECRET_KEY, Base64.encodeBytes(ed25519KeyPair.secretKey.asBytes))
    }

    fun hasV2KeyPair(context: Context): Boolean {
        return (IdentityKeyUtil.retrieve(context, IdentityKeyUtil.ED25519_SECRET_KEY) != null)
    }

    fun getUserED25519KeyPair(context: Context): KeyPair? {
        val base64EncodedED25519PublicKey = IdentityKeyUtil.retrieve(context, IdentityKeyUtil.ED25519_PUBLIC_KEY) ?: return null
        val base64EncodedED25519SecretKey = IdentityKeyUtil.retrieve(context, IdentityKeyUtil.ED25519_SECRET_KEY) ?: return null
        val ed25519PublicKey = Key.fromBytes(Base64.decode(base64EncodedED25519PublicKey))
        val ed25519SecretKey = Key.fromBytes(Base64.decode(base64EncodedED25519SecretKey))
        return KeyPair(ed25519PublicKey, ed25519SecretKey)
    }

    data class KeyPairGenerationResult(
        val seed: ByteArray,
        val ed25519KeyPair: KeyPair,
        val x25519KeyPair: ECKeyPair
    )
}