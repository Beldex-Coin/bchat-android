package io.beldex.bchat.my_account.domain

data class ChangeLogModel(
    val version: String,
    val logs: List<String>
)
