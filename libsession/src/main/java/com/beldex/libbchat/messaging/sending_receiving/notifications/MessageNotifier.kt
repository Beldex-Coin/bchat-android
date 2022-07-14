package com.beldex.libbchat.messaging.sending_receiving.notifications

import android.content.Context
import com.beldex.libbchat.utilities.recipients.Recipient

interface MessageNotifier {
    fun setHomeScreenVisible(isVisible: Boolean)
    fun setVisibleThread(threadId: Long)
    fun setLastDesktopActivityTimestamp(timestamp: Long)
    fun notifyMessageDeliveryFailed(context: Context?, recipient: Recipient?, threadId: Long)
    fun cancelDelayedNotifications()
    fun updateNotification(context: Context)
    fun updateNotification(context: Context, threadId: Long)
    fun updateNotification(context: Context, threadId: Long, signal: Boolean)
    fun updateNotification(context: Context, signal: Boolean, reminderCount: Int)
    fun clearReminder(context: Context)
}