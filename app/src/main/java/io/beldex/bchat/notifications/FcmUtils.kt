@file:JvmName("FcmUtils")
package io.beldex.bchat.notifications

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*


fun getFcmInstanceId(body: (Task<String>)->Unit): Job = MainScope().launch(Dispatchers.IO) {
    val task = FirebaseMessaging.getInstance().token
    while (!task.isComplete && isActive) {
        // wait for task to complete while we are active
    }
    if (!isActive) return@launch // don't 'complete' task if we were canceled
    withContext(Dispatchers.Main) {
        body(task)
    }
}