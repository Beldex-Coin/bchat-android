package io.beldex.bchat.webrtc.audio

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.app.ActivityCompat
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.webrtc.AudioManagerCommand
import java.util.concurrent.TimeUnit

/**
 * Manages the bluetooth lifecycle with a headset. This class doesn't make any
 * determination on if bluetooth should be used. It determines if a device is connected,
 * reports that to the [SignalAudioManager], and then handles connecting/disconnecting
 * to the device if requested by [SignalAudioManager].
 */
class SignalBluetoothManager(
    private val context: Context,
    private val audioManager: SignalAudioManager,
    private val androidAudioManager: AudioManagerCompat,
    private val handler: SignalAudioHandler
) {

    var state: State = State.UNINITIALIZED
        get() {
            handler.assertHandlerThread()
            return field
        }
        private set

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private var scoConnectionAttempts = 0

    private val bluetoothListener = BluetoothServiceListener()
    private var bluetoothReceiver: BluetoothHeadsetBroadcastReceiver? = null

    private val bluetoothTimeout = { onBluetoothTimeout() }

    fun start() {
        handler.assertHandlerThread()

        Log.d(TAG, "start(): $state")

        if (state != State.UNINITIALIZED) {
            Log.w(TAG, "Invalid starting state")
            return
        }

        bluetoothHeadset = null
        scoConnectionAttempts = 0

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.i(TAG, "Device does not support Bluetooth")
            return
        }

        if (!androidAudioManager.isBluetoothScoAvailableOffCall) {
            Log.w(TAG, "Bluetooth SCO audio is not available off call")
            return
        }

        if (bluetoothAdapter?.getProfileProxy(context, bluetoothListener, BluetoothProfile.HEADSET) != true) {
            Log.e(TAG, "BluetoothAdapter.getProfileProxy(HEADSET) failed")
            return
        }

        val bluetoothHeadsetFilter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }

        bluetoothReceiver = BluetoothHeadsetBroadcastReceiver()
        context.registerReceiver(bluetoothReceiver, bluetoothHeadsetFilter)

        Log.i(TAG, "Bluetooth proxy for headset profile has started")
        state = State.AVAILABLE
    }

    fun stop() {
        handler.assertHandlerThread()

        Log.d(TAG, "stop(): state: $state")

        if (bluetoothAdapter == null) {
            return
        }

        stopScoAudio()

        if (state == State.UNINITIALIZED) {
            return
        }

        bluetoothReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e(TAG,"error unregistering bluetoothReceiver", e)
            }
        }
        bluetoothReceiver = null

        cancelTimer()

        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        bluetoothHeadset = null

        bluetoothAdapter = null
        state = State.UNINITIALIZED
    }

    fun startScoAudio(): Boolean {
        handler.assertHandlerThread()

        Log.i(TAG, "startScoAudio(): $state attempts: $scoConnectionAttempts")

        if (scoConnectionAttempts >= MAX_CONNECTION_ATTEMPTS) {
            Log.w(TAG, "SCO connection attempts maxed out")
            return false
        }

        if (state != State.AVAILABLE) {
            Log.w(TAG, "SCO connection failed as no headset available")
            return false
        }

        state = State.CONNECTING
        androidAudioManager.startBluetoothSco()
        scoConnectionAttempts++
        startTimer()

        return true
    }

    fun stopScoAudio() {
        handler.assertHandlerThread()

        Log.i(TAG, "stopScoAudio(): $state")

        if (state != State.CONNECTING && state != State.CONNECTED) {
            return
        }

        cancelTimer()
        androidAudioManager.stopBluetoothSco()
        androidAudioManager.isBluetoothScoOn = false
        state = State.AVAILABLE
    }

    fun updateDevice() {
        handler.assertHandlerThread()

        Log.d(TAG, "updateDevice(): state: $state")

        if (state == State.UNINITIALIZED || bluetoothHeadset == null || ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (bluetoothAdapter!!.getProfileConnectionState(BluetoothProfile.HEADSET) !in arrayOf(
                BluetoothProfile.STATE_CONNECTED)) {
            state = State.UNAVAILABLE
            Log.i(TAG, "No connected bluetooth headset")
        } else {
            state = State.AVAILABLE
            Log.i(TAG, "Connected bluetooth headset.")
        }
    }

    private fun updateAudioDeviceState() {
        audioManager.handleCommand(AudioManagerCommand.UpdateAudioDeviceState)
    }

    private fun startTimer() {
        handler.postDelayed(bluetoothTimeout, SCO_TIMEOUT)
    }

    private fun cancelTimer() {
        handler.removeCallbacks(bluetoothTimeout)
    }

    private fun onBluetoothTimeout() {
        Log.i(TAG, "onBluetoothTimeout: state: $state bluetoothHeadset: $bluetoothHeadset")

        if (state == State.UNINITIALIZED || bluetoothHeadset == null || state != State.CONNECTING) {
            return
        }

        var scoConnected = false

        if (audioManager.isBluetoothScoOn()) {
            Log.d(TAG, "Connected with device")
            scoConnected = true
        } else {
            Log.d(TAG, "Not connected with device")
        }

        if (scoConnected) {
            Log.i(TAG, "Device actually connected and not timed out")
            state = State.CONNECTED
            scoConnectionAttempts = 0
        } else {
            Log.w(TAG, "Failed to connect after timeout")
            stopScoAudio()
        }

        updateAudioDeviceState()
    }

    private fun onServiceConnected(proxy: BluetoothHeadset?) {
        bluetoothHeadset = proxy
        androidAudioManager.isBluetoothScoOn = true
        updateAudioDeviceState()
    }

    private fun onServiceDisconnected() {
        stopScoAudio()
        bluetoothHeadset = null
        state = State.UNAVAILABLE
        updateAudioDeviceState()
    }

    private fun onHeadsetConnectionStateChanged(connectionState: Int) {
        Log.i(TAG, "onHeadsetConnectionStateChanged: state: $state connectionState: ${connectionState.toStateString()}")

        when (connectionState) {
            BluetoothHeadset.STATE_CONNECTED -> {
                scoConnectionAttempts = 0
                updateAudioDeviceState()
            }
            BluetoothHeadset.STATE_DISCONNECTED -> {
                stopScoAudio()
                updateAudioDeviceState()
            }
        }
    }

    private fun onAudioStateChanged(audioState: Int, isInitialStateChange: Boolean) {
        Log.i(TAG, "onAudioStateChanged: state: $state audioState: ${audioState.toStateString()} initialSticky: $isInitialStateChange")

        if (audioState == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
            cancelTimer()
            if (state == State.CONNECTING) {
                Log.d(TAG, "Bluetooth audio SCO is now connected")
                state = State.CONNECTED
                scoConnectionAttempts = 0
                updateAudioDeviceState()
            } else {
                Log.w(TAG, "Unexpected state ${audioState.toStateString()}")
            }
        } else if (audioState == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
            Log.d(TAG, "Bluetooth audio SCO is now connecting...")
        } else if (audioState == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
            Log.d(TAG, "Bluetooth audio SCO is now disconnected")
            if (isInitialStateChange) {
                Log.d(TAG, "Ignore ${audioState.toStateString()} initial sticky broadcast.")
                return
            }
            updateAudioDeviceState()
        }
    }

    private inner class BluetoothServiceListener : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HEADSET) {
                handler.post {
                    if (state != State.UNINITIALIZED) {
                        audioManager.bluetoothConnectionStatus = true
                        onServiceConnected(proxy as? BluetoothHeadset)
                    }
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                handler.post {
                    if (state != State.UNINITIALIZED) {
                        audioManager.bluetoothConnectionStatus = false
                        onServiceDisconnected()
                    }
                }
            }
        }
    }

    private inner class BluetoothHeadsetBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED) {
                val connectionState: Int = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED)
                handler.post {
                    if (state != State.UNINITIALIZED) {
                        if(connectionState == BluetoothHeadset.STATE_CONNECTED || connectionState == BluetoothHeadset.STATE_AUDIO_CONNECTED ){
                            state = State.AVAILABLE
                        } else  if(connectionState == BluetoothHeadset.STATE_DISCONNECTED || connectionState == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                            state = State.UNAVAILABLE
                        }
                        onHeadsetConnectionStateChanged(connectionState)
                    }
                }
            } else if (intent.action == BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED) {
//                val connectionState: Int = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED)
//                handler.post {
//                    if (state != State.UNINITIALIZED) {
//                        onAudioStateChanged(connectionState, isInitialStickyBroadcast)
//                    }
//                }
            } else if (intent.action == AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED) {
                val scoState: Int = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.ERROR)
                handler.post {
                    if (state != State.UNINITIALIZED) {
                        onAudioStateChanged(scoState, isInitialStickyBroadcast)
                    }
                }
            }
        }
    }

    enum class State {
        UNINITIALIZED,
        UNAVAILABLE,
        AVAILABLE,
        DISCONNECTING,
        CONNECTING,
        CONNECTED,
        ERROR;

        fun shouldUpdate(): Boolean {
            return this == AVAILABLE || this == UNAVAILABLE || this == DISCONNECTING
        }

        fun hasDevice(): Boolean {
            return this == CONNECTED || this == CONNECTING || this == AVAILABLE
        }
    }

    companion object {
        private val TAG = Log.tag(SignalBluetoothManager::class.java)
        private val SCO_TIMEOUT = TimeUnit.SECONDS.toMillis(4)
        private const val MAX_CONNECTION_ATTEMPTS = 4
    }
}

private fun Int.toStateString(): String {
    return when (this) {
        BluetoothAdapter.STATE_DISCONNECTED -> "DISCONNECTED"
        BluetoothAdapter.STATE_CONNECTED -> "CONNECTED"
        BluetoothAdapter.STATE_CONNECTING -> "CONNECTING"
        BluetoothAdapter.STATE_DISCONNECTING -> "DISCONNECTING"
        BluetoothAdapter.STATE_OFF -> "OFF"
        BluetoothAdapter.STATE_ON -> "ON"
        BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
        BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
        else -> "UNKNOWN"
    }
}

private fun Int.toScoString(): String = when (this) {
    AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> "DISCONNECTED"
    AudioManager.SCO_AUDIO_STATE_CONNECTED -> "CONNECTED"
    AudioManager.SCO_AUDIO_STATE_CONNECTING -> "CONNECTING"
    AudioManager.SCO_AUDIO_STATE_ERROR -> "ERROR"
    else -> "UNKNOWN"
}