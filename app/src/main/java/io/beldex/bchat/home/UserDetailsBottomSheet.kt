package io.beldex.bchat.home

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentUserDetailsBottomSheetBinding
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.database.ThreadDatabase
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.mms.GlideApp
import io.beldex.bchat.util.UiModeUtilities
import javax.inject.Inject

@AndroidEntryPoint
class UserDetailsBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var threadDb: ThreadDatabase

    private lateinit var binding: FragmentUserDetailsBottomSheetBinding
    companion object {
        const val ARGUMENT_PUBLIC_KEY = "publicKey"
        const val ARGUMENT_THREAD_ID = "threadId"
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentUserDetailsBottomSheetBinding.inflate(inflater, container, false)
        setStyle(STYLE_NORMAL, R.style.Theme_Bchat_BottomSheet)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val publicKey = arguments?.getString(ARGUMENT_PUBLIC_KEY) ?: return dismiss()
        val threadID = arguments?.getLong(ARGUMENT_THREAD_ID) ?: return dismiss()
        val recipient = Recipient.from(requireContext(), Address.fromSerialized(publicKey), false)
        val threadRecipient = threadDb.getRecipientForThreadId(threadID) ?: return dismiss()
        with(binding) {
            profilePictureView.root.publicKey = publicKey
            profilePictureView.root.glide = GlideApp.with(this@UserDetailsBottomSheet)
            profilePictureView.root.isLarge = true
            profilePictureView.root.update(recipient)
            nameTextViewContainer.visibility = View.VISIBLE
            nameEditIcon.setOnClickListener {
                nameTextViewContainer.visibility = View.INVISIBLE
                nameEditTextContainer.visibility = View.VISIBLE
                nicknameEditText.text = null
                nicknameEditText.requestFocus()
                showSoftKeyboard()
            }
            cancelNicknameEditingButton.setOnClickListener {
                nicknameEditText.clearFocus()
                hideSoftKeyboard()
                nameTextViewContainer.visibility = View.VISIBLE
                nameEditTextContainer.visibility = View.INVISIBLE
            }
            saveNicknameButton.setOnClickListener {
                saveNickName(recipient)
            }
            nicknameEditText.setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        saveNickName(recipient)
                        return@setOnEditorActionListener true
                    }
                    else -> return@setOnEditorActionListener false
                }
            }
            nameTextView.text = recipient.name ?: publicKey // Uses the Contact API internally

            publicKeyTextView.isVisible = !threadRecipient.isOpenGroupRecipient
            messageButton.isVisible = !threadRecipient.isOpenGroupRecipient
            publicKeyTextView.text = publicKey
            publicKeyTextView.setOnLongClickListener {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Bchat ID", publicKey)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT)
                    .show()
                true
            }
            messageButton.setOnClickListener {
                val threadId = MessagingModuleConfiguration.shared.storage.getThreadId(recipient)
                /*val intent = Intent(
                    context,
                    ConversationActivityV2::class.java
                )
                intent.putExtra(ConversationActivityV2.ADDRESS, recipient.address)
                intent.putExtra(ConversationActivityV2.THREAD_ID, threadId ?: -1)
                startActivity(intent)*/
                activityCallback?.callConversationFragmentV2(recipient.address,threadId?:-1)
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        val isLightMode = UiModeUtilities.isDayUiMode(requireContext())
        window.setDimAmount(if (isLightMode) 0.1f else 0.75f)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideSoftKeyboard()
    }

    private fun saveNickName(recipient: Recipient) = with(binding) {
        if (nicknameEditText.text.trim().isEmpty()) {
            Toast.makeText(context,R.string.enter_a_valid_nickname,Toast.LENGTH_SHORT).show()
        }else{
            nicknameEditText.clearFocus()
            hideSoftKeyboard()
            nameTextViewContainer.visibility = View.VISIBLE
            nameEditTextContainer.visibility = View.INVISIBLE
            val publicKey = recipient.address.serialize()
            val contactDB = DatabaseComponent.get(requireContext()).bchatContactDatabase()
            val contact = contactDB.getContactWithBchatID(publicKey) ?: Contact(publicKey)
            contact.nickname = nicknameEditText.text.toString()
            contactDB.setContact(contact)
            nameTextView.text = recipient.name ?: publicKey // Uses the Contact API internally
        }
    }

    @SuppressLint("ServiceCast")
    fun showSoftKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.nicknameEditText, 0)
    }

    fun hideSoftKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.nicknameEditText.windowToken, 0)
    }
}