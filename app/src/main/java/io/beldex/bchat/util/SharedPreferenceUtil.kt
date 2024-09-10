package io.beldex.bchat.util

import android.content.Context
import android.content.SharedPreferences

class SharedPreferenceUtil(
    private val context: Context
) {

    companion object {
        const val NODES_PREFS_NAME = "nodes"
        const val SELECTED_NODE_PREFS_NAME = "selected_node"
        const val PREF_DAEMON_TESTNET = "daemon_testnet"
        const val PREF_DAEMON_STAGENET = "daemon_stagenet"
        const val PREF_DAEMON_MAINNET = "daemon_mainnet"
    }

    private fun getPreference(prefKey: String, mode: Int): SharedPreferences {
        return context.getSharedPreferences(prefKey, mode)
    }

    fun getStoredNodes(): SharedPreferences {
        return getPreference(NODES_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedNodeId(): String? {
        return context.getSharedPreferences(SELECTED_NODE_PREFS_NAME, Context.MODE_PRIVATE).getString("0", null)
    }
}