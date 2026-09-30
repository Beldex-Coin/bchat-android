package io.beldex.bchat.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.unicodeNamePattern
import javax.inject.Inject

@AndroidEntryPoint
class UserDetailsBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var threadDb: ThreadDatabase

    companion object {
        const val ARGUMENT_PUBLIC_KEY = "publicKey"
        const val ARGUMENT_THREAD_ID = "threadId"
        const val TAG = "userDetailsBottomSheet"
    }

    interface UserDetailsBottomSheetListener{
        fun callConversationFragmentV2(address: Address, threadId: Long)
    }

    var activityCallback: UserDetailsBottomSheetListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UserDetailsBottomSheetListener) {
            activityCallback = context
        } else {
            throw ClassCastException(
                context.toString()
                        + " must implement Listener"
            )
        }
    }

    private var isEditingName by mutableStateOf(false)
    private var nicknameInput by mutableStateOf("")
    private var displayNameState by mutableStateOf("")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setStyle(STYLE_NORMAL, R.style.Theme_Bchat_BottomSheet)

        val publicKey = arguments?.getString(ARGUMENT_PUBLIC_KEY) ?: run { dismiss(); return ComposeView(requireContext()) }
        val threadID = arguments?.getLong(ARGUMENT_THREAD_ID) ?: run { dismiss(); return ComposeView(requireContext()) }
        val recipient = Recipient.from(requireContext(), Address.fromSerialized(publicKey), false)
        val threadRecipient = threadDb.getRecipientForThreadId(threadID) ?: run { dismiss(); return ComposeView(requireContext()) }
        // Uses the Contact API internally
        displayNameState = if (publicKey == BuildConfig.REPORT_ISSUE_ID) getString(R.string.report_issue) else recipient.name ?: publicKey

        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme {
                    UserDetailsSheetContent(
                        publicKey = publicKey,
                        name = displayNameState,
                        isEditingName = isEditingName,
                        nicknameInput = nicknameInput,
                        onNicknameInputChange = { nicknameInput = it },
                        onEditNameClick = {
                            nicknameInput = ""
                            isEditingName = true
                        },
                        onCancelEditClick = { isEditingName = false },
                        onSaveNicknameClick = { saveNickName(recipient) },
                        showBchatIdAndMessageButton = !threadRecipient.isOpenGroupRecipient,
                        onCopyBchatId = {
                            val clipboard =
                                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("BChat ID", publicKey)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        },
                        onMessageClick = {
                            val threadId = MessagingModuleConfiguration.shared.storage.getThreadId(recipient)
                            activityCallback?.callConversationFragmentV2(recipient.address, threadId ?: -1)
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        val isLightMode = UiModeUtilities.isDayUiMode(requireContext())
        window.setDimAmount(if (isLightMode) 0.1f else 0.75f)

        applyLandscapeSizing()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyLandscapeSizing()
    }

    private fun applyLandscapeSizing() {
        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        val behavior = BottomSheetBehavior.from(bottomSheet)

        if (isLandscape) {
            val displayMetrics = resources.displayMetrics

            bottomSheet.layoutParams.width = (displayMetrics.widthPixels * 0.55f).toInt()
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

            behavior.maxHeight = (displayMetrics.heightPixels * 0.9f).toInt()
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheet.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        bottomSheet.requestLayout()
    }

    private fun saveNickName(recipient: Recipient) {
        val nickname = nicknameInput.trim()
        when {
            nickname.isEmpty() -> {
                Toast.makeText(
                    context,
                    R.string.enter_a_valid_nickname,
                    Toast.LENGTH_SHORT
                ).show()
            }

            nickname.toByteArray().size > SSKEnvironment.ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH -> {
                Toast.makeText(
                    context,
                    R.string.nick_name_too_long_error,
                    Toast.LENGTH_SHORT
                ).show()
            }

            !nickname.matches(unicodeNamePattern.toRegex()) -> {
                Toast.makeText(
                    context,
                    R.string.display_name_validation,
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                val publicKey = recipient.address.serialize()
                val contactDB =
                    DatabaseComponent.get(requireContext()).bchatContactDatabase()

                val contact =
                    contactDB.getContactWithBchatID(publicKey) ?: Contact(publicKey)

                contact.nickname = nickname
                contactDB.setContact(contact)

                displayNameState = recipient.name ?: publicKey
                isEditingName = false
            }
        }
    }
}