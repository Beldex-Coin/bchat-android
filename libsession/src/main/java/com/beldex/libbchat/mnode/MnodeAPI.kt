@file:Suppress("NAME_SHADOWING")

package com.beldex.libbchat.mnode

import android.os.Build
import com.beldex.libbchat.BuildConfig
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.GenericHash
import com.goterl.lazysodium.interfaces.PwHash
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.interfaces.Sign
import com.goterl.lazysodium.utils.Key
import nl.komponents.kovenant.*
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.utilities.MessageWrapper
import com.beldex.libsignal.crypto.getRandomElement
import com.beldex.libsignal.database.BeldexAPIDatabaseProtocol
import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.*
import com.beldex.libsignal.utilities.Base64
import java.security.SecureRandom
import java.util.*
import kotlin.Pair

object MnodeAPI {
    private val sodium by lazy { LazySodiumAndroid(SodiumAndroid()) }
    internal val database: BeldexAPIDatabaseProtocol
        get() = MnodeModule.shared.storage
    private val broadcaster: Broadcaster
        get() = MnodeModule.shared.broadcaster

    internal var mnodeFailureCount: MutableMap<Mnode, Int> = mutableMapOf()
    internal var mnodePool: Set<Mnode>
        get() = database.getMnodePool()
        set(newValue) { database.setMnodePool(newValue) }
    /**
     * The offset between the user's clock and the Service Node's clock. Used in cases where the
     * user's clock is incorrect.
     */
    internal var clockOffset = 0L

    @JvmStatic
    public val nowWithOffset
        get() = System.currentTimeMillis() + clockOffset

    // Settings
    private val maxRetryCount = 6
    private val minimumMnodePoolCount = 12
    private val minimumSwarmMnodeCount = 3
    // Use port 4433 if the API level can handle the network security configuration and enforce pinned certificates
    private val seedNodePort = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) 443 else 4433
    //New Line
    private val nodePort = 443
    private val seedNodePool by lazy {
        if (useTestnet) {
            Log.d("beldex","here testnet $useTestnet")
              setOf("http://38.242.196.72:19095","http://154.26.139.105:19095")
        } else {
            Log.d("beldex","here mainnet $useTestnet")
            setOf("https://publicnode5.rpcnode.stream:$nodePort","https://publicnode1.rpcnode.stream:$nodePort","https://publicnode2.rpcnode.stream:$nodePort","https://publicnode3.rpcnode.stream:$nodePort","https://publicnode4.rpcnode.stream:$nodePort")//"https://mainnet.beldex.io:29095","https://explorer.beldex.io:19091","https://publicnode5.rpcnode.stream:29095","http://publicnode1.rpcnode.stream:29095","http://publicnode2.rpcnode.stream:29095","http://publicnode3.rpcnode.stream:29095","http://publicnode4.rpcnode.stream:29095"
        }
    }
    private val mnodeFailureThreshold = 3
    private val targetSwarmMnodeCount = 2
    private val useOnionRequests = true

    internal val useTestnet = BuildConfig.USE_TESTNET

    // Error
    internal sealed class Error(val description: String) : Exception(description) {
        object Generic : Error("An error occurred.")
        object ClockOutOfSync : Error("Your clock is out of sync with the Master Node network.")
        object NoKeyPair : Error("Missing user key pair.")
        object SigningFailed : Error("Couldn't sign verification data.")
        // ONS
        object DecryptionFailed : Error("Couldn't decrypt BNS name.")
        object HashingFailed : Error("Couldn't compute BNS name hash.")
        object ValidationFailed : Error("BNS name validation failed.")
    }

    // Internal API
    internal fun invoke(method: Mnode.Method, mnode: Mnode, publicKey: String? = null, parameters: Map<String, Any>): RawResponsePromise {
        val url = "${mnode.address}:${mnode.port}/storage_rpc/v1"
        if (useOnionRequests) {
            //-Log.d("Beldex","new payload in invoke fun Send url $url")
            //-Log.d("Beldex","new payload in invoke fun Send method $method")
            //-Log.d("Beldex","new payload in invoke fun Send parameters $parameters")
            //-Log.d("Beldex","new payload in invoke fun Send mnode $mnode")
            //-Log.d("Beldex","new payload in invoke fun Send publickey $publicKey")

            return OnionRequestAPI.sendOnionRequest(method, parameters, mnode, publicKey)
        } else {
            val deferred = deferred<Map<*, *>, Exception>()
            ThreadUtils.queue {
                val payload = mapOf( "method" to method.rawValue, "params" to parameters )
                try {
                    val json = HTTP.execute(HTTP.Verb.POST, url, payload)
                    deferred.resolve(json)
                } catch (exception: Exception) {
                    val httpRequestFailedException = exception as? HTTP.HTTPRequestFailedException
                    if (httpRequestFailedException != null) {
                        val error = handleMnodeError(httpRequestFailedException.statusCode, httpRequestFailedException.json, mnode, publicKey)
                        if (error != null) { return@queue deferred.reject(exception) }
                    }
                    Log.d("Beldex", "Unhandled exception: $exception.")
                    deferred.reject(exception)
                }
            }
            return deferred.promise
        }
    }

    internal fun getRandomMnode(): Promise<Mnode, Exception> {
        val mnodePool = this.mnodePool
        if (mnodePool.count() < minimumMnodePoolCount) {
            val target = seedNodePool.random()
            val url = "$target/json_rpc"
            Log.d("Beldex", "Populating mnode pool using: $target")
            val parameters = mapOf(
                "method" to "get_n_master_nodes",
                "params" to mapOf(
                    "active_only" to true,
                    "limit" to 256,
                    "fields" to mapOf("public_ip" to true, "storage_port" to true, "pubkey_x25519" to true, "pubkey_ed25519" to true)
                )
            )
            val deferred = deferred<Mnode, Exception>()
            deferred<Mnode, Exception>()
            ThreadUtils.queue {
                try {
                    val json = HTTP.execute(HTTP.Verb.POST, url, parameters, useSeedNodeConnection = true)
                    //-Log.d("beldex","Json MnodeAPI $json")
                    val intermediate = json["result"] as? Map<*, *>
                    val rawMnodes = intermediate?.get("master_node_states") as? List<*>
                    if (rawMnodes != null) {
                        val mnodePool = rawMnodes.mapNotNull { rawMnode ->
                            val rawMnodeAsJSON = rawMnode as? Map<*, *>
                            val address = rawMnodeAsJSON?.get("public_ip") as? String
                            Log.d("Beldex","bchat id validation -- get address ")
                            val port = rawMnodeAsJSON?.get("storage_port") as? Int
                            val ed25519Key = rawMnodeAsJSON?.get("pubkey_ed25519") as? String
                            val x25519Key = rawMnodeAsJSON?.get("pubkey_x25519") as? String
                            if (address != null && port != null && ed25519Key != null && x25519Key != null && address != "0.0.0.0") {
                                //-Log.d("Beldex", "Country IP Address:https://$address\", $port, ${Mnode.KeySet(ed25519Key, x25519Key)}")
                                Mnode("https://$address", port, Mnode.KeySet(ed25519Key, x25519Key))
                            } else {
                                //-Log.d("Beldex", "Failed to parse: ${rawMnode?.prettifiedDescription()}.")
                                null
                            }
                        }.toMutableSet()
                        Log.d("Beldex", "Persisting mnode pool to database.")
                        this.mnodePool = mnodePool
                        try {
                            deferred.resolve(mnodePool.getRandomElement())
                        } catch (exception: Exception) {
                            Log.d("Beldex", "Got an empty mnode pool from: $target.")
                            deferred.reject(MnodeAPI.Error.Generic)
                        }
                    } else {
                        Log.d("Beldex", "Failed to update mnode pool from: ${(rawMnodes as List<*>?)?.prettifiedDescription()}.")
                        deferred.reject(MnodeAPI.Error.Generic)
                    }
                } catch (exception: Exception) {
                    deferred.reject(exception)
                }
            }
            return deferred.promise
        } else {
            Log.d("Beldex","ip mnodePool.getRandomElement()")
            return Promise.of(mnodePool.getRandomElement())
        }
    }

    internal fun dropMnodeFromSwarmIfNeeded(mnode: Mnode, publicKey: String) {
        val swarm = database.getSwarm(publicKey)?.toMutableSet()
        if (swarm != null && swarm.contains(mnode)) {
            swarm.remove(mnode)
            database.setSwarm(publicKey, swarm)
        }
    }

    internal fun getSingleTargetMnode(publicKey: String): Promise<Mnode, Exception> {
        // SecureRandom() should be cryptographically secure
        return getSwarm(publicKey).map { it.shuffled(SecureRandom()).random() }
    }

    // Public API
    fun getBchatID(bnsName: String): Promise<String, Exception> {
        val deferred = deferred<String, Exception>()
        val promise = deferred.promise
        val validationCount = 3
        val bchatIDByteCount = 33
        // Hash the BNS name using BLAKE2b
        val bnsName = bnsName.toLowerCase(Locale.US)
        val nameAsData = bnsName.toByteArray()
        val nameHash = ByteArray(GenericHash.BYTES)
        if (!sodium.cryptoGenericHash(nameHash, nameHash.size, nameAsData, nameAsData.size.toLong())) {
            deferred.reject(Error.HashingFailed)
            return promise
        }
        val base64EncodedNameHash = Base64.encodeBytes(nameHash)
        // Ask 3 different mnodes for the BChat ID associated with the given name hash
        val parameters = mapOf(
                "endpoint" to "bns_resolve",
                "params" to mapOf( "type" to 0, "name_hash" to base64EncodedNameHash )
        )
        val promises = (1..validationCount).map {
            getRandomMnode().bind { mnode ->
                retryIfNeeded(maxRetryCount) {
                    Log.d("Beldex", "invoke MnodeAPI.kt 1")
                    invoke(Mnode.Method.BeldexDaemonRPCCall, mnode, null, parameters)
                }
            }
        }
        all(promises).success { results ->
            Log.d("promises success","Ok")
            val bchatIDs = mutableListOf<String>()
            for (json in results) {
                val intermediate = json["result"] as? Map<*, *>
                val hexEncodedCiphertext = intermediate?.get("encrypted_value") as? String
                if (hexEncodedCiphertext != null) {
                    val ciphertext = Hex.fromStringCondensed(hexEncodedCiphertext)
                    val isArgon2Based = (intermediate["nonce"] == null)
                    if (isArgon2Based) {
                        // Handle old Argon2-based encryption used before HF16
                        val salt = ByteArray(PwHash.SALTBYTES)
                        val key: ByteArray
                        val nonce = ByteArray(SecretBox.NONCEBYTES)
                        val bchatIDAsData = ByteArray(bchatIDByteCount)
                        try {
                            key = Key.fromHexString(sodium.cryptoPwHash(bnsName, SecretBox.KEYBYTES, salt, PwHash.OPSLIMIT_MODERATE, PwHash.MEMLIMIT_MODERATE, PwHash.Alg.PWHASH_ALG_ARGON2ID13)).asBytes
                        } catch (e: SodiumException) {
                            deferred.reject(Error.HashingFailed)
                            return@success
                        }
                        if (!sodium.cryptoSecretBoxOpenEasy(bchatIDAsData, ciphertext, ciphertext.size.toLong(), nonce, key)) {
                            deferred.reject(Error.DecryptionFailed)
                            return@success
                        }
                        bchatIDs.add(Hex.toStringCondensed(bchatIDAsData))
                    } else {
                        val hexEncodedNonce = intermediate["nonce"] as? String
                        if (hexEncodedNonce == null) {
                            deferred.reject(Error.Generic)
                            return@success
                        }
                        val nonce = Hex.fromStringCondensed(hexEncodedNonce)
                        val key = ByteArray(GenericHash.BYTES)
                        if (!sodium.cryptoGenericHash(key, key.size, nameAsData, nameAsData.size.toLong(), nameHash, nameHash.size)) {
                            deferred.reject(Error.HashingFailed)
                            return@success
                        }
                        val bchatIDAsData = ByteArray(bchatIDByteCount)
                        if (!sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(bchatIDAsData, null, null, ciphertext, ciphertext.size.toLong(), null, 0, nonce, key)) {
                            deferred.reject(Error.DecryptionFailed)
                            return@success
                        }
                        bchatIDs.add(Hex.toStringCondensed(bchatIDAsData))
                    }
                } else {
                    deferred.reject(Error.Generic)
                    return@success
                }
            }
            if (bchatIDs.size == validationCount && bchatIDs.toSet().size == 1) {
                deferred.resolve(bchatIDs.first())
            } else {
                deferred.reject(Error.ValidationFailed)
            }
        }
        return promise
    }

    fun getTargetMnodes(publicKey: String): Promise<List<Mnode>, Exception> {
        // SecureRandom() should be cryptographically secure
        return getSwarm(publicKey).map { it.shuffled(SecureRandom()).take(targetSwarmMnodeCount) }
    }
    fun getSwarm(publicKey: String): Promise<Set<Mnode>, Exception> {
        val cachedSwarm = database.getSwarm(publicKey)
        if (cachedSwarm != null && cachedSwarm.size >= minimumSwarmMnodeCount) {
            val cachedSwarmCopy = mutableSetOf<Mnode>() // Workaround for a Kotlin compiler issue
            cachedSwarmCopy.addAll(cachedSwarm)
            return task { cachedSwarmCopy }
        } else {
            val parameters = mapOf( "pubKey" to if (useTestnet) publicKey.removingbdPrefixIfNeeded() else publicKey )
            return getRandomMnode().bind {
                Log.d("Beldex", "invoke MnodeAPI.kt 2")
                invoke(Mnode.Method.GetSwarm, it, publicKey, parameters)
            }.map {
                parseMnodes(it).toSet()
            }.success {
                database.setSwarm(publicKey, it)
            }
        }
    }

    fun getRawMessages(mnode: Mnode, publicKey: String): RawResponsePromise {
//        val userED25519KeyPair = MessagingModuleConfiguration.shared.getUserED25519KeyPair() ?: return Promise.ofFail(Error.NoKeyPair)
        // Get last message hash
        val lastHashValue = database.getLastMessageHashValue(mnode, publicKey) ?: ""
        // Construct signature
//        val timestamp = Date().time + MnodeAPI.clockOffset
//        val ed25519PublicKey = userED25519KeyPair.publicKey.asHexString
//        val verificationData = "retrieve$timestamp".toByteArray()
//        val signature = ByteArray(Sign.BYTES)
//        try {
//            sodium.cryptoSignDetached(signature, verificationData, verificationData.size.toLong(), userED25519KeyPair.secretKey.asBytes)
//        } catch (exception: Exception) {
//            return Promise.ofFail(Error.SigningFailed)
//        }
        // Make the request
        val parameters = mapOf(
            "pubKey" to if (useTestnet) publicKey.removingbdPrefixIfNeeded() else publicKey,
            "lastHash" to lastHashValue,
//            "timestamp" to timestamp,
//            "pubkey_ed25519" to ed25519PublicKey,
//            "signature" to Base64.encodeBytes(signature)
        )
        Log.d("Beldex", "invoke MnodeAPI.kt 3")
        return invoke(Mnode.Method.GetMessages, mnode, publicKey, parameters)
    }

    fun getMessages(publicKey: String): MessageListPromise {
        return retryIfNeeded(maxRetryCount) {
           getSingleTargetMnode(publicKey).bind { mnode ->
               Log.d("Beldex", "invoke MnodeAPI 1")
                getRawMessages(mnode, publicKey).map { parseRawMessagesResponse(it, mnode, publicKey) }
            }
        }
    }

    private fun getNetworkTime(mnode: Mnode): Promise<Pair<Mnode,Long>, Exception> {
        Log.d("Beldex", "invoke MnodeAPI.kt 4")
        return invoke(Mnode.Method.Info, mnode, null, emptyMap()).map { rawResponse ->
            val timestamp = rawResponse["timestamp"] as? Long ?: -1
            mnode to timestamp
        }
    }

    fun sendMessage(message: MnodeMessage): Promise<Set<RawResponsePromise>, Exception> {
        val destination = if (useTestnet) message.recipient.removingbdPrefixIfNeeded() else message.recipient
        Log.d("Beldex","bchat id validation -- check the test net  or mainnet for remove prefix")
        return retryIfNeeded(maxRetryCount) {
            getTargetMnodes(destination).map { swarm ->
                swarm.map { mnode ->
                    val parameters = message.toJSON()
                    //-Log.d("beldex","ttl value ${message.ttl.toString()}")
                    Log.d("Beldex", "invoke MnodeAPI.kt 5")
                    invoke(Mnode.Method.SendMessage, mnode, destination, parameters)
                }.toSet()
            }
        }
    }

    fun deleteMessage(publicKey: String, serverHashes: List<String>): Promise<Map<String,Boolean>, Exception> {
        return retryIfNeeded(maxRetryCount) {
            val module = MessagingModuleConfiguration.shared
            val userED25519KeyPair = module.getUserED25519KeyPair() ?: return@retryIfNeeded Promise.ofFail(Error.NoKeyPair)
            val userPublicKey = module.storage.getUserPublicKey() ?: return@retryIfNeeded Promise.ofFail(Error.NoKeyPair)
            getSingleTargetMnode(publicKey).bind { mnode ->
                retryIfNeeded(maxRetryCount) {
                    val signature = ByteArray(Sign.BYTES)
                    val verificationData = (Mnode.Method.DeleteMessage.rawValue + serverHashes.fold("") { a, v -> a + v }).toByteArray()
                    sodium.cryptoSignDetached(signature, verificationData, verificationData.size.toLong(), userED25519KeyPair.secretKey.asBytes)
                    val deleteMessageParams = mapOf(
                        "pubkey" to userPublicKey,
                        "pubkey_ed25519" to userED25519KeyPair.publicKey.asHexString,
                        "messages" to serverHashes,
                        "signature" to Base64.encodeBytes(signature)
                    )
                    Log.d("Beldex", "invoke MnodeAPI.kt 6")
                    invoke(Mnode.Method.DeleteMessage, mnode, publicKey, deleteMessageParams).map { rawResponse ->
                        val swarms = rawResponse["swarm"] as? Map<String, Any> ?: return@map mapOf()
                        val result = swarms.mapNotNull { (hexMnodePublicKey, rawJSON) ->
                            val json = rawJSON as? Map<String, Any> ?: return@mapNotNull null
                            val isFailed = json["failed"] as? Boolean ?: false
                            val statusCode = json["code"] as? String
                            val reason = json["reason"] as? String
                            hexMnodePublicKey to if (isFailed) {
                                Log.e("Beldex", "Failed to delete messages from: $hexMnodePublicKey due to error: $reason ($statusCode).")
                                false
                            } else {
                                val hashes = json["deleted"] as List<String> // Hashes of deleted messages
                                val signature = json["signature"] as String
                                val mnodePublicKey = Key.fromHexString(hexMnodePublicKey)
                                // The signature looks like ( PUBKEY_HEX || RMSG[0] || ... || RMSG[N] || DMSG[0] || ... || DMSG[M] )
                                val message = (userPublicKey + serverHashes.fold("") { a, v -> a + v } + hashes.fold("") { a, v -> a + v }).toByteArray()
                                sodium.cryptoSignVerifyDetached(Base64.decode(signature), message, message.size, mnodePublicKey.asBytes)
                            }
                        }
                        return@map result.toMap()
                    }.fail { e ->
                        Log.e("Beldex", "Failed to delete messages", e)
                    }
                }
            }
        }
    }

    // Parsing
    private fun parseMnodes(rawResponse: Any): List<Mnode> {
        val json = rawResponse as? Map<*, *>
        val rawMnodes = json?.get("mnodes") as? List<*>
        if (rawMnodes != null) {
            return rawMnodes.mapNotNull { rawMnode ->
                val rawMnodeAsJSON = rawMnode as? Map<*, *>
                val address = rawMnodeAsJSON?.get("ip") as? String
                val portAsString = rawMnodeAsJSON?.get("port") as? String
                val port = portAsString?.toInt()
                val ed25519Key = rawMnodeAsJSON?.get("pubkey_ed25519") as? String
                val x25519Key = rawMnodeAsJSON?.get("pubkey_x25519") as? String
                if (address != null && port != null && ed25519Key != null && x25519Key != null && address != "0.0.0.0") {
                    Mnode("https://$address", port, Mnode.KeySet(ed25519Key, x25519Key))
                } else {
                    Log.d("Beldex", "Failed to parse mnode from: ${rawMnode?.prettifiedDescription()}.")
                    null
                }
            }
        } else {
            Log.d("Beldex", "Failed to parse mnodes from: ${rawResponse.prettifiedDescription()}.")
            return listOf()
        }
    }

    fun deleteAllMessages(): Promise<Map<String,Boolean>, Exception> {
        return retryIfNeeded(maxRetryCount) {
            val module = MessagingModuleConfiguration.shared
            val userED25519KeyPair = module.getUserED25519KeyPair() ?: return@retryIfNeeded Promise.ofFail(Error.NoKeyPair)
            val userPublicKey = module.storage.getUserPublicKey() ?: return@retryIfNeeded Promise.ofFail(Error.NoKeyPair)
            getSingleTargetMnode(userPublicKey).bind { mnode ->
                retryIfNeeded(maxRetryCount) {
                    getNetworkTime(mnode).bind { (_, timestamp) ->
                        val signature = ByteArray(Sign.BYTES)
                        val verificationData = (Mnode.Method.DeleteAll.rawValue + timestamp.toString()).toByteArray()
                        sodium.cryptoSignDetached(signature, verificationData, verificationData.size.toLong(), userED25519KeyPair.secretKey.asBytes)
                        val deleteMessageParams = mapOf(
                            "pubkey" to userPublicKey,
                            "pubkey_ed25519" to userED25519KeyPair.publicKey.asHexString,
                            "timestamp" to timestamp,
                            "signature" to Base64.encodeBytes(signature)
                        )
                        Log.d("Beldex", "invoke MnodeAPI.kt 7")
                        invoke(Mnode.Method.DeleteAll, mnode, userPublicKey, deleteMessageParams).map {
                            rawResponse -> parseDeletions(userPublicKey, timestamp, rawResponse)
                        }.fail { e ->
                            Log.e("Beldex", "Failed to clear data", e)
                        }
                    }
                }
            }
        }
    }

    fun parseRawMessagesResponse(rawResponse: RawResponse, mnode: Mnode, publicKey: String): List<Pair<SignalServiceProtos.Envelope, String?>> {
        val messages = rawResponse["messages"] as? List<*>
        return if (messages != null) {
            updateLastMessageHashValueIfPossible(mnode, publicKey, messages)
            val newRawMessages = removeDuplicates(publicKey, messages)
            return parseEnvelopes(newRawMessages)
        } else {
            listOf()
        }
    }

    private fun updateLastMessageHashValueIfPossible(mnode: Mnode, publicKey: String, rawMessages: List<*>) {
        val lastMessageAsJSON = rawMessages.lastOrNull() as? Map<*, *>
        val hashValue = lastMessageAsJSON?.get("hash") as? String
        if (hashValue != null) {
            database.setLastMessageHashValue(mnode, publicKey, hashValue)
        } else if (rawMessages.isNotEmpty()) {
            Log.d("Beldex", "Failed to update last message hash value from: ${rawMessages.prettifiedDescription()}.")
        }
    }

    private fun removeDuplicates(publicKey: String, rawMessages: List<*>): List<*> {
        /*val receivedMessageHashValues = database.getReceivedMessageHashValues(publicKey)?.toMutableSet() ?: mutableSetOf()*/
        val originalMessageHashValues = database.getReceivedMessageHashValues(publicKey)?.toMutableSet() ?: mutableSetOf()
        val receivedMessageHashValues = originalMessageHashValues.toMutableSet()
        val result = rawMessages.filter { rawMessage ->
            val rawMessageAsJSON = rawMessage as? Map<*, *>
            val hashValue = rawMessageAsJSON?.get("hash") as? String
            if (hashValue != null) {
                val isDuplicate = receivedMessageHashValues.contains(hashValue)
                receivedMessageHashValues.add(hashValue)
                !isDuplicate
            } else {
                Log.d("Beldex", "Missing hash value for message: ${rawMessage?.prettifiedDescription()}.")
                false
            }
        }
        if (originalMessageHashValues != receivedMessageHashValues) {
            database.setReceivedMessageHashValues(publicKey, receivedMessageHashValues)
        }
        return result
    }

    private fun parseEnvelopes(rawMessages: List<*>): List<Pair<SignalServiceProtos.Envelope, String?>> {
        return rawMessages.mapNotNull { rawMessage ->
            val rawMessageAsJSON = rawMessage as? Map<*, *>
            val base64EncodedData = rawMessageAsJSON?.get("data") as? String
            val data = base64EncodedData?.let { Base64.decode(it) }
            if (data != null) {
                try {
                    Pair(MessageWrapper.unwrap(data), rawMessageAsJSON.get("hash") as? String)
                } catch (e: Exception) {
                    Log.d("Beldex", "Failed to unwrap data for message: ${rawMessage.prettifiedDescription()}.")
                    null
                }
            } else {
                Log.d("Beldex", "Failed to decode data for message: ${rawMessage?.prettifiedDescription()}.")
                null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseDeletions(userPublicKey: String, timestamp: Long, rawResponse: RawResponse): Map<String, Boolean> {
        val swarms = rawResponse["swarm"] as? Map<String, Any> ?: return mapOf()
        val result = swarms.mapNotNull { (hexMnodePublicKey, rawJSON) ->
            val json = rawJSON as? Map<String, Any> ?: return@mapNotNull null
            val isFailed = json["failed"] as? Boolean ?: false
            val statusCode = json["code"] as? String
            val reason = json["reason"] as? String
            hexMnodePublicKey to if (isFailed) {
                Log.e("Beldex", "Failed to delete all messages from: $hexMnodePublicKey due to error: $reason ($statusCode).")
                false
            } else {
                val hashes = json["deleted"] as List<String> // Hashes of deleted messages
                val signature = json["signature"] as String
                val mnodePublicKey = Key.fromHexString(hexMnodePublicKey)
                // The signature looks like ( PUBKEY_HEX || TIMESTAMP || DELETEDHASH[0] || ... || DELETEDHASH[N] )
                val message = (userPublicKey + timestamp.toString() + hashes.fold("") { a, v -> a + v }).toByteArray()
                sodium.cryptoSignVerifyDetached(Base64.decode(signature), message, message.size, mnodePublicKey.asBytes)
            }
        }
        return result.toMap()
    }

    // endregion

    // Error Handling
    internal fun handleMnodeError(statusCode: Int, json: Map<*, *>?, mnode: Mnode, publicKey: String? = null): Exception? {
        fun handleBadMnode() {
            val oldFailureCount = mnodeFailureCount[mnode] ?: 0
            val newFailureCount = oldFailureCount + 1
            mnodeFailureCount[mnode] = newFailureCount
            Log.d("Beldex", "Couldn't reach mnode at $mnode; setting failure count to $newFailureCount.")
            if (newFailureCount >= mnodeFailureThreshold) {
                Log.d("Beldex", "Failure threshold reached for: $mnode; dropping it.")
                if (publicKey != null) {
                    dropMnodeFromSwarmIfNeeded(mnode, publicKey)
                }
                mnodePool = mnodePool.toMutableSet().minus(mnode).toSet()
                Log.d("Beldex", "Mnode pool count: ${mnodePool.count()}.")
                mnodeFailureCount[mnode] = 0
            }
        }
        when (statusCode) {
            400, 500, 502, 503 -> { // Usually indicates that the mnode isn't up to date
                handleBadMnode()
            }
            406 -> {
                Log.d("Beldex", "The user's clock is out of sync with the service node network.")
                broadcaster.broadcast("clockOutOfSync")
                return Error.ClockOutOfSync
            }
            421 -> {
                // The mnode isn't associated with the given public key anymore
                if (publicKey != null) {
                    fun invalidateSwarm() {
                        Log.d("Beldex", "Invalidating swarm for: $publicKey.")
                        dropMnodeFromSwarmIfNeeded(mnode, publicKey)
                    }
                    if (json != null) {
                        val mnodes = parseMnodes(json)
                        if (mnodes.isNotEmpty()) {
                            database.setSwarm(publicKey, mnodes.toSet())
                        } else {
                            invalidateSwarm()
                        }
                    } else {
                        invalidateSwarm()
                    }
                } else {
                    Log.d("Beldex", "Got a 421 without an associated public key.")
                }
            }
            else -> {
                handleBadMnode()
                Log.d("Beldex", "Unhandled response code: ${statusCode}.")
                return Error.Generic
            }
        }
        return null
    }
}

// Type Aliases
typealias RawResponse = Map<*, *>
typealias MessageListPromise = Promise<List<Pair<SignalServiceProtos.Envelope, String?>>, Exception>
typealias RawResponsePromise = Promise<RawResponse, Exception>
