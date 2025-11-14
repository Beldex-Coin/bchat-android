package io.beldex.bchat.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.all
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.jobs.BatchMessageReceiveJob
import com.beldex.libbchat.messaging.jobs.MessageReceiveParameters
import com.beldex.libbchat.messaging.sending_receiving.pollers.ClosedGroupPollerV2
import com.beldex.libbchat.messaging.sending_receiving.pollers.OpenGroupPollerV2
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.beldex.bchat.dependencies.DatabaseComponent
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import java.util.concurrent.TimeUnit

class BackgroundPollWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "BackgroundPollWorker"
        private const val REQUEST_TARGETS = "REQUEST_TARGETS"

        @JvmStatic
        fun schedulePeriodic(context: Context) {
            Log.v(TAG, "Scheduling periodic work.")
            val builder = PeriodicWorkRequestBuilder<BackgroundPollWorker>(15, TimeUnit.MINUTES)
            builder.setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            val workRequest = builder.build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelPeriodic(context: Context) {
            Log.v(TAG, "Cancelling periodic work.")
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }

        fun scheduleOnce(context: Context) {
            Log.v(TAG, "Scheduling single run.")
            val builder = OneTimeWorkRequestBuilder<BackgroundPollWorker>()
            builder.setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())

            val dataBuilder = Data.Builder()
            //dataBuilder.putStringArray(REQUEST_TARGETS, targets.map { it.name }.toTypedArray())
            builder.setInputData(dataBuilder.build())

            val workRequest = builder.build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result {
        if (TextSecurePreferences.getLocalNumber(context) == null) {

            Log.v(TAG, "User not registered yet.")
            return Result.failure()
        }

        try {

            Log.v(TAG, "Performing background poll.")
            val promises = mutableListOf<Promise<Unit, Exception>>()

            // DMs
            val userPublicKey = TextSecurePreferences.getLocalNumber(context)!!
            val dmsPromise = MnodeAPI.getMessages(userPublicKey).bind { envelopes ->
                val params = envelopes.map { (envelope, serverHash) ->
                    // FIXME: Using a job here seems like a bad idea...
                    MessageReceiveParameters(envelope.toByteArray(), serverHash, null)
                }
                BatchMessageReceiveJob(params).executeAsync()
            }
            promises.add(dmsPromise)

            // Secret groups
            val closedGroupPoller = ClosedGroupPollerV2() // Intentionally don't use shared
            val storage = MessagingModuleConfiguration.shared.storage
            val allGroupPublicKeys = storage.getAllClosedGroupPublicKeys()
            allGroupPublicKeys.iterator().forEach { closedGroupPoller.poll(it) }

            // Social Groups
            val threadDB = DatabaseComponent.get(context).beldexThreadDatabase()
            val v2OpenGroups = threadDB.getAllV2OpenGroups()
            val v2OpenGroupServers = v2OpenGroups.map { it.value.server }.toSet()

            for (server in v2OpenGroupServers) {
                val poller = OpenGroupPollerV2(server, null)
                poller.hasStarted = true
                promises.add(poller.poll(true))
            }

            // Wait until all the promises are resolved
            all(promises).get()

            return Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Background poll failed due to error: ${exception.message}.", exception)
            return Result.retry()
        }
    }

     class BootBroadcastReceiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                Log.v(TAG, "Boot broadcast caught.")
                schedulePeriodic(context)
            }
        }
    }
}
