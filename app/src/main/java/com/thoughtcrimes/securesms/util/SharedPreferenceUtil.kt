package com.thoughtcrimes.securesms.util

import android.content.Context
import android.content.SharedPreferences
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.data.NodeInfo

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

    fun saveFavourites(favouriteNodes: HashSet<NodeInfo>) {
        val editor = context.getSharedPreferences(NODES_PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.clear()
        var i = 1
        for (info in favouriteNodes) {
            val nodeString = info.toNodeString()
            editor.putString(i.toString(), nodeString)
            i++
        }
        editor.apply()
    }

    fun saveSelectedNode(nodeInfo: NodeInfo?) { // save only if changed
        val selectedNodeId = getSelectedNodeId()
        if (nodeInfo != null) {
            if (!nodeInfo.toNodeString().equals(selectedNodeId)) saveNode(nodeInfo)
        } else {
            if (selectedNodeId != null) saveNode(null)
        }
    }

    private fun saveNode(nodeInfo: NodeInfo?) {
        val editor = context.getSharedPreferences(SELECTED_NODE_PREFS_NAME, Context.MODE_PRIVATE).edit()
        if (nodeInfo == null) {
            editor.clear()
        } else {
            editor.putString("0", nodeInfo.toNodeString())
        }
        editor.apply()
    }

    fun getProfileName(): String? {
        return TextSecurePreferences.getProfileName(context)
    }

    fun getPublicKey(): String {
        return TextSecurePreferences.getLocalNumber(context)!!
    }

    fun getSavedPassword(): String? {
        return TextSecurePreferences.getMyPassword(context)
    }

    fun setPassword(pinCode: String) {
        TextSecurePreferences.setMyPassword(context, pinCode)
    }

}