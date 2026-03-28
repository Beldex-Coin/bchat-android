package io.beldex.bchat.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import io.beldex.bchat.R
import com.beldex.libbchat.mnode.OnionRequestAPI
import io.beldex.bchat.util.getColorWithID
import io.beldex.bchat.util.toPx

class PathStatusView : View {
    private val broadcastReceivers = mutableListOf<BroadcastReceiver>()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    @ColorInt var mainColor: Int = 0
        set(newValue) { field = newValue; paint.color = newValue }
    @ColorInt var bchatShadowColor: Int = 0
        set(newValue) { field = newValue; paint.setShadowLayer(toPx(8, resources).toFloat(), 0.0f, 0.0f, newValue) }

    private val paint: Paint by lazy {
        val result = Paint()
        result.style = Paint.Style.FILL
        result.isAntiAlias = true
        result
    }
    private var isNetworkAvailable = false

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initialize()
    }

    private fun initialize() {
        update()
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerObservers()
        registerNetworkCallback()
    }

    private fun registerObservers() {
        val buildingPathsReceiver: BroadcastReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                handleBuildingPathsEvent()
            }
        }
        broadcastReceivers.add(buildingPathsReceiver)
        LocalBroadcastManager.getInstance(context).registerReceiver(buildingPathsReceiver, IntentFilter("buildingPaths"))
        val pathsBuiltReceiver: BroadcastReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                handlePathsBuiltEvent()
            }
        }
        broadcastReceivers.add(pathsBuiltReceiver)
        LocalBroadcastManager.getInstance(context).registerReceiver(pathsBuiltReceiver, IntentFilter("pathsBuilt"))
    }

    override fun onDetachedFromWindow() {
        for (receiver in broadcastReceivers) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
        unregisterNetworkCallback()
        super.onDetachedFromWindow()
    }

    private fun registerNetworkCallback() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                post {
                    isNetworkAvailable = true
                    update()
                }
            }

            override fun onLost(network: Network) {
                post {
                    isNetworkAvailable = false
                    update()
                }
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    private fun unregisterNetworkCallback() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
    }

    private fun handleBuildingPathsEvent() { update() }
    private fun handlePathsBuiltEvent() { update() }

    private fun update() {

        val isConnectedToHops = OnionRequestAPI.paths.isNotEmpty()

        if (!isNetworkAvailable) {
            setBackgroundResource(R.drawable.paths_building_dot)
            mainColor = ContextCompat.getColor(context, R.color.clear_red_color)
            bchatShadowColor = ContextCompat.getColor(context, R.color.clear_red_color)

        } else if (isConnectedToHops) {
            setBackgroundResource(R.drawable.accent_dot)
            mainColor = ContextCompat.getColor(context, R.color.button_green)
            bchatShadowColor = ContextCompat.getColor(context, R.color.button_green)

        } else {
            setBackgroundResource(R.drawable.paths_building_dot)
            mainColor = ContextCompat.getColor(context, R.color.clear_red_color)
            bchatShadowColor = ContextCompat.getColor(context, R.color.clear_red_color)
        }

        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawCircle(w / 2, h / 2, w / 2, paint)
        super.onDraw(c)
    }
}