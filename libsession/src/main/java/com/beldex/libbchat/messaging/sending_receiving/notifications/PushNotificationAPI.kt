package com.beldex.libbchat.messaging.sending_receiving.notifications

import android.annotation.SuppressLint
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.JsonUtil
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.retryIfNeeded
import nl.komponents.kovenant.functional.map

@SuppressLint("StaticFieldLeak")
object PushNotificationAPI {
    val context = MessagingModuleConfiguration.shared.context
    //val server = "http://13.233.251.36:3000"
    //val serverPublicKey = "efcaecf00aebf5b75e62cf1fd550c6052842e1415a9339406e256c8b27cd2039"
    //31-05-2022 4.25 PM
    //val server = "http://49.206.200.190:3000"
    //val serverPublicKey = "b82d79da680cde0486f44e979fa831a04f9635c592b5a27e00cae386e7a11d22"
    //01-06-2022 - 10.30 AM
    val server = "http://notification.rpcnode.stream"
    val serverPublicKey = "54e8ce6a688f6decd414350408cae373ab6070d91d4512e17454d2470c7cf911"
    private val maxRetryCount = 4
    private val tokenExpirationInterval = 12 * 60 * 60 * 1000

    enum class ClosedGroupOperation {
        Subscribe, Unsubscribe;

        val rawValue: String
            get() {
                return when (this) {
                    Subscribe -> "subscribe_closed_group"
                    Unsubscribe -> "unsubscribe_closed_group"
                }
            }
    }

    fun unregister(token: String) {
        val parameters = mapOf( "token" to token )
        val url = "$server/unregister"
        val body = RequestBody.create(MediaType.get("application/json"), JsonUtil.toJson(parameters))
        val request = Request.Builder().url(url).post(body)
        retryIfNeeded(maxRetryCount) {
            OnionRequestAPI.sendOnionRequest(request.build(), server, serverPublicKey, "/beldex/v2/lsrpc").map { json ->
                val code = json["code"] as? Int
                if (code != null && code != 0) {
                    TextSecurePreferences.setIsUsingFCM(context, false)
                } else {
                    Log.d("Beldex", "Couldn't disable FCM due to error: ${json["message"] as? String ?: "null"}.")
                }
            }.fail { exception ->
                Log.d("Beldex", "Couldn't disable FCM due to error: ${exception}.")
            }
        }
        // Unsubscribe from all closed groups
        val allClosedGroupPublicKeys = MessagingModuleConfiguration.shared.storage.getAllClosedGroupPublicKeys()
        val userPublicKey = MessagingModuleConfiguration.shared.storage.getUserPublicKey()!!
        allClosedGroupPublicKeys.iterator().forEach { closedGroup ->
            performOperation(ClosedGroupOperation.Unsubscribe, closedGroup, userPublicKey)
        }
    }

    fun register(token: String, publicKey: String, force: Boolean) {
        val oldToken = TextSecurePreferences.getFCMToken(context)
        val lastUploadDate = TextSecurePreferences.getLastFCMUploadTime(context)
        if (!force && token == oldToken && System.currentTimeMillis() - lastUploadDate < tokenExpirationInterval) { return }
        val parameters = mapOf( "token" to token, "pubKey" to publicKey )
        val url = "$server/register"
        val body = RequestBody.create(MediaType.get("application/json"), JsonUtil.toJson(parameters))
        val request = Request.Builder().url(url).post(body)
        retryIfNeeded(maxRetryCount) {
            OnionRequestAPI.sendOnionRequest(request.build(), server, serverPublicKey, "/beldex/v2/lsrpc").map { json ->
                val code = json["code"] as? Int
                if (code != null && code != 0) {
                    TextSecurePreferences.setIsUsingFCM(context, true)
                    TextSecurePreferences.setFCMToken(context, token)
                    TextSecurePreferences.setLastFCMUploadTime(context, System.currentTimeMillis())
                } else {
                    Log.d("Beldex", "Couldn't register for FCM due to error: ${json["message"] as? String ?: "null"}.")
                }
            }.fail { exception ->
                Log.d("Beldex", "Couldn't register for FCM due to error: ${exception}.")
            }
        }
        // Subscribe to all closed groups
        val allClosedGroupPublicKeys = MessagingModuleConfiguration.shared.storage.getAllClosedGroupPublicKeys()
        allClosedGroupPublicKeys.iterator().forEach { closedGroup ->
            performOperation(ClosedGroupOperation.Subscribe, closedGroup, publicKey)
        }
    }

    fun performOperation(operation: ClosedGroupOperation, closedGroupPublicKey: String, publicKey: String) {
        if (!TextSecurePreferences.isUsingFCM(context)) { return }
        val parameters = mapOf( "closedGroupPublicKey" to closedGroupPublicKey, "pubKey" to publicKey )
        val url = "$server/${operation.rawValue}"
        val body = RequestBody.create(MediaType.get("application/json"), JsonUtil.toJson(parameters))
        val request = Request.Builder().url(url).post(body)
        retryIfNeeded(maxRetryCount) {
            OnionRequestAPI.sendOnionRequest(request.build(), server, serverPublicKey, "/beldex/v2/lsrpc").map { json ->
                val code = json["code"] as? Int
                if (code == null || code == 0) {
                    Log.d("Beldex", "Couldn't subscribe/unsubscribe closed group: $closedGroupPublicKey due to error: ${json["message"] as? String ?: "null"}.")
                }
            }.fail { exception ->
                Log.d("Beldex", "Couldn't subscribe/unsubscribe closed group: $closedGroupPublicKey due to error: ${exception}.")
            }
        }
    }
}
