package io.beldex.bchat.webrtc

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.GlobalScope
import com.beldex.libbchat.database.StorageProtocol
import com.beldex.libbchat.messaging.calls.CallMessageType
import com.beldex.libbchat.messaging.messages.control.CallMessage
import com.beldex.libbchat.messaging.utilities.WebRtcUtils
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.util.CallNotificationBuilder
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import javax.inject.Inject
import javax.inject.Singleton
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.ANSWER
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.END_CALL
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.ICE_CANDIDATES
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.OFFER
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.PRE_OFFER
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.PROVISIONAL_ANSWER
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers.IO
import io.beldex.bchat.permissions.Permissions

@Singleton
class CallMessageProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val textSecurePreferences: TextSecurePreferences,
    private val webRtcBridge: WebRtcCallBridge,
    private val storage: StorageProtocol
) {

    companion object {
        private const val TAG = "CallMessageProcessor"
        private const val VERY_EXPIRED_TIME  = 15 * 60 * 1000L
    }
    /*Hales63*/
    init {
        GlobalScope.launch(IO) {
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
                // or if the user has not granted audio/microphone permissions
                else if (!Permissions.hasAll(context, Manifest.permission.RECORD_AUDIO)) {
                    if (nextMessage.type != PRE_OFFER) continue
                    val sentTimestamp = nextMessage.sentTimestamp ?: continue
                    Log.d("Beldex", "Attempted to receive a call without audio permissions")
                    insertMissedPermissionCall(sender, sentTimestamp)
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

    private fun insertMissedPermissionCall(sender: String, sentTimestamp: Long) {
        val currentUserPublicKey = storage.getUserPublicKey()
        if (sender == currentUserPublicKey) return // don't insert a "missed" due to call notifications disabled if it's our own sender
        storage.insertCallMessage(sender, CallMessageType.CALL_MISSED_PERMISSION, sentTimestamp)
    }


    private fun incomingHangup(callMessage: CallMessage) {
        Log.d("", "CallMessageProcessor: incomingHangup")
        val callId = callMessage.callId ?: return
        webRtcBridge.handleRemoteHangup(callId)
    }

    private fun incomingAnswer(callMessage: CallMessage) {
        Log.d("", "CallMessageProcessor: incomingAnswer")
        val recipientAddress = callMessage.sender ?: return //Log.w(TAG, "Cannot answer incoming call without sender")
        val callId = callMessage.callId ?: return //Log.w(TAG, "Cannot answer incoming call without callId" )
        val sdp = callMessage.sdps.firstOrNull() ?: return //Log.w(TAG, "Cannot answer incoming call without sdp")
        webRtcBridge.handleAnswerIncoming(
            address = Address.fromSerialized(recipientAddress),
            sdp = sdp,
            callId = callId
        )
    }

    private fun handleIceCandidates(callMessage: CallMessage) {
        Log.d("", "CallMessageProcessor: handleIceCandidates")
        val callId = callMessage.callId ?: return
        val sender = callMessage.sender ?: return

        val iceCandidates = callMessage.iceCandidates()
        if (iceCandidates.isEmpty()) return

        webRtcBridge.handleRemoteIceCandidate(
            iceCandidates = iceCandidates,
            callId = callId
        )
    }

    private fun incomingPreOffer(callMessage: CallMessage) {
        // handle notification state
        Log.d("", "CallMessageProcessor: incomingPreOffer")
        val recipientAddress = callMessage.sender ?: return
        val callId = callMessage.callId ?: return
        webRtcBridge.handlePreOffer(
            address = Address.fromSerialized(recipientAddress),
            callId = callId,
            callTime = callMessage.sentTimestamp!!
        )
    }

    private fun incomingCall(callMessage: CallMessage) {
        Log.d("", "CallMessageProcessor: incomingCall")

        val recipientAddress = callMessage.sender ?: return
        val callId = callMessage.callId ?: return
        val sdp = callMessage.sdps.firstOrNull() ?: return
        webRtcBridge.onIncomingCall(
            address = Address.fromSerialized(recipientAddress),
            sdp = sdp,
            callId = callId,
            callTime = callMessage.sentTimestamp!!
        )

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