package com.thoughtcrimes.securesms.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.observeQuery
import com.thoughtcrimes.securesms.data.NetworkNodes
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.database.DatabaseContentProviders
import com.thoughtcrimes.securesms.database.ThreadDatabase
import com.thoughtcrimes.securesms.database.model.ThreadRecord
import com.thoughtcrimes.securesms.util.Helper
import com.thoughtcrimes.securesms.util.SharedPreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
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
        val newSet = favouritesNodes.value ?: hashSetOf()
        if (newSet.isEmpty()) {
            for (node in NetworkNodes.getNodes()) {
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

    fun setFavouriteNodes(nodes: MutableCollection<NodeInfo>?) {
        val newNodes = hashSetOf<NodeInfo>()
        nodes?.forEach { node ->
            if (node.isFavourite) newNodes.add(node)
        }
        sharedPreferenceUtil.saveFavourites(newNodes)
    }

}