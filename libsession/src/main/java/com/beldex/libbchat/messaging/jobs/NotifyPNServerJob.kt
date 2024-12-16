package com.beldex.libbchat.messaging.jobs

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import com.beldex.libbchat.messaging.jobs.Job.Companion.MAX_BUFFER_SIZE

import com.beldex.libbchat.messaging.sending_receiving.notifications.Server
import com.beldex.libbchat.messaging.utilities.Data
import com.beldex.libbchat.mnode.MnodeMessage
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.mnode.Version
import com.beldex.libsignal.utilities.JsonUtil
import com.beldex.libsignal.utilities.Log

import com.beldex.libsignal.utilities.retryIfNeeded

class NotifyPNServerJob(val message: MnodeMessage) : Job {
    override var delegate: JobDelegate? = null
    override var id: String? = null
    override var failureCount: Int = 0

    override val maxFailureCount: Int = 20
    companion object {
        val KEY: String = "NotifyPNServerJob"

        // Keys used for database storage
        private val MESSAGE_KEY = "message"
    }

    override fun execute() {
        val server = Server.LEGACY
        val parameters = mapOf( "data" to message.data, "send_to" to message.recipient )
        val url = "${server.url}/notify"
        val body = RequestBody.create(MediaType.get("application/json"), JsonUtil.toJson(parameters))
        val request = Request.Builder().url(url).post(body).build()
        retryIfNeeded(4) {
            OnionRequestAPI.sendOnionRequest(request, server.url, server.publicKey, Version.V2) success { response ->
                when (response.code) {
                    null, 0 -> Log.d("NotifyPNServerJob", "Couldn't notify PN server due to error: ${response.message}.")
                }
            } fail { exception ->
                Log.d("NotifyPNServerJob", "_Couldn't notify PN server due to error: $exception.")
                //New Line
                handleFailure(exception)
            }
        } success {
            handleSuccess()
        } fail {
            handleFailure(it)
        }
    }

    private fun handleSuccess() {
        delegate?.handleJobSucceeded(this)
    }

    private fun handleFailure(error: Exception) {
        delegate?.handleJobFailed(this, error)
    }

    override fun serialize(): Data {
        val kryo = Kryo()
        kryo.isRegistrationRequired = false
        val serializedMessage = ByteArray(4096)
        val output = Output(serializedMessage, MAX_BUFFER_SIZE)
        kryo.writeObject(output, message)
        output.close()
        return Data.Builder()
            .putByteArray(MESSAGE_KEY, serializedMessage)
            .build();
    }

    override fun getFactoryKey(): String {
        return KEY
    }

    class Factory : Job.Factory<NotifyPNServerJob> {

        override fun create(data: Data): NotifyPNServerJob {
            val serializedMessage = data.getByteArray(MESSAGE_KEY)
            val kryo = Kryo()
            kryo.isRegistrationRequired = false
            val input = Input(serializedMessage)
            val message = kryo.readObject(input, MnodeMessage::class.java)
            input.close()
            return NotifyPNServerJob(message)
        }
    }
}