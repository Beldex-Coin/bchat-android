package io.beldex.bchat.database

import android.content.ContentValues
import android.content.Context
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.ecc.DjbECPrivateKey
import com.beldex.libsignal.crypto.ecc.DjbECPublicKey
import com.beldex.libsignal.crypto.ecc.ECKeyPair
import com.beldex.libsignal.database.BeldexAPIDatabaseProtocol
import com.beldex.libsignal.utilities.ForkInfo
import com.beldex.libsignal.utilities.Hex
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.Mnode
import com.beldex.libsignal.utilities.PublicKeyValidation
import com.beldex.libsignal.utilities.removingbdPrefixIfNeeded
import com.beldex.libsignal.utilities.toHexString
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.database.helpers.SQLCipherOpenHelper
import java.util.Date

class BeldexAPIDatabase(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper), BeldexAPIDatabaseProtocol {

    companion object {
        // Shared
        private const val publicKey = "public_key"
        private const val timestamp = "timestamp"
        private const val mnode = "mnode"
        // Mnode pool
        public const val mnodePoolTable = "beldex_mnode_pool_cache"
        private const val dummyKey = "dummy_key"
        private const val mnodePool = "mnode_pool_key"
        @JvmStatic val createMnodePoolTableCommand = "CREATE TABLE $mnodePoolTable ($dummyKey TEXT PRIMARY KEY, $mnodePool TEXT);"
        // Onion request paths
        private const val onionRequestPathTable = "beldex_path_cache"
        private const val indexPath = "index_path"
        @JvmStatic val createOnionRequestPathTableCommand = "CREATE TABLE $onionRequestPathTable ($indexPath TEXT PRIMARY KEY, $mnode TEXT);"
        // Swarms
        public const val swarmTable = "beldex_api_swarm_cache"
        private const val swarmPublicKey = "hex_encoded_public_key"
        private const val swarm = "swarm"
        @JvmStatic val createSwarmTableCommand = "CREATE TABLE $swarmTable ($swarmPublicKey TEXT PRIMARY KEY, $swarm TEXT);"
        // Last message hash values
        private const val legacyLastMessageHashValueTable2 = "last_message_hash_value_table"
        private const val lastMessageHashValueTable2 = "bchat_last_message_hash_value_table"
        private const val lastMessageHashValue = "last_message_hash_value"
        private const val lastMessageHashNamespace = "last_message_namespace"
        @JvmStatic val createLastMessageHashValueTable2Command
            = "CREATE TABLE $legacyLastMessageHashValueTable2 ($mnode TEXT, $publicKey TEXT, $lastMessageHashValue TEXT, PRIMARY KEY ($mnode, $publicKey));"
        // Received message hash values
        private const val legacyReceivedMessageHashValuesTable3 = "received_message_hash_values_table_3"
        private const val receivedMessageHashValuesTable = "bchat_received_message_hash_values_table"
        private const val receivedMessageHashValues = "received_message_hash_values"
        private const val receivedMessageHashNamespace = "received_message_namespace"
        @JvmStatic val createReceivedMessageHashValuesTable3Command
            = "CREATE TABLE $legacyReceivedMessageHashValuesTable3 ($publicKey STRING PRIMARY KEY, $receivedMessageHashValues TEXT);"
        // Social group auth tokens
        private const val openGroupAuthTokenTable = "beldex_api_group_chat_auth_token_database"
        private const val server = "server"
        private const val token = "token"
        @JvmStatic val createOpenGroupAuthTokenTableCommand = "CREATE TABLE $openGroupAuthTokenTable ($server TEXT PRIMARY KEY, $token TEXT);"
        // Last message server IDs
        private const val lastMessageServerIDTable = "beldex_api_last_message_server_id_cache"
        private const val lastMessageServerIDTableIndex = "beldex_api_last_message_server_id_cache_index"
        private const val lastMessageServerID = "last_message_server_id"
        @JvmStatic val createLastMessageServerIDTableCommand = "CREATE TABLE $lastMessageServerIDTable ($lastMessageServerIDTableIndex STRING PRIMARY KEY, $lastMessageServerID INTEGER DEFAULT 0);"
        // Last deletion server IDs
        private const val lastDeletionServerIDTable = "beldex_api_last_deletion_server_id_cache"
        private const val lastDeletionServerIDTableIndex = "beldex_api_last_deletion_server_id_cache_index"
        private const val lastDeletionServerID = "last_deletion_server_id"
        @JvmStatic val createLastDeletionServerIDTableCommand = "CREATE TABLE $lastDeletionServerIDTable ($lastDeletionServerIDTableIndex STRING PRIMARY KEY, $lastDeletionServerID INTEGER DEFAULT 0);"
        // User counts
        private const val userCountTable = "beldex_user_count_cache"
        private const val publicChatID = "public_chat_id"
        private const val userCount = "user_count"
        @JvmStatic val createUserCountTableCommand = "CREATE TABLE $userCountTable ($publicChatID STRING PRIMARY KEY, $userCount INTEGER DEFAULT 0);"
        // Bchat request sent timestamps
        private const val bchatRequestSentTimestampTable = "bchat_request_sent_timestamp_cache"
        @JvmStatic val createBchatRequestSentTimestampTableCommand = "CREATE TABLE $bchatRequestSentTimestampTable ($publicKey STRING PRIMARY KEY, $timestamp INTEGER DEFAULT 0);"
        // Bchat request processed timestamp cache
        private const val bchatRequestProcessedTimestampTable = "bchat_request_processed_timestamp_cache"
        @JvmStatic val createBchatRequestProcessedTimestampTableCommand = "CREATE TABLE $bchatRequestProcessedTimestampTable ($publicKey STRING PRIMARY KEY, $timestamp INTEGER DEFAULT 0);"
        // Social group public keys
        private const val openGroupPublicKeyTable = "open_group_public_keys"
        @JvmStatic val createOpenGroupPublicKeyTableCommand = "CREATE TABLE $openGroupPublicKeyTable ($server STRING PRIMARY KEY, $publicKey INTEGER DEFAULT 0);"
        // Social group profile picture cache
        public const val openGroupProfilePictureTable = "open_group_avatar_cache"
        private const val openGroupProfilePicture = "open_group_avatar"
        @JvmStatic val createOpenGroupProfilePictureTableCommand = "CREATE TABLE $openGroupProfilePictureTable ($publicChatID STRING PRIMARY KEY, $openGroupProfilePicture TEXT NULLABLE DEFAULT NULL);"
        // Secret groups (V2)
        public const val closedGroupEncryptionKeyPairsTable = "closed_group_encryption_key_pairs_table"
        public const val closedGroupsEncryptionKeyPairIndex = "closed_group_encryption_key_pair_index"
        public const val encryptionKeyPairPublicKey = "encryption_key_pair_public_key"
        public const val encryptionKeyPairPrivateKey = "encryption_key_pair_private_key"
        @JvmStatic
        val createClosedGroupEncryptionKeyPairsTable = "CREATE TABLE $closedGroupEncryptionKeyPairsTable ($closedGroupsEncryptionKeyPairIndex STRING PRIMARY KEY, $encryptionKeyPairPublicKey STRING, $encryptionKeyPairPrivateKey STRING)"
        public const val closedGroupPublicKeysTable = "closed_group_public_keys_table"
        public const val groupPublicKey = "group_public_key"
        @JvmStatic
        val createClosedGroupPublicKeysTable = "CREATE TABLE $closedGroupPublicKeysTable ($groupPublicKey STRING PRIMARY KEY)"
        // Hard fork master node info
        const val FORK_INFO_TABLE = "fork_info"
        const val DUMMY_KEY = "dummy_key"
        const val DUMMY_VALUE = "1"
        const val HF_VALUE = "hf_value"
        const val SF_VALUE = "sf_value"
        const val CREATE_FORK_INFO_TABLE_COMMAND = "CREATE TABLE $FORK_INFO_TABLE ($DUMMY_KEY INTEGER PRIMARY KEY, $HF_VALUE INTEGER, $SF_VALUE INTEGER);"
        const val CREATE_DEFAULT_FORK_INFO_COMMAND = "INSERT INTO $FORK_INFO_TABLE ($DUMMY_KEY, $HF_VALUE, $SF_VALUE) VALUES ($DUMMY_VALUE, 17, 0);"

        const val UPDATE_HASHES_INCLUDE_NAMESPACE_COMMAND = "CREATE TABLE IF NOT EXISTS $lastMessageHashValueTable2($mnode TEXT, $publicKey TEXT, $lastMessageHashValue TEXT, $lastMessageHashNamespace INTEGER DEFAULT 0, PRIMARY KEY ($mnode, $publicKey, $lastMessageHashNamespace));"
        const val INSERT_LAST_HASH_DATA = "INSERT OR IGNORE INTO $lastMessageHashValueTable2($mnode, $publicKey, $lastMessageHashValue) SELECT $mnode, $publicKey, $lastMessageHashValue FROM $legacyLastMessageHashValueTable2;"
        const val DROP_LEGACY_LAST_HASH = "DROP TABLE $legacyLastMessageHashValueTable2;"

        const val UPDATE_RECEIVED_INCLUDE_NAMESPACE_COMMAND = "CREATE TABLE IF NOT EXISTS $receivedMessageHashValuesTable($publicKey STRING, $receivedMessageHashValues TEXT, $receivedMessageHashNamespace INTEGER DEFAULT 0, PRIMARY KEY ($publicKey, $receivedMessageHashNamespace));"
        const val INSERT_RECEIVED_HASHES_DATA = "INSERT OR IGNORE INTO $receivedMessageHashValuesTable($publicKey, $receivedMessageHashValues) SELECT $publicKey, $receivedMessageHashValues FROM $legacyReceivedMessageHashValuesTable3;"
        const val DROP_LEGACY_RECEIVED_HASHES = "DROP TABLE $legacyReceivedMessageHashValuesTable3;"

        // region Deprecated
        private const val deviceLinkCache = "beldex_pairing_authorisation_cache"
        private const val masterPublicKey = "primary_device"
        private const val slavePublicKey = "secondary_device"
        private const val requestSignature = "request_signature"
        private const val authorizationSignature = "grant_signature"
        @JvmStatic val createDeviceLinkCacheCommand = "CREATE TABLE $deviceLinkCache ($masterPublicKey STRING, $slavePublicKey STRING, " +
            "$requestSignature STRING NULLABLE DEFAULT NULL, $authorizationSignature STRING NULLABLE DEFAULT NULL, PRIMARY KEY ($masterPublicKey, $slavePublicKey));"
        private const val bchatRequestTimestampCache = "bchat_request_timestamp_cache"
        @JvmStatic val createBchatRequestTimestampCacheCommand = "CREATE TABLE $bchatRequestTimestampCache ($publicKey STRING PRIMARY KEY, $timestamp STRING);"
        // endregion
    }


    override fun getMnodePool(): Set<Mnode> {
        val database = databaseHelper.readableDatabase
        return database.get(mnodePoolTable, "$dummyKey = ?", wrap("dummy_key")) { cursor ->
            val mnodePoolAsString = cursor.getString(cursor.getColumnIndexOrThrow(mnodePool))
            mnodePoolAsString.split(", ").mapNotNull { mnodeAsString ->
                val components = mnodeAsString.split("-")
                val address = components[0]
                val port = components.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                val ed25519Key = components.getOrNull(2) ?: return@mapNotNull null
                val x25519Key = components.getOrNull(3) ?: return@mapNotNull null
                Mnode(address, port, Mnode.KeySet(ed25519Key, x25519Key))
            }
        }?.toSet() ?: setOf()
    }

    override fun setMnodePool(newValue: Set<Mnode>) {
        val database = databaseHelper.writableDatabase
        val mnodePoolAsString = newValue.joinToString(", ") { mnode ->
            var string = "${mnode.address}-${mnode.port}"
            val keySet = mnode.publicKeySet
            if (keySet != null) {
                string += "-${keySet.ed25519Key}-${keySet.x25519Key}"
            }
            string
        }
        val row = wrap(mapOf( dummyKey to "dummy_key", mnodePool to mnodePoolAsString ))
        database.insertOrUpdate(mnodePoolTable, row, "$dummyKey = ?", wrap("dummy_key"))
    }

    override fun setOnionRequestPaths(newValue: List<List<Mnode>>) {
        // FIXME: This approach assumes either 1 or 2 paths of length 3 each. We should do better than this.
        val database = databaseHelper.writableDatabase
        fun set(indexPath: String, mnode: Mnode) {
            var mnodeAsString = "${mnode.address}-${mnode.port}"
            val keySet = mnode.publicKeySet
            if (keySet != null) {
                mnodeAsString += "-${keySet.ed25519Key}-${keySet.x25519Key}"
            }
            val row = wrap(mapOf( Companion.indexPath to indexPath, Companion.mnode to mnodeAsString ))
            database.insertOrUpdate(onionRequestPathTable, row, "${Companion.indexPath} = ?", wrap(indexPath))
        }
        Log.d("Beldex", "Persisting onion request paths to database.")
        clearOnionRequestPaths()
        if (newValue.isEmpty()) { return }
        val path0 = newValue[0]
        if (path0.count() != 3) { return }
        set("0-0", path0[0]); set("0-1", path0[1]); set("0-2", path0[2])
        if (newValue.count() < 2) { return }
        val path1 = newValue[1]
        if (path1.count() != 3) { return }
        set("1-0", path1[0]); set("1-1", path1[1]); set("1-2", path1[2])
    }

    override fun getOnionRequestPaths(): List<List<Mnode>> {
        val database = databaseHelper.readableDatabase
        fun get(indexPath: String): Mnode? {
            return database.get(onionRequestPathTable, "${Companion.indexPath} = ?", wrap(indexPath)) { cursor ->
                val mnodeAsString = cursor.getString(cursor.getColumnIndexOrThrow(mnode))
                val components = mnodeAsString.split("-")
                val address = components[0]
                val port = components.getOrNull(1)?.toIntOrNull()
                val ed25519Key = components.getOrNull(2)
                val x25519Key = components.getOrNull(3)
                if (port != null && ed25519Key != null && x25519Key != null) {
                    Mnode(address, port, Mnode.KeySet(ed25519Key, x25519Key))
                } else {
                    null
                }
            }
        }
        val result = mutableListOf<List<Mnode>>()
        val path0Mnode0 = get("0-0"); val path0Mnode1 = get("0-1"); val path0Mnode2 = get("0-2")
        if (path0Mnode0 != null && path0Mnode1 != null && path0Mnode2 != null) {
            result.add(listOf( path0Mnode0, path0Mnode1, path0Mnode2 ))
        }
        val path1Mnode0 = get("1-0"); val path1Mnode1 = get("1-1"); val path1Mnode2 = get("1-2")
        if (path1Mnode0 != null && path1Mnode1 != null && path1Mnode2 != null) {
            result.add(listOf( path1Mnode0, path1Mnode1, path1Mnode2 ))
        }
        return result
    }

    override fun clearOnionRequestPaths() {
        val database = databaseHelper.writableDatabase
        fun delete(indexPath: String) {
            database.delete(onionRequestPathTable, "${Companion.indexPath} = ?", wrap(indexPath))
        }
        delete("0-0"); delete("0-1")
        delete("0-2"); delete("1-0")
        delete("1-1"); delete("1-2")
    }

    override fun getSwarm(publicKey: String): Set<Mnode>? {
        val database = databaseHelper.readableDatabase
        return database.get(swarmTable, "$swarmPublicKey = ?", wrap(publicKey)) { cursor ->
            val swarmAsString = cursor.getString(cursor.getColumnIndexOrThrow(swarm))
            swarmAsString.split(", ").mapNotNull { targetAsString ->
                val components = targetAsString.split("-")
                val address = components[0]
                val port = components.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                val ed25519Key = components.getOrNull(2) ?: return@mapNotNull null
                val x25519Key = components.getOrNull(3) ?: return@mapNotNull null
                Mnode(address, port, Mnode.KeySet(ed25519Key, x25519Key))
            }
        }?.toSet()
    }

    override fun setSwarm(publicKey: String, newValue: Set<Mnode>) {
        val database = databaseHelper.writableDatabase
        val swarmAsString = newValue.joinToString(", ") { target ->
            var string = "${target.address}-${target.port}"
            val keySet = target.publicKeySet
            if (keySet != null) {
                string += "-${keySet.ed25519Key}-${keySet.x25519Key}"
            }
            string
        }
        val row = wrap(mapOf( swarmPublicKey to publicKey, swarm to swarmAsString ))
        database.insertOrUpdate(swarmTable, row, "$swarmPublicKey = ?", wrap(publicKey))
    }

    override fun getLastMessageHashValue(mnode: Mnode, publicKey: String, namespace: Int): String? {
        val database = databaseHelper.readableDatabase
        val query = "${Companion.mnode} = ? AND ${Companion.publicKey} = ? AND $lastMessageHashNamespace = ?"
        return database.get(lastMessageHashValueTable2, query, arrayOf( mnode.toString(), publicKey, namespace.toString() )) { cursor ->
            cursor.getString(cursor.getColumnIndexOrThrow(lastMessageHashValue))
        }
    }

    override fun setLastMessageHashValue(mnode: Mnode, publicKey: String, newValue: String, namespace: Int) {
        val database = databaseHelper.writableDatabase
        val row = wrap(mapOf( Companion.mnode to mnode.toString(), Companion.publicKey to publicKey, lastMessageHashValue to newValue, lastMessageHashNamespace to namespace.toString() ))
        val query = "${Companion.mnode} = ? AND ${Companion.publicKey} = ? AND $lastMessageHashNamespace = ?"
        database.insertOrUpdate(lastMessageHashValueTable2, row, query, arrayOf( mnode.toString(), publicKey, namespace.toString()))
    }

    override fun getReceivedMessageHashValues(publicKey: String, namespace: Int): Set<String>? {
        val database = databaseHelper.readableDatabase
        val query = "${Companion.publicKey} = ? AND ${receivedMessageHashNamespace} = ?"
        return database.get(receivedMessageHashValuesTable, query, arrayOf( publicKey, namespace.toString() )) { cursor ->
            val receivedMessageHashValuesAsString = cursor.getString(cursor.getColumnIndexOrThrow(
                receivedMessageHashValues
            ))
            receivedMessageHashValuesAsString.split("-").toSet()
        }
    }

    override fun setReceivedMessageHashValues(publicKey: String, newValue: Set<String>, namespace: Int) {
        val database = databaseHelper.writableDatabase
        val receivedMessageHashValuesAsString = newValue.joinToString("-")
        val row = wrap(mapOf( Companion.publicKey to publicKey, receivedMessageHashValues to receivedMessageHashValuesAsString, receivedMessageHashNamespace to namespace.toString() ))
        val query = "${Companion.publicKey} = ? AND $receivedMessageHashNamespace = ?"
        database.insertOrUpdate(receivedMessageHashValuesTable, row, query, arrayOf( publicKey, namespace.toString() ))
    }

    override fun getAuthToken(server: String): String? {
        val database = databaseHelper.readableDatabase
        return database.get(openGroupAuthTokenTable, "${Companion.server} = ?", wrap(server)) { cursor ->
            cursor.getString(cursor.getColumnIndexOrThrow(token))
        }
    }

    override fun setAuthToken(server: String, newValue: String?) {
        val database = databaseHelper.writableDatabase
        if (newValue != null) {
            val row = wrap(mapOf( Companion.server to server, token to newValue ))
            database.insertOrUpdate(openGroupAuthTokenTable, row, "${Companion.server} = ?", wrap(server))
        } else {
            database.delete(openGroupAuthTokenTable, "${Companion.server} = ?", wrap(server))
        }
    }

    override fun getLastMessageServerID(room: String, server: String): Long? {
        val database = databaseHelper.writableDatabase
        val index = "$server.$room"
        return database.get(lastMessageServerIDTable, "$lastMessageServerIDTableIndex = ?", wrap(index)) { cursor ->
            cursor.getInt(lastMessageServerID)
        }?.toLong()
    }

    override fun setLastMessageServerID(room: String, server: String, newValue: Long) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$room"
        val row = wrap(mapOf( lastMessageServerIDTableIndex to index, lastMessageServerID to newValue.toString() ))
        database.insertOrUpdate(lastMessageServerIDTable, row, "$lastMessageServerIDTableIndex = ?", wrap(index))
    }

    fun removeLastMessageServerID(room: String, server:String) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$room"
        database.delete(lastMessageServerIDTable, "$lastMessageServerIDTableIndex = ?", wrap(index))
    }

    override fun getLastDeletionServerID(room: String, server: String): Long? {
        val database = databaseHelper.readableDatabase
        val index = "$server.$room"
        return database.get(lastDeletionServerIDTable, "$lastDeletionServerIDTableIndex = ?", wrap(index)) { cursor ->
            cursor.getInt(lastDeletionServerID)
        }?.toLong()
    }

    override fun setLastDeletionServerID(room: String, server: String, newValue: Long) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$room"
        val row = wrap(mapOf(lastDeletionServerIDTableIndex to index, lastDeletionServerID to newValue.toString()))
        database.insertOrUpdate(lastDeletionServerIDTable, row, "$lastDeletionServerIDTableIndex = ?", wrap(index))
    }

    fun removeLastDeletionServerID(room: String, server: String) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$room"
        database.delete(lastDeletionServerIDTable, "$lastDeletionServerIDTableIndex = ?", wrap(index))
    }

    fun removeLastDeletionServerID(group: Long, server: String) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$group"
        database.delete(lastDeletionServerIDTable,"$lastDeletionServerIDTableIndex = ?", wrap(index))
    }

    fun getUserCount(group: Long, server: String): Int? {
        val database = databaseHelper.readableDatabase
        val index = "$server.$group"
        return database.get(userCountTable, "$publicChatID = ?", wrap(index)) { cursor ->
            cursor.getInt(userCount)
        }?.toInt()
    }

    fun getUserCount(room: String, server: String): Int? {
        val database = databaseHelper.readableDatabase
        val index = "$server.$room"
        return database.get(userCountTable, "$publicChatID = ?", wrap(index)) { cursor ->
            cursor.getInt(userCount)
        }?.toInt()
    }

    override fun setUserCount(group: Long, server: String, newValue: Int) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$group"
        val row = wrap(mapOf( publicChatID to index, userCount to newValue.toString() ))
        database.insertOrUpdate(userCountTable, row, "$publicChatID = ?", wrap(index))
    }

    override fun setUserCount(room: String, server: String, newValue: Int) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$room"
        val row = wrap(mapOf( publicChatID to index, userCount to newValue.toString() ))
        database.insertOrUpdate(userCountTable, row, "$publicChatID = ?", wrap(index))
    }

    override fun getOpenGroupPublicKey(server: String): String? {
        val database = databaseHelper.readableDatabase
        return database.get(openGroupPublicKeyTable, "${BeldexAPIDatabase.server} = ?", wrap(server)) { cursor ->
            cursor.getString(BeldexAPIDatabase.publicKey)
        }
    }

    override fun setOpenGroupPublicKey(server: String, newValue: String) {
        val database = databaseHelper.writableDatabase
        val row = wrap(mapOf( BeldexAPIDatabase.server to server, BeldexAPIDatabase.publicKey to newValue ))
        database.insertOrUpdate(openGroupPublicKeyTable, row, "${BeldexAPIDatabase.server} = ?", wrap(server))
    }

    override fun getLastMnodePoolRefreshDate(): Date? {
        val time = TextSecurePreferences.getLastMnodePoolRefreshDate(context)
        if (time <= 0) { return null }
        return Date(time)
    }

    override fun setLastMnodePoolRefreshDate(date: Date) {
        TextSecurePreferences.setLastMnodePoolRefreshDate(context, date)
    }

    override fun getUserX25519KeyPair(): ECKeyPair {
        val keyPair = IdentityKeyUtil.getIdentityKeyPair(context)
        return ECKeyPair(
            DjbECPublicKey(
                keyPair.publicKey.serialize().removingbdPrefixIfNeeded()
            ),
            DjbECPrivateKey(keyPair.privateKey.serialize())
        )
    }

    fun addClosedGroupEncryptionKeyPair(encryptionKeyPair: ECKeyPair, groupPublicKey: String) {
        val database = databaseHelper.writableDatabase
        val timestamp = Date().time.toString()
        val index = "$groupPublicKey-$timestamp"
        val encryptionKeyPairPublicKey = encryptionKeyPair.publicKey.serialize().toHexString().removingbdPrefixIfNeeded()
        val encryptionKeyPairPrivateKey = encryptionKeyPair.privateKey.serialize().toHexString()
        val row = wrap(mapOf(closedGroupsEncryptionKeyPairIndex to index, Companion.encryptionKeyPairPublicKey to encryptionKeyPairPublicKey,
                Companion.encryptionKeyPairPrivateKey to encryptionKeyPairPrivateKey ))
        database.insertOrUpdate(closedGroupEncryptionKeyPairsTable, row, "${closedGroupsEncryptionKeyPairIndex} = ?", wrap(index))
    }

    override fun getClosedGroupEncryptionKeyPairs(groupPublicKey: String): List<ECKeyPair> {
        val database = databaseHelper.readableDatabase
        val timestampsAndKeyPairs = database.getAll(closedGroupEncryptionKeyPairsTable, "$closedGroupsEncryptionKeyPairIndex LIKE ?", wrap("$groupPublicKey%")) { cursor ->
            val timestamp = cursor.getString(cursor.getColumnIndexOrThrow(
                closedGroupsEncryptionKeyPairIndex
            )).split("-").last()
            val encryptionKeyPairPublicKey = cursor.getString(cursor.getColumnIndexOrThrow(
                encryptionKeyPairPublicKey
            ))
            val encryptionKeyPairPrivateKey = cursor.getString(cursor.getColumnIndexOrThrow(
                encryptionKeyPairPrivateKey
            ))
            val keyPair = ECKeyPair(
                DjbECPublicKey(
                    Hex.fromStringCondensed(
                        encryptionKeyPairPublicKey
                    )
                ),
                DjbECPrivateKey(
                    Hex.fromStringCondensed(
                        encryptionKeyPairPrivateKey
                    )
                )
            )
            Pair(timestamp, keyPair)
        }
        return timestampsAndKeyPairs.sortedBy { it.first.toLong() }.map { it.second }
    }

    override fun getLatestClosedGroupEncryptionKeyPair(groupPublicKey: String): ECKeyPair? {
        return getClosedGroupEncryptionKeyPairs(groupPublicKey).lastOrNull()
    }

    fun removeAllClosedGroupEncryptionKeyPairs(groupPublicKey: String) {
        val database = databaseHelper.writableDatabase
        database.delete(closedGroupEncryptionKeyPairsTable, "$closedGroupsEncryptionKeyPairIndex LIKE ?", wrap("$groupPublicKey%"))
    }

    fun addClosedGroupPublicKey(groupPublicKey: String) {
        val database = databaseHelper.writableDatabase
        val row = wrap(mapOf( Companion.groupPublicKey to groupPublicKey ))
        database.insertOrUpdate(closedGroupPublicKeysTable, row, "${Companion.groupPublicKey} = ?", wrap(groupPublicKey))
    }

    fun getAllClosedGroupPublicKeys(): Set<String> {
        val database = databaseHelper.readableDatabase
        return database.getAll(closedGroupPublicKeysTable, null, null) { cursor ->
            cursor.getString(cursor.getColumnIndexOrThrow(groupPublicKey))
        }.toSet()
    }

    override fun isClosedGroup(groupPublicKey: String): Boolean {
        if (!PublicKeyValidation.isValid(groupPublicKey)) { return false }
        return getAllClosedGroupPublicKeys().contains(groupPublicKey)
    }

    fun removeClosedGroupPublicKey(groupPublicKey: String) {
        val database = databaseHelper.writableDatabase
        database.delete(closedGroupPublicKeysTable, "${Companion.groupPublicKey} = ?", wrap(groupPublicKey))
    }

    override fun getForkInfo(): ForkInfo {
        val database = databaseHelper.readableDatabase
        val queryCursor = database.query(FORK_INFO_TABLE, arrayOf(HF_VALUE, SF_VALUE), "$DUMMY_KEY = $DUMMY_VALUE", null, null, null, null)
        val forkInfo = queryCursor.use { cursor ->
            if (!cursor.moveToNext()) {
                ForkInfo(17, 0) // no HF info, none set will at least be the version
            } else {
                ForkInfo(cursor.getInt(0), cursor.getInt(1))
            }
        }
        return forkInfo
    }

    override fun setForkInfo(forkInfo: ForkInfo) {
        val database = databaseHelper.writableDatabase
        val query = "$DUMMY_KEY = $DUMMY_VALUE"
        val contentValues = ContentValues(3)
        contentValues.put(DUMMY_KEY, DUMMY_VALUE)
        contentValues.put(HF_VALUE, forkInfo.hf)
        contentValues.put(SF_VALUE, forkInfo.sf)
        database.insertOrUpdate(FORK_INFO_TABLE, contentValues, query, emptyArray())
    }
}

// region Convenience
private inline fun <reified T> wrap(x: T): Array<T> {
    return Array(1) { x }
}

private fun wrap(x: Map<String, String>): ContentValues {
    val result = ContentValues(x.size)
    x.iterator().forEach { result.put(it.key, it.value) }
    return result
}
// endregion