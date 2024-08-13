package io.beldex.bchat.data

import io.beldex.bchat.BuildConfig

object NetworkNodes  {
    private val mainNetNodes = listOf(
        "publicnode1.rpcnode.stream:29095",
        "publicnode2.rpcnode.stream:29095",
        "publicnode3.rpcnode.stream:29095",
        "publicnode4.rpcnode.stream:29095",
        "publicnode5.rpcnode.stream:29095"
    )
    private val testNetModes = listOf(
        "149.102.156.174:19095"
    )

    fun getNodes(): List<String> {
        return if (BuildConfig.USE_TESTNET) {
            testNetModes
        } else {
            mainNetNodes
        }
    }
}