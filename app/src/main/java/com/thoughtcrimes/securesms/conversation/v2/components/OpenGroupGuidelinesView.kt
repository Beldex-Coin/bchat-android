package com.thoughtcrimes.securesms.conversation.v2.components

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import io.beldex.bchat.databinding.ViewOpenGroupGuidelinesBinding
import com.thoughtcrimes.securesms.groups.OpenGroupGuidelinesActivity
import com.thoughtcrimes.securesms.home.HomeActivity
import com.thoughtcrimes.securesms.util.push

class OpenGroupGuidelinesView : FrameLayout {

    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        ViewOpenGroupGuidelinesBinding.inflate(LayoutInflater.from(context), this, true).apply {
            readButton.setOnClickListener {
                val intent = Intent(context, OpenGroupGuidelinesActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
}