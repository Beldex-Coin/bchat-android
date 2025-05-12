package io.beldex.bchat.conversation.v2.messages

import io.beldex.bchat.database.model.MessageId
import io.beldex.bchat.database.model.MessageRecord

interface VisibleMessageViewDelegate {
    fun playVoiceMessageAtIndexIfPossible(indexInAdapter: Int)
    fun isAudioPlaying(isPlaying : Boolean, audioPlayingIndex : Int)
    fun stopVoiceMessages(indexInAdapter : Int)
    fun scrollToMessageIfPossible(timestamp: Long)
    fun onReactionClicked(emoji: String, messageId: MessageId, userWasSender: Boolean)
    fun onReactionLongClicked(messageId: MessageId, emoji : String?)
    fun onItemLongPress(messageRecord : MessageRecord, visibleMessageView : VisibleMessageView, position: Int)
}