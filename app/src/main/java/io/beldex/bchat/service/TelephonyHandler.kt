package io.beldex.bchat.service

import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import io.beldex.bchat.webrtc.HangUpRtcTelephonyCallback
import java.util.concurrent.ExecutorService
internal interface TelephonyHandler {
    fun register(telephonyManager: TelephonyManager)
    fun unregister(telephonyManager: TelephonyManager)
}
internal fun TelephonyHandler(serviceExecutor: ExecutorService, callback: () -> Unit) = TelephonyHandlerV31(serviceExecutor, callback)
@RequiresApi(Build.VERSION_CODES.S)
class TelephonyHandlerV31(private val serviceExecutor: ExecutorService, callback: () -> Unit): TelephonyHandler {
    private val callback = HangUpRtcTelephonyCallback(callback)
    override fun register(telephonyManager: TelephonyManager) {
        telephonyManager.registerTelephonyCallback(serviceExecutor, callback)
    }
    override fun unregister(telephonyManager: TelephonyManager) {
        telephonyManager.unregisterTelephonyCallback(callback)
    }
}
