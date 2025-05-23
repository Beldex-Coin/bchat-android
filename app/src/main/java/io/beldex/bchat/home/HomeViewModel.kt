package io.beldex.bchat.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.beldex.bchat.data.NetworkNodes
import io.beldex.bchat.data.NodeInfo
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val threadDb: ThreadDatabase,
    private val sharedPreferenceUtil: SharedPreferenceUtil
): ViewModel() {

    private val _favouritesNodes = MutableStateFlow<HashSet<NodeInfo>?>(null)
    val favouritesNodes: StateFlow<HashSet<NodeInfo>?> = _favouritesNodes

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
        viewModelScope.launch {
            val jobsList = ArrayList<Job>()
            for (nodeEntry in storedNodes.entries) {
                jobsList.add(
                    launch {
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
                )
            }
            jobsList.joinAll()
            _favouritesNodes.value = nodes.toHashSet()
        }
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

    fun getOrPopulateFavourites(context: Context): MutableSet<NodeInfo> {
        val newSet = favouritesNodes.value ?: hashSetOf()
        if (newSet.isEmpty()) {
            for (node in NetworkNodes.getNodes(context)) {
                val nodeInfo = NodeInfo.fromString(node)
                if (nodeInfo != null) {
                    nodeInfo.isFavourite = true
                    newSet.add(nodeInfo)
                }
            }
            sharedPreferenceUtil.saveFavourites(newSet)
            _favouritesNodes.value = newSet
        }
        return newSet
    }

    fun getOrPopulateFavouritesRemoteNodeList(context : Context, storeNodes : Boolean): MutableSet<NodeInfo> {
        val newSet = favouritesNodes.value ?: hashSetOf()
        newSet.clear()
        if (newSet.isEmpty()) {
            for (node in NetworkNodes.getNodes(context)) {
                val nodeInfo = NodeInfo.fromString(node)
                if (nodeInfo != null) {
                    nodeInfo.isFavourite = true
                    newSet.add(nodeInfo)
                }
            }
            if(storeNodes){
                sharedPreferenceUtil.saveFavourites(newSet)
            }
            _favouritesNodes.value = newSet
        }
        return newSet
    }

    fun setFavouriteNodes(nodes: MutableCollection<NodeInfo>?) {
        val newNodes = hashSetOf<NodeInfo>()
        nodes?.forEach { node ->
            if (node.isFavourite) newNodes.add(node)
        }
        sharedPreferenceUtil.saveFavourites(newNodes)
    }

}