package io.beldex.bchat.webrtc

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.beldex.libbchat.database.StorageProtocol
import com.beldex.libbchat.messaging.calls.CallMessageType
import com.beldex.libbchat.messaging.messages.control.CallMessage
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.Debouncer
import com.beldex.libbchat.utilities.Util
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.webrtc.audio.AudioManagerCompat
import io.beldex.bchat.webrtc.audio.OutgoingRinger
import io.beldex.bchat.webrtc.audio.SignalAudioManager
import io.beldex.bchat.webrtc.data.Event
import io.beldex.bchat.webrtc.data.StateProcessor
import io.beldex.bchat.webrtc.locks.LockManager
import io.beldex.bchat.webrtc.video.CameraEventListener
import io.beldex.bchat.webrtc.video.CameraState
import io.beldex.bchat.webrtc.video.RemoteRotationVideoProxySink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.*
import nl.komponents.kovenant.Promise
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.*
import io.beldex.bchat.webrtc.data.State as CallState
import com.beldex.libsignal.protos.SignalServiceProtos.CallMessage.Type.ICE_CANDIDATES
import io.beldex.bchat.service.WebRtcCallService
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nl.komponents.kovenant.functional.bind

class CallManager(context: Context, audioManager: AudioManagerCompat, private val storage: StorageProtocol): PeerConnection.Observer,
    SignalAudioManager.EventListener, CameraEventListener, DataChannel.Observer {

    sealed class StateEvent {
        data class AudioEnabled(val isEnabled: Boolean): StateEvent()
        data class VideoEnabled(val isEnabled: Boolean): StateEvent()
        data class CallStateUpdate(val state: CallState): StateEvent()
        data class AudioDeviceUpdate(val selectedDevice: SignalAudioManager.AudioDevice, val audioDevices: Set<SignalAudioManager.AudioDevice>): StateEvent()
        data class RecipientUpdate(val recipient: Recipient?): StateEvent() {
            companion object {
                val UNKNOWN = RecipientUpdate(recipient = null)
            }
        }
    }

    companion object {
        val VIDEO_DISABLED_JSON by lazy { buildJsonObject { put("video", false) } }
        val VIDEO_ENABLED_JSON by lazy { buildJsonObject { put("video", true) } }
        val HANGUP_JSON by lazy { buildJsonObject { put("hangup", true) } }
        //SteveJosephh21 --
        val AUDIO_DISABLED_JSON by lazy { buildJsonObject { put("audio",false) }}
        val AUDIO_ENABLED_JSON by lazy { buildJsonObject { put("audio",true) }}
        val VIDEO_STATUS_DISABLED_JSON by lazy { buildJsonObject { put("video_status", false) } }
        val VIDEO_STATUS_ENABLED_JSON by lazy { buildJsonObject { put("video_status", true) } }

        private val TAG = Log.tag(CallManager::class.java)
        private const val DATA_CHANNEL_NAME = "signaling"
    }

    private val signalAudioManager: SignalAudioManager = SignalAudioManager(context, this, audioManager)

    private val peerConnectionObservers = mutableSetOf<WebRtcListener>()

    fun registerListener(listener: WebRtcListener) {
        peerConnectionObservers.add(listener)
    }

    fun unregisterListener(listener: WebRtcListener) {
        peerConnectionObservers.remove(listener)
    }

    fun shutDownAudioManager() {
        signalAudioManager.shutdown()
    }
    fun getBluetoothConnectionStatus(): Boolean {
        return signalAudioManager.isBluetoothConnected()
    }

    private val _audioEvents = MutableStateFlow(StateEvent.AudioEnabled(false))
    val audioEvents = _audioEvents.asSharedFlow()
    private val _videoState: MutableStateFlow<VideoState> = MutableStateFlow(
        VideoState(
            swapped = false,
            userVideoEnabled = false,
            remoteVideoEnabled = false
        )
    )
    val videoState = _videoState.asStateFlow()

    //SteveJosephh21 --
    private val _remoteAudioEvents = MutableStateFlow(StateEvent.AudioEnabled(true))
    val remoteAudioEvents = _remoteAudioEvents.asSharedFlow()
    private val _remoteVideoStatusEvents = MutableStateFlow(StateEvent.VideoEnabled(false))
    val remoteVideoStatusEvents = _remoteVideoStatusEvents.asSharedFlow()

    private val stateProcessor = StateProcessor(CallState.Idle)

    private val _callStateEvents = MutableStateFlow(CallViewModel.State.CALL_PENDING)
    val callStateEvents = _callStateEvents.asSharedFlow()
    private val _recipientEvents = MutableStateFlow(StateEvent.RecipientUpdate.UNKNOWN)
    val recipientEvents = _recipientEvents.asSharedFlow()
    private var localCameraState: CameraState = CameraState.UNKNOWN

    private val _audioDeviceEvents = MutableStateFlow(
        StateEvent.AudioDeviceUpdate(
            SignalAudioManager.AudioDevice.NONE,
            setOf()
        )
    )
    val audioDeviceEvents = _audioDeviceEvents.asSharedFlow()

    val currentConnectionState
        get() = stateProcessor.currentState

    val currentCallState
        get() = _callStateEvents.value

    private var iceState = PeerConnection.IceConnectionState.CLOSED

    private var eglBase: EglBase? = null

    //SteveJosephh21 -
    private var videoEnabledStatus:Boolean = false

    var pendingOffer: String? = null
    var pendingOfferTime: Long = -1
    var preOfferCallData: PreOffer? = null
    var callId: UUID? = null
    var recipient: Recipient? = null
        set(value) {
            field = value
            _recipientEvents.value = StateEvent.RecipientUpdate(value)
        }
    var callStartTime: Long = -1

    private var peerConnection: PeerConnectionWrapper? = null
    private var dataChannel: DataChannel? = null

    private val pendingOutgoingIceUpdates = ArrayDeque<IceCandidate>()
    private val pendingIncomingIceUpdates = ArrayDeque<IceCandidate>()

    private val outgoingIceDebouncer = Debouncer(200L)

    var floatingRenderer: SurfaceViewRenderer? = null
    var remoteRotationSink: RemoteRotationVideoProxySink? = null
    var fullscreenRenderer: SurfaceViewRenderer? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null

    fun clearPendingIceUpdates() {
        pendingOutgoingIceUpdates.clear()
        pendingIncomingIceUpdates.clear()
    }

    fun initializeAudioForCall() {
        Log.d("Beldex","signalAudioManager.handleCommand 1")
        signalAudioManager.handleCommand(AudioManagerCommand.Initialize)
    }

    fun startOutgoingRinger(ringerType: OutgoingRinger.Type) {
        if (ringerType == OutgoingRinger.Type.RINGING) {
            Log.d("Beldex","signalAudioManager.handleCommand 2")
            signalAudioManager.handleCommand(AudioManagerCommand.UpdateAudioDeviceState)
        }
        Log.d("Beldex","signalAudioManager.handleCommand 3")
        signalAudioManager.handleCommand(AudioManagerCommand.StartOutgoingRinger(ringerType))
    }

    fun silenceIncomingRinger() {
        Log.d("Beldex","signalAudioManager.handleCommand 4")
        signalAudioManager.handleCommand(AudioManagerCommand.SilenceIncomingRinger)
    }

    fun postConnectionEvent(transition: Event, onSuccess: ()->Unit): Boolean {
        return stateProcessor.processEvent(transition, onSuccess)
    }

    fun postConnectionError(): Boolean {
        return stateProcessor.processEvent(Event.Error)
    }

    fun postViewModelState(newState: CallViewModel.State) {
        Log.d("Beldex", "Posting view model state $newState")
        _callStateEvents.value = newState
    }

    fun isBusy(context: Context, callId: UUID): Boolean {
        // Make sure we have the permission before accessing the callState
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            return (
                    callId != this.callId && (
                            currentConnectionState != CallState.Idle ||
                                    context.getSystemService(TelephonyManager::class.java).callState != TelephonyManager.CALL_STATE_IDLE
                            )
                    )
        }

        return (
                callId != this.callId &&
                        currentConnectionState != CallState.Idle
                )
    }

    fun isPreOffer() = currentConnectionState == CallState.RemotePreOffer

    fun isIdle() = currentConnectionState == CallState.Idle

    fun isCurrentUser(recipient: Recipient) = recipient.address.serialize() == storage.getUserPublicKey()

    fun initializeVideo(context: Context) {
        Util.runOnMainSync {
            val base = EglBase.create()
            eglBase = base
            floatingRenderer = SurfaceViewRenderer(context)
            floatingRenderer?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            fullscreenRenderer = SurfaceViewRenderer(context)
            fullscreenRenderer?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

            remoteRotationSink = RemoteRotationVideoProxySink()


            floatingRenderer?.init(base.eglBaseContext, null)
            /* if(localCameraState.activeDirection == CameraState.Direction.FRONT)
             {
                 floatingRenderer?.setMirror(false)
             }
             else{
                 floatingRenderer?.setMirror(true)
             }*/
            fullscreenRenderer?.init(base.eglBaseContext, null)
            remoteRotationSink!!.setSink(fullscreenRenderer!!)

            val encoderFactory = DefaultVideoEncoderFactory(base.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(base.eglBaseContext)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(object: PeerConnectionFactory.Options() {
                    init {
                        networkIgnoreMask = 1 shl 4
                    }
                })
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()
        }
    }

    fun callEnded() {
        peerConnection?.dispose()
        peerConnection = null
    }

    fun setAudioEnabled(isEnabled: Boolean) {
        currentConnectionState.withState(*CallState.CAN_HANGUP_STATES) {
            peerConnection?.setAudioEnabled(isEnabled)
            _audioEvents.value = StateEvent.AudioEnabled(true)
        }
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState) {
        peerConnectionObservers.forEach { listener -> listener.onSignalingChange(newState) }
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
        Log.d("Beldex", "New ice connection state = $newState")
        iceState = newState
        peerConnectionObservers.forEach { listener -> listener.onIceConnectionChange(newState) }
        if (newState == PeerConnection.IceConnectionState.CONNECTED) {
            callStartTime = System.currentTimeMillis()
        }
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        peerConnectionObservers.forEach { listener -> listener.onIceConnectionReceivingChange(receiving) }
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
        peerConnectionObservers.forEach { listener -> listener.onIceGatheringChange(newState) }
    }

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        peerConnectionObservers.forEach { listener -> listener.onIceCandidate(iceCandidate) }
        val expectedCallId = this.callId ?: return
        val expectedRecipient = this.recipient ?: return
        pendingOutgoingIceUpdates.add(iceCandidate)

        if (peerConnection?.readyForIce != true) return

        queueOutgoingIce(expectedCallId, expectedRecipient)
    }

    private fun queueOutgoingIce(expectedCallId: UUID, expectedRecipient: Recipient) {
        outgoingIceDebouncer.publish {
            val currentCallId = this.callId ?: return@publish
            val currentRecipient = this.recipient ?: return@publish
            if (currentCallId == expectedCallId && expectedRecipient == currentRecipient) {
                val currentPendings = mutableSetOf<IceCandidate>()
                while (pendingOutgoingIceUpdates.isNotEmpty()) {
                    currentPendings.add(pendingOutgoingIceUpdates.pop())
                }
                val sdps = currentPendings.map { it.sdp }
                val sdpMLineIndexes = currentPendings.map { it.sdpMLineIndex }
                val sdpMids = currentPendings.map { it.sdpMid }

                MessageSender.sendNonDurably(
                    CallMessage(
                        ICE_CANDIDATES,
                        sdps = sdps,
                        sdpMLineIndexes = sdpMLineIndexes,
                        sdpMids = sdpMids,
                        currentCallId
                    ), currentRecipient.address)
            }
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        peerConnectionObservers.forEach { listener -> listener.onIceCandidatesRemoved(candidates) }
    }

    override fun onAddStream(stream: MediaStream) {
        peerConnectionObservers.forEach { listener -> listener.onAddStream(stream) }
        for (track in stream.audioTracks) {
            track.setEnabled(true)
        }

        if (stream.videoTracks != null && stream.videoTracks.size == 1) {
            val videoTrack = stream.videoTracks.first()
            videoTrack.setEnabled(true)
            videoTrack.addSink(remoteRotationSink)
        }
    }

    override fun onRemoveStream(p0: MediaStream?) {
        peerConnectionObservers.forEach { listener -> listener.onRemoveStream(p0) }
    }

    override fun onDataChannel(p0: DataChannel?) {
        peerConnectionObservers.forEach { listener -> listener.onDataChannel(p0) }
    }

    override fun onRenegotiationNeeded() {
        peerConnectionObservers.forEach { listener -> listener.onRenegotiationNeeded() }
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        peerConnectionObservers.forEach { listener -> listener.onAddTrack(p0, p1) }
    }

    override fun onBufferedAmountChange(l: Long) {
        Log.i(TAG,"onBufferedAmountChange: $l")
    }

    override fun onStateChange() {
        Log.i(TAG,"onStateChange")
    }

    override fun onMessage(buffer: DataChannel.Buffer?) {
        Log.i(TAG,"onMessage...")
        buffer ?: return

        try {
            val byteArray = ByteArray(buffer.data.remaining()) { buffer.data[it] }
            val json = Json.parseToJsonElement(byteArray.decodeToString()) as JsonObject
            if (json.containsKey("video")) {
                _videoState.update { it.copy(remoteVideoEnabled = json["video"]?.jsonPrimitive?.boolean ?: false) }
                handleMirroring()
            } else if (json.containsKey("hangup")) {
                peerConnectionObservers.forEach(WebRtcListener::onHangup)
            }
            //SteveJosephh21 --
            else if (json.containsKey("audio")) {
                _remoteAudioEvents.value =
                    StateEvent.AudioEnabled((json["audio"] as JsonPrimitive).boolean)
            }
            else if (json.containsKey("video_status")) {
                _remoteVideoStatusEvents.value =
                    StateEvent.VideoEnabled((json["video_status"] as JsonPrimitive).boolean)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize data channel message", e)
        }
    }

    override fun onAudioDeviceChanged(activeDevice: SignalAudioManager.AudioDevice, devices: Set<SignalAudioManager.AudioDevice>) {
        _audioDeviceEvents.value = StateEvent.AudioDeviceUpdate(activeDevice, devices)
    }

    fun stop() {
        val isOutgoing = currentConnectionState in CallState.OUTGOING_STATES
        stateProcessor.processEvent(Event.Cleanup) {
            Log.d("Beldex","signalAudioManager.handleCommand 5")
            signalAudioManager.handleCommand(AudioManagerCommand.Stop(true))
            peerConnection?.dispose()
            peerConnection = null

            floatingRenderer?.release()
            remoteRotationSink?.release()
            fullscreenRenderer?.release()
            eglBase?.release()

            floatingRenderer = null
            fullscreenRenderer = null
            eglBase = null

            localCameraState = CameraState.UNKNOWN
            recipient = null
            callId = null
            pendingOfferTime = -1
            pendingOffer = null
            callStartTime = -1
            _audioEvents.value = StateEvent.AudioEnabled(false)
            _videoState.value = VideoState(
                swapped = false,
                userVideoEnabled = false,
                remoteVideoEnabled = false
            )
            //SteveJosephh21
            _remoteAudioEvents.value =StateEvent.AudioEnabled(true)
            _remoteVideoStatusEvents.value = StateEvent.VideoEnabled(false)

            pendingOutgoingIceUpdates.clear()
            pendingIncomingIceUpdates.clear()
        }
    }

    override fun onCameraSwitchCompleted(newCameraState: CameraState) {
        localCameraState = newCameraState
        handleMirroring()
    }

    fun onPreOffer(callId: UUID, recipient: Recipient, onSuccess: () -> Unit) {
        stateProcessor.processEvent(Event.ReceivePreOffer) {
            if (preOfferCallData != null) {
                Log.d(TAG, "Received new pre-offer when we are already expecting one")
            }
            this.recipient = recipient
            this.callId = callId
            preOfferCallData = PreOffer(callId, recipient)
            onSuccess()
        }
    }

    fun onNewOffer(offer: String, callId: UUID, recipient: Recipient): Promise<Unit, Exception> {
        if (callId != this.callId) return Promise.ofFail(NullPointerException("No callId"))
        if (recipient != this.recipient) return Promise.ofFail(NullPointerException("No recipient"))

        val connection = peerConnection ?: return Promise.ofFail(NullPointerException("No peer connection wrapper"))

        val reconnected = stateProcessor.processEvent(Event.ReceiveOffer) && stateProcessor.processEvent(Event.SendAnswer)
        return if (reconnected) {
            Log.i("Beldex", "Handling new offer, restarting ice bchat")
            connection.setNewRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offer))
            // re-established an ice
            val answer = connection.createAnswer(MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
            })
            connection.setLocalDescription(answer)
            pendingIncomingIceUpdates.toList().forEach { update ->
                connection.addIceCandidate(update)
            }
            pendingIncomingIceUpdates.clear()
            val answerMessage = CallMessage.answer(answer.description, callId)
            Log.i("Beldex", "Posting new answer")
            MessageSender.sendNonDurably(answerMessage, recipient.address)
        } else {
            Promise.ofFail(Exception("Couldn't reconnect from current state"))
        }
    }

    fun onIncomingRing(offer: String, callId: UUID, recipient: Recipient, callTime: Long, onSuccess: () -> Unit) {
        postConnectionEvent(Event.ReceiveOffer) {
            this.callId = callId
            this.recipient = recipient
            this.pendingOffer = offer
            this.pendingOfferTime = callTime
            Log.d("Beldex","signalAudioManager.handleCommand 1(3)")
            initializeAudioForCall()
            startIncomingRinger()
            onSuccess()
        }
    }

    fun onIncomingCall(context: Context, isAlwaysTurn: Boolean = false): Promise<Unit, Exception> {
        val callId = callId ?: return Promise.ofFail(NullPointerException("callId is null"))
        val recipient = recipient ?: return Promise.ofFail(NullPointerException("recipient is null"))
        val offer = pendingOffer ?: return Promise.ofFail(NullPointerException("pendingOffer is null"))
        val factory = peerConnectionFactory ?: return Promise.ofFail(NullPointerException("peerConnectionFactory is null"))
        val local = floatingRenderer ?: return Promise.ofFail(NullPointerException("localRenderer is null"))
        val base = eglBase ?: return Promise.ofFail(NullPointerException("eglBase is null"))
        val connection = PeerConnectionWrapper(
            context,
            factory,
            this,
            local,
            this,
            base,
            isAlwaysTurn
        )
        peerConnection = connection
        localCameraState = connection.getCameraState()
        val dataChannel = connection.createDataChannel(DATA_CHANNEL_NAME)
        this.dataChannel = dataChannel
        dataChannel.registerObserver(this)
        connection.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offer))
        val answer = connection.createAnswer(MediaConstraints())
        connection.setLocalDescription(answer)
        val answerMessage = CallMessage.answer(answer.description, callId)
        val userAddress = storage.getUserPublicKey() ?: return Promise.ofFail(NullPointerException("No user public key"))
        MessageSender.sendNonDurably(answerMessage, Address.fromSerialized(userAddress))
        val sendAnswerMessage = MessageSender.sendNonDurably(CallMessage.answer(
            answer.description,
            callId
        ), recipient.address)

        insertCallMessage(recipient.address.serialize(), CallMessageType.CALL_INCOMING, false)

        while (pendingIncomingIceUpdates.isNotEmpty()) {
            val candidate = pendingIncomingIceUpdates.pop() ?: break
            connection.addIceCandidate(candidate)
        }
        return sendAnswerMessage.success {
            pendingOffer = null
            pendingOfferTime = -1
        }
    }

    fun onOutgoingCall(context: Context, isAlwaysTurn: Boolean = false): Promise<Unit, Exception> {
        val callId = callId ?: return Promise.ofFail(NullPointerException("callId is null"))
        val recipient = recipient
            ?: return Promise.ofFail(NullPointerException("recipient is null"))
        val factory = peerConnectionFactory
            ?: return Promise.ofFail(NullPointerException("peerConnectionFactory is null"))
        val local = floatingRenderer
            ?: return Promise.ofFail(NullPointerException("localRenderer is null"))
        val base = eglBase ?: return Promise.ofFail(NullPointerException("eglBase is null"))

        val sentOffer = stateProcessor.processEvent(Event.SendOffer)

        if (!sentOffer) {
            return Promise.ofFail(Exception("Couldn't transition to sent offer state"))
        } else {
            val connection = PeerConnectionWrapper(
                context,
                factory,
                this,
                local,
                this,
                base,
                isAlwaysTurn
            )

            peerConnection = connection
            localCameraState = connection.getCameraState()
            val dataChannel = connection.createDataChannel(DATA_CHANNEL_NAME)
            dataChannel.registerObserver(this)
            this.dataChannel = dataChannel
            val offer = connection.createOffer(MediaConstraints())
            connection.setLocalDescription(offer)

            Log.d("Beldex", "Sending pre-offer")
            return MessageSender.sendNonDurably(CallMessage.preOffer(
                callId
            ), recipient.address).bind {
                Log.d("Beldex", "Sent pre-offer")
                Log.d("Beldex", "Sending offer")
                MessageSender.sendNonDurably(CallMessage.offer(
                    offer.description,
                    callId
                ), recipient.address).success {
                    Log.d("Beldex", "Sent offer")
                }.fail {
                    Log.e("Beldex", "Failed to send offer", it)
                }
            }
        }
    }

    fun handleDenyCall() {
        val callId = callId ?: return
        val recipient = recipient ?: return
        val userAddress = storage.getUserPublicKey() ?: return
        stateProcessor.processEvent(Event.DeclineCall) {
            MessageSender.sendNonDurably(CallMessage.endCall(callId), Address.fromSerialized(userAddress))
            MessageSender.sendNonDurably(CallMessage.endCall(callId), recipient.address)
            insertCallMessage(recipient.address.serialize(), CallMessageType.CALL_MISSED)
        }
    }

    fun handleLocalHangup(intentRecipient: Recipient?) {
        val recipient = recipient ?: return
        val callId = callId ?: return

        val currentUserPublicKey  = storage.getUserPublicKey()
        val sendHangup = intentRecipient == null || (intentRecipient == recipient && recipient.address.serialize() != currentUserPublicKey)

        postViewModelState(CallViewModel.State.CALL_DISCONNECTED)
        stateProcessor.processEvent(Event.Hangup)
        if (sendHangup) {
            dataChannel?.let { channel ->
                val buffer = DataChannel.Buffer(ByteBuffer.wrap(HANGUP_JSON.toString().encodeToByteArray()), false)
                channel.send(buffer)
            }
            MessageSender.sendNonDurably(CallMessage.endCall(callId), recipient.address)
        }
    }

    fun insertCallMessage(threadPublicKey: String, callMessageType: CallMessageType, signal: Boolean = false, sentTimestamp: Long = MnodeAPI.nowWithOffset) {
        storage.insertCallMessage(threadPublicKey, callMessageType, sentTimestamp)
    }

    fun handleRemoteHangup() {
        when (currentConnectionState) {
            CallState.LocalRing,
            CallState.RemoteRing -> postViewModelState(CallViewModel.State.RECIPIENT_UNAVAILABLE)
            else -> postViewModelState(CallViewModel.State.CALL_DISCONNECTED)
        }
        if (!stateProcessor.processEvent(Event.Hangup)) {
            Log.e("Beldex", "")
            stateProcessor.processEvent(Event.Error)
        }
    }

    fun swapVideos() {
        // update the state
        _videoState.update { it.copy(swapped = !it.swapped) }
        handleMirroring()
        if (_videoState.value.swapped) {
            peerConnection?.rotationVideoSink?.setSink(fullscreenRenderer)
            floatingRenderer?.let{remoteRotationSink?.setSink(it) }
        } else {
            peerConnection?.rotationVideoSink?.setSink(floatingRenderer)
            fullscreenRenderer?.let { remoteRotationSink?.setSink(it) }
        }
    }

    fun handleSetMuteAudio(muted: Boolean) {
        _audioEvents.value = StateEvent.AudioEnabled(!muted)
        peerConnection?.setAudioEnabled(!muted)
        //SteveJosephh21 --
        dataChannel?.let { channel ->
            val toSend = if (muted) AUDIO_DISABLED_JSON else AUDIO_ENABLED_JSON
            val buffer = DataChannel.Buffer(ByteBuffer.wrap(toSend.toString().encodeToByteArray()), false)
            channel.send(buffer)
        }
    }

    /**
     * Returns the renderer currently showing the user's video, not the contact's
     */
    private fun getUserRenderer() = if(_videoState.value.swapped) fullscreenRenderer else floatingRenderer
    /**
     * Returns the renderer currently showing the contact's video, not the user's
     */
    private fun getRemoteRenderer() = if(_videoState.value.swapped) floatingRenderer else fullscreenRenderer
    /**
     * Makes sure the user's renderer applies mirroring if necessary
     */
    private fun handleMirroring() {
        val videoState = _videoState.value
        // if we have user video and the camera is front facing, make sure to mirror stream
        if(videoState.userVideoEnabled) {
            getUserRenderer()?.setMirror(isCameraFrontFacing())
        }
        // the remote video is never mirrored
        if(videoState.remoteVideoEnabled){
            getRemoteRenderer()?.setMirror(false)
        }
    }

    fun handleSetMuteVideo(muted: Boolean, lockManager: LockManager) {
        _videoState.update { it.copy(userVideoEnabled = !muted) }
        handleMirroring()

        val connection = peerConnection ?: return
        connection.setVideoEnabled(!muted)
        dataChannel?.let { channel ->
            val toSend = if (muted) VIDEO_DISABLED_JSON else VIDEO_ENABLED_JSON
            val buffer = DataChannel.Buffer(ByteBuffer.wrap(toSend.toString().encodeToByteArray()), false)
            channel.send(buffer)
        }

        if (currentConnectionState == CallState.Connected) {
            if (connection.isVideoEnabled()) lockManager.updatePhoneState(LockManager.PhoneState.IN_VIDEO)
            else lockManager.updatePhoneState(LockManager.PhoneState.IN_CALL)
        }

        if (localCameraState.enabled
            && !signalAudioManager.isSpeakerphoneOn()
            && !signalAudioManager.isBluetoothScoOn()
            && !signalAudioManager.isWiredHeadsetOn()
        ) {
            Log.d("Beldex","signalAudioManager.handleCommand 6")
            signalAudioManager.handleCommand(AudioManagerCommand.SetUserDevice(SignalAudioManager.AudioDevice.SPEAKER_PHONE))
        }
    }

    fun handleSetCameraFlip() {
        if (!localCameraState.enabled) return
        peerConnection?.let { connection ->
            connection.flipCamera()
            localCameraState = connection.getCameraState()

            /*if(localCameraState.activeDirection == CameraState.Direction.FRONT)
            {
                localRenderer?.setMirror(false)
                localCameraState.activeDirection = CameraState.Direction.PENDING
            }
            else
            {
                localRenderer?.setMirror(true)
                localCameraState.activeDirection = CameraState.Direction.FRONT
            }*/
        }
    }

    fun setDeviceOrientation(orientation: Orientation) {
        // set rotation to the video based on the device's orientation and the camera facing direction
        val rotation = when (orientation) {
            Orientation.PORTRAIT -> 0
            Orientation.LANDSCAPE -> if (isCameraFrontFacing()) 90 else -90
            Orientation.REVERSED_LANDSCAPE -> 270
            else -> 0
        }
        // apply the rotation to the streams
        peerConnection?.setDeviceRotation(rotation)
        remoteRotationSink?.rotation = rotation
    }

    fun handleWiredHeadsetChanged(present: Boolean) {
        if (currentConnectionState in arrayOf(CallState.Connected,
                CallState.LocalRing,
                CallState.RemoteRing)) {
            if (present && signalAudioManager.isSpeakerphoneOn()) {
                Log.d("Beldex","signalAudioManager.handleCommand 7")
                signalAudioManager.handleCommand(AudioManagerCommand.SetUserDevice(
                    SignalAudioManager.AudioDevice.WIRED_HEADSET))
            } else if (!present && !signalAudioManager.isSpeakerphoneOn() && !signalAudioManager.isBluetoothScoOn() && localCameraState.enabled) {
                Log.d("Beldex","signalAudioManager.handleCommand 8")
                signalAudioManager.handleCommand(AudioManagerCommand.SetUserDevice(
                    SignalAudioManager.AudioDevice.SPEAKER_PHONE))
            }
        }
    }

    fun handleScreenOffChange(context: Context) {
        if (currentConnectionState in arrayOf(CallState.Connecting, CallState.LocalRing)) {
            Log.d("Beldex","signalAudioManager.handleCommand 9")
            signalAudioManager.handleCommand(AudioManagerCommand.SilenceIncomingRinger)
        }
        if(currentConnectionState in arrayOf(CallState.Connected)){
            val connection = peerConnection
            if(connection?.isVideoEnabled() == true){
                videoEnabledStatus = true
                dataChannel?.let { channel ->
                    val toSend = VIDEO_STATUS_ENABLED_JSON
                    val buffer = DataChannel.Buffer(ByteBuffer.wrap(toSend.toString().encodeToByteArray()), false)
                    channel.send(buffer)
                }
                val intent = WebRtcCallService.cameraEnabled(context, false)
                context.startService(intent)
            }
        }
    }

    //SteveJosephh21 -
    fun handleScreenOnChange(context: Context) {
        if (currentConnectionState in arrayOf(CallState.Connected)) {
            if(videoEnabledStatus){
                videoEnabledStatus = false
                dataChannel?.let { channel ->
                    val toSend = VIDEO_STATUS_DISABLED_JSON
                    val buffer = DataChannel.Buffer(ByteBuffer.wrap(toSend.toString().encodeToByteArray()), false)
                    channel.send(buffer)
                }
                val intent = WebRtcCallService.cameraEnabled(context, true)
                context.startService(intent)
            }
        }
    }

    fun handleResponseMessage(recipient: Recipient, callId: UUID, answer: SessionDescription) {
        if (recipient != this.recipient || callId != this.callId) {
            Log.w(TAG,"Got answer for recipient and call ID we're not currently dialing")
            return
        }

        stateProcessor.processEvent(Event.ReceiveAnswer) {
            val connection = peerConnection ?: throw AssertionError("assert")

            connection.setRemoteDescription(answer)
            while (pendingIncomingIceUpdates.isNotEmpty()) {
                connection.addIceCandidate(pendingIncomingIceUpdates.pop())
            }
            queueOutgoingIce(callId, recipient)
        }
    }

    fun handleRemoteIceCandidate(iceCandidates: List<IceCandidate>, callId: UUID) {
        if (callId != this.callId) {
            Log.w(TAG, "Got remote ice candidates for a call that isn't active")
            return
        }

        val connection = peerConnection
        if (connection != null && connection.readyForIce && currentConnectionState != CallState.Reconnecting) {
            Log.i("Beldex", "Handling connection ice candidate")
            iceCandidates.forEach { candidate ->
                connection.addIceCandidate(candidate)
            }
        } else {
            Log.i("Beldex", "Handling add to pending ice candidate")
            pendingIncomingIceUpdates.addAll(iceCandidates)
        }
    }

    fun startIncomingRinger() {
        Log.d("Beldex","signalAudioManager.handleCommand 10")
        signalAudioManager.handleCommand(AudioManagerCommand.StartIncomingRinger(true))
    }

    fun startCommunication(lockManager: LockManager) {
        Log.d("Beldex","signalAudioManager.handleCommand 11")
        signalAudioManager.handleCommand(AudioManagerCommand.Start)
        val connection = peerConnection ?: return
        if (connection.isVideoEnabled()) lockManager.updatePhoneState(LockManager.PhoneState.IN_VIDEO)
        else lockManager.updatePhoneState(LockManager.PhoneState.IN_CALL)
        connection.setCommunicationMode()
        setAudioEnabled(true)
        dataChannel?.let { channel ->
            val toSend = if (_videoState.value.userVideoEnabled) VIDEO_ENABLED_JSON else VIDEO_DISABLED_JSON
            val buffer = DataChannel.Buffer(ByteBuffer.wrap(toSend.toString().encodeToByteArray()), false)
            channel.send(buffer)
        }
    }

    fun handleAudioCommand(audioCommand: AudioManagerCommand) {
        Log.d("Beldex","signalAudioManager.handleCommand 12")
        signalAudioManager.handleCommand(audioCommand)
    }

    fun networkReestablished() {
        val connection = peerConnection ?: return
        val callId = callId ?: return
        val recipient = recipient ?: return

        postConnectionEvent(Event.NetworkReconnect) {
            Log.d("Beldex", "start re-establish")

            val offer = connection.createOffer(MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
            })
            connection.setLocalDescription(offer)

            MessageSender.sendNonDurably(CallMessage.offer(offer.description, callId), recipient.address)
        }
    }

    fun isInitiator(): Boolean = peerConnection?.isInitiator() == true

    fun isCameraFrontFacing() = localCameraState.activeDirection != CameraState.Direction.BACK

    interface WebRtcListener: PeerConnection.Observer {
        fun onHangup()
    }


}