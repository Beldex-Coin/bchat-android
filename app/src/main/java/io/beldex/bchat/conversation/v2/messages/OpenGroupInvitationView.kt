package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.InnerTileDrawable
import io.beldex.bchat.conversation.v2.dialogs.JoinOpenGroupDialog
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.databinding.ViewOpenGroupInvitationBinding
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.util.DateUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class OpenGroupInvitationView : LinearLayout {
    private val binding: ViewOpenGroupInvitationBinding by lazy { ViewOpenGroupInvitationBinding.bind(this) }
    private var data: UpdateMessageData.Kind.OpenGroupInvitation? = null
    private var groupUrl: String?= null
    private var groupName: String?= null

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    fun bind(message: MessageRecord, @ColorInt textColor: Int) {
        // FIXME: This is a really weird approach...
        val umd = UpdateMessageData.fromJSON(message.body)!!
        val data = umd.kind as UpdateMessageData.Kind.OpenGroupInvitation
        this.data = data
        val iconID = if (message.isOutgoing) R.drawable.ic_social_group_chat else R.drawable.ic_social_group_chat
        with(binding){
            invitationImageView.setImageResource(iconID)
            invitationImageView.imageTintList= ColorStateList.valueOf(R.color.contact_card_action_text)
            try {
                val mainObject = JSONObject(message.body)
                val uniObject = mainObject.getJSONObject("kind")
                groupUrl = uniObject.getString("groupUrl")
                groupName = uniObject.getString("groupName")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            val trimmedURL : Array<String> = groupUrl?.split("?")!!.toTypedArray()
            groupNameTextView.text = groupName
            socialGroupUrl.text= trimmedURL[0]
            socialGroupMessageTime.text =  DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
            val density = resources.displayMetrics.density
            invitationTile.background = InnerTileDrawable(
                notchPx = 8 * density,
                barWidthPx = 3 * density,
                fillColor = if (message.isOutgoing) 0xFF000800.toInt() else 0xFF0B0B0B.toInt(),
                barColor = 0xFF444444.toInt()
            )
            socialGroupCardView.setCardBackgroundColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))
            titleTextView.setTextColor(R.color.disappear_icon_color)
            socialGroupUrl.setTextColor(textColor)
            socialGroupMessageTime.setTextColor(textColor)
            binding.socialGroupCardView.setOnClickListener {
                ActivityDispatcher.get(context)?.showDialog(JoinOpenGroupDialog(data.groupName,data.groupUrl), VisibleMessageContentView.JOIN_SOCIAL_GROUP_POPUP)
            }
    }
}
    private fun getTitleTextColor(isOutgoingMessage: Boolean): Int {
        return when {
            isOutgoingMessage -> R.color.white
            else -> R.color.text
        }
    }
}