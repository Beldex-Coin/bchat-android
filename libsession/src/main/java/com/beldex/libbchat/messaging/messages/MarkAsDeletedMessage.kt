package com.beldex.libbchat.messaging.messages

data class MarkAsDeletedMessage(
    val messageId: Long,
    val isOutgoing: Boolean
)