package io.beldex.bchat.webrtc

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.beldex.libbchat.database.StorageProtocol
import com.beldex.libbchat.messaging.calls.CallMessageType
import com.beldex.libbchat.messaging.messages.control.CallMessage
import com.beldex.libbchat.messaging.utilities.WebRtcUtils
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.service.WebRtcCallService
import io.beldex.bchat.util.CallNotificationBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.ANSWER
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.END_CALL
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.ICE_CANDIDATES
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.OFFER
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.PRE_OFFER
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.PROVISIONAL_ANSWER
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.permissions.Permissions

class CallMessageProcessor (private val context: Context, private val textSecurePreferences: TextSecurePreferences, lifecycle: Lifecycle, private val storage: StorageProtocol) {

    companion object {
        private const val TAG = "CallMessageProcessor"
        private const val VERY_EXPIRED_TIME = 15 * 60 * 1000L

        fun safeStartForegroundService(context: Context, intent: Intent) {
            // Wake up the device (if required) before attempting to start any services - otherwise on Android 12 and above we get
            // a BackgroundServiceStartNotAllowedException such as:
            //      Unable to start CallMessage intent: startForegroundService() not allowed due to mAllowStartForeground false:
            //      service ic.beldex.bchat/io.beldex.bchat.service.WebRtcCallService
            (context as ApplicationContext).wakeUpDeviceAndDismissKeyguardIfRequired()
            // Attempt to start the call service..
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("Beldex", "Unable to start service: ${e.message}", e)
                try {
                    ContextCompat.startForegroundService(context, intent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Unable to start CallMessage intent: ${e2.message}", e2)
                }
            }
        }
    }
    /*Hales63*/
    init {
        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                val nextMessage = WebRtcUtils.SIGNAL_QUEUE.receive()
                Log.d("Beldex", nextMessage.type?.name ?: "CALL MESSAGE RECEIVED")
                val sender = nextMessage.sender ?: continue
                /*Hales63*/
                /*val approvedContact = Recipient.from(context, Address.fromSerialized(sender), false).isApproved
                Log.i("Beldex", "Contact is approved?: $approvedContact")
                if (!approvedContact && storage.getUserPublicKey() != sender) continue*/

                // If the user has not enabled voice/video calls or if the user has not granted audio/microphone permissions
                if ( !textSecurePreferences.isCallNotificationsEnabled() ||
                    !Permissions.hasAll(context, Manifest.permission.RECORD_AUDIO)) {
                    Log.d("Beldex","Dropping call message if call notifications disabled")
                    if (nextMessage.type != PRE_OFFER) continue
                    val sentTimestamp = nextMessage.sentTimestamp ?: continue
                    if (textSecurePreferences.setShownCallNotification()) {
                        // first time call notification encountered
                        val notification = CallNotificationBuilder.getFirstCallNotification(context)
                        context.getSystemService(NotificationManager::class.java).notify(CallNotificationBuilder.WEBRTC_NOTIFICATION, notification)
                        insertMissedCall(sender, sentTimestamp, isFirstCall = true)
                        Log.d("Beldex","busy call called 1")
                    } else {
                        Log.d("Beldex","busy call called 2")
                        insertMissedCall(sender, sentTimestamp)
                    }
                    continue
                }

                val isVeryExpired = (nextMessage.sentTimestamp?:0) + VERY_EXPIRED_TIME < MnodeAPI.nowWithOffset
                if (isVeryExpired) {
                    Log.e("Beldex", "Dropping very expired call message")
                    continue
                }
                when (nextMessage.type) {
                    OFFER -> incomingCall(nextMessage)
                    ANSWER -> incomingAnswer(nextMessage)
                    END_CALL -> incomingHangup(nextMessage)
                    ICE_CANDIDATES -> handleIceCandidates(nextMessage)
                    PRE_OFFER -> incomingPreOffer(nextMessage)
                    PROVISIONAL_ANSWER, null -> {} // TODO: if necessary
                }
            }
        }
    }

    private fun insertMissedCall(sender: String, sentTimestamp: Long, isFirstCall: Boolean = false) {
        val currentUserPublicKey = storage.getUserPublicKey()
        if (sender == currentUserPublicKey) return // don't insert a "missed" due to call notifications disabled if it's our own sender
        if (isFirstCall) {
            storage.insertCallMessage(sender, CallMessageType.CALL_FIRST_MISSED, sentTimestamp)
        } else {
            storage.insertCallMessage(sender, CallMessageType.CALL_MISSED, sentTimestamp)
        }
    }

    private fun incomingHangup(callMessage: CallMessage) {
        //TextSecurePreferences.setRemoteCallEnded(context, true)
        //SteveJosephh21 - ContextCompat.startForegroundService()
        val callId = callMessage.callId ?: return
        val hangupIntent = WebRtcCallService.remoteHangupIntent(context, callId)
        Log.d("startForegroundService->","3")
        safeStartForegroundService(context, hangupIntent)
    }

    private fun incomingAnswer(callMessage: CallMessage) {
        val recipientAddress = callMessage.sender ?: return //Log.w(TAG, "Cannot answer incoming call without sender")
        val callId = callMessage.callId ?: return //Log.w(TAG, "Cannot answer incoming call without callId" )
        val sdp = callMessage.sdps.firstOrNull() ?: return //Log.w(TAG, "Cannot answer incoming call without sdp")
        val answerIntent = WebRtcCallService.incomingAnswer(
            context = context,
            address = Address.fromSerialized(recipientAddress),
            sdp = sdp,
            callId = callId
        )
        safeStartForegroundService(context, answerIntent)
    }

    private fun handleIceCandidates(callMessage: CallMessage) {
        val callId = callMessage.callId ?: return
        val sender = callMessage.sender ?: return

        val iceCandidates = callMessage.iceCandidates()
        if (iceCandidates.isEmpty()) return

        val iceIntent = WebRtcCallService.iceCandidates(
            context = context,
            iceCandidates = iceCandidates,
            callId = callId,
            address = Address.fromSerialized(sender)
        )
        safeStartForegroundService(context, iceIntent)
    }

    private fun incomingPreOffer(callMessage: CallMessage) {
        // handle notification state
        val recipientAddress = callMessage.sender ?: return
        val callId = callMessage.callId ?: return
        val incomingIntent = WebRtcCallService.preOffer(
            context = context,
            address = Address.fromSerialized(recipientAddress),
            callId = callId,
            callTime = callMessage.sentTimestamp!!
        )
        safeStartForegroundService(context, incomingIntent)
    }

    private fun incomingCall(callMessage: CallMessage) {
        val recipientAddress = callMessage.sender ?: return
        val callId = callMessage.callId ?: return
        val sdp = callMessage.sdps.firstOrNull() ?: return
        val incomingIntent = WebRtcCallService.incomingCall(
            context = context,
            address = Address.fromSerialized(recipientAddress),
            sdp = sdp,
            callId = callId,
            callTime = callMessage.sentTimestamp!!
        )
        safeStartForegroundService(context, incomingIntent)

    }

    private fun CallMessage.iceCandidates(): List<IceCandidate> {
        if (sdpMids.size != sdpMLineIndexes.size || sdpMLineIndexes.size != sdps.size) {
            return listOf() // uneven sdp numbers
        }
        val candidateSize = sdpMids.size
        return (0 until candidateSize).map { i ->
            IceCandidate(sdpMids[i], sdpMLineIndexes[i], sdps[i])
        }
    }

}