package com.beldex.libbchat.messaging.messages.visible

import com.beldex.libbchat.messaging.jobs.MessageReceiveParameters
import com.beldex.libbchat.messaging.messages.Message
import com.beldex.libsignal.protos.SignalServiceProtos

data class ParsedMessage(
    val parameters: MessageReceiveParameters,
    val message: Message,
    val proto: SignalServiceProtos.Content
)