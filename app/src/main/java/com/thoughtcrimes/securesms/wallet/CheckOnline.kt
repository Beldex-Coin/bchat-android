package com.thoughtcrimes.securesms.wallet

import android.content.ContentValues
import android.content.Context
import android.net.*
import android.os.Build
import com.beldex.libsignal.utilities.Log

import android.net.ConnectivityManager

import androidx.annotation.RequiresApi
import java.io.IOException
import java.net.InetSocketAddress
import javax.net.SocketFactory


class CheckOnline {

    companion object {
        @RequiresApi(Build.VERSION_CODES.N)
        fun isOnline(context: Context): Boolean {


            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            var hasInternet = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(object :
                    ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        hasInternet = DoesNetworkHaveInternet.execute(network.socketFactory)
                        Log.e("Internet", "check internet hasInternet first: $hasInternet")
                    }
                })
            } else {
                hasInternet = true
            }

           // Thread.sleep(100L)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return hasInternet
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        Log.e("Internet", "check internet hasInternet first 1: $hasInternet")
                        Log.e("Internet", "check internet hasInternet first 2: $hasInternet")
                        return hasInternet
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return hasInternet
                    }
                }
            }
            return false
        }

    }

    object DoesNetworkHaveInternet {
        fun execute(socketFactory: SocketFactory): Boolean {
            return try {
               Log.d("Beldex", "PINGING Google...")
                val socket = socketFactory.createSocket() ?: throw IOException("Socket is null.")
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                socket.close()
                true
            } catch (e: IOException) {
                false
            }
        }
    }


}

