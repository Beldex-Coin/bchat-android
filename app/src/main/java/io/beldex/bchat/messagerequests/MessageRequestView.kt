package io.beldex.bchat.messagerequests

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.conversation.v2.utilities.MentionUtilities.highlightMentions
import io.beldex.bchat.database.model.ThreadRecord
import com.bumptech.glide.RequestManager
import io.beldex.bchat.util.DateUtils
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityMessageRequestViewBinding
import java.util.*

class MessageRequestView : LinearLayout {
    private lateinit var binding: ActivityMessageRequestViewBinding
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    var thread: ThreadRecord? = null

    // region Lifecycle
    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        binding = ActivityMessageRequestViewBinding.inflate(LayoutInflater.from(context), this, true)
        layoutParams = RecyclerView.LayoutParams(screenWidth, RecyclerView.LayoutParams.WRAP_CONTENT)
    }
    // endregion

    // region Updating
    fun bind(thread: ThreadRecord, glide: RequestManager) {
        this.thread = thread
        binding.profilePictureView.root.glide = glide
        val senderDisplayName = getUserDisplayName(thread.recipient)
            ?: thread.recipient.address.toString()
        binding.displayNameTextView.text = senderDisplayName
        binding.timestampTextView.text = DateUtils.getDisplayFormattedTimeSpanString(context, Locale.getDefault(), thread.date)
        val rawSnippet = thread.getDisplayBody(context)
        val snippet = highlightMentions(rawSnippet, thread.threadId, context)
        binding.snippetTextView.text = snippet

        post {
            binding.profilePictureView.root.update(thread.recipient)
        }
    }

    fun recycle() {
        binding.profilePictureView.root.recycle()
    }

    private fun getUserDisplayName(recipient: Recipient): String? {
        return if (recipient.isLocalNumber) {
            context.getString(R.string.note_to_self)
        } else {
            recipient.name // Internally uses the Contact API
        }
    }
    // endregion
}
