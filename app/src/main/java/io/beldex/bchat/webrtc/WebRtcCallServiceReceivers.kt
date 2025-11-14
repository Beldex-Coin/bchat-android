package io.beldex.bchat.webrtc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.beldex.libsignal.utilities.Log
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.webrtc.locks.LockManager
import javax.inject.Inject

class PowerButtonReceiver(val onScreenOffChange: ()->Unit, val onScreenOnChange: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_SCREEN_OFF == intent.action) {
            try {
                /*val serviceIntent = Intent(context, WebRtcCallService::class.java)
                    .setAction(WebRtcCallService.ACTION_SCREEN_OFF)
                if (getCallisActive(ApplicationContext.getInstance(context))) {
                    ContextCompat.startForegroundService(context, serviceIntent)
                }*/

                onScreenOffChange()
            }catch (e:Exception){
                Log.d("WebRtcCallServiceReceivers ", "ACTION_SCREEN_OFF $e")
            }
        }

        //SteveJosephh21 -
        if (Intent.ACTION_USER_PRESENT == intent.action) {
            try {
                /*val serviceIntent = Intent(context, WebRtcCallService::class.java)
                    .setAction(WebRtcCallService.ACTION_SCREEN_ON)
                if (getCallisActive(ApplicationContext.getInstance(context))) {
                    ContextCompat.startForegroundService(context, serviceIntent)
                }*/

                onScreenOnChange()
            } catch (e:Exception){
                Log.d("WebRtcCallServiceReceivers ", "ACTION_USER_PRESENT $e")
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

class WiredHeadsetStateReceiver(val onWiredHeadsetChanged: (Boolean)->Unit): BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra("state", -1)
        onWiredHeadsetChanged(state != 0)
    }
}
@AndroidEntryPoint
class EndCallReceiver(): BroadcastReceiver() {
    @Inject
    lateinit var webRtcCallBridge : WebRtcCallBridge

    override fun onReceive(context : Context, intent : Intent) {
        when (intent.action) {
            WebRtcCallBridge.ACTION_LOCAL_HANGUP -> {
                webRtcCallBridge.handleLocalHangup(null)
            }

            WebRtcCallBridge.ACTION_IGNORE_CALL -> {
                webRtcCallBridge.handleIgnoreCall()
            }

            else -> webRtcCallBridge.handleDenyCall()
        }
    }
}