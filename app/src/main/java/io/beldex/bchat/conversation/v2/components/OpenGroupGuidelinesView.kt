package io.beldex.bchat.conversation.v2.components

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import io.beldex.bchat.databinding.ViewOpenGroupGuidelinesBinding
import io.beldex.bchat.groups.OpenGroupGuidelinesActivity
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.push
import io.beldex.bchat.R

class OpenGroupGuidelinesView : FrameLayout {

    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        ViewOpenGroupGuidelinesBinding.inflate(LayoutInflater.from(context), this, true).apply {
            val isDarkMode=UiModeUtilities.getUserSelectedUiMode(context) == UiMode.NIGHT
            if (isDarkMode) {
                pinIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pinned_dark))
                guidelinesCard.setCardBackgroundColor(Color.argb(230, 17, 17, 17))
                guidelinesCard.strokeColor = Color.parseColor("#333333")
                guidelinesTitle.setTextColor(Color.parseColor("#EBEBEB"))
                readButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0B0B0B"))
                readButton.setTextColor(Color.WHITE)
                readButton.strokeColor = ColorStateList.valueOf(Color.parseColor("#078720"))
            } else {
                pinIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pinned_light))
                guidelinesCard.setCardBackgroundColor(Color.argb(166, 240, 240, 240))
                guidelinesCard.strokeColor = Color.parseColor("#D0D0D0")
                guidelinesTitle.setTextColor(Color.parseColor("#222222"))
                readButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0B0B0B"))
                readButton.setTextColor(Color.WHITE)
                readButton.strokeColor = ColorStateList.valueOf(Color.parseColor("#078720"))
            }

            readButton.setOnClickListener {
                val intent = Intent(context, OpenGroupGuidelinesActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
}
