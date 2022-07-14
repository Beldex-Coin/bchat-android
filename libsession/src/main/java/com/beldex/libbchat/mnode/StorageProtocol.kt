package com.beldex.libbchat.mnode

import com.beldex.libsignal.utilities.Mnode

interface MnodeStorageProtocol {

    fun getMnodePool(): Set<Mnode>
    fun setMnodePool(newValue: Set<Mnode>)
    fun getOnionRequestPaths(): List<List<Mnode>>
    fun clearOnionRequestPaths()
    fun setOnionRequestPaths(newValue: List<List<Mnode>>)
    fun getSwarm(publicKey: String): Set<Mnode>?
    fun setSwarm(publicKey: String, newValue: Set<Mnode>)
    fun getLastMessageHashValue(mnode: Mnode, publicKey: String): String?
    fun setLastMessageHashValue(mnode: Mnode, publicKey: String, newValue: String)
    fun getReceivedMessageHashValues(publicKey: String): Set<String>?
    fun setReceivedMessageHashValues(publicKey: String, newValue: Set<String>)
}