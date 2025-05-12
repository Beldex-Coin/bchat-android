package com.beldex.libbchat.utilities.recipients

enum class MessageType {
    ONE_ON_ONE, LEGACY_GROUP, NOTE_TO_SELF, COMMUNITY
}
fun Recipient.getType(): MessageType =
    when{
        isCommunityRecipient -> MessageType.COMMUNITY
        isLocalNumber -> MessageType.NOTE_TO_SELF
        isClosedGroupRecipient -> MessageType.LEGACY_GROUP
        else -> MessageType.ONE_ON_ONE
    }