package com.thoughtcrimes.securesms.conversation.v2.dialogs

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import io.beldex.bchat.R
import io.beldex.bchat.databinding.DialogJoinOpenGroupBinding
import com.beldex.libbchat.utilities.OpenGroupUrlParser
import com.beldex.libsignal.utilities.ThreadUtils
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import com.thoughtcrimes.securesms.groups.OpenGroupManager
import com.thoughtcrimes.securesms.util.ConfigurationMessageUtilities

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
        binding.joinButton.setOnClickListener { join() }
        builder.setView(binding.root)
    }

    private fun join() {
        val openGroup = OpenGroupUrlParser.parseUrl(url)
        ThreadUtils.queue {
            try {
                OpenGroupManager.add(openGroup.server, openGroup.room, openGroup.serverPublicKey, requireActivity())
                MessagingModuleConfiguration.shared.storage.onOpenGroupAdded(openGroup.server)
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(requireActivity())
            } catch (e: Exception) {
                Toast.makeText(requireActivity(), R.string.activity_join_public_chat_error, Toast.LENGTH_SHORT).show()
            }
        }
        dismiss()
    }
}