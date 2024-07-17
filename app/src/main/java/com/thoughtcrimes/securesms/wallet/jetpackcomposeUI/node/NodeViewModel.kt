package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.node

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.thoughtcrimes.securesms.data.NetworkNodes.getNodes
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.model.NetworkType
import com.thoughtcrimes.securesms.model.WalletManager
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.NodePinger
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil.Companion.PREF_DAEMON_MAINNET
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil.Companion.PREF_DAEMON_STAGENET
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil.Companion.PREF_DAEMON_TESTNET
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import javax.inject.Inject

@HiltViewModel

class NodeViewModel @Inject constructor(
        private val sharedPreferenceUtil : SharedPreferenceUtil) : ViewModel() {

    data class UiState(
            val multiSelectedActivated: Boolean = false,
            val nodeList: List<NodeInfo> = emptyList()
    )


    private val NODES_PREFS_NAME="nodes"
    private val SELECTED_NODE_PREFS_NAME="selected_node"
    private val pingSelected=0
    private val findbest=1
    var nodeInfo : NodeInfo?=null

    private val _uiState=MutableStateFlow(UiState())
    val uiState=_uiState.asStateFlow()

    private var _favouritesNodes = MutableLiveData<MutableSet<NodeInfo>>()
    val favouritesNodes: LiveData<MutableSet<NodeInfo>> = _favouritesNodes

    private val _currentNode=MutableLiveData<String>()
    val currentNode : LiveData<String> get()=_currentNode

    init {
        _favouritesNodes.value=mutableSetOf()
    }

    fun getNode() : NodeInfo {
        return nodeInfo!!
    }

    fun updateNodeList(nodeList : NodeInfo) {
        val currentList=_favouritesNodes.value ?: mutableSetOf()
        currentList.add(nodeList)
        _favouritesNodes.postValue(currentList)
    }

    private fun addFavourite(nodeString : String) : NodeInfo? {
        val nodeInfo=NodeInfo.fromString(nodeString)
        if (nodeInfo != null) {
            nodeInfo.setFavourite(true)
            _favouritesNodes.value?.add(nodeInfo)
        }
        return nodeInfo
    }


    fun loadFavouritesWithNetwork() {
        Helper.runWithNetwork {
            loadFavoriteNodes()
            true
        }
    }

    private fun loadFavoriteNodes() {
        val selectedNodeId=sharedPreferenceUtil.getSelectedNodeId()
        val storedNodes=sharedPreferenceUtil.getStoredNodes().all
        val nodes=mutableSetOf<NodeInfo>()
        for (nodeEntry in storedNodes.entries) {
            if (nodeEntry != null) { // just in case, ignore possible future errors
                val nodeId=nodeEntry.value as String
                val addedNode=addFavourite(nodeId)
                if (addedNode != null) {
                    if (nodeId == selectedNodeId) {
                        addedNode.isSelected=true
                    }
                }
            }
        }
        if (storedNodes.isEmpty()) { // try to load legacy list & remove it (i.e. migrate the data once)
            val sharedPref : SharedPreferences=sharedPreferenceUtil.getPreference("", Context.MODE_PRIVATE)
            when (WalletManager.getInstance().networkType) {
                NetworkType.NetworkType_Mainnet -> {
                    loadLegacyList(sharedPref.getString(PREF_DAEMON_MAINNET, null))
                    sharedPref.edit().remove(PREF_DAEMON_MAINNET).apply()
                }

                NetworkType.NetworkType_Stagenet -> {
                    loadLegacyList(sharedPref.getString(PREF_DAEMON_STAGENET, null))
                    sharedPref.edit().remove(PREF_DAEMON_STAGENET).apply()
                }

                NetworkType.NetworkType_Testnet -> {
                    loadLegacyList(sharedPref.getString(PREF_DAEMON_TESTNET, null))
                    sharedPref.edit().remove(PREF_DAEMON_TESTNET).apply()
                }

                else -> throw java.lang.IllegalStateException("unsupported net " + WalletManager.getInstance().networkType)
            }
        }
    }

    fun loadLegacyList(legacyListString : String?) {
        if (legacyListString == null) return
        val nodeStrings : Array<String> =legacyListString.split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        for (nodeString in nodeStrings) {
            addFavourite(nodeString)
        }
    }

    private fun saveFavourites() {
        val editor : SharedPreferences.Editor=sharedPreferenceUtil.getPreference(NODES_PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.clear()
        var i=1
        for (info in _favouritesNodes.value!!) {
            val nodeString=info.toNodeString()
            editor.putString(i.toString(), nodeString)
            i++
        }
        editor.apply()
    }

    fun getOrPopulateFavourites(context : Context?) : Set<NodeInfo> {
        if (_favouritesNodes.value?.isEmpty() == true) {
            for (node in getNodes(context!!)) {
                val nodeInfo=NodeInfo.fromString(node)
                if (nodeInfo != null) {
                    nodeInfo.setFavourite(true)
                    favouritesNodes.value
                    _favouritesNodes.value!!.add(nodeInfo)
                }
            }
            saveFavourites()
        }
        return _favouritesNodes.value!!
    }

    fun saveNodes(value : MutableSet<NodeInfo>) {
        sharedPreferenceUtil.saveFavourites(value.toHashSet())
    }

    fun addNode(node : NodeInfo, context : Context) {
        val newItems : MutableSet<NodeInfo>?=_favouritesNodes.value
        if (!favouritesNodes.value!!.contains(node)) {
            newItems!!.add(node)
        }
        setNode(newItems as NodeInfo, true, context) // in case the nodeinfo has changed
    }

    fun setNode(node : NodeInfo?, save : Boolean, context : Context) {
        if (node != nodeInfo) {
            if (node != null) {
                require(node.networkType == WalletManager.getInstance().networkType) { "network type does not match" }
            }
            nodeInfo=node
            for (nodeInfo in _favouritesNodes.value!!) {
                nodeInfo.isSelected=nodeInfo == node
            }
            WalletManager.getInstance().setDaemon(node)

            if (save) saveSelectedNode(context)
        }
    }

    fun saveSelectedNode(nodeInfo : NodeInfo?, context : Context) {
        val editor=context.getSharedPreferences(SELECTED_NODE_PREFS_NAME, ComponentActivity.MODE_PRIVATE).edit()
        if (nodeInfo == null) {
            editor.clear()
        } else {
            editor.putString("0", getNode()?.toNodeString())
            _currentNode.postValue(getNode()?.toNodeString())
        }
        editor.apply()
    }

    fun getSelectedNodeId(context : Context) : String? {
        return context.getSharedPreferences(SELECTED_NODE_PREFS_NAME, Context.MODE_PRIVATE)
                .getString("0", null)
    }


    fun saveSelectedNode(context : Context) {
        // save only if changed
        val nodeInfo=getNode()
        val selectedNodeId=getSelectedNodeId(context)
        if (nodeInfo?.toNodeString() != selectedNodeId) saveSelectedNode(nodeInfo, context)
    }

    fun saveFavourites(favouriteNodes : HashSet<NodeInfo>, context : Context) {
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

    fun setFavouriteNodes(nodes : MutableCollection<NodeInfo>?, context : Context) {
        val newNodes=hashSetOf<NodeInfo>()
        nodes?.forEach { node ->
            if (node.isFavourite) newNodes.add(node)
        }
        saveFavourites(newNodes, context)
    }

    fun asyncFindBestNode(context : Context, vararg params : Int?) : LiveData<NodeInfo?> {
        return liveData {
            val favourites : Set<NodeInfo> =getOrPopulateFavourites(context)
            var selectedNode : NodeInfo?
            if (params[0] == findbest) {
                selectedNode=autoselect(favourites)
            } else if (params[0] == pingSelected) {
                selectedNode=getNode()
                if (!_favouritesNodes.value!!.contains(selectedNode)) selectedNode=null // it's not in the favourites (any longer)
                if (selectedNode == null) {
                    for (node in favourites) {
                        if (node.isSelected) {
                            selectedNode=node
                            break
                        }
                    }
                }
                selectedNode?.testRpcService() ?: run {
                    selectedNode=autoselect(favourites)
                }
            } else {
                throw IllegalStateException()
            }
            if (selectedNode != null && selectedNode!!.isValid) {
                setNode(selectedNode!!, true, context)
                emit(selectedNode)
            } else {
                setNode(selectedNode!!, true, context)
                emit(null)
            }
        }
    }

    fun autoselect(nodes : Set<NodeInfo?>?) : NodeInfo? {
        if (nodes?.isEmpty() == true) return null
        NodePinger.execute(nodes, null)
        val nodeList : List<NodeInfo> =java.util.ArrayList<NodeInfo>(nodes)
        Collections.sort(nodeList, NodeInfo.BestNodeComparator)
        return nodeList[0]
    }

    fun addNode(node : NodeInfo) {
        val newItems : MutableList<NodeInfo> =java.util.ArrayList<com.thoughtcrimes.securesms.data.NodeInfo>(_favouritesNodes.value)
        if (!_favouritesNodes.value?.contains(node)!!) newItems.add(node)
        setNodes(newItems) // in case the nodeinfo has changed
    }

    fun setNodes(newItemsCollection : Collection<NodeInfo>?) {
        val newItems : List<NodeInfo>
        if (newItemsCollection != null) {
            newItems=ArrayList(newItemsCollection)
            Collections.sort(newItems, NodeInfo.BestNodeComparator)
        } else {
            newItems=ArrayList()
        }
        _favouritesNodes.value?.clear()
        _favouritesNodes.value?.addAll(newItems)
    }
}