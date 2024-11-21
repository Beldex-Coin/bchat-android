package io.beldex.bchat.notifications
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
@Singleton
class FirebaseTokenFetcher @Inject constructor(): TokenFetcher {
    override suspend fun fetch() = withContext(Dispatchers.IO) {
        FirebaseMessaging.getInstance().token.await().takeIf { isActive } ?: throw Exception("Firebase token is null")
    }
}