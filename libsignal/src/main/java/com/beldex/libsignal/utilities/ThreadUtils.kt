package com.beldex.libsignal.utilities

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import android.os.Process

object ThreadUtils {
    const val PRIORITY_IMPORTANT_BACKGROUND_THREAD = Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
    val executorPool: ExecutorService = Executors.newCachedThreadPool()

    @JvmStatic
    fun queue(target: Runnable) {
        executorPool.execute(target)
    }

    fun queue(target: () -> Unit) {
        executorPool.execute(target)
    }

    @JvmStatic
    fun newDynamicSingleThreadedExecutor(): ExecutorService {
        val executor = ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, LinkedBlockingQueue())
        executor.allowCoreThreadTimeOut(true)
        return executor
    }
}