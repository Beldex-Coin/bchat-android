package io.beldex.bchat.wallet.send.interfaces

import io.beldex.bchat.model.PendingTransaction

interface SendConfirm {
    fun sendFailed(errorText: String?)
    fun createTransactionFailed(errorText: String?)
    fun transactionCreated(txTag: String?, pendingTransaction: PendingTransaction?)
}