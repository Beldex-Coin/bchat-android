package io.beldex.bchat.webrtc

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import androidx.lifecycle.ViewModel
import io.beldex.bchat.webrtc.audio.SignalAudioManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.conversation.v2.ViewUtil
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_ANSWER_INCOMING
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_ANSWER_OUTGOING
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_CONNECTED
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_DISCONNECTED
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_HANDLING_ICE
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_OFFER_INCOMING
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_OFFER_OUTGOING
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_PRE_OFFER_INCOMING
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_PRE_OFFER_OUTGOING
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_RECONNECTING
import io.beldex.bchat.webrtc.CallViewModel.State.CALL_SENDING_ICE
import io.beldex.bchat.webrtc.CallViewModel.State.NETWORK_FAILURE
import io.beldex.bchat.webrtc.CallViewModel.State.RECIPIENT_UNAVAILABLE
import io.beldex.bchat.R

@HiltViewModel
public class CallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callManager: CallManager,
    private val rtcCallBridge: WebRtcCallBridge,
): ViewModel() {

    enum class State {
        CALL_INITIALIZING, // default starting state before any rtc state kicks in

        CALL_PRE_OFFER_INCOMING,
        CALL_PRE_OFFER_OUTGOING,
        CALL_OFFER_INCOMING,
        CALL_OFFER_OUTGOING,
        CALL_ANSWER_INCOMING,
        CALL_ANSWER_OUTGOING,
        CALL_HANDLING_ICE,
        CALL_SENDING_ICE,

        CALL_CONNECTED,
        CALL_RINGING,
        CALL_OUTGOING,
        CALL_INCOMING,
        CALL_DISCONNECTED,
        CALL_RECONNECTING,

        NETWORK_FAILURE,
        RECIPIENT_UNAVAILABLE,
        CALL_PRE_INIT
    }

    val floatingRenderer: SurfaceViewRenderer?
        get() = callManager.floatingRenderer

    val fullscreenRenderer: SurfaceViewRenderer?
        get() = callManager.fullscreenRenderer

    var microphoneEnabled: Boolean = true
        private set

    private var _isBluetooth: Boolean = false
    private var _bluetoothConnectionState = MutableLiveData<Boolean>()
    val bluetoothConnectionState: LiveData<Boolean> = _bluetoothConnectionState

    fun setBooleanValue(value: Boolean){
        _bluetoothConnectionState.value = value
    }

    val isBluetooth: Boolean
        get() = _isBluetooth


    val audioDeviceState
        get() = callManager.audioDeviceEvents

    val audioBluetoothDeviceState
        get() = callManager.audioDeviceEvents
            .onEach {
                _isBluetooth = it.selectedDevice == SignalAudioManager.AudioDevice.BLUETOOTH
            }
    val bluetoothConnectionStatus
        get() = callManager.getBluetoothConnectionStatus()


    val localAudioEnabledState
        get() = callManager.audioEvents.map { it.isEnabled }
            .onEach { microphoneEnabled = it }

    val videoState: StateFlow<VideoState>
        get() = callManager.videoState

    //SteveJosephh21 --
    val remoteAudioEnabledState
        get() = callManager.remoteAudioEvents.map { it.isEnabled }
    //SteveJosephh21 --
    val remoteVideoStatusEnabledState
        get() = callManager.remoteVideoStatusEvents.map { it.isEnabled }

    var deviceOrientation: Orientation = Orientation.UNKNOWN
        set(value) {
            field = value
            callManager.setDeviceOrientation(value)
        }

    val currentCallState
        get() = callManager.currentCallState

    val connectionState: StateFlow<io.beldex.bchat.webrtc.data.State>
        get() = callManager.currentConnectionStateFlow

    private val initialCallState = CallState("", "", false, false, false, false, false, false)
    private val initialAccumulator = CallAccumulator(emptySet(), initialCallState)

    val callState: StateFlow<CallState> = callManager.callStateEvents
        .combine(rtcCallBridge.hasAcceptedCall) { state, accepted ->
            Pair(state, accepted)
        }.scan(initialAccumulator) { acc, (state, accepted) ->
            // reset the set on  preoffers
            val newSteps = if (state in listOf(
                    CALL_PRE_OFFER_OUTGOING,
                    CALL_PRE_OFFER_INCOMING
                )
            ) {
                setOf(state)
            } else {
                acc.callSteps + state
            }

            val callTitle = when (state) {
                CALL_PRE_OFFER_OUTGOING, CALL_PRE_OFFER_INCOMING,
                CALL_OFFER_OUTGOING, CALL_OFFER_INCOMING ->
                    context.getString(R.string.callsRinging)
                CALL_ANSWER_INCOMING, CALL_ANSWER_OUTGOING ->
                    context.getString(R.string.callsConnecting)
                CALL_CONNECTED -> ""
                RECIPIENT_UNAVAILABLE, CALL_DISCONNECTED ->
                    context.getString(R.string.callsEnded)
                NETWORK_FAILURE -> context.getString(R.string.callsErrorStart)
                else -> acc.callState.callLabelTitle // keep previous title
            }

            val callSubtitle = when (state) {
                CALL_PRE_OFFER_OUTGOING -> constructCallLabel(R.string.creatingCall, newSteps.size)
                CALL_PRE_OFFER_INCOMING -> constructCallLabel(R.string.receivingPreOffer, newSteps.size)
                CALL_OFFER_OUTGOING -> constructCallLabel(R.string.sendingCallOffer, newSteps.size)
                CALL_OFFER_INCOMING -> constructCallLabel(R.string.receivingCallOffer, newSteps.size)
                CALL_ANSWER_OUTGOING, CALL_ANSWER_INCOMING -> constructCallLabel(R.string.receivedAnswer, newSteps.size)
                CALL_SENDING_ICE -> constructCallLabel(R.string.sendingConnectionCandidates, newSteps.size)
                CALL_HANDLING_ICE -> constructCallLabel(R.string.handlingConnectionCandidates, newSteps.size)
                else -> ""
            }

            val showReconnecting = state == CALL_RECONNECTING

            val showCallControls = state in listOf(
                CALL_CONNECTED,
                CALL_PRE_OFFER_OUTGOING,
                CALL_OFFER_OUTGOING,
                CALL_ANSWER_OUTGOING,
                CALL_ANSWER_INCOMING
            ) || (state in listOf(
                CALL_PRE_OFFER_INCOMING,
                CALL_OFFER_INCOMING,
                CALL_HANDLING_ICE,
                CALL_SENDING_ICE
            ) && accepted)

            val showEndCallButton = showCallControls || state == CALL_RECONNECTING

            val showPreCallButtons = state in listOf(
                CALL_PRE_OFFER_INCOMING,
                CALL_OFFER_INCOMING,
                CALL_HANDLING_ICE,
                CALL_SENDING_ICE
            ) && !accepted

            val showCallLoading = state !in listOf(
                CALL_CONNECTED, State.CALL_RINGING, State.CALL_PRE_INIT
            )

            val showBluetoothConnected = state == CALL_DISCONNECTED

            val newCallState = CallState(
                callLabelTitle = callTitle,
                callLabelSubtitle = callSubtitle,
                showCallButtons = showCallControls,
                showPreCallButtons = showPreCallButtons,
                showEndCallButton = showEndCallButton,
                showReconnecting = showReconnecting,
                showCallLoading = showCallLoading,
                showBluetoothConnected = showBluetoothConnected
            )

            CallAccumulator(newSteps, newCallState)
        }
        .map { it.callState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), initialCallState)


    val recipient
        get() = callManager.recipientEvents

    val callStartTime: Long
        get() = callManager.callStartTime

    data class CallAccumulator(
        val callSteps: Set<State>,
        val callState: CallState
    )

    private val MAX_CALL_STEPS: Int = 5

    private fun constructCallLabel(@StringRes label: Int, stepsCount: Int): String {
        return if(ViewUtil.isLtr(context)) {
            "${context.getString(label)} $stepsCount/$MAX_CALL_STEPS"
        } else {
            "$MAX_CALL_STEPS/$stepsCount ${context.getString(label)}"
        }
    }

    fun swapVideos() = callManager.swapVideos()

    fun toggleMute()= callManager.toggleMuteAudio()

    fun toggleSpeakerphone() = callManager.toggleSpeakerphone()

    fun toggleBluetoothPhone() = callManager.toggleBluetoothPhone()

    fun toggleVideo(context : Context) = callManager.toggleVideo(context)

    fun flipCamera() = callManager.flipCamera()

    fun answerCall() = rtcCallBridge.handleAnswerCall()

    fun denyCall() = rtcCallBridge.handleDenyCall()

    fun createCall(recipientAddress: Address) =
        rtcCallBridge.handleOutgoingCall(Recipient.from(context, recipientAddress, true))

    fun hangUp() = rtcCallBridge.handleLocalHangup(null)

    data class CallState(
        val callLabelTitle: String?,
        val callLabelSubtitle: String,
        val showCallButtons: Boolean,
        val showPreCallButtons: Boolean,
        val showEndCallButton: Boolean,
        val showReconnecting: Boolean,
        val showCallLoading: Boolean,
        val showBluetoothConnected: Boolean

    )
}