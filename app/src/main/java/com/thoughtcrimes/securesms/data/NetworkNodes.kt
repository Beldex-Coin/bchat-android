package com.thoughtcrimes.securesms.data

import io.beldex.bchat.BuildConfig

object NetworkNodes  {
    private val mainNetNodes = listOf(
        "explorer.beldex.io:19091",
        "publicnode1.rpcnode.stream:29095",
        "publicnode2.rpcnode.stream:29095",
        "publicnode3.rpcnode.stream:29095",
        "publicnode4.rpcnode.stream:29095",
        "mainnet.beldex.io:29095"
    )
    private val testNetModes = listOf(
        "38.242.196.72:19095",
        "154.26.139.105:19095"
    )

    fun getNodes(): List<String> {
        return if (BuildConfig.USE_TESTNET) {
            testNetModes
        } else {
            mainNetNodes
        }
    }
}