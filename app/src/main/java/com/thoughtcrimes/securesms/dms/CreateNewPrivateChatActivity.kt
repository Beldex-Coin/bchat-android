package com.thoughtcrimes.securesms.dms

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityCreateNewPrivateChatBinding
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.PublicKeyValidation
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent

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
                val intent = Intent(this@CreateNewPrivateChatActivity,PrivateChatScanQRCodeActivity::class.java)
                privateChatScanQRCodeActivityResultLauncher.launch(intent)
            }

            //SteveJosephh21
            createPrivateChatButton.setTextColor(ContextCompat.getColor(this@CreateNewPrivateChatActivity, R.color.disable_button_text_color))
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                createPrivateChatButton.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@CreateNewPrivateChatActivity,
                        R.drawable.prominent_filled_button_medium_background_disable
                    )
                );
            } else {
                createPrivateChatButton.background =
                    ContextCompat.getDrawable(
                        this@CreateNewPrivateChatActivity,
                        R.drawable.prominent_filled_button_medium_background_disable
                    );
            }
            createPrivateChatButton.isEnabled = publicKeyEditText.text.isNotEmpty()
            publicKeyEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                    if (s.length == 0){
                        createPrivateChatButton.isEnabled = false
                        createPrivateChatButton.setTextColor(ContextCompat.getColor(this@CreateNewPrivateChatActivity, R.color.disable_button_text_color))
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                            createPrivateChatButton.setBackgroundDrawable(
                                ContextCompat.getDrawable(
                                    this@CreateNewPrivateChatActivity,
                                    R.drawable.prominent_filled_button_medium_background_disable
                                )
                            );
                        } else {
                            createPrivateChatButton.background =
                                ContextCompat.getDrawable(
                                    this@CreateNewPrivateChatActivity,
                                    R.drawable.prominent_filled_button_medium_background_disable
                                );
                        }
                    }else{
                        createPrivateChatButton.isEnabled = true
                        createPrivateChatButton.setTextColor(
                            ContextCompat.getColor(
                            this@CreateNewPrivateChatActivity, R.color.white))
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                            createPrivateChatButton.setBackgroundDrawable(
                                ContextCompat.getDrawable(
                                    this@CreateNewPrivateChatActivity,
                                    R.drawable.prominent_filled_button_medium_background
                                )
                            );
                        } else {
                            createPrivateChatButton.background =
                                ContextCompat.getDrawable(
                                    this@CreateNewPrivateChatActivity,
                                    R.drawable.prominent_filled_button_medium_background
                                );
                        }
                    }
                }
            })
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        hideSoftKeyboard()
        binding.publicKeyEditText.isFocusable = false
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

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    binding.loader.visibility = View.GONE
                }
            })
    }

    //BNS disabled 16-01-2023
    private fun createPrivateChatIfPossible(bnsNameOrPublicKey: String) {
        if (PublicKeyValidation.isValid(bnsNameOrPublicKey)) {
            Log.d("PublicKeyValidation", "OK")
            createPrivateChat(bnsNameOrPublicKey)
        } else {
            Log.d("PublicKeyValidation", "Cancel")

            Toast.makeText(this, R.string.invalid_bchat_id, Toast.LENGTH_SHORT).show()

            //Important 02-06-2022 - 2.30 PM
            // This could be an BNS name
            /*showLoader()
                MnodeAPI.getBchatID(bnsNameOrPublicKey).successUi { hexEncodedPublicKey ->
                Log.d("PublicKeyValidation", "successUi")
                hideLoader()
                Log.d("Beldex", "value of Bchat id for BNS name $hexEncodedPublicKey")
                this.createPrivateChat(hexEncodedPublicKey)
            }.failUi { exception ->
                hideLoader()
                val message = resources.getString(R.string.fragment_enter_public_key_error_message)
                exception.localizedMessage?.let {
                    *//*message = it*//*
                    Log.d("Beldex","BNS exception $it")
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }*/
        }
    }

    private fun createPrivateChat(hexEncodedPublicKey: String) {
        val recipient = Recipient.from(this, Address.fromSerialized(hexEncodedPublicKey), false)
        val bundle = Bundle()
        bundle.putParcelable(ConversationFragmentV2.URI,intent.data)
        bundle.putString(ConversationFragmentV2.TYPE,intent.type)
        val returnIntent = Intent()
        returnIntent.putExtra(ConversationFragmentV2.ADDRESS, recipient.address)
        //returnIntent.setDataAndType(intent.data, intent.type)
        val existingThread =
            DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        returnIntent.putExtra(ConversationFragmentV2.THREAD_ID, existingThread)
        returnIntent.putExtras(bundle)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private var privateChatScanQRCodeActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val hexEncodedPublicKey = result.data!!.getStringExtra(ConversationFragmentV2.HEX_ENCODED_PUBLIC_KEY)
            if(hexEncodedPublicKey!=null) {
                val recipient = Recipient.from(this, Address.fromSerialized(hexEncodedPublicKey), false)
                val bundle = Bundle()
                bundle.putParcelable(ConversationFragmentV2.URI,result.data!!.getParcelableExtra(ConversationFragmentV2.URI))
                bundle.putString(ConversationFragmentV2.TYPE,result.data!!.getStringExtra(ConversationFragmentV2.TYPE))
                val returnIntent = Intent()
                returnIntent.putExtra(ConversationFragmentV2.ADDRESS, recipient.address)
                //returnIntent.setDataAndType(intent.data, intent.type)
                val existingThread =
                    DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
                returnIntent.putExtra(ConversationFragmentV2.THREAD_ID, existingThread)
                returnIntent.putExtras(bundle)
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        }
    }
    private fun hideSoftKeyboard() {
        if (currentFocus != null) {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }
}