package io.beldex.bchat.util

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.beldex.libsignal.utilities.Broadcaster

class Broadcaster(private val context: Context) : Broadcaster {

    override fun broadcast(event: String) {
        val intent = Intent(event)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    override fun broadcast(event: String, long: Long) {
        val intent = Intent(event)
        intent.putExtra("long", long)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}