package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.node

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.thoughtcrimes.securesms.data.NetworkNodes
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.model.WalletManager
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.NodePinger
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

@HiltViewModel

class NodeViewModel @Inject constructor(
        private val sharedPreferenceUtil: SharedPreferenceUtil) : ViewModel() {

    data class UiState(
            val multiSelectedActivated: Boolean = false,
            val nodeList: List<NodeInfo> = emptyList()
    )


    private val NODES_PREFS_NAME="nodes"
    private val SELECTED_NODE_PREFS_NAME="selected_node"
    private val pingSelected=0
    private val findbest=1
    var nodeInfo: NodeInfo? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var _favouritesNodes = MutableLiveData<MutableSet<NodeInfo>>()
    val favouritesNodes: LiveData<MutableSet<NodeInfo>> = _favouritesNodes

    init {
        _favouritesNodes.value = mutableSetOf()
    }

    fun getNode(): NodeInfo {
        return nodeInfo!!
    }

    fun updateNodeList(nodeList: NodeInfo){
        val currentList = _favouritesNodes.value ?: mutableSetOf()
        currentList.add(nodeList)
        _favouritesNodes.postValue(currentList)
    }


    fun loadFavouritesWithNetwork() {
        Helper.runWithNetwork {
            loadFavoriteNodes()
            true
        }
    }
    private fun loadFavoriteNodes() {
        val selectedNodeId = sharedPreferenceUtil.getSelectedNodeId()
        val storedNodes = sharedPreferenceUtil.getStoredNodes().all
        val nodes = mutableSetOf<NodeInfo>()
        for (nodeEntry in storedNodes.entries) {
            val nodeId = nodeEntry.value as String
            val nodeInfo = NodeInfo.fromString(nodeId)
            nodeInfo?.let {
                nodeInfo.isFavourite = true
                if (nodeId == selectedNodeId) {
                    nodeInfo.isSelected = true
                }
                nodes.add(nodeInfo)
            } ?: Timber.w("nodeString invalid: %s", nodeId)
        }
        _favouritesNodes.value = nodes.toHashSet()
    }

    fun loadLegacyList(legacyListString: String?) {
        if (legacyListString == null) return
        val nodeStrings = legacyListString.split(";".toRegex()).toTypedArray()
        val nodes = mutableSetOf<NodeInfo>()
        for (nodeString in nodeStrings) {
            val nodeInfo = NodeInfo.fromString(nodeString)
            nodeInfo?.let {
                nodeInfo.isFavourite = true
                nodes.add(nodeInfo)
            } ?: Timber.w("nodeString invalid: %s", nodeString)
        }
        _favouritesNodes.value = nodes.toHashSet()
    }

    fun getOrPopulateFavourites(): MutableSet<NodeInfo> {
        val newSet = favouritesNodes.value ?: mutableSetOf()
        if (newSet.isEmpty()) {
            for (node in NetworkNodes.getNodes()) {
                val nodeInfo = NodeInfo.fromString(node)
                if (nodeInfo != null) {
                    nodeInfo.isFavourite = true
                    newSet.add(nodeInfo)
                }
            }
            sharedPreferenceUtil.saveFavourites(newSet.toHashSet())
            _favouritesNodes.value = newSet
        }
        return newSet
    }

    fun addNode(node: NodeInfo, context: Context) {
        val newItems: MutableSet<NodeInfo>?= _favouritesNodes.value
        if (!favouritesNodes.value!!.contains(node)) newItems!!.add(node)
        setNode(newItems as NodeInfo,true, context) // in case the nodeinfo has changed
    }

    fun setNode(node: NodeInfo?, save: Boolean, context: Context) {
        if (node != nodeInfo) {
            if (node != null) {
                require(node.networkType == WalletManager.getInstance().networkType) { "network type does not match" }
            }
            nodeInfo = node
            for (nodeInfo in _favouritesNodes.value!!) {
                nodeInfo.isSelected=nodeInfo == node
            }
            WalletManager.getInstance().setDaemon(node)

            if (save) saveSelectedNode(context)
        }
    }

    private fun saveSelectedNode(nodeInfo: NodeInfo?, context: Context) {
        val editor= context.getSharedPreferences(SELECTED_NODE_PREFS_NAME, ComponentActivity.MODE_PRIVATE).edit()
        if (nodeInfo == null) {
            editor.clear()
        } else {
            editor.putString("0", getNode().toNodeString())
        }
        editor.apply()
    }
    private fun getSelectedNodeId(context: Context): String? {
        return context.getSharedPreferences(SELECTED_NODE_PREFS_NAME, Context.MODE_PRIVATE)
                .getString("0", null)
    }


    private fun saveSelectedNode(context: Context) {
        // save only if changed
        val nodeInfo = getNode()
        val selectedNodeId = getSelectedNodeId(context)
        if (nodeInfo.toNodeString() != selectedNodeId) saveSelectedNode(nodeInfo, context)
    }
    private fun saveFavourites(favouriteNodes: HashSet<NodeInfo>, context: Context) {
        val editor=context.getSharedPreferences(NODES_PREFS_NAME, ComponentActivity.MODE_PRIVATE).edit()
        editor.clear()
        var i=1
        for (info in favouriteNodes) {
            val nodeString=info.toNodeString()
            editor.putString(i.toString(), nodeString)
            i++
        }
        editor.apply()
    }

    fun setFavouriteNodes(nodes: MutableCollection<NodeInfo>?, context: Context) {
        val newNodes = hashSetOf<NodeInfo>()
        nodes?.forEach { node ->
            if (node.isFavourite) newNodes.add(node)
        }
        saveFavourites(newNodes,context)
    }

    fun asyncFindBestNode(context: Context,vararg params: Int?): LiveData<NodeInfo?> {
        return liveData {

            val favourites: Set<NodeInfo> =  getOrPopulateFavourites()
            var selectedNode: NodeInfo?
            if (params[0] == findbest) {
                selectedNode = autoSelect(favourites)
            } else if (params[0] == pingSelected) {
                selectedNode = getNode()
                if (!_favouritesNodes.value!!.contains(selectedNode)) selectedNode = null // it's not in the favourites (any longer)
                if (selectedNode == null) {
                    for (node in favourites) {
                        if (node.isSelected) {
                            selectedNode = node
                            break
                        }
                    }
                }
                selectedNode?.testRpcService() ?: run {
                    selectedNode = autoSelect(favourites)
                }
            } else {
                throw IllegalStateException()
            }
            if (selectedNode != null && selectedNode!!.isValid) {
                setNode(selectedNode!!,true, context)
                emit(selectedNode)
            } else {
                setNode(selectedNode!!,true, context)
                emit(null)
            }
        }
    }

    private fun autoSelect(nodes: Set<NodeInfo>): NodeInfo? {
        if (nodes.isEmpty()) return null
        NodePinger.execute(nodes, null)
        val nodeList: MutableList<NodeInfo> = nodes.toMutableList()
        Collections.sort(nodeList, NodeInfo.BestNodeComparator)
        return nodeList[0]
    }

}