package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.InnerTileDrawable
import io.beldex.bchat.databinding.ViewDocumentBinding
import io.beldex.bchat.database.model.MmsMessageRecord
import io.beldex.bchat.util.DateUtils
import java.util.Locale

class DocumentView : LinearLayout {
    private val binding: ViewDocumentBinding by lazy { ViewDocumentBinding.bind(this) }

    // region Lifecycle
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    // endregion

    // region Updating
    fun bind(message: MmsMessageRecord, @ColorInt textColor: Int) {
        val document = message.slideDeck.documentSlide!!
        val fontSize = TextSecurePreferences.getChatFontSize(context)
        val fileName = document.fileName.or("Untitled File").toString()
        val fileFormat = fileName.substringAfterLast(".","-")
        val fileNameWithoutFormat = fileName.substringBeforeLast(".",fileName)
        val newFileName=if(fileNameWithoutFormat.length >12){
            "${fileNameWithoutFormat.subSequence(0,12)}... "
        }else{
            fileNameWithoutFormat
        }
        binding.documentTitleTextView.textSize = 12f
        if(fileFormat == "-"){
            binding.documentTitleTextView.text = newFileName
        }else{
            binding.documentTitleTextView.text = "$newFileName.$fileFormat"
        }
        binding.documentTitleTextView.setTextColor(0xFFEBEBEB.toInt())
        val contentType = document.contentType ?: ""
        binding.documentViewIconImageView.setImageResource(
            when {
                contentType.startsWith("video/") -> R.drawable.ic_play_new
                contentType.startsWith("audio/") -> R.drawable.ic_microphone
                else -> R.drawable.ic_message_document
            }
        )
        binding.documentViewIconImageView.imageTintList = ColorStateList.valueOf(0xFFACACAC.toInt())
        //New Line Image Upload time show progress bar function
        if (!message.isFailed) {
            if(!message.isPending && !message.isRead && !message.isDelivered) {
                if (message.slideDeck.documentSlide!!.uri != null) {
                    binding.gifProgress.visibility = View.GONE
                } else {
                    binding.gifProgress.visibility = View.VISIBLE
                }
            }
            else{
                binding.gifProgress.visibility = View.VISIBLE
            }
        }
        val density = resources.displayMetrics.density
        binding.documentViewContainer.background = InnerTileDrawable(
            notchPx = 8 * density,
            barWidthPx = 3 * density,
            fillColor = if (message.isOutgoing) 0xFF000800.toInt() else 0xFF0B0B0B.toInt(),
            barColor = 0xFF444444.toInt()
        )

        binding.documentViewMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
        binding.documentViewMessageTime.setTextColor(
            VisibleMessageContentView.getTimeTextColor(
                context,
                message.isOutgoing
            )
        )
    }

    private fun getContainerColor(isOutgoingMessage: Boolean): Int {
        return when {
            isOutgoingMessage -> R.color.outgoing_call_background
            else -> R.color.quote_view_background
        }
    }
// endregion
}