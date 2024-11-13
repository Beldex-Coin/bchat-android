package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewDeletedMessageBinding
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.util.DateUtils
import java.util.Locale

class DeletedMessageView : LinearLayout {
    private val binding: ViewDeletedMessageBinding by lazy { ViewDeletedMessageBinding.bind(this) }
    // region Lifecycle
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    // endregion

    // region Updating
    fun bind(message: MessageRecord, @ColorInt textColor: Int) {
        assert(message.isDeleted)
        binding.deleteTitleTextView.text = context.getString(R.string.deleted_message)
        binding.deleteTitleTextView.setTextColor(textColor)
        binding.deletedMessageViewIconImageView.imageTintList = ColorStateList.valueOf(textColor)

        binding.deleteMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
        binding.deleteMessageTime.setTextColor(
            VisibleMessageContentView.getTimeTextColor(
                context,
                message.isOutgoing
            )
        )
    }
    // endregion
}