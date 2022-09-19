package com.thoughtcrimes.securesms.wallet.send.interfaces

import com.thoughtcrimes.securesms.model.PendingTransaction

interface SendConfirm {
    fun sendFailed(errorText: String?)
    fun createTransactionFailed(errorText: String?)
    fun transactionCreated(txTag: String?, pendingTransaction: PendingTransaction?)
}