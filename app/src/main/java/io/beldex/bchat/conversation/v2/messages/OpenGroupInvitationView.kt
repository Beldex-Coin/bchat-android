package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import io.beldex.bchat.R
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
            invitationImageView.imageTintList= ColorStateList.valueOf(ResourcesCompat.getColor(resources, if (message.isOutgoing) {
                R.color.outgoing_reply_message_icon
            } else {
                R.color.incoming_reply_message_icon
            }, context.theme))
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
            val backgroundColorID = if (message.isOutgoing) {
                R.color.outgoing_call_background
            } else {
                R.color.quote_view_background
            }
            val backgroundColor =
                ResourcesCompat.getColor(resources, backgroundColorID, context.theme)
            container.backgroundTintList= ColorStateList.valueOf(backgroundColor)
            val cardBackgroundColorID = if (message.isOutgoing) {
                R.color.button_green
            } else {
                R.color.received_message_background
            }
            val invitationIconColorID = if (message.isOutgoing) {
                R.color.button_green
            } else {
                R.color.user_view_background
            }
            val cardBackgroundColor =
                ResourcesCompat.getColor(resources, cardBackgroundColorID, context.theme)
            val invitationIconBackgroundColor =
                ResourcesCompat.getColor(resources, invitationIconColorID, context.theme)
            socialGroupCardView.setCardBackgroundColor(ColorStateList.valueOf(cardBackgroundColor))
            InvitationIconPreviewContainer.backgroundTintList = ColorStateList.valueOf(invitationIconBackgroundColor)

            val titleColor = getTitleTextColor(message.isOutgoing)
            titleTextView.setTextColor(resources.getColor(titleColor, null))

    }
}
    private fun getTitleTextColor(isOutgoingMessage: Boolean): Int {
        return when {
            isOutgoingMessage -> R.color.white
            else -> R.color.text
        }
    }
    fun joinOpenGroup() {
        val data = data ?: return
        ActivityDispatcher.get(context)?.showDialog(JoinOpenGroupDialog(data.groupName,data.groupUrl),"Join Open Group Dialog")
    }
}