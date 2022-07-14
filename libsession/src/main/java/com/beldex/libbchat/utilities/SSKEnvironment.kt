package com.beldex.libbchat.utilities

import android.content.Context
import com.beldex.libbchat.messaging.messages.control.ExpirationTimerUpdate
import com.beldex.libbchat.messaging.sending_receiving.notifications.MessageNotifier
import com.beldex.libbchat.utilities.recipients.Recipient

class SSKEnvironment(
    val typingIndicators: TypingIndicatorsProtocol,
    val readReceiptManager: ReadReceiptManagerProtocol,
    val profileManager: ProfileManagerProtocol,
    val notificationManager: MessageNotifier,
    val messageExpirationManager: MessageExpirationManagerProtocol
) {

    interface TypingIndicatorsProtocol {
        fun didReceiveTypingStartedMessage(context: Context, threadId: Long, author: Address, device: Int)
        fun didReceiveTypingStoppedMessage(context: Context, threadId: Long, author: Address, device: Int, isReplacedByIncomingMessage: Boolean)
        fun didReceiveIncomingMessage(context: Context, threadId: Long, author: Address, device: Int)
    }

    interface ReadReceiptManagerProtocol {
        fun processReadReceipts(context: Context, fromRecipientId: String, sentTimestamps: List<Long>, readTimestamp: Long)
    }

    interface ProfileManagerProtocol {
        companion object {
            const val NAME_PADDED_LENGTH = 26
        }

        fun setNickname(context: Context, recipient: Recipient, nickname: String?)
        fun setName(context: Context, recipient: Recipient, name: String)
        fun setBeldexAddress(context: Context, recipient: Recipient, address: String)
        fun setProfilePictureURL(context: Context, recipient: Recipient, profilePictureURL: String)
        fun setProfileKey(context: Context, recipient: Recipient, profileKey: ByteArray)
        fun setUnidentifiedAccessMode(context: Context, recipient: Recipient, unidentifiedAccessMode: Recipient.UnidentifiedAccessMode)
    }

    interface MessageExpirationManagerProtocol {
        fun setExpirationTimer(message: ExpirationTimerUpdate)
        fun disableExpirationTimer(message: ExpirationTimerUpdate)
        fun startAnyExpiration(timestamp: Long, author: String)
    }

    companion object {
        lateinit var shared: SSKEnvironment

        fun configure(typingIndicators: TypingIndicatorsProtocol,
                      readReceiptManager: ReadReceiptManagerProtocol,
                      profileManager: ProfileManagerProtocol,
                      notificationManager: MessageNotifier,
                      messageExpirationManager: MessageExpirationManagerProtocol) {
            if (Companion::shared.isInitialized) { return }
            shared = SSKEnvironment(typingIndicators, readReceiptManager, profileManager, notificationManager, messageExpirationManager)
        }
    }
}