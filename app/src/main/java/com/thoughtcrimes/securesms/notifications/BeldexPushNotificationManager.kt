package com.thoughtcrimes.securesms.notifications

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import com.beldex.libbchat.messaging.sending_receiving.notifications.PushNotificationAPI
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.JsonUtil
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.retryIfNeeded
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import nl.komponents.kovenant.functional.map

object BeldexPushNotificationManager {
    private val maxRetryCount = 4
    private val tokenExpirationInterval = 12 * 60 * 60 * 1000

    private val server by lazy {
        PushNotificationAPI.server
    }
    private val pnServerPublicKey by lazy {
        PushNotificationAPI.serverPublicKey
    }

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

    @JvmStatic
    fun unregister(token: String, context: Context) {
        val parameters = mapOf( "token" to token )
        val url = "$server/unregister"
        val body = RequestBody.create("application/json".toMediaType(), JsonUtil.toJson(parameters))
        val request = Request.Builder().url(url).post(body)
        retryIfNeeded(maxRetryCount) {
            OnionRequestAPI.sendOnionRequest(request.build(), server, pnServerPublicKey, "/beldex/v2/lsrpc").map { json ->
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
        // Unsubscribe from all secret groups
        val allClosedGroupPublicKeys = DatabaseComponent.get(context).beldexAPIDatabase().getAllClosedGroupPublicKeys()
        val userPublicKey = TextSecurePreferences.getLocalNumber(context)!!
        allClosedGroupPublicKeys.iterator().forEach { closedGroup ->
            performOperation(context, ClosedGroupOperation.Unsubscribe, closedGroup, userPublicKey)
        }
    }

    @JvmStatic
    fun register(token: String, publicKey: String, context: Context, force: Boolean) {
        val oldToken = TextSecurePreferences.getFCMToken(context)
        val lastUploadDate = TextSecurePreferences.getLastFCMUploadTime(context)
        if (!force && token == oldToken && System.currentTimeMillis() - lastUploadDate < tokenExpirationInterval) { return }
        val parameters = mapOf( "token" to token, "pubKey" to publicKey )
        val url = "$server/register"
        val body = RequestBody.create("application/json".toMediaType(), JsonUtil.toJson(parameters))
        val request = Request.Builder().url(url).post(body)
        retryIfNeeded(maxRetryCount) {
            OnionRequestAPI.sendOnionRequest(request.build(), server, pnServerPublicKey, "/beldex/v2/lsrpc").map { json ->
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
        // Subscribe to all secret groups
        val allClosedGroupPublicKeys = DatabaseComponent.get(context).beldexAPIDatabase().getAllClosedGroupPublicKeys()
        allClosedGroupPublicKeys.iterator().forEach { closedGroup ->
            performOperation(context, ClosedGroupOperation.Subscribe, closedGroup, publicKey)
        }
    }

    @JvmStatic
    fun performOperation(context: Context, operation: ClosedGroupOperation, closedGroupPublicKey: String, publicKey: String) {
        if (!TextSecurePreferences.isUsingFCM(context)) { return }
        val parameters = mapOf( "closedGroupPublicKey" to closedGroupPublicKey, "pubKey" to publicKey )
        val url = "$server/${operation.rawValue}"
        val body = RequestBody.create("application/json".toMediaType(), JsonUtil.toJson(parameters))
        val request = Request.Builder().url(url).post(body)
        retryIfNeeded(maxRetryCount) {
            OnionRequestAPI.sendOnionRequest(request.build(), server, pnServerPublicKey, "/beldex/v2/lsrpc").map { json ->
                val code = json["code"] as? Int
                if (code == null || code == 0) {
                    Log.d("Beldex", "Couldn't subscribe/unsubscribe secret group: $closedGroupPublicKey due to error: ${json["message"] as? String ?: "null"}.")
                }
            }.fail { exception ->
                Log.d("Beldex", "Couldn't subscribe/unsubscribe secret group: $closedGroupPublicKey due to error: ${exception}.")
            }
        }
    }
}
