package com.beldex.libsignal.database

import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.beldex.libsignal.utilities.Mnode
import java.util.*

interface BeldexAPIDatabaseProtocol {

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
    fun getAuthToken(server: String): String?
    fun setAuthToken(server: String, newValue: String?)
    fun setUserCount(group: Long, server: String, newValue: Int)
    fun setUserCount(room: String, server: String, newValue: Int)
    fun getLastMessageServerID(room: String, server: String): Long?
    fun setLastMessageServerID(room: String, server: String, newValue: Long)
    fun getLastDeletionServerID(room: String, server: String): Long?
    fun setLastDeletionServerID(room: String, server: String, newValue: Long)
    fun getOpenGroupPublicKey(server: String): String?
    fun setOpenGroupPublicKey(server: String, newValue: String)
    fun getLastMnodePoolRefreshDate(): Date?
    fun setLastMnodePoolRefreshDate(newValue: Date)
    fun getUserX25519KeyPair(): ECKeyPair
    fun getClosedGroupEncryptionKeyPairs(groupPublicKey: String): List<ECKeyPair>
    fun getLatestClosedGroupEncryptionKeyPair(groupPublicKey: String): ECKeyPair?
    fun isClosedGroup(groupPublicKey: String): Boolean
}
