package com.beldex.libbchat.messaging.jobs

import com.beldex.libbchat.messaging.utilities.Data

class BchatJobInstantiator(private val jobFactories: Map<String, Job.Factory<out Job>>) {

    fun instantiate(jobFactoryKey: String, data: Data): Job? {
        if (jobFactories.containsKey(jobFactoryKey)) {
            return jobFactories[jobFactoryKey]?.create(data)
        } else {
            return null
        }
    }
}