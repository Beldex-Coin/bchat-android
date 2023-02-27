package com.beldex.libsignal.utilities

import okhttp3.*
import java.lang.IllegalStateException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object HTTP {

    var isConnectedToNetwork: (() -> Boolean) = { false }

    private val seedNodeConnection by lazy {
        OkHttpClient().newBuilder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build()
    }

    private val defaultConnection by lazy {
        // Mnode to mnode communication uses self-signed certificates but clients can safely ignore this
        val trustManager = object : X509TrustManager {

            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authorizationType: String?) { }
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authorizationType: String?) { }
            override fun getAcceptedIssuers(): Array<X509Certificate> { return arrayOf() }
        }
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf( trustManager ), SecureRandom())
        OkHttpClient().newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build()
    }

    private fun getDefaultConnection(timeout: Long): OkHttpClient {
        // Mnode to mnode communication uses self-signed certificates but clients can safely ignore this
        val trustManager = object : X509TrustManager {

            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authorizationType: String?) { }
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authorizationType: String?) { }
            override fun getAcceptedIssuers(): Array<X509Certificate> { return arrayOf() }
        }
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf( trustManager ), SecureRandom())
        return OkHttpClient().newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .build()
    }

    private const val timeout: Long = 10

    open class HTTPRequestFailedException(val statusCode: Int, val json: Map<*, *>?, message: String = "HTTP request failed with status code $statusCode")
        : kotlin.Exception(message)

    class HTTPNoNetworkException : HTTPRequestFailedException(0, null, "No network connection")

    enum class Verb(val rawValue: String) {
        GET("GET"), PUT("PUT"), POST("POST"), DELETE("DELETE")
    }

    /**
     * Sync. Don't call from the main thread.
     */
    fun execute(verb: Verb, url: String, timeout: Long = HTTP.timeout, useSeedNodeConnection: Boolean = false): Map<*, *> {
        return execute(verb = verb, url = url, body = null, timeout = timeout, useSeedNodeConnection = useSeedNodeConnection)
    }

    /**
     * Sync. Don't call from the main thread.
     */
    fun execute(verb: Verb, url: String, parameters: Map<String, Any>?, timeout: Long = HTTP.timeout, useSeedNodeConnection: Boolean = false): Map<*, *> {
        if (parameters != null) {
            Log.d("Beldex","parameters in HTTP first execute fun  $parameters")
            val body = JsonUtil.toJson(parameters).toByteArray()
            Log.d("Beldex","body in HTTP first execute fun $body")

            return execute(verb = verb, url = url, body = body, timeout = timeout, useSeedNodeConnection = useSeedNodeConnection)
        } else {
            return execute(verb = verb, url = url, body = null, timeout = timeout, useSeedNodeConnection = useSeedNodeConnection)
        }
    }

    /**
     * Sync. Don't call from the main thread.
     */
    fun execute(verb: Verb, url: String, body: ByteArray?, timeout: Long = HTTP.timeout, useSeedNodeConnection: Boolean = false): Map<*, *> {
        val request = Request.Builder().url(url)
            .removeHeader("User-Agent").addHeader("User-Agent", "WhatsApp") // Set a fake value
            .removeHeader("Accept-Language").addHeader("Accept-Language", "en-us") // Set a fake value
        Log.d("Beldex","request in HTTP execute fun $request")
        when (verb) {
            Verb.GET -> request.get()
            Verb.PUT, Verb.POST -> {
                Log.d("Beldex","body in HTTP execute fun $body")
                if (body == null) { throw Exception("Invalid request body.") }
                val contentType = MediaType.get("application/json; charset=utf-8")
                Log.d("Beldex","contentType in HTTP execute fun $contentType")
                @Suppress("NAME_SHADOWING") val body = RequestBody.create(contentType, body)
                if (verb == Verb.PUT) request.put(body) else request.post(body)
            }
            Verb.DELETE -> request.delete()
        }
        lateinit var response: Response
        try {
            val connection: OkHttpClient
            if (timeout != HTTP.timeout) { // Custom timeout
                if (useSeedNodeConnection) {
                    Log.d("Beldex","if condition in HTTP execute fun ")
                    throw IllegalStateException("Setting a custom timeout is only allowed for requests to mnodes.")
                }
                connection = getDefaultConnection(timeout)
            } else {
                connection = if (useSeedNodeConnection) seedNodeConnection else defaultConnection
            }
            response = connection.newCall(request.build()).execute()
            Log.d("Beldex","response in HTTP execute fun  $response ")
            Log.d("Beldex", "Three onion request path response-- $response")
        } catch (exception: Exception) {
            Log.d("Beldex", "${verb.rawValue} request to $url failed due to error: ${exception.localizedMessage}.")
            if (!isConnectedToNetwork()) { throw HTTPNoNetworkException() }
            // Override the actual error so that we can correctly catch failed requests in OnionRequestAPI
            throw HTTPRequestFailedException(0, null, "HTTP request failed due to: ${exception.message}")
        }
        when (val statusCode = response.code()) {
            200 -> {
                val bodyAsString = response.body()?.string() ?: throw Exception("An error occurred.")
                try {
                    return JsonUtil.fromJson(bodyAsString, Map::class.java)
                } catch (exception: Exception) {
                    return mapOf( "result" to bodyAsString)
                }
            }
            else -> {
                Log.d("Beldex", "${verb.rawValue} request to $url failed with status code: $statusCode.")
                throw HTTPRequestFailedException(statusCode, null)
            }
        }
    }
}
