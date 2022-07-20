package com.thoughtcrimes.securesms.webrtc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import com.beldex.libsignal.utilities.Log

class NetworkChangeReceiver(private val onNetworkChangedCallback: (Boolean)->Unit) {

    private val networkList: MutableSet<Network> = mutableSetOf()

    val broadcastDelegate = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
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
        onNetworkChangedCallback(connected)
    }

    fun Context.isConnected() : Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork != null
    }

    fun register(context: Context) {
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        context.registerReceiver(broadcastDelegate, intentFilter)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            cm.registerDefaultNetworkCallback(defaultObserver)
//        } else {
//
//        }
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(broadcastDelegate)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            cm.unregisterNetworkCallback(defaultObserver)
//        } else {
//
//        }
    }

}