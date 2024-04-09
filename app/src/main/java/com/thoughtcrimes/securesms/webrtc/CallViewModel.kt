package com.thoughtcrimes.securesms.webrtc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.thoughtcrimes.securesms.webrtc.audio.SignalAudioManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
public class CallViewModel @Inject constructor(private val callManager: CallManager): ViewModel() {

    enum class State {
        CALL_PENDING,

        CALL_PRE_INIT,
        CALL_INCOMING,
        CALL_OUTGOING,
        CALL_CONNECTED,
        CALL_RINGING,
        CALL_BUSY,
        CALL_DISCONNECTED,
        CALL_RECONNECTING,

        NETWORK_FAILURE,
        RECIPIENT_UNAVAILABLE,
        NO_SUCH_USER,
        UNTRUSTED_IDENTITY,
    }

    val localRenderer: SurfaceViewRenderer?
        get() = callManager.localRenderer

    val remoteRenderer: SurfaceViewRenderer?
        get() = callManager.remoteRenderer

    private var _videoEnabled: Boolean = false

    val videoEnabled: Boolean
        get() = _videoEnabled

    private var _microphoneEnabled: Boolean = true

    val microphoneEnabled: Boolean
        get() = _microphoneEnabled

    private var _isSpeaker: Boolean = false
    private var _isBluetooth: Boolean = false
    private var _bluetoothConnectionState = MutableLiveData<Boolean>()
    val bluetoothConnectionState: LiveData<Boolean> = _bluetoothConnectionState

    fun setBooleanValue(value: Boolean){
        _bluetoothConnectionState.value = value
    }
    val isSpeaker: Boolean
        get() = _isSpeaker
    val isBluetooth: Boolean
        get() = _isBluetooth

    val audioDeviceState
        get() = callManager.audioDeviceEvents
            .onEach {
                _isSpeaker = it.selectedDevice == SignalAudioManager.AudioDevice.SPEAKER_PHONE
            }
    val audioBluetoothDeviceState
        get() = callManager.audioDeviceEvents
                .onEach {
                    _isBluetooth = it.selectedDevice == SignalAudioManager.AudioDevice.BLUETOOTH
                }
    var bluetoothConnectionStatus = false
        get() = callManager.getBluetoothConnectionStatus()

    val localAudioEnabledState
        get() = callManager.audioEvents.map { it.isEnabled }
            .onEach { _microphoneEnabled = it }

    val localVideoEnabledState
        get() = callManager.videoEvents
            .map { it.isEnabled }
            .onEach { _videoEnabled = it }

    val remoteVideoEnabledState
        get() = callManager.remoteVideoEvents.map { it.isEnabled }

    //SteveJosephh21 --
    val remoteAudioEnabledState
        get() = callManager.remoteAudioEvents.map { it.isEnabled }
    //SteveJosephh21 --
    val remoteVideoStatusEnabledState
        get() = callManager.remoteVideoStatusEvents.map { it.isEnabled }

    var deviceRotation: Int = 0
        set(value) {
            field = value
            callManager.setDeviceRotation(value)
        }

    val currentCallState
        get() = callManager.currentCallState

    val callState
        get() = callManager.callStateEvents

    val recipient
        get() = callManager.recipientEvents

    val callStartTime: Long
        get() = callManager.callStartTime

}