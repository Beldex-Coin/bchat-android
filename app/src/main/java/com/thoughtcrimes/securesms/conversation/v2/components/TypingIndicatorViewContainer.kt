package com.thoughtcrimes.securesms.conversation.v2.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.databinding.ViewConversationTypingContainerBinding

class TypingIndicatorViewContainer : LinearLayout {
    private lateinit var binding: ViewConversationTypingContainerBinding

    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        binding = ViewConversationTypingContainerBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setTypists(typists: List<Recipient>) {
        if (typists.isEmpty()) { binding.typingIndicator.root.stopAnimation(); return }
        binding.typingIndicator.root.startAnimation()
    }
}