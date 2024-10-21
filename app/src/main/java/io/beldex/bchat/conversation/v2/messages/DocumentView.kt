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
import io.beldex.bchat.databinding.ViewDocumentBinding
import io.beldex.bchat.database.model.MmsMessageRecord

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
        val fileFormat = fileName.substringAfter(".","-")
        val fileNameWithoutFormat = fileName.substringBefore(".",fileName)
        val newFileName=if(fileNameWithoutFormat.length >12){
            "${fileNameWithoutFormat.subSequence(0,12)}... "
        }else{
            fileNameWithoutFormat
        }
        binding.documentTitleTextView.textSize = fontSize!!.toFloat()
        if(fileFormat == "-"){
            binding.documentTitleTextView.text = newFileName
        }else{
            binding.documentTitleTextView.text = "$newFileName.$fileFormat"
        }
        binding.documentTitleTextView.setTextColor(textColor)
        binding.documentViewIconImageView.imageTintList = ColorStateList.valueOf(textColor)
        binding.documentViewIconTextView.text = fileName.substring(
                fileName.lastIndexOf(".") + 1
        )
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
        val backgroundColorID = if (message.isOutgoing) {
            R.color.button_green
        } else {
            R.color.user_view_background
        }
        val backgroundColor =
            ResourcesCompat.getColor(resources, backgroundColorID, context.theme)
        binding.documentTypePreviewContainer.backgroundTintList =
            ColorStateList.valueOf(backgroundColor)

        binding.documentViewContainer.setBackgroundColor(resources.getColor(getContainerColor(message.isOutgoing), null))
        if (message.isOutgoing) {
            binding.documentViewContainer.background=ContextCompat.getDrawable(
                context,
                R.drawable.document_view_background
            )
        } else {
            binding.documentViewContainer.background=ContextCompat.getDrawable(
                context,
                R.drawable.document_incoming_view_background
            )
        }
    }

    private fun getContainerColor(isOutgoingMessage: Boolean): Int {
        return when {
            isOutgoingMessage -> R.color.outgoing_call_background
            else -> R.color.quote_view_background
        }
    }
// endregion
}