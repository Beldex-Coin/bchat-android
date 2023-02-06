package com.thoughtcrimes.securesms.webrtc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.beldex.libsignal.utilities.Log
import com.thoughtcrimes.securesms.service.WebRtcCallService
import com.thoughtcrimes.securesms.webrtc.locks.LockManager

class HangUpRtcOnPstnCallAnsweredListener(private val hangupListener: ()->Unit): PhoneStateListener() {

    companion object {
        private val TAG = Log.tag(HangUpRtcOnPstnCallAnsweredListener::class.java)
    }

    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        super.onCallStateChanged(state, phoneNumber)
        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            hangupListener()
            Log.i(TAG, "Device phone call ended Bchat call.")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
class HangUpRtcTelephonyCallback(private val hangupListener: ()->Unit): TelephonyCallback(), TelephonyCallback.CallStateListener {

    companion object {
        private val TAG = Log.tag(HangUpRtcTelephonyCallback::class.java)
    }

    override fun onCallStateChanged(state: Int) {
        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            hangupListener()
            Log.i(TAG, "Device phone call ended Session call.")
        }
    }
}
class PowerButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_SCREEN_OFF == intent.action) {
            val serviceIntent = Intent(context, WebRtcCallService::class.java)
                .setAction(WebRtcCallService.ACTION_SCREEN_OFF)
            context.startService(serviceIntent)
        }

        //SteveJosephh21 -
        if (Intent.ACTION_USER_PRESENT == intent.action) {
            val serviceIntent = Intent(context, WebRtcCallService::class.java)
                .setAction(WebRtcCallService.ACTION_SCREEN_ON)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    context.startService(serviceIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    Handler(Looper.getMainLooper()).post {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                }
                else -> {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}

class ProximityLockRelease(private val lockManager: LockManager): Thread.UncaughtExceptionHandler {
    companion object {
        private val TAG = Log.tag(ProximityLockRelease::class.java)
    }
    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e(TAG,"Uncaught exception - releasing proximity lock", e)
        lockManager.updatePhoneState(LockManager.PhoneState.IDLE)
    }
}

class WiredHeadsetStateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra("state", -1)
        val serviceIntent = Intent(context, WebRtcCallService::class.java)
            .setAction(WebRtcCallService.ACTION_WIRED_HEADSET_CHANGE)
            .putExtra(WebRtcCallService.EXTRA_AVAILABLE, state != 0)

        context.startService(serviceIntent)
    }
}