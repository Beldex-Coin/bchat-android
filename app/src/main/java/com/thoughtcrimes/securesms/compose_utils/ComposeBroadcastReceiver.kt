package com.thoughtcrimes.securesms.compose_utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.localbroadcastmanager.content.LocalBroadcastManager

@Composable
fun ComposeBroadcastReceiver(
    systemAction: String,
    onSystemEvent: (intent: Intent?) -> Unit
) {
    val context = LocalContext.current

    val currentOnSystemEvent by rememberUpdatedState( onSystemEvent )

    DisposableEffect(context, systemAction){

        val intentFilter = IntentFilter( systemAction )

        val receiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                currentOnSystemEvent( intent )
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(
                receiver, intentFilter
        )
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(
                    receiver)
        }
    }
}