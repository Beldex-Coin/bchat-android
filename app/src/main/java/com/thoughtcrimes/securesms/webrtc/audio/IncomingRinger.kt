package com.thoughtcrimes.securesms.webrtc.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Vibrator
import com.beldex.libbchat.utilities.ServiceUtil
import com.beldex.libsignal.utilities.Log
import android.media.Ringtone
import android.net.Uri
import org.webrtc.ContextUtils.getApplicationContext


class IncomingRinger(private val context: Context) {
    companion object {
        const val TAG = "IncomingRinger"
        val PATTERN = longArrayOf(0L, 1000L, 1000L)
    }

    private val vibrator: Vibrator? = ServiceUtil.getVibrator(context)
    var mediaPlayer: MediaPlayer? = null

    val isRinging: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    fun start(vibrate: Boolean) {
        val audioManager = ServiceUtil.getAudioManager(context)
        mediaPlayer?.release()
        mediaPlayer = createMediaPlayer()
        val ringerMode = audioManager.ringerMode

        if (shouldVibrate(mediaPlayer, ringerMode, vibrate)) {
            Log.i(TAG,"Starting vibration")
            vibrator?.vibrate(PATTERN, 1)
        } else {
            Log.i(TAG,"Skipping vibration")
        }

        mediaPlayer?.let { player ->
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                try {
                    if (!player.isPlaying) {
                        player.prepare()
                        player.start()
                        Log.i(TAG,"Playing ringtone")
                    }
                } catch (e: Exception) {
                    Log.e(TAG,"Failed to start mediaPlayer", e)
                }
            }
        } ?: run {
            Log.w(TAG,"Not ringing, mediaPlayer: ${mediaPlayer?.let{"available"}}, mode: $ringerMode")
        }

    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun shouldVibrate(player: MediaPlayer?, ringerMode: Int, vibrate: Boolean): Boolean {
        player ?: return true

        if (vibrator == null || !vibrator.hasVibrator()) return false

        return if (vibrate) ringerMode != AudioManager.RINGER_MODE_SILENT
        else ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }

    private fun createMediaPlayer(): MediaPlayer? {
        try {
            val defaultRingtone = try {
                RingtoneManager.getDefaultUri( RingtoneManager.TYPE_RINGTONE)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get default system ringtone", e)
                null
            } ?: return null

            try {
                val mediaPlayer = MediaPlayer()
               mediaPlayer.setDataSource(context, defaultRingtone)
                return mediaPlayer
            } catch (e: SecurityException) {
                Log.w(TAG, "Failed to create player with ringtone the normal way", e)
            }
        } catch (e: Exception) {
            Log.e(TAG,"Failed to create mediaPlayer")
        }

        return null
    }



}