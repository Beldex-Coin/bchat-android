package io.beldex.bchat.conversation.v2.dialogs

import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.OpenGroupUrlParser
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.PublicKeyValidation
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.groups.OpenGroupManager
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.ConversationActivityV2
import io.beldex.bchat.databinding.DialogJoinOpenGroupBinding
import io.beldex.bchat.groups.GroupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Shown upon tapping an social group invitation. */
class JoinOpenGroupDialog(private val name: String, private val url: String) : BaseDialog() {

    private val tag = JoinOpenGroupDialog::class.java.simpleName

    override fun setContentView(builder: AlertDialog.Builder) {
        val binding = DialogJoinOpenGroupBinding.inflate(LayoutInflater.from(requireContext()))
        val title = resources.getString(R.string.dialog_join_open_group_title, name)
        binding.joinOpenGroupTitleTextView.text = title
        val explanation = resources.getString(R.string.dialog_join_open_group_explanation, name)
        val spannable = SpannableStringBuilder(explanation)
        val startIndex = explanation.indexOf(name)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            startIndex + name.count(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.joinOpenGroupExplanationTextView.text = spannable
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.joinButton.setOnClickListener { join(name) }
        builder.setView(binding.root)
    }

    private fun join(name: String) {

        val activity = activity ?: return

        val openGroup = OpenGroupUrlParser.parseUrl(url)

        val stringWithExplicitScheme = if (!url.startsWith("http")) {
            "http://$url"
        } else {
            url
        }

        val parsedUrl = stringWithExplicitScheme.toHttpUrlOrNull()

        if (parsedUrl == null) {
            Toast.makeText(
                context,
                R.string.invalid_url,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val room = parsedUrl.pathSegments.firstOrNull()

        val publicKey = parsedUrl.queryParameter("public_key")

        val isV2OpenGroup = !room.isNullOrEmpty()

        if (isV2OpenGroup && (publicKey == null || !PublicKeyValidation.isValid(
                publicKey,
                64,
                false
            ))
        ) {
            Toast.makeText(
                context,
                R.string.invalid_public_key,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val (threadID, groupID) = if (isV2OpenGroup) {
                    val sanitizedServer = openGroup.server.removeSuffix("/")
                    val openGroupID = "$sanitizedServer.${room!!}"
                    OpenGroupManager.add(
                        openGroup.server,
                        openGroup.room,
                        openGroup.serverPublicKey,
                        activity
                    )
                    MessagingModuleConfiguration.shared.storage.onOpenGroupAdded(
                        stringWithExplicitScheme
                    )
                    val threadID = GroupManager.getOpenGroupThreadID(openGroupID, activity)
                    val groupID = GroupUtil.getEncodedOpenGroupID(openGroupID.toByteArray())
                    threadID to groupID
                } else {
                    throw Exception("No longer supported.")
                }
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(activity)
                withContext(Dispatchers.Main) {

                    if (!isAdded) return@withContext

                    val recipient = Recipient.from(
                        activity,
                        Address.fromSerialized(groupID),
                        false
                    )

                    Toast.makeText(
                        activity,
                        resources.getString(
                            R.string.activity_public_chat_success_message,
                            name
                        ),
                        Toast.LENGTH_SHORT
                    ).show()

                    dismissAllowingStateLoss()

                    activity.window?.decorView?.postDelayed({

                        openConversationActivity(
                            threadID,
                            recipient,
                            activity
                        )

                        activity.finish()

                    }, 1200)
                }
            } catch (e: Exception) {
                Log.e(tag, "Couldn't join social group.", e)

                withContext(Dispatchers.Main) {

                    if (!isAdded) return@withContext

                    Toast.makeText(
                        activity,
                        R.string.activity_join_public_chat_error,
                        Toast.LENGTH_SHORT
                    ).show()

                    dismissAllowingStateLoss()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val activity = activity
        if (activity != null) {
            val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val view = activity.currentFocus ?: View(activity)
            if (imm.isAcceptingText) {
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }
    }

    private fun openConversationActivity(
        threadId: Long,
        recipient: Recipient,
        activity: Activity
    ) {
        val extras = Bundle().apply {
            putLong(ConversationActivityV2.THREAD_ID, threadId)
            putParcelable(
                ConversationActivityV2.ADDRESS,
                recipient.address
            )
        }

        val intent = Intent(
            activity,
            ConversationActivityV2::class.java
        ).apply {
            putExtras(extras)
        }

        activity.startActivity(intent)
    }
}