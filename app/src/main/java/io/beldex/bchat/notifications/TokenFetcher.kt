package io.beldex.bchat.notifications
interface TokenFetcher {
    suspend fun fetch(): String?
}