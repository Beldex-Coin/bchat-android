package com.beldex.libbchat.mnode

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.all
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import okhttp3.Request
import com.beldex.libbchat.messaging.file_server.FileServerAPIV2
import com.beldex.libbchat.utilities.AESGCM
import com.beldex.libsignal.utilities.*
import com.beldex.libsignal.utilities.Mnode
import com.beldex.libbchat.utilities.AESGCM.EncryptionResult
import com.beldex.libbchat.utilities.getBodyForOnionRequest
import com.beldex.libbchat.utilities.getHeadersForOnionRequest
import com.beldex.libsignal.crypto.getRandomElement
import com.beldex.libsignal.crypto.getRandomElementOrNull
import com.beldex.libsignal.utilities.Broadcaster
import com.beldex.libsignal.utilities.HTTP
import com.beldex.libsignal.database.BeldexAPIDatabaseProtocol
import com.beldex.libsignal.utilities.Base64
import java.util.*

private typealias Path = List<Mnode>

object OnionRequestAPI {
    private var buildPathsPromise: Promise<List<Path>, Exception>? = null
    private val database: BeldexAPIDatabaseProtocol
        get() = MnodeModule.shared.storage
    private val broadcaster: Broadcaster
        get() = MnodeModule.shared.broadcaster
    private val pathFailureCount = mutableMapOf<Path, Int>()
    private val mnodeFailureCount = mutableMapOf<Mnode, Int>()

    var guardMnodes = setOf<Mnode>()
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
     * The number of mnodes (including the guard mnode) in a path.
     */
    private const val pathSize = 3
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

    class HTTPRequestFailedAtDestinationException(val statusCode: Int, val json: Map<*, *>, val destination: String)
        : Exception("HTTP request failed at destination ($destination) with status code $statusCode.")
    class InsufficientMnodesException : Exception("Couldn't find enough mnodes to build a path.")

    private data class OnionBuildingResult(
        val guardMnode: Mnode,
        val finalEncryptionResult: EncryptionResult,
        val destinationSymmetricKey: ByteArray
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
                val json = HTTP.execute(HTTP.Verb.GET, url, 3)
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
                        getGuardMnode().success {
                            deferred.resolve(candidate)
                        }.fail { exception ->
                            if (exception is InsufficientMnodesException) {
                                deferred.reject(exception)
                            }
                        }
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
    private fun buildPaths(reusablePaths: List<Path>): Promise<List<Path>, Exception> {
        val existingBuildPathsPromise = buildPathsPromise
        if (existingBuildPathsPromise != null) { return existingBuildPathsPromise }
        Log.d("Beldex", "Building onion request paths.")
        broadcaster.broadcast("buildingPaths")
        val promise = MnodeAPI.getRandomMnode().bind { // Just used to populate the mnode pool
            val reusableGuardMnodes = reusablePaths.map { it[0] }
            getguardMnodes(reusableGuardMnodes).map { guardMnodes ->
                var unusedMnodes = MnodeAPI.mnodePool.minus(guardMnodes).minus(reusablePaths.flatten())
                val reusableGuardMnodeCount = reusableGuardMnodes.count()
                val pathMnodeCount = (targetGuardMnodeCount - reusableGuardMnodeCount) * pathSize - (targetGuardMnodeCount - reusableGuardMnodeCount)
                if (unusedMnodes.count() < pathMnodeCount) { throw InsufficientMnodesException() }
                // Don't test path mnodes as this would reveal the user's IP to them
                guardMnodes.minus(reusableGuardMnodes).map { guardMnode ->
                    val result = listOf( guardMnode ) + (0 until (pathSize - 1)).map {
                        val pathMnode = unusedMnodes.getRandomElement()
                        unusedMnodes = unusedMnodes.minus(pathMnode)
                        pathMnode
                    }
                    Log.d("Beldex","Three onion request path result guardMnode  $guardMnode")

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
        buildPathsPromise = promise
        return promise
    }

    /**
     * Returns a `Path` to be used for building an onion request. Builds new paths as needed.
     */
    private fun getPath(mnodeToExclude: Mnode?): Promise<Path, Exception> {
        if (pathSize < 1) { throw Exception("Can't build path of size zero.") }
        val paths = this.paths
        val guardMnodes = mutableSetOf<Mnode>()
        if (paths.isNotEmpty()) {
            guardMnodes.add(paths[0][0])
            if (paths.count() >= 2) {
                guardMnodes.add(paths[1][0])
            }
        }
        OnionRequestAPI.guardMnodes = guardMnodes
        fun getPath(paths: List<Path>): Path {
            if (mnodeToExclude != null) {
                return paths.filter { !it.contains(mnodeToExclude) }.getRandomElement()
            } else {
                return paths.getRandomElement()
            }
        }
        Log.d("Beldex", "Three onion request path ${paths.count().toString()}, $targetPathCount ")

        if (paths.count() >= targetPathCount) {
            Log.d("Beldex", "Three onion request path if")

            return Promise.of(getPath(paths))
        } else if (paths.isNotEmpty()) {
            Log.d("Beldex", "Three onion request path else")

            if (paths.any { !it.contains(mnodeToExclude) }) {
                Log.d("Beldex", "Three onion request second if condition ${mnodeToExclude.toString()}")
                buildPaths(paths) // Re-build paths in the background
                return Promise.of(getPath(paths))
            } else {
                Log.d("Beldex", "Three onion request second else condition")
                return buildPaths(paths).map { newPaths ->
                    getPath(newPaths)
                }
            }
        } else {
            Log.d("Beldex", "Three onion request first else condition")
            return buildPaths(listOf()).map { newPaths ->
                getPath(newPaths)
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
    private fun buildOnionForDestination(payload: Map<*, *>, destination: Destination): Promise<OnionBuildingResult, Exception> {
        lateinit var guardMnode: Mnode
        lateinit var destinationSymmetricKey: ByteArray // Needed by BeldexAPI to decrypt the response sent back by the destination
        lateinit var encryptionResult: EncryptionResult
        val mnodeToExclude = when (destination) {
            is Destination.Mnode -> destination.mnode
            is Destination.Server -> null
        }
        Log.d("Beldex","Path build mnodeToExclude  $mnodeToExclude")
        return getPath(mnodeToExclude).bind { path ->
            guardMnode = path.first()
            Log.d("Beldex","Path build first  $guardMnode")
            // Encrypt in reverse order, i.e. the destination first
            OnionRequestEncryption.encryptPayloadForDestination(payload, destination).bind { r ->
                destinationSymmetricKey = r.symmetricKey
                // Recursively encrypt the layers of the onion (again in reverse order)
                encryptionResult = r
                @Suppress("NAME_SHADOWING") var path = path
                var rhs = destination
                fun addLayer(): Promise<EncryptionResult, Exception> {
                    if (path.isEmpty()) {
                        return Promise.of(encryptionResult)
                    } else {
                        val lhs = Destination.Mnode(path.last())
                        path = path.dropLast(1)
                        return OnionRequestEncryption.encryptHop(lhs, rhs, encryptionResult).bind { r ->
                            encryptionResult = r
                            rhs = lhs
                            addLayer()
                        }
                    }
                }
                addLayer()
            }
        }.map { OnionBuildingResult(guardMnode, encryptionResult, destinationSymmetricKey) }
    }

    /**
     * Sends an onion request to `destination`. Builds new paths as needed.
     */
    private fun sendOnionRequest(destination: Destination, payload: Map<*, *>): Promise<Map<*, *>, Exception> {
        val deferred = deferred<Map<*, *>, Exception>()
        lateinit var guardMnode: Mnode
        Log.d("Beldex --> payload onion req uest ","$payload")
        Log.d("Beldex --> destination onion request ","$destination")
        buildOnionForDestination(payload, destination).success { result ->
            guardMnode = result.guardMnode
            Log.d("Beldex","guard node-- $guardMnode")
            //Original
            val url = "${guardMnode.address}:${guardMnode.port}/onion_req/v2"

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
                    val json = HTTP.execute(HTTP.Verb.POST, url, body)
                    //-Log.d("Beldex","json Onion request $json")
                    val base64EncodedIVAndCiphertext = json["result"] as? String ?: return@queue deferred.reject(Exception("Invalid JSON"))
                    val ivAndCiphertext = Base64.decode(base64EncodedIVAndCiphertext)

                    try {
                        val plaintext = AESGCM.decrypt(ivAndCiphertext, destinationSymmetricKey)

                        try {
                            @Suppress("NAME_SHADOWING") val json = JsonUtil.fromJson(plaintext.toString(Charsets.UTF_8), Map::class.java)
                            val statusCode = json["status_code"] as? Int ?: json["status"] as Int
                            if (statusCode == 406) {
                                @Suppress("NAME_SHADOWING") val body = mapOf( "result" to "Your clock is out of sync with the master node network." )
                                val exception = HTTPRequestFailedAtDestinationException(statusCode, body, destination.description)
                                return@queue deferred.reject(exception)
                            } else if (json["body"] != null) {
                                @Suppress("NAME_SHADOWING") val body: Map<*, *>
                                if (json["body"] is Map<*, *>) {
                                    body = json["body"] as Map<*, *>
                                } else {
                                    val bodyAsString = json["body"] as String
                                    body = JsonUtil.fromJson(bodyAsString, Map::class.java)
                                }
                                if (body["t"] != null) {
                                    val timestamp = body["t"] as Long
                                    val offset = timestamp - Date().time
                                    MnodeAPI.clockOffset = offset
                                }
                                if (statusCode != 200) {
                                    val exception = HTTPRequestFailedAtDestinationException(statusCode, body, destination.description)
                                    return@queue deferred.reject(exception)
                                }
                                deferred.resolve(body)
                            } else {
                                if (statusCode != 200) {
                                    val exception = HTTPRequestFailedAtDestinationException(statusCode, json, destination.description)
                                    return@queue deferred.reject(exception)
                                }
                                deferred.resolve(json)
                            }
                        } catch (exception: Exception) {
                            deferred.reject(Exception("Invalid JSON: ${plaintext.toString(Charsets.UTF_8)}."))
                        }
                    } catch (exception: Exception) {
                        deferred.reject(exception)
                    }
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
                val path = paths.firstOrNull { it.contains(guardMnode) }
                fun handleUnspecificError() {
                    if (path == null) { return }
                    var pathFailureCount = OnionRequestAPI.pathFailureCount[path] ?: 0
                    pathFailureCount += 1
                    if (pathFailureCount >= pathFailureThreshold) {
                        dropGuardMnode(guardMnode)
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
    internal fun sendOnionRequest(method: Mnode.Method, parameters: Map<*, *>, mnode: Mnode, publicKey: String? = null): Promise<Map<*, *>, Exception> {
        val payload = mapOf( "method" to method.rawValue, "params" to parameters )
        //-Log.d("Beldex","payload in sendOnionRequest $payload ")
        //-Log.d("Beldex","parameters in sendOnionRequest $parameters ")
        return sendOnionRequest(Destination.Mnode(mnode), payload).recover { exception ->
            val error = when (exception) {
                is HTTP.HTTPRequestFailedException -> MnodeAPI.handleMnodeError(exception.statusCode, exception.json, mnode, publicKey)
                is HTTPRequestFailedAtDestinationException -> MnodeAPI.handleMnodeError(exception.statusCode, exception.json, mnode, publicKey)
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
    fun sendOnionRequest(request: Request, server: String, x25519PublicKey: String, target: String = "/beldex/v3/lsrpc"): Promise<Map<*, *>, Exception> {
        val headers = request.getHeadersForOnionRequest()
        Log.d("Beldex","sendOnionRequest for social group header $headers")
        val url = request.url()
        Log.d("Beldex","sendOnionRequest for social group url $url")
        val urlAsString = url.toString()
        Log.d("Beldex","sendOnionRequest for social group urlAsString $urlAsString")
        val host = url.host()
        Log.d("Beldex","sendOnionRequest for social group host $host")
        val endpoint = when {
            server.count() < urlAsString.count() -> urlAsString.substringAfter(server).removePrefix("/")
            else -> ""
        }
        Log.d("Beldex","sendOnionRequest for social group end point  $endpoint")
        val body = request.getBodyForOnionRequest() ?: "null"
        //-Log.d("Beldex","sendOnionRequest for social group body  $body")
        val payload = mapOf(
            "body" to body,
            "endpoint" to endpoint,
            "method" to request.method(),
            "headers" to headers
        )
        //-Log.d("Beldex","sendOnionRequest for social group payload $payload")
        val destination = Destination.Server(host, target, x25519PublicKey, url.scheme(), url.port())
        Log.d("Beldex","sendOnionRequest for social group destination $destination")
        return sendOnionRequest(destination, payload).recover { exception ->
            Log.d("Beldex", "Couldn't reach server: $urlAsString due to error: $exception.")
            throw exception
        }
    }
    // endregion
}
