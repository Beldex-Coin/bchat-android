package io.beldex.bchat.util

import android.content.Context
import android.content.SharedPreferences
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.data.NodeInfo

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

}