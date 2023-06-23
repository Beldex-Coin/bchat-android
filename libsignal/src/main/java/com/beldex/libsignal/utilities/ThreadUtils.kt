package com.beldex.libsignal.utilities

import java.util.concurrent.*
import android.os.Process

object ThreadUtils {
    const val PRIORITY_IMPORTANT_BACKGROUND_THREAD = Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
    val executorPool = Executors.newCachedThreadPool()

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