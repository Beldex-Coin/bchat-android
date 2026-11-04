package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.conversation.v2.dialogs.DownloadDialog
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.InnerTileDrawable
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.database.model.Quote
import io.beldex.bchat.databinding.ViewUntrustedAttachmentBinding
import io.beldex.bchat.util.DateUtils
import java.util.Locale

class UntrustedAttachmentView: LinearLayout {
    private val binding: ViewUntrustedAttachmentBinding by lazy { ViewUntrustedAttachmentBinding.bind(this) }
    enum class AttachmentType {
        AUDIO,
        DOCUMENT,
        MEDIA
    }

    // region Lifecycle
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    // endregion

    // region Updating
    fun bind(
        message: MessageRecord,
        quote: Quote?,
        attachmentType: AttachmentType,
        @ColorInt textColor: Int
    ) {
        val (iconRes, stringRes) = when (attachmentType) {
            AttachmentType.AUDIO -> R.drawable.ic_microphone to R.string.Slide_audio
            AttachmentType.DOCUMENT -> R.drawable.ic_message_document to R.string.document
            AttachmentType.MEDIA -> R.drawable.ic_image_attachment to R.string.MediaOverviewActivity_Media
        }
        val iconDrawable = ContextCompat.getDrawable(context,iconRes)!!
        iconDrawable.mutate().setTint(ContextCompat.getColor(context, R.color.message_secondary_text))

        binding.untrustedAttachmentIcon.setImageDrawable(iconDrawable)
        binding.untrustedAttachmentTitle.text = context.getString(stringRes)
        binding.untrustedAttachmentSubtitle.text = context.getString(
            R.string.UntrustedAttachmentView_download_attachment, ""
        ).trim()
        val density = resources.displayMetrics.density
        binding.untrustedTile.background = InnerTileDrawable(
            notchPx = 8 * density,
            barWidthPx = 3 * density,
            fillColor = ContextCompat.getColor(context, if (message.isOutgoing) R.color.message_tile_sent_background else R.color.edit_group_toolbar_bg),
            barColor = ContextCompat.getColor(context, R.color.message_tile_bar)
        )

        binding.untrustedAttachmentMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
        binding.untrustedAttachmentMessageTime.setTextColor(
            VisibleMessageContentView.getTimeTextColor(
                context,
                message.isOutgoing
            )
        )
        if(quote != null) {
            binding.untrustedCardView.setOnClickListener {
                showTrustDialog(message.individualRecipient)
            }
        }
    }
    // endregion

    // region Interaction
    fun showTrustDialog(recipient: Recipient) {
        ActivityDispatcher.get(context)?.showDialog(DownloadDialog(recipient), VisibleMessageContentView.UNTRUSTED_DIALOG)
    }

}