package com.beldex.libsignal.utilities

interface Broadcaster {

    fun broadcast(event: String)
    fun broadcast(event: String, long: Long)
}
