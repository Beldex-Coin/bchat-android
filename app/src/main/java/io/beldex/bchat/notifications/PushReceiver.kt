package io.beldex.bchat.notifications
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.AEAD
import com.goterl.lazysodium.utils.Key
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.beldex.libbchat.messaging.jobs.BatchMessageReceiveJob
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.messaging.jobs.MessageReceiveParameters
import com.beldex.libbchat.messaging.sending_receiving.notifications.PushNotificationMetadata
import com.beldex.libbchat.messaging.utilities.MessageWrapper
import com.beldex.libbchat.utilities.bencode.Bencode
import com.beldex.libbchat.utilities.bencode.BencodeList
import com.beldex.libbchat.utilities.bencode.BencodeString
import com.beldex.libsignal.utilities.Base64
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.crypto.IdentityKeyUtil
import javax.inject.Inject
private const val TAG = "PushHandler"
class PushReceiver @Inject constructor(@ApplicationContext val context: Context) {
    private val sodium by lazy { LazySodiumAndroid(SodiumAndroid()) }
    private val json = Json { ignoreUnknownKeys = true }
    fun onPush(dataMap: Map<String, String>?) {
        onPush(dataMap?.asByteArray())
    }
    private fun onPush(data: ByteArray?) {
        if (data == null) {
            onPush()
            return
        }
        try {
            val envelopeAsData = MessageWrapper.unwrap(data).toByteArray()
            val job = BatchMessageReceiveJob(listOf(MessageReceiveParameters(envelopeAsData)), null)
            JobQueue.shared.add(job)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to unwrap data for message due to error.", e)
        }
    }
    private fun onPush() {
        // no need to do anything if notification permissions are not granted
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        Log.d(TAG, "Failed to decode data for message.")
        val builder = NotificationCompat.Builder(context, NotificationChannels.OTHER)
            .setSmallIcon(io.beldex.bchat.R.drawable.ic_notification_)
            .setColor(context.getColor(io.beldex.bchat.R.color.textsecure_primary))
            .setContentTitle("BChat")
            .setContentText("You've got a new message.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        NotificationManagerCompat.from(context).notify(11111, builder.build())
    }
    private fun Map<String, String>.asByteArray() =
        when {
            // this is a v2 push notification
            containsKey("spns") -> {
                try {
                    decrypt(Base64.decode(this["enc_payload"]))
                } catch (e: Exception) {
                    Log.e(TAG, "Invalid push notification", e)
                    null
                }
            }
            // old v1 push notification; we still need this for receiving legacy closed group notifications
            else -> this["ENCRYPTED_DATA"]?.let(Base64::decode)
        }
    private fun decrypt(encPayload: ByteArray): ByteArray? {
        Log.d(TAG, "decrypt() called")
        val encKey = getOrCreateNotificationKey()
        val nonce = encPayload.take(AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES).toByteArray()
        val payload = encPayload.drop(AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES).toByteArray()
        val padded = decrypt(payload, encKey.asBytes, nonce)
            ?: error("Failed to decrypt push notification")
        val decrypted = padded.dropLastWhile { it.toInt() == 0 }.toByteArray()
        val bencoded = Bencode.Decoder(decrypted)
        val expectedList = (bencoded.decode() as? BencodeList)?.values
            ?: error("Failed to decode bencoded list from payload")
        val metadataJson = (expectedList[0] as? BencodeString)?.value ?: error("no metadata")
        val metadata: PushNotificationMetadata = json.decodeFromString(String(metadataJson))
        return (expectedList.getOrNull(1) as? BencodeString)?.value.also {
            // null content is valid only if we got a "data_too_long" flag
            it?.let { check(metadata.data_len == it.size) { "wrong message data size" } }
                ?: check(metadata.data_too_long) { "missing message data, but no too-long flag" }
        }
    }
    private fun decrypt(ciphertext: ByteArray, decryptionKey: ByteArray, nonce: ByteArray): ByteArray? {
        val plaintextSize = ciphertext.size - AEAD.XCHACHA20POLY1305_IETF_ABYTES
        val plaintext = ByteArray(plaintextSize)
        return if (sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
                plaintext,
                longArrayOf(plaintextSize.toLong()),
                null,
                ciphertext,
                ciphertext.size.toLong(),
                null,
                0L,
                nonce,
                decryptionKey
            )
        ) {
            plaintext
        } else null
    }
    fun getOrCreateNotificationKey(): Key {
        if (IdentityKeyUtil.retrieve(context, IdentityKeyUtil.NOTIFICATION_KEY) == null) {
            // generate the key and store it
            val key = sodium.keygen(AEAD.Method.XCHACHA20_POLY1305_IETF)
            IdentityKeyUtil.save(context, IdentityKeyUtil.NOTIFICATION_KEY, key.asHexString)
        }
        return Key.fromHexString(
            IdentityKeyUtil.retrieve(
                context,
                IdentityKeyUtil.NOTIFICATION_KEY
            )
        )
    }
}