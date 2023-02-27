package com.beldex.libbchat.messaging.file_server

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.functional.map
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.RequestBody
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libsignal.utilities.Base64
import com.beldex.libsignal.utilities.HTTP
import com.beldex.libsignal.utilities.JsonUtil
import com.beldex.libsignal.utilities.Log

object FileServerAPIV2 {

    //http://49.206.200.190:8080/Diva?public_key=d79d96957f574852ececd0f60930fa160837f5bf7dfc122e99af1cc5f09a8e38
    //private const val serverPublicKey = "d79d96957f574852ececd0f60930fa160837f5bf7dfc122e99af1cc5f09a8e38"

    //Original
    //private const val serverPublicKey = "4eb70e67faebd5da48b144096e2082f7e253c67ff48aabd8510a66286b81986f"
    //10-05-2022
    //private const val serverPublicKey = "1071fdc1e1cc6565a0306285d1c79c52a376fc7262313e0532a8b1ac7578902b"
    //const val server = "http://49.206.200.190:8080"
    //Original
    //const val server = "http://13.233.251.36"
    //10-05-2022
    //const val server = "http://fs.rpcnode.stream"
    //10-06-2022

    //Mainnet
    private const val serverPublicKey = "f3024b309be838eff764c6804c417b667096d6c5301184f90fb66e9e4515444c"
    const val server = "http://fs1.rpcnode.stream"

    //Testnet
    /*private const val serverPublicKey = "51a8cd2e8a2c7e012d2ed4e0a24b5ed26f6f4f5b89e0d9d9c8856a5fd7140314"
    const val server = "http://13.233.252.86"*/
    const val maxFileSize = 10_000_000 // 10 MB

    /**
     * The file server has a file size limit of `maxFileSize`, which the Service Nodes try to enforce as well. However, the limit applied by the Service Nodes
     * is on the **HTTP request** and not the actual file size. Because the file server expects the file data to be base 64 encoded, the size of the HTTP
     * request for a given file will be at least `ceil(n / 3) * 4` bytes, where n is the file size in bytes. This is the minimum size because there might also
     * be other parameters in the request. On average the multiplier appears to be about 1.5, so when checking whether the file will exceed the file size limit when
     * uploading a file we just divide the size of the file by this number. The alternative would be to actually check the size of the HTTP request but that's only
     * possible after proof of work has been calculated and the onion request encryption has happened, which takes several seconds.
     */
    const val fileSizeORMultiplier = 2 // TODO: It should be possible to set this to 1.5?

    sealed class Error(message: String) : Exception(message) {
        object ParsingFailed : Error("Invalid response.")
        object InvalidURL : Error("Invalid URL.")
    }

    data class Request(
            val verb: HTTP.Verb,
            val endpoint: String,
            val queryParameters: Map<String, String> = mapOf(),
            val parameters: Any? = null,
            val headers: Map<String, String> = mapOf(),
            /**
         * Always `true` under normal circumstances. You might want to disable
         * this when running over Beldex.
         */
        val useOnionRouting: Boolean = true
    )

    private fun createBody(parameters: Any?): RequestBody? {
        if (parameters == null) return null
        val parametersAsJSON = JsonUtil.toJson(parameters)
        return RequestBody.create(MediaType.get("application/json"), parametersAsJSON)
    }

    private fun send(request: Request): Promise<Map<*, *>, Exception> {
        val url = HttpUrl.parse(server) ?: return Promise.ofFail(OpenGroupAPIV2.Error.InvalidURL)
        //-Log.d("Beldex"," file server send URL $url")
        val urlBuilder = HttpUrl.Builder()
            .scheme(url.scheme())
            .host(url.host())
            .port(url.port())
            .addPathSegments(request.endpoint)
        //-Log.d("Beldex"," file server send Url builder $urlBuilder")
        if (request.verb == HTTP.Verb.GET) {
            for ((key, value) in request.queryParameters) {
                urlBuilder.addQueryParameter(key, value)
            }
        }
        val requestBuilder = okhttp3.Request.Builder()
            .url(urlBuilder.build())
            .headers(Headers.of(request.headers))
        when (request.verb) {
            HTTP.Verb.GET -> requestBuilder.get()
            HTTP.Verb.PUT -> requestBuilder.put(createBody(request.parameters)!!)
            HTTP.Verb.POST -> requestBuilder.post(createBody(request.parameters)!!)
            HTTP.Verb.DELETE -> requestBuilder.delete(createBody(request.parameters))
        }
        if (request.useOnionRouting) {
            //-Log.d("Beldex","request for fileserver ${request.useOnionRouting}")
            return OnionRequestAPI.sendOnionRequest(requestBuilder.build(), server, serverPublicKey).fail { e ->
                //Log.e("Beldex", "File server request failed.", e)
                when (e) {
                    // No need for the stack trace for HTTP errors
                    is HTTP.HTTPRequestFailedException -> Log.e("Beldex", "File server request failed due to error: ${e.message}")
                    else -> Log.e("Beldex", "File server request failed", e)
                }
            }
        } else {
            return Promise.ofFail(IllegalStateException("It's currently not allowed to send non onion routed requests."))
        }
    }

    fun upload(file: ByteArray): Promise<Long, Exception> {
        val base64EncodedFile = Base64.encodeBytes(file)
        val parameters = mapOf( "file" to base64EncodedFile )
        val request = Request(verb = HTTP.Verb.POST, endpoint = "files", parameters = parameters)
        return send(request).map { json ->
            json["result"] as? Long ?: throw OpenGroupAPIV2.Error.ParsingFailed
        }
    }

    fun download(file: Long): Promise<ByteArray, Exception> {
        val request = Request(verb = HTTP.Verb.GET, endpoint = "files/$file")
        return send(request).map { json ->
            val base64EncodedFile = json["result"] as? String ?: throw Error.ParsingFailed
            Base64.decode(base64EncodedFile) ?: throw Error.ParsingFailed
        }
    }
}