package io.beldex.bchat.notifications

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpTokenFetcher @Inject constructor() : TokenFetcher {
    override suspend fun fetch(): String? = null
}