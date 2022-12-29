package com.thoughtcrimes.securesms.notifications

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.beldex.libbchat.messaging.jobs.JobQueue
import com.beldex.libbchat.messaging.jobs.MessageReceiveJob
import com.beldex.libbchat.messaging.utilities.MessageWrapper
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Base64
import com.beldex.libsignal.utilities.Log

class PushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("Beldex", "New FCM token: $token.")
        val userPublicKey = TextSecurePreferences.getLocalNumber(this) ?: return
        BeldexPushNotificationManager.register(token, userPublicKey, this, false)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("Beldex", "Received a push notification.")
        val base64EncodedData = message.data?.get("ENCRYPTED_DATA")
        val data = base64EncodedData?.let { Base64.decode(it) }
        if (data != null) {
            try {
                val envelopeAsData = MessageWrapper.unwrap(data).toByteArray()
                val job = MessageReceiveJob(envelopeAsData)
                JobQueue.shared.add(job)
            } catch (e: Exception) {
                Log.d("Beldex", "Failed to unwrap data for message due to error: $e.")
            }
        } else {
            Log.d("Beldex", "Failed to decode data for message.")
            val builder = NotificationCompat.Builder(this, NotificationChannels.OTHER)
                .setSmallIcon(io.beldex.bchat.R.drawable.ic_bchat_logo)
                .setColor(this.getResources().getColor(io.beldex.bchat.R.color.textsecure_primary))
                .setContentTitle("BChat")
                .setContentText("You've got a new message.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
            with(NotificationManagerCompat.from(this)) {
                notify(11111, builder.build())
            }
        }
    }

    override fun onDeletedMessages() {
        Log.d("Beldex", "Called onDeletedMessages.")
        super.onDeletedMessages()
        val token = TextSecurePreferences.getFCMToken(this)!!
        val userPublicKey = TextSecurePreferences.getLocalNumber(this) ?: return
        BeldexPushNotificationManager.register(token, userPublicKey, this, true)
    }
}