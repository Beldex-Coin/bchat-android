package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.conversation.v2.dialogs.DownloadDialog
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewUntrustedAttachmentBinding
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
    fun bind(attachmentType: AttachmentType, @ColorInt textColor: Int) {
        val (iconRes, stringRes) = when (attachmentType) {
            AttachmentType.AUDIO -> R.drawable.ic_microphone to R.string.Slide_audio
            AttachmentType.DOCUMENT -> R.drawable.ic_document_large_light to R.string.document
            AttachmentType.MEDIA -> R.drawable.ic_image_white_24dp to R.string.media
        }
        val iconDrawable = ContextCompat.getDrawable(context,iconRes)!!
        iconDrawable.mutate().setTint(textColor)
        val text = context.getString(R.string.UntrustedAttachmentView_download_attachment, context.getString(stringRes).toLowerCase(Locale.ROOT))

        binding.untrustedAttachmentIcon.setImageDrawable(iconDrawable)
        binding.untrustedAttachmentTitle.text = text
    }
    // endregion

    // region Interaction
    fun showTrustDialog(recipient: Recipient) {
        ActivityDispatcher.get(context)?.showDialog(DownloadDialog(recipient))
    }

}