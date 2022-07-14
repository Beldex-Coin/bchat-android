package com.beldex.libbchat.messaging.jobs

interface JobDelegate {

    fun handleJobSucceeded(job: Job)
    fun handleJobFailed(job: Job, error: Exception)
    fun handleJobFailedPermanently(job: Job, error: Exception)
}