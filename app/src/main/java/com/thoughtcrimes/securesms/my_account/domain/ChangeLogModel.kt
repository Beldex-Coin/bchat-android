package com.thoughtcrimes.securesms.my_account.domain

data class ChangeLogModel(
    val version: String,
    val logs: List<String>
)
