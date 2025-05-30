package io.beldex.bchat.conversation.v2.dialogs

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.utilities.OpenGroupUrlParser
import com.beldex.libsignal.utilities.ThreadUtils
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.groups.OpenGroupManager
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.R
import io.beldex.bchat.databinding.DialogJoinOpenGroupBinding

/** Shown upon tapping an social group invitation. */
class JoinOpenGroupDialog(private val name: String, private val url: String) : BaseDialog() {

    override fun setContentView(builder: AlertDialog.Builder) {
        val binding = DialogJoinOpenGroupBinding.inflate(LayoutInflater.from(requireContext()))
        val title = resources.getString(R.string.dialog_join_open_group_title, name)
        binding.joinOpenGroupTitleTextView.text = title
        val explanation = resources.getString(R.string.dialog_join_open_group_explanation, name)
        val spannable = SpannableStringBuilder(explanation)
        val startIndex = explanation.indexOf(name)
        spannable.setSpan(StyleSpan(Typeface.BOLD), startIndex, startIndex + name.count(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.joinOpenGroupExplanationTextView.text = spannable
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.joinButton.setOnClickListener { join(name) }
        builder.setView(binding.root)
    }

    private fun join(name: String) {
        val openGroup = OpenGroupUrlParser.parseUrl(url)
        ThreadUtils.queue {
            val activity = activity
            if (isAdded && activity != null) {
                try {
                    OpenGroupManager.add(openGroup.server, openGroup.room, openGroup.serverPublicKey,activity)
                    MessagingModuleConfiguration.shared.storage.onOpenGroupAdded(openGroup.server)
                    if(ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(activity).isSuccess()){
                        Toast.makeText(activity, resources.getString(R.string.activity_public_chat_success_message, name), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    activity.runOnUiThread(Runnable {
                        Toast.makeText(activity, R.string.activity_join_public_chat_error, Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }
        dismiss()
    }
}