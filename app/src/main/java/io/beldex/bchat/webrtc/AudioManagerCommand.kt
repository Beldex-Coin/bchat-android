package io.beldex.bchat.webrtc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import io.beldex.bchat.webrtc.audio.OutgoingRinger
import io.beldex.bchat.webrtc.audio.SignalAudioManager

@Parcelize
open class AudioManagerCommand: Parcelable {
    @Parcelize
    object Initialize: AudioManagerCommand()

    @Parcelize
    object UpdateAudioDeviceState: AudioManagerCommand()

    @Parcelize
    data class StartOutgoingRinger(val type: OutgoingRinger.Type): AudioManagerCommand()

    @Parcelize
    object SilenceIncomingRinger: AudioManagerCommand()

    @Parcelize
    object Start: AudioManagerCommand()

    @Parcelize
    data class Stop(val playDisconnect: Boolean): AudioManagerCommand()

    @Parcelize
    data class StartIncomingRinger(val vibrate: Boolean): AudioManagerCommand()

    @Parcelize
    data class SetUserDevice(val device: SignalAudioManager.AudioDevice): AudioManagerCommand()

    @Parcelize
    data class SetDefaultDevice(val device: SignalAudioManager.AudioDevice,
                                val clearUserEarpieceSelection: Boolean): AudioManagerCommand()
}