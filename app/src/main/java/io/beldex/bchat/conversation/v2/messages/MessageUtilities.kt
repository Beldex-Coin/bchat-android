package io.beldex.bchat.conversation.v2.messages

import android.widget.TextView
import androidx.core.view.isVisible
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.util.isSameDayMessage
import java.util.Locale

private const val maxTimeBetweenBreaks = 5 * 60 * 1000L // 5 minutes

fun TextView.showDateBreak(message: MessageRecord, previous: MessageRecord?) {
//    val showDateBreak = (previous == null || message.timestamp - previous.timestamp > maxTimeBetweenBreaks)
    val showDateBreak = (previous == null || !isSameDayMessage(message, previous))
    isVisible = showDateBreak
    text = if (showDateBreak) DateUtils.getCoversationDisplayFormattedTimeSpanString(context, Locale.getDefault(), message.timestamp) else ""
}
