package com.thoughtcrimes.securesms.dms

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityCreateNewPrivateChatBinding
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.PublicKeyValidation
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.conversation.v2.ConversationActivityV2
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.util.push

class CreateNewPrivateChatActivity : PassphraseRequiredActionBarActivity() {
    private lateinit var binding: ActivityCreateNewPrivateChatBinding

    var isKeyboardShowing = false
        set(value) {
            field = value; handleIsKeyboardShowingChanged()
        }

    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityCreateNewPrivateChatBinding.inflate(layoutInflater)
        // Set content view
        setContentView(binding.root)

        // Set title
        supportActionBar!!.title = resources.getString(R.string.activity_create_private_chat_title)

        with(binding) {
            publicKeyEditText.imeOptions =
                EditorInfo.IME_ACTION_DONE or 16777216 // Always use incognito keyboard
            publicKeyEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
            publicKeyEditText.setOnEditorActionListener { v, actionID, _ ->
                if (actionID == EditorInfo.IME_ACTION_DONE) {
                    val imm =
                        v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    createPrivateChatIfPossible()
                    true
                } else {
                    false
                }
            }
            publicKeyTextView.text = hexEncodedPublicKey
            copyButton.setOnClickListener { copyPublicKey() }
            shareButton.setOnClickListener { sharePublicKey() }
            createPrivateChatButton.setOnClickListener {
                if (publicKeyEditText.text.isEmpty()) {
                    Toast.makeText(
                        this@CreateNewPrivateChatActivity,
                        "Please enter BChat ID",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    createPrivateChatIfPossible()
                }
            }
            scanQRCode.setOnClickListener {
                val intent = Intent(
                    this@CreateNewPrivateChatActivity,
                    PrivateChatScanQRCodeActivity::class.java
                )
                push(intent)
                finish()
            }
        }

    }
    // endregion

    private fun handleIsKeyboardShowingChanged() {
        binding.optionalContentContainer.isVisible = !isKeyboardShowing
    }

    private fun copyPublicKey() {
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BChat ID", hexEncodedPublicKey)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun sharePublicKey() {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, hexEncodedPublicKey)
        intent.type = "text/plain"
        startActivity(intent)
    }

    private fun createPrivateChatIfPossible() {
        val hexEncodedPublicKey = binding.publicKeyEditText.text?.trim().toString()
        createPrivateChatIfPossible(hexEncodedPublicKey)
    }

    // region Updating
    private fun showLoader() {
        binding.loader.visibility = View.VISIBLE
        binding.loader.animate().setDuration(150).alpha(1.0f).start()
    }

    private fun hideLoader() {
        binding.loader.animate().setDuration(150).alpha(0.0f)
            .setListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    binding.loader.visibility = View.GONE
                }
            })
    }

    fun createPrivateChatIfPossible(bnsNameOrPublicKey: String) {
        if (PublicKeyValidation.isValid(bnsNameOrPublicKey)) {
            Log.d("PublicKeyValidation", "OK")
            createPrivateChat(bnsNameOrPublicKey)
        } else {
            Log.d("PublicKeyValidation", "Cancel")
            Toast.makeText(this, "Invalid BChat ID", Toast.LENGTH_SHORT).show()

            //Important 02-06-2022 - 2.30 PM
           /* // This could be an BNS name
            showLoader()
            MnodeAPI.getBchatID(bnsNameOrPublicKey).successUi { hexEncodedPublicKey ->
                Log.d("PublicKeyValidation", "successUi")
                hideLoader()
                this.createPrivateChat(hexEncodedPublicKey)
            }.failUi { exception ->
                hideLoader()
                var message = resources.getString(R.string.fragment_enter_public_key_error_message)
                exception.localizedMessage?.let {
                    message = it
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }*/
        }
    }

    private fun createPrivateChat(hexEncodedPublicKey: String) {
        val recipient = Recipient.from(this, Address.fromSerialized(hexEncodedPublicKey), false)
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.ADDRESS, recipient.address)
        intent.setDataAndType(getIntent().data, getIntent().type)
        val existingThread =
            DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        intent.putExtra(ConversationActivityV2.THREAD_ID, existingThread)
        startActivity(intent)
        finish()
    }
}