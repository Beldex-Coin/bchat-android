package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewOpenGroupInvitationBinding
import com.beldex.libbchat.messaging.utilities.UpdateMessageData
import com.beldex.libbchat.utilities.OpenGroupUrlParser
import io.beldex.bchat.conversation.v2.dialogs.JoinOpenGroupDialog
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.util.ActivityDispatcher
import io.beldex.bchat.util.DateUtils
import java.util.Locale

class OpenGroupInvitationView : LinearLayout {
    private val binding: ViewOpenGroupInvitationBinding by lazy { ViewOpenGroupInvitationBinding.bind(this) }
    private var data: UpdateMessageData.Kind.OpenGroupInvitation? = null

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
            openGroupInvitationIconImageView.setImageResource(iconID)
            openGroupTitleTextView.text = data.groupName
            openGroupURLTextView.text = OpenGroupUrlParser.trimQueryParameter(data.groupUrl)
            openGroupTitleTextView.setTextColor(textColor)
            openGroupJoinMessageTextView.setTextColor(textColor)
            openGroupURLTextView.setTextColor(textColor)
            openGroupMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
            openGroupMessageTime.setTextColor(
                VisibleMessageContentView.getTimeTextColor(
                    context,
                    message.isOutgoing
                )
            )
        }
    }

    fun joinOpenGroup() {
        val data = data ?: return
        ActivityDispatcher.get(context)?.showDialog(JoinOpenGroupDialog(data.groupName,data.groupUrl),"Join Open Group Dialog")
    }
}