package io.beldex.bchat.notifications
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Sign
import com.goterl.lazysodium.utils.KeyPair
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.functional.map
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import com.beldex.libbchat.messaging.sending_receiving.notifications.Response
import com.beldex.libbchat.messaging.sending_receiving.notifications.Server
import com.beldex.libbchat.messaging.sending_receiving.notifications.SubscriptionRequest
import com.beldex.libbchat.messaging.sending_receiving.notifications.SubscriptionResponse
import com.beldex.libbchat.messaging.sending_receiving.notifications.UnsubscribeResponse
import com.beldex.libbchat.messaging.sending_receiving.notifications.UnsubscriptionRequest
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.mnode.Version
import com.beldex.libbchat.utilities.Device
import com.beldex.libsignal.utilities.Base64
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.Namespace
import com.beldex.libsignal.utilities.retryIfNeeded
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton
private val TAG = PushRegistryV2::class.java.name
private const val maxRetryCount = 4
@Singleton
class PushRegistryV2 @Inject constructor(private val pushReceiver: PushReceiver) {
    private val sodium = LazySodiumAndroid(SodiumAndroid())
    fun register(
        device: Device,
        token: String,
        publicKey: String,
        userEd25519Key: KeyPair,
        namespaces: List<Int>
    ): Promise<SubscriptionResponse, Exception> {
        val pnKey = pushReceiver.getOrCreateNotificationKey()
        val timestamp = MnodeAPI.nowWithOffset / 1000 // get timestamp in ms -> s
        // if we want to support passing namespace list, here is the place to do it
        val sigData = "MONITOR${publicKey}${timestamp}1${namespaces.joinToString(separator = ",")}".encodeToByteArray()
        val signature = ByteArray(Sign.BYTES)
        sodium.cryptoSignDetached(signature, sigData, sigData.size.toLong(), userEd25519Key.secretKey.asBytes)
        val requestParameters = SubscriptionRequest(
            pubkey = publicKey,
            session_ed25519 = userEd25519Key.publicKey.asHexString,
            namespaces = listOf(Namespace.DEFAULT),
            data = true, // only permit data subscription for now (?)
            service = device.service,
            sig_ts = timestamp,
            signature = Base64.encodeBytes(signature),
            service_info = mapOf("token" to token),
            enc_key = pnKey.asHexString,
        ).let(Json::encodeToString)
        return retryResponseBody<SubscriptionResponse>("subscribe", requestParameters) success {
            Log.d(TAG, "registerV2 success")
        }
    }
    fun unregister(
        device: Device,
        token: String,
        userPublicKey: String,
        userEdKey: KeyPair
    ): Promise<UnsubscribeResponse, Exception> {
        val timestamp = MnodeAPI.nowWithOffset / 1000 // get timestamp in ms -> s
        // if we want to support passing namespace list, here is the place to do it
        val sigData = "UNSUBSCRIBE${userPublicKey}${timestamp}".encodeToByteArray()
        val signature = ByteArray(Sign.BYTES)
        sodium.cryptoSignDetached(signature, sigData, sigData.size.toLong(), userEdKey.secretKey.asBytes)
        val requestParameters = UnsubscriptionRequest(
            pubkey = userPublicKey,
            session_ed25519 = userEdKey.publicKey.asHexString,
            service = device.service,
            sig_ts = timestamp,
            signature = Base64.encodeBytes(signature),
            service_info = mapOf("token" to token),
        ).let(Json::encodeToString)
        return retryResponseBody<UnsubscribeResponse>("unsubscribe", requestParameters) success {
            Log.d(TAG, "unregisterV2 success")
        }
    }
    private inline fun <reified T: Response> retryResponseBody(path: String, requestParameters: String): Promise<T, Exception> =
        retryIfNeeded(maxRetryCount) { getResponseBody(path, requestParameters) }
    private inline fun <reified T: Response> getResponseBody(path: String, requestParameters: String): Promise<T, Exception> {
        val server = Server.LATEST
        val url = "${server.url}/$path"
        val body = RequestBody.create("application/json".toMediaType(), requestParameters)
        val request = Request.Builder().url(url).post(body).build()
        return OnionRequestAPI.sendOnionRequest(
            request,
            server.url,
            server.publicKey,
            Version.V4
        ).map { response ->
            response.body!!.inputStream()
                .let { Json.decodeFromStream<T>(it) }
                .also { if (it.isFailure()) throw Exception("error: ${it.message}.") }
        }
    }
}