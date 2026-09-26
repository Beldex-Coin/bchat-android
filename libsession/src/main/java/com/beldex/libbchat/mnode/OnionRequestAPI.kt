package com.beldex.libbchat.mnode

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.all
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import okhttp3.Request
import com.beldex.libbchat.messaging.file_server.FileServerAPIV2
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.AESGCM
import com.beldex.libsignal.utilities.*
import com.beldex.libsignal.utilities.Mnode
import com.beldex.libbchat.utilities.AESGCM.EncryptionResult
import com.beldex.libbchat.mnode.utilities.getBodyForOnionRequest
import com.beldex.libbchat.mnode.utilities.getHeadersForOnionRequest
import com.beldex.libsignal.crypto.getRandomElement
import com.beldex.libsignal.crypto.getRandomElementOrNull
import com.beldex.libsignal.utilities.Broadcaster
import com.beldex.libsignal.utilities.HTTP
import com.beldex.libsignal.database.BeldexAPIDatabaseProtocol
import com.beldex.libsignal.utilities.Base64
import nl.komponents.kovenant.Deferred
import java.util.*

private typealias Path = List<Mnode>

object OnionRequestAPI {
    private var buildPathsPromise: PathBuildRequest? = null
    private data class PathBuildRequest(val requestedPathSize: Int, val promise: Promise<List<Path>, Exception>)
    private val database: BeldexAPIDatabaseProtocol
        get() = MnodeModule.shared.storage
    private val broadcaster: Broadcaster
        get() = MnodeModule.shared.broadcaster
    private val pathFailureCount = mutableMapOf<Path, Int>()
    private val mnodeFailureCount = mutableMapOf<Mnode, Int>()

    var guardMnodes = setOf<Mnode>()

    /**
     * Sets the onion-routing hop count to use for all newly built paths (1 = one hop,
     * 3 = three hops). The default remains 3 for full onion-routing privacy.
     *
     * Changing this at runtime is only safe if every cached path is discarded at the same
     * time: a path that was built for 3 hops has a different layer count than one built for
     * 1 hop, and reusing a stale cached path would fail layer decryption at the first hop
     * (which is exactly the kind of onion path mismatch that crashed the app / broke sends
     * and message restore before). So when the user switches the setting we wipe the cached
     * DB paths and the in-memory guard set, and force a fresh path build on the next send.
     */
    fun setOnionRequestPathCount(count: Int) {
        if (count != 1 && count != 3) return
        val cachedPaths = paths
        val cachedPathsMatchSelection = cachedPaths.isNotEmpty() && cachedPaths.all { it.size == count }
        if (cachedPathsMatchSelection) return
        Log.d("ONION_HOP", "setOnionRequestPathCount: hop count changed to $count, wiping ${cachedPaths.size} cached path(s) and guard mnode cache")
        paths = emptyList()              // wipes DB onionRequestPaths via the paths setter
        guardMnodes = emptySet()
        pathFailureCount.clear()
        mnodeFailureCount.clear()
    }
    /**
     * Triggers a fresh path build right away if the cached paths are missing or incomplete
     * (e.g. just after the user picks a new hop count in Settings, which wipes the cache).
     *
     * Path building is lazy by design (it happens on the next onion request via getPath()), so
     * without this call the home screen status light stays red and the hops screen stays empty
     * until the next message/sync is sent. This kicks off the build immediately and lets the
     * UI refresh via the "buildingPaths"/"pathsBuilt" broadcasts. The default is a no-op when
     * a full set of paths already exists, so it is safe to call on every settings confirm.
     */
    fun rebuildPathsIfNeeded() {
        if (paths.size >= targetPathCount) { return }
        buildPaths(listOf())
    }

    var paths: List<Path> // Not a set to ensure we consistently show the same path to the user
        get() = database.getOnionRequestPaths()
        set(newValue) {
            Log.d("beldex","Three newvalue value $newValue")
            if (newValue.isEmpty()) {
                Log.d("beldex","Three newvalue value if $newValue")
                database.clearOnionRequestPaths()
            } else {
                Log.d("beldex","Three newvalue value else $newValue")

                database.setOnionRequestPaths(newValue)
            }
        }

    // region Settings
    /**
     * The number of mnodes (including the guard) per path. Sourced directly from the persisted
     * user preference rather than an in-memory field, so background workers (which never run
     * HomeActivity.update()) and any process always build paths matching the user's selection
     * (1 = one hop, 3 = three hops).
     *
     * When the user has selected "Off" (0) the count is clamped up to 1, but it is never
     * actually used: "Off" goes fully direct for mnodes (MnodeAPI.invoke) and for external
     * servers (the sendDirectRequest helper), so this only acts as a safety net should a
     * call slip through that can't go direct.
     */
    private val pathSize: Int
        get() = TextSecurePreferences.getOnionRequestPathCount(MessagingModuleConfiguration.shared.context).coerceAtLeast(1)
    /**
     * Whether the user has onion routing enabled at all. When disabled, every request - to
     * mnodes and to external servers alike - is sent directly with no onion layers.
     */
    val isOnionRoutingEnabled: Boolean
        get() = TextSecurePreferences.isOnionRoutingEnabled(MessagingModuleConfiguration.shared.context)
    /**
     * Whether external server traffic (file server, open groups, push registry, notify) should be
     * onion-routed. Single-hop relaying to an external HTTP server is broken at the masternode
     * level (the guard returns a malformed chunked response, e.g. "Expected leading [0-9a-fA-F]
     * character but was 0x67"), so server traffic is routed over the onion only in the full 3-hop
     * mode; 1-hop mode falls back to the No Hops (Direct) flow instead.
     */
    val isServerOnionRoutingEnabled: Boolean
        get() = isOnionRoutingEnabled && pathSize >= 3
    /**
     * The number of times a path can fail before it's replaced.
     */
    private const val pathFailureThreshold = 10 // 25-05-2022 change the pathFailureThreshold = 10 in before  pathFailureThreshold = 3
    /**
     * The number of times a mnode can fail before it's replaced.
     */
    private const val mnodeFailureThreshold = 5 // 25-05-2022 change the mnodeFailureThreshold = 5 in before  mnodeFailureThreshold = 3
    /**
     * The number of guard mnodes required to maintain `targetPathCount` paths.
     */
    private val targetGuardMnodeCount
        get() = targetPathCount // One per path
    /**
     * The number of paths to maintain.
     */
    const val targetPathCount = 2 // A main path and a backup path for the case where the target mnode is in the main path
    // endregion

    open class HTTPRequestFailedAtDestinationException(statusCode: Int, json: Map<*, *>, val destination: String)
        : HTTP.HTTPRequestFailedException(statusCode, json, "HTTP request failed at destination ($destination) with status code $statusCode.")
    class InsufficientMnodesException : Exception("Couldn't find enough mnodes to build a path.")

    private data class OnionBuildingResult(
        val guardMnode: Mnode,
        val finalEncryptionResult: EncryptionResult,
        val destinationSymmetricKey: ByteArray,
        val pathSize: Int
    )

    internal sealed class Destination(val description: String) {
        class Mnode(val mnode: com.beldex.libsignal.utilities.Mnode) : Destination("Master node ${mnode.ip}:${mnode.port}")
        class Server(val host: String, val target: String, val x25519PublicKey: String, val scheme: String, val port: Int) : Destination("$host")
    }

    // region Private API
    /**
     * Tests the given mnode. The returned promise errors out if the mnode is faulty; the promise is fulfilled otherwise.
     */
    private fun testMnode(mnode: Mnode): Promise<Unit, Exception> {
        val deferred = deferred<Unit, Exception>()
        ThreadUtils.queue { // No need to block the shared context for this
            val url = "${mnode.address}:${mnode.port}/get_stats/v1"
            try {
                val response = HTTP.execute(HTTP.Verb.GET, url, 3).decodeToString()
                val json = JsonUtil.fromJson(response, Map::class.java)
                val version = json["version"] as? String
                if (version == null) { deferred.reject(Exception("Missing mnode version.")); return@queue }
                if (version >= "2.0.7") {
                    deferred.resolve(Unit)
                } else {
                    val message = "Unsupported mnode version: $version."
                    Log.d("Beldex", message)
                    deferred.reject(Exception(message))
                }
            } catch (exception: Exception) {
                deferred.reject(exception)
            }
        }
        return deferred.promise
    }

    /**
     * Finds `targetGuardMnodeCount` guard mnodes to use for path building. The returned promise errors out if not
     * enough (reliable) mnodes are available.
     */
    private fun getguardMnodes(reusableGuardMnodes: List<Mnode>): Promise<Set<Mnode>, Exception> {
        Log.d("Beldex", "Populating guard mnode cache. ${guardMnodes.count().toString()}, $targetGuardMnodeCount")
        if (guardMnodes.count() >= targetGuardMnodeCount) {
            return Promise.of(guardMnodes)
        } else {
            Log.d("Beldex", "Populating guard mnode cache.")
            return MnodeAPI.getRandomMnode().bind { // Just used to populate the mnode pool
                var unusedMnodes = MnodeAPI.mnodePool.minus(reusableGuardMnodes)
                val reusableGuardMnodeCount = reusableGuardMnodes.count()
                if (unusedMnodes.count() < (targetGuardMnodeCount - reusableGuardMnodeCount)) { throw InsufficientMnodesException() }
                fun getGuardMnode(): Promise<Mnode, Exception> {
                    val candidate = unusedMnodes.getRandomElementOrNull()
                        ?: return Promise.ofFail(InsufficientMnodesException())
                    unusedMnodes = unusedMnodes.minus(candidate)
                    //-Log.d("Beldex", "Testing guard mnode: $candidate.")
                    // Loop until a reliable guard mnode is found
                    val deferred = deferred<Mnode, Exception>()
                    testMnode(candidate).success {
                        deferred.resolve(candidate)
                    }.fail {
                        deferred.reject(it)
                    }
                    return deferred.promise
                }
                val promises = (0 until (targetGuardMnodeCount - reusableGuardMnodeCount)).map { getGuardMnode() }
                all(promises).map { guardMnodes ->
                    val guardMnodesAsSet = (guardMnodes + reusableGuardMnodes).toSet()
                    OnionRequestAPI.guardMnodes = guardMnodesAsSet
                    guardMnodesAsSet
                }
            }
        }
    }

    /**
     * Builds and returns `targetPathCount` paths. The returned promise errors out if not
     * enough (reliable) mnodes are available.
     */
    private fun buildPaths(reusablePaths: List<Path>, requestedPathSize: Int = pathSize): Promise<List<Path>, Exception> {
        val existingBuildPathsPromise = buildPathsPromise?.takeIf { it.requestedPathSize == requestedPathSize }?.promise
        if (existingBuildPathsPromise != null) { return existingBuildPathsPromise }
        Log.d("Beldex", "Building onion request paths.")
        broadcaster.broadcast("buildingPaths")
        val promise = MnodeAPI.getRandomMnode().bind { // Just used to populate the mnode pool
            val reusableGuardMnodes = reusablePaths.map { it[0] }
            getguardMnodes(reusableGuardMnodes).map { guardMnodes ->
                var unusedMnodes = MnodeAPI.mnodePool.minus(guardMnodes).minus(reusablePaths.flatten())
                val reusableGuardMnodeCount = reusableGuardMnodes.count()
                val pathMnodeCount = (targetGuardMnodeCount - reusableGuardMnodeCount) * requestedPathSize - (targetGuardMnodeCount - reusableGuardMnodeCount)
                if (unusedMnodes.count() < pathMnodeCount) { throw InsufficientMnodesException() }
                // Don't test path mnodes as this would reveal the user's IP to them
                guardMnodes.minus(reusableGuardMnodes).map { guardMnode ->
                    val result = listOf( guardMnode ) + (0 until (requestedPathSize - 1)).map {
                        val pathMnode = unusedMnodes.getRandomElement()
                        unusedMnodes = unusedMnodes.minus(pathMnode)
                        pathMnode
                    }
                    Log.d("Beldex","Three onion request path result guardMnode  $guardMnode")
                    // Prints the full path values for both 1-hop (requestedPathSize=1) and 3-hop (requestedPathSize=3)
                    Log.d("ONION_HOP", "Built ${requestedPathSize}-hop path (${result.size} mnode(s)): " +
                        result.mapIndexed { index, mnode ->
                            "H${index + 1}=${mnode.address}:${mnode.port} (ed25519=${mnode.publicKeySet?.ed25519Key}, x25519=${mnode.publicKeySet?.x25519Key})"
                        }.joinToString(" -> "))

//                     Built new onion request path: [https://54.202.123.159:19090, https://3.26.207.159:19090, https://13.208.47.131:19090]

                    //-Log.d("Beldex", "Three onion request path: $result.")
                    result
                }
            }.map { paths ->
                OnionRequestAPI.paths = paths + reusablePaths
                //-Log.d("Beldex", "Three onion request path OnionRequestAPI.paths: ${OnionRequestAPI.paths}.")
                broadcaster.broadcast("pathsBuilt")
                paths
            }
        }
        promise.success { buildPathsPromise = null }
        promise.fail { buildPathsPromise = null }
        buildPathsPromise = PathBuildRequest(requestedPathSize, promise)
        return promise
    }

    /**
     * Returns a `Path` to be used for building an onion request. Builds new paths as needed.
     */
    private fun getPath(mnodeToExclude: Mnode?, requestedPathSize: Int = pathSize): Promise<Path, Exception> {
        if (requestedPathSize < 1) { throw Exception("Can't build path of size zero.") }
        // Only consider cached paths that match the requested hop count. Cached paths may hold a
        // mix of sizes (e.g. 1-hop message paths and 3-hop server paths); any path whose length
        // doesn't match the current request is ignored (and rebuilt for the size that needs it).
        var paths = this.paths.filter { it.size == requestedPathSize }
        if (this.paths.any { it.size != requestedPathSize }) {
            Log.d("ONION_HOP", "getPath: ${this.paths.count { it.size != requestedPathSize }} cached path(s) with a different hop count ignored")
        }
        val guardMnodes = mutableSetOf<Mnode>()
        if (paths.isNotEmpty()) {
            guardMnodes.add(paths[0][0])
            if (paths.count() >= 2) {
                guardMnodes.add(paths[1][0])
            }
        }
        OnionRequestAPI.guardMnodes = guardMnodes
        fun selectPath(paths: List<Path>): Path {
            val selected = if (mnodeToExclude != null) {
                paths.filter { !it.contains(mnodeToExclude) }.getRandomElement()
            } else {
                paths.getRandomElement()
            }
            Log.d("ONION_HOP", "getPath selected ${selected.size}-hop path: " +
                selected.mapIndexed { index, mnode ->
                    "H${index + 1}=${mnode.address}:${mnode.port}"
                }.joinToString(" -> ") + " (excluded target: $mnodeToExclude)")
            return selected
        }
        Log.d("Beldex", "Three onion request path ${paths.count().toString()}, $targetPathCount ")

        when {
            paths.count() >= targetPathCount -> {
                Log.d("Beldex", "Three onion request path if")
                return Promise.of(selectPath(paths))
            }
            paths.isNotEmpty() -> {
                Log.d("Beldex", "Three onion request path else")
                return if (paths.any { !it.contains(mnodeToExclude) }) {
                    Log.d("Beldex", "Three onion request second if condition ${mnodeToExclude.toString()}")
                    buildPaths(paths, requestedPathSize) // Re-build paths in the background
                    Promise.of(selectPath(paths))
                } else {
                    Log.d("Beldex", "Three onion request second else condition")
                    buildPaths(paths, requestedPathSize).bind { newPaths ->
                        Promise.of(selectPath(newPaths))
                    }
                }
            }
            else -> {
                Log.d("Beldex", "Three onion request first else condition")
                return buildPaths(listOf(), requestedPathSize).bind { newPaths ->
                    Promise.of(selectPath(newPaths))
                }
            }
        }
    }

    private fun dropGuardMnode(mnode: Mnode) {
        guardMnodes = guardMnodes.filter { it != mnode }.toSet()
    }

    private fun dropMnode(mnode: Mnode) {
        // We repair the path here because we can do it sync. In the case where we drop a whole
        // path we leave the re-building up to getPath() because re-building the path in that case
        // is async.
        mnodeFailureCount[mnode] = 0
        val oldPaths = paths.toMutableList()
        val pathIndex = oldPaths.indexOfFirst { it.contains(mnode) }
        if (pathIndex == -1) { return }
        val path = oldPaths[pathIndex].toMutableList()
        val mnodeIndex = path.indexOf(mnode)
        if (mnodeIndex == -1) { return }
        path.removeAt(mnodeIndex)
        val unusedMnodes = MnodeAPI.mnodePool.minus(oldPaths.flatten())
        if (unusedMnodes.isEmpty()) { throw InsufficientMnodesException() }
        path.add(unusedMnodes.getRandomElement())
        // Don't test the new mnode as this would reveal the user's IP
        oldPaths.removeAt(pathIndex)
        val newPaths = oldPaths + listOf( path )
        paths = newPaths
    }

    private fun dropPath(path: Path) {
        Log.d("Beldex", "Three onion request path -- ${paths.count().toString()}, $targetPathCount ")
        pathFailureCount[path] = 0
        val paths = OnionRequestAPI.paths.toMutableList()
        val pathIndex = paths.indexOf(path)
        if (pathIndex == -1) { return }
        paths.removeAt(pathIndex)
        OnionRequestAPI.paths = paths
    }

    /**
     * Builds an onion around `payload` and returns the result.
     */
    private fun buildOnionForDestination(payload: ByteArray, destination: Destination, version: Version): Promise<OnionBuildingResult, Exception> {
        lateinit var guardMnode: Mnode
        var usedPathSize = 0
        lateinit var destinationSymmetricKey: ByteArray // Needed by BeldexAPI to decrypt the response sent back by the destination
        lateinit var encryptionResult: EncryptionResult
        val mnodeToExclude = when (destination) {
            is Destination.Mnode -> destination.mnode
            is Destination.Server -> null
        }
        Log.d("Beldex", "Path build mnodeToExclude  $mnodeToExclude")
        // Chat traffic (mnodes) follows the user's hop count selection. External servers (file
        // server, open groups, push registry, notify) never reach this builder at 1-hop: they are
        // gated on isServerOnionRoutingEnabled and in 1-hop mode they go direct (sendDirectRequest)
        // instead, because a single-hop relay to an external HTTP server returns a malformed
        // chunked response from the guard. When servers DO use the onion (3-hop mode) the path size
        // is 3 anyway, so the requested size always matches here.
        val requestedPathSize = pathSize
        return getPath(mnodeToExclude, requestedPathSize).bind { path ->
            guardMnode = path.first()
            usedPathSize = path.size
            Log.d("Beldex","Path build first  $guardMnode")
            // 1-hop vs 3-hop decision point: prints the path being used and exactly how many onion layers will be added
            Log.d("ONION_HOP", "buildOnionForDestination: using ${path.size}-hop path (guard ${guardMnode.address}:${guardMnode.port}) -> encryption layers = ${path.size}")
            // Encrypt in reverse order, i.e. the destination first
            OnionRequestEncryption.encryptPayloadForDestination(payload, destination, version).bind { r ->
                destinationSymmetricKey = r.symmetricKey
                // Recursively encrypt the layers of the onion (again in reverse order)
                encryptionResult = r
                @Suppress("NAME_SHADOWING") var path = path
                var rhs = destination
                fun addLayer(): Promise<EncryptionResult, Exception> {
                    return if (path.isEmpty()) {
                        Promise.of(encryptionResult)
                    } else {
                        val lhs = Destination.Mnode(path.last())
                        path = path.dropLast(1)
                        Log.d("ONION_HOP", "Wrapping onion layer: lhs=${lhs.description} -> rhs=${rhs.description} (${path.size} layer(s) remaining)")
                        OnionRequestEncryption.encryptHop(lhs, rhs, encryptionResult).bind { r ->
                            encryptionResult = r
                            rhs = lhs
                            addLayer()
                        }
                    }
                }
                addLayer()
            }
        }.map { OnionBuildingResult(guardMnode, encryptionResult, destinationSymmetricKey, usedPathSize) }
    }

    /**
     * Sends an onion request to `destination`. Builds new paths as needed.
     */
    private fun sendOnionRequest(destination: Destination, payload: ByteArray, version: Version): Promise<OnionResponse, Exception> {
        val deferred = deferred<OnionResponse, Exception>()
        var guardMnode: Mnode? = null
        Log.d("Beldex --> payload onion request ","$payload")
        Log.d("Beldex --> destination onion request ","$destination")
        buildOnionForDestination(payload, destination, version).success { result ->
            guardMnode = result.guardMnode
            Log.d("Beldex","guard node-- $guardMnode")
            //Original
            val nonNullGuardSnode = result.guardMnode
            val url = "${nonNullGuardSnode.address}:${nonNullGuardSnode.port}/onion_req/v2"
            Log.d("ONION_HOP", "sendOnionRequest: POSTing ${result.pathSize}-hop onion to guard ${nonNullGuardSnode.address}:${nonNullGuardSnode.port} at $url, ciphertext size=${result.finalEncryptionResult.ciphertext.size} bytes")

            //10-05-2022 - 5.21 PM
            //val url = "https://13.233.54.176:19090/onion_req/v2"
            //12-05-2022 - 5.39 PM
            //val url = "https://3.7.154.25:19090/onion_req/v2"
            //-Log.d("Beldex --> onion request url","$url")
            val finalEncryptionResult = result.finalEncryptionResult
            val onion = finalEncryptionResult.ciphertext
            if (destination is Destination.Server && onion.count().toDouble() > 0.75 * FileServerAPIV2.maxFileSize.toDouble()) {
                //-Log.d("Beldex", "Approaching request size limit: ~${onion.count()} bytes.")
            }
            @Suppress("NAME_SHADOWING") val parameters = mapOf(
                "ephemeral_key" to finalEncryptionResult.ephemeralPublicKey.toHexString()
            )
            val body: ByteArray
            try {
                //-Log.d("Beldex","url for Onion request $url")
                body = OnionRequestEncryption.encode(onion, parameters)
                //-Log.d("Beldex --> OnionRequestEncryption ","$body")
            } catch (exception: Exception) {
                return@success deferred.reject(exception)
            }
            val destinationSymmetricKey = result.destinationSymmetricKey
            ThreadUtils.queue {
                try {
                    Log.d("Beldex","am try in Onion req")
                    val response = HTTP.execute(HTTP.Verb.POST, url, body)
                    handleResponse(response, destinationSymmetricKey, destination, version, deferred)
                } catch (exception: Exception) {
                    Log.d("Beldex","Am Exception 2 in Onion request")
                    deferred.reject(exception)
                }
            }
        }.fail { exception ->
            deferred.reject(exception)
        }
        val promise = deferred.promise
        promise.fail { exception ->
            if (exception is HTTP.HTTPRequestFailedException && MnodeModule.isInitialized) {
                val checkedGuardMnode = guardMnode
                val path =
                    if (checkedGuardMnode == null) null
                    else paths.firstOrNull { it.contains(checkedGuardMnode) }
                fun handleUnspecificError() {
                    if (path == null) { return }
                    var pathFailureCount = OnionRequestAPI.pathFailureCount[path] ?: 0
                    pathFailureCount += 1
                    if (pathFailureCount >= pathFailureThreshold) {
                        guardMnode?.let { dropGuardMnode(it) }
                        path.forEach { mnode ->
                            @Suppress("ThrowableNotThrown")
                            MnodeAPI.handleMnodeError(exception.statusCode, exception.json, mnode, null) // Intentionally don't throw
                            Log.d("Beldex", "Three onion request path new-- ${paths.count().toString()}, $targetPathCount ")
                        }
                        //Important function just testing for command below this function
                        dropPath(path)
                    } else {
                        OnionRequestAPI.pathFailureCount[path] = pathFailureCount
                    }
                }
                val json = exception.json
                val message = json?.get("result") as? String
                val prefix = "Next node not found: "
                if (message != null && message.startsWith(prefix)) {
                    val ed25519PublicKey = message.substringAfter(prefix)
                    val mnode = path?.firstOrNull { it.publicKeySet!!.ed25519Key == ed25519PublicKey }
                    if (mnode != null) {
                        var mnodeFailureCount = OnionRequestAPI.mnodeFailureCount[mnode] ?: 0
                        mnodeFailureCount += 1
                        if (mnodeFailureCount >= mnodeFailureThreshold) {
                            @Suppress("ThrowableNotThrown")
                            MnodeAPI.handleMnodeError(exception.statusCode, json, mnode, null) // Intentionally don't throw
                            try {
                                dropMnode(mnode)
                            } catch (exception: Exception) {
                                handleUnspecificError()
                            }
                        } else {
                            OnionRequestAPI.mnodeFailureCount[mnode] = mnodeFailureCount
                        }
                    } else {
                        handleUnspecificError()
                    }
                } else if (destination is Destination.Server && exception.statusCode == 400) {
                    Log.d("Beldex","Destination server returned ${exception.statusCode}")
                } else if (message == "Beldex Server error") {
                    Log.d("Beldex", "message was $message")
                } else if (exception.statusCode == 404) {
                    // 404 is probably file server missing a file, don't rebuild path or mark a mnode as bad here
                } else { // Only drop mnode/path if not receiving above two exception cases
                    handleUnspecificError()
                }
            }
        }
        return promise
    }
    // endregion

    // region Internal API
    /**
     * Sends an onion request to `mnode`. Builds new paths as needed.
     */
    internal fun sendOnionRequest(method: Mnode.Method, parameters: Map<*, *>, mnode: Mnode, publicKey: String? = null, version: Version): Promise<OnionResponse, Exception> {
        val payload = mapOf( "method" to method.rawValue, "params" to parameters )
        //-Log.d("Beldex","payload in sendOnionRequest $payload ")
        //-Log.d("Beldex","parameters in sendOnionRequest $parameters ")
        val payloadData = JsonUtil.toJson(payload).toByteArray()
        return sendOnionRequest(Destination.Mnode(mnode), payloadData, version).recover { exception ->
            val error = when (exception) {
                is HTTPRequestFailedAtDestinationException -> MnodeAPI.handleMnodeError(exception.statusCode, exception.json, mnode, publicKey)
                is HTTP.HTTPRequestFailedException -> MnodeAPI.handleMnodeError(exception.statusCode, exception.json, mnode, publicKey)
                else -> null
            }
            if (error != null) { throw error }
            throw exception
        }
    }

    /**
     * Sends an onion request to `server`. Builds new paths as needed.
     *
     * `publicKey` is the hex encoded public key of the user the call is associated with. This is needed for swarm cache maintenance.
     */
    fun sendOnionRequest(
        request: Request,
        server: String,
        x25519PublicKey: String,
        version: Version = Version.V4
    ): Promise<OnionResponse, Exception> {
        val url = request.url
        val payload = generatePayload(request, server, version)
        val destination = Destination.Server(url.host, version.value, x25519PublicKey, url.scheme, url.port)
        return sendOnionRequest(destination, payload, version).recover { exception ->
            Log.d("Beldex", "Couldn't reach server: $url due to error: $exception.")
            throw exception
        }
    }

    /**
     * Sends `request` directly to the server with no onion layers. Used when the user has
     * onion routing disabled. The server's plain (JSON) response is parsed and wrapped in an
     * [OnionResponse]; no AESGCM decryption is performed since the transport isn't onion
     * encrypted. If the server answers with a `{ "body": ... }` envelope the body is unwrapped,
     * mirroring what the onion response handler does.
     */
    fun sendDirectRequest(request: Request): Promise<OnionResponse, Exception> {
        val deferred = deferred<OnionResponse, Exception>()
        ThreadUtils.queue {
            try {
                Log.d("ONION_HOP", "sendDirectRequest: ${request.method} ${request.url}")
                val response = HTTP.execute(request)
                val json = try {
                    JsonUtil.fromJson(response, Map::class.java)
                } catch (exception: Exception) {
                    mapOf("result" to response.decodeToString())
                }
                val info: Map<*, *> = when (val body = json["body"]) {
                    is Map<*, *> -> body
                    is String -> {
                        try {
                            JsonUtil.fromJson(body, Map::class.java)
                        } catch (exception: Exception) {
                            json
                        }
                    }
                    else -> json
                }
                deferred.resolve(OnionResponse(info, JsonUtil.toJson(info).toByteArray()))
            } catch (exception: Exception) {
                deferred.reject(exception)
            }
        }
        return deferred.promise
    }
    private fun generatePayload(request: Request, server: String, version: Version): ByteArray {
        val headers = request.getHeadersForOnionRequest().toMutableMap()
        Log.d("Beldex","sendOnionRequest for social group header $headers")
        val url = request.url
        Log.d("Beldex","sendOnionRequest for social group url $url")
        val urlAsString = url.toString()
        Log.d("Beldex","sendOnionRequest for social group urlAsString $urlAsString")
        val body = request.getBodyForOnionRequest() ?: "null"
        Log.d("Beldex","sendOnionRequest for social group host $body")
        val endpoint = when {
            server.count() < urlAsString.count() -> urlAsString.substringAfter(server)
            else -> ""
        }
        Log.d("Beldex","sendOnionRequest for social group end point  $endpoint")
        return if (version == Version.V4) {
            if (request.body != null &&
                headers.keys.find { it.equals("Content-Type", true) } == null) {
                headers["Content-Type"] = "application/json"
            }
            val requestPayload = mapOf(
                "endpoint" to endpoint,
                "method" to request.method,
                "headers" to headers
            )
            val requestData = JsonUtil.toJson(requestPayload).toByteArray()
            val prefixData = "l${requestData.size}:".toByteArray(Charsets.US_ASCII)
            val suffixData = "e".toByteArray(Charsets.US_ASCII)
            if (request.body != null) {
                val bodyData = body.toString().toByteArray()
                val bodyLengthData = "${bodyData.size}:".toByteArray(Charsets.US_ASCII)
                prefixData + requestData + bodyLengthData + bodyData + suffixData
            } else {
                prefixData + requestData + suffixData
            }
        } else {
            val payload = mapOf(
                "body" to body,
                "endpoint" to endpoint.removePrefix("/"),
                "method" to request.method,
                "headers" to headers
            )
            JsonUtil.toJson(payload).toByteArray()
        }
    }
    private fun handleResponse(
        response: ByteArray,
        destinationSymmetricKey: ByteArray,
        destination: Destination,
        version: Version,
        deferred: Deferred<OnionResponse, Exception>
    ) {
        if (version == Version.V4) {
            try {
                if (response.size <= AESGCM.ivSize) return deferred.reject(Exception("Invalid response"))
                // The data will be in the form of `l123:jsone` or `l123:json456:bodye` so we need to break the data into
                // parts to properly process it
                val plaintext = AESGCM.decrypt(response, destinationSymmetricKey)
                if (!byteArrayOf(plaintext.first()).contentEquals("l".toByteArray())) return deferred.reject(Exception("Invalid response"))
                val infoSepIdx = plaintext.indexOfFirst { byteArrayOf(it).contentEquals(":".toByteArray()) }
                val infoLenSlice = plaintext.slice(1 until infoSepIdx)
                val infoLength = infoLenSlice.toByteArray().toString(Charsets.US_ASCII).toIntOrNull()
                if (infoLenSlice.size <= 1 || infoLength == null) return deferred.reject(Exception("Invalid response"))
                val infoStartIndex = "l$infoLength".length + 1
                val infoEndIndex = infoStartIndex + infoLength
                val info = plaintext.slice(infoStartIndex until infoEndIndex)
                val responseInfo = JsonUtil.fromJson(info.toByteArray(), Map::class.java)
                when (val statusCode = responseInfo["code"].toString().toInt()) {
                    // Custom handle a clock out of sync error (v4 returns '425' but included the '406' just in case)
                    406, 425 -> {
                        @Suppress("NAME_SHADOWING")
                        val exception = HTTPRequestFailedAtDestinationException(
                            statusCode,
                            mapOf("result" to "Your clock is out of sync with the master node network."),
                            destination.description
                        )
                        return deferred.reject(exception)
                    }
                    // Handle error status codes
                    !in 200..299 -> {
                        val exception = HTTPRequestFailedAtDestinationException(
                            statusCode,
                            responseInfo,
                            destination.description
                        )
                        return deferred.reject(exception)
                    }
                }
                val responseBody = plaintext.getBody(infoLength, infoEndIndex)
                // If there is no data in the response, i.e. only `l123:jsone`, then just return the ResponseInfo
                if (responseBody.isEmpty()) {
                    return deferred.resolve(OnionResponse(responseInfo, null))
                }
                return deferred.resolve(OnionResponse(responseInfo, responseBody))
            } catch (exception: Exception) {
                deferred.reject(exception)
            }
        } else {
            val json = try {
                JsonUtil.fromJson(response, Map::class.java)
            } catch (exception: Exception) {
                mapOf( "result" to response.decodeToString())
            }
            //-Log.d("Beldex","json Onion request $json")
            val base64EncodedIVAndCiphertext = json["result"] as? String ?: return deferred.reject(Exception("Invalid JSON"))
            val ivAndCiphertext = Base64.decode(base64EncodedIVAndCiphertext)
            try {
                val plaintext = AESGCM.decrypt(ivAndCiphertext, destinationSymmetricKey)
                try {
                    @Suppress("NAME_SHADOWING") val json = JsonUtil.fromJson(plaintext.toString(Charsets.UTF_8), Map::class.java)
                    val statusCode = json["status_code"] as? Int ?: json["status"] as Int
                    if (statusCode == 406) {
                        @Suppress("NAME_SHADOWING") val body = mapOf( "result" to "Your clock is out of sync with the master node network." )
                        val exception = HTTPRequestFailedAtDestinationException(statusCode, body, destination.description)
                        return deferred.reject(exception)
                    } else if (json["body"] != null) {
                        @Suppress("NAME_SHADOWING")
                        val body: Map<*, *> = if (json["body"] is Map<*, *>) {
                            json["body"] as Map<*, *>
                        } else {
                            val bodyAsString = json["body"] as String
                            JsonUtil.fromJson(bodyAsString, Map::class.java)
                        }
                        if (body["t"] != null) {
                            val timestamp = body["t"] as Long
                            val offset = timestamp - Date().time
                            MnodeAPI.clockOffset = offset
                        }
                        if (body.containsKey("hf")) {
                            @Suppress("UNCHECKED_CAST")
                            val currentHf = body["hf"] as List<Int>
                            if (currentHf.size < 2) {
                                Log.e("Beldex", "Response contains fork information but doesn't have a hard and soft number")
                            } else {
                                val hf = currentHf[0]
                                val sf = currentHf[1]
                                val newForkInfo = ForkInfo(hf, sf)
                                if (newForkInfo > MnodeAPI.forkInfo) {
                                    MnodeAPI.forkInfo = ForkInfo(hf,sf)
                                } else if (newForkInfo < MnodeAPI.forkInfo) {
                                    Log.w("Beldex", "Got a new mnode info fork version that was $newForkInfo, less than current known ${MnodeAPI.forkInfo}")
                                }
                            }
                        }
                        if (statusCode != 200) {
                            val exception = HTTPRequestFailedAtDestinationException(statusCode, body, destination.description)
                            return deferred.reject(exception)
                        }
                        deferred.resolve(OnionResponse(body, JsonUtil.toJson(body).toByteArray()))
                    } else {
                        if (statusCode != 200) {
                            val exception = HTTPRequestFailedAtDestinationException(statusCode, json, destination.description)
                            return deferred.reject(exception)
                        }
                        deferred.resolve(OnionResponse(json, JsonUtil.toJson(json).toByteArray()))
                    }
                } catch (exception: Exception) {
                    deferred.reject(Exception("Invalid JSON: ${plaintext.toString(Charsets.UTF_8)}."))
                }
            } catch (exception: Exception) {
                deferred.reject(exception)
            }
        }
    }
    private fun ByteArray.getBody(infoLength: Int, infoEndIndex: Int): ByteArray {
        // If there is no data in the response, i.e. only `l123:jsone`, then just return the ResponseInfo
        val infoLengthStringLength = infoLength.toString().length
        if (size <= infoLength + infoLengthStringLength + 2/*l and e bytes*/) {
            return byteArrayOf()
        }
        // Extract the response data as well
        val dataSlice = slice(infoEndIndex + 1 until size - 1)
        val dataSepIdx = dataSlice.indexOfFirst { byteArrayOf(it).contentEquals(":".toByteArray()) }
        val responseBody = dataSlice.slice(dataSepIdx + 1 until dataSlice.size)
        return responseBody.toByteArray()
    }
    // endregion
}

enum class Version(val value: String) {
    V2("/beldex/v2/lsrpc"),
    V3("/beldex/v3/lsrpc"),
    V4("/beldex/v4/lsrpc");
}
data class OnionResponse(
    val info: Map<*, *>,
    val body: ByteArray? = null
) {
    val code: Int? get() = info["code"] as? Int
    val message: String? get() = info["message"] as? String
}
