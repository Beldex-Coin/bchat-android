package io.beldex.bchat.webrtc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import com.beldex.libsignal.utilities.Log

class NetworkChangeReceiver(private val onNetworkChangedCallback: (Boolean)->Unit) {

    private val networkList: MutableSet<Network> = mutableSetOf()

    private val broadcastDelegate = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("Beldex","Unregister called 2")
            receiveBroadcast(context, intent)
        }
    }

    val defaultObserver = object: ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i("Beldex", "onAvailable: $network")
            networkList += network
            onNetworkChangedCallback(networkList.isNotEmpty())
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            Log.i("Beldex", "onLosing: $network, maxMsToLive: $maxMsToLive")
        }

        override fun onLost(network: Network) {
            Log.i("Beldex", "onLost: $network")
            networkList -= network
            onNetworkChangedCallback(networkList.isNotEmpty())
        }

        override fun onUnavailable() {
            Log.i("Beldex", "onUnavailable")
        }
    }

    fun receiveBroadcast(context: Context, intent: Intent) {
        val connected = context.isConnected()
        Log.i("Beldex", "received broadcast, network connected: $connected")
        Log.d("Beldex","Unregister called 3")
        onNetworkChangedCallback(connected)
    }

    private fun Context.isConnected() : Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork != null
    }

    fun register(context: Context) {
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        context.registerReceiver(broadcastDelegate, intentFilter)
        //val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        //cm.registerDefaultNetworkCallback(defaultObserver)
    }

    fun unregister(context: Context) {
        Log.d("Beldex","Unregister called 1")
        context.unregisterReceiver(broadcastDelegate)
        //val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        //cm.unregisterNetworkCallback(defaultObserver)
    }

}