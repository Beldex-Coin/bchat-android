package com.thoughtcrimes.securesms.dms

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityCreatePrivateChatBinding
import io.beldex.bchat.databinding.FragmentEnterPublicKeyBinding
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.PublicKeyValidation
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.util.ScanQRCodeWrapperFragment
import com.thoughtcrimes.securesms.util.ScanQRCodeWrapperFragmentDelegate
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class CreatePrivateChatActivity : PassphraseRequiredActionBarActivity(), ScanQRCodeWrapperFragmentDelegate {
    private lateinit var binding: ActivityCreatePrivateChatBinding
    private val adapter = CreatePrivateChatActivityAdapter(this)
    private var isKeyboardShowing = false
        set(value) {
            val hasChanged = (field != value)
            field = value
            if (hasChanged) {
                adapter.isKeyboardShowing = value
            }
        }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityCreatePrivateChatBinding.inflate(layoutInflater)
        // Set content view
        setContentView(binding.root)
        // Set title
        supportActionBar!!.title = resources.getString(R.string.activity_create_private_chat_title)
        // Set up view pager
        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        //New Line
        binding.tabLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.page_background))
        //binding.tabLayout.setTabTextColors(Color.parseColor("#FFFFFF"),Color.parseColor("#8D8D8D"))

        binding.rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val diff = binding.rootLayout.rootView.height - binding.rootLayout.height
            val displayMetrics = this@CreatePrivateChatActivity.resources.displayMetrics
            val estimatedKeyboardHeight =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200.0f, displayMetrics)
            this@CreatePrivateChatActivity.isKeyboardShowing = (diff > estimatedKeyboardHeight)
        }
    }
    // endregion

    // region Updating
    private fun showLoader() {
        binding.loader.visibility = View.VISIBLE
        binding.loader.animate().setDuration(150).alpha(1.0f).start()
    }

    private fun hideLoader() {
        binding.loader.animate().setDuration(150).alpha(0.0f).setListener(object : AnimatorListenerAdapter() {

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                binding.loader.visibility = View.GONE
            }
        })
    }
    // endregion

    // region Interaction
    override fun handleQRCodeScanned(hexEncodedPublicKey: String) {
        createPrivateChatIfPossible(hexEncodedPublicKey)
    }

    fun createPrivateChatIfPossible(onsNameOrPublicKey: String) {
        if (PublicKeyValidation.isValid(onsNameOrPublicKey)) {
            createPrivateChat(onsNameOrPublicKey)
        } else {
            // This could be an BNS name
            showLoader()
            MnodeAPI.getBchatID(onsNameOrPublicKey).successUi { hexEncodedPublicKey ->
                hideLoader()
                this.createPrivateChat(hexEncodedPublicKey)
            }.failUi { exception ->
                hideLoader()
                var message = resources.getString(R.string.fragment_enter_public_key_error_message)
                exception.localizedMessage?.let {
                    message = it
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createPrivateChat(hexEncodedPublicKey: String) {
        /*val recipient = Recipient.from(this, Address.fromSerialized(hexEncodedPublicKey), false)
        //-Log.d("Beldex","recipient in create private chat ${recipient.address}")
        val intent = Intent(this, ConversationActivityV2::class.java)
        intent.putExtra(ConversationActivityV2.ADDRESS, recipient.address)
        intent.setDataAndType(getIntent().data, getIntent().type)
        val existingThread = DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient)
        intent.putExtra(ConversationActivityV2.THREAD_ID, existingThread)
        startActivity(intent)
        finish()*/
    }
    // endregion
}

// region Adapter
private class CreatePrivateChatActivityAdapter(val activity: CreatePrivateChatActivity) : FragmentPagerAdapter(activity.supportFragmentManager) {
    val enterPublicKeyFragment = EnterPublicKeyFragment()
    var isKeyboardShowing = false
        set(value) { field = value; enterPublicKeyFragment.isKeyboardShowing = isKeyboardShowing }

    override fun getCount(): Int {
        return 2
    }

    override fun getItem(index: Int): Fragment {
        return when (index) {
            0 -> enterPublicKeyFragment
            1 -> {
                val result = ScanQRCodeWrapperFragment()
                result.delegate = activity
                result.message = activity.resources.getString(R.string.activity_create_private_chat_scan_qr_code_explanation)
                result
            }
            else -> throw IllegalStateException()
        }
    }

    override fun getPageTitle(index: Int): CharSequence? {
        return when (index) {
            0 -> activity.resources.getString(R.string.activity_create_private_chat_enter_bchat_id_tab_title)
            1 -> activity.resources.getString(R.string.activity_create_private_chat_scan_qr_code_tab_title)
            else -> throw IllegalStateException()
        }
    }
}
// endregion

// region Enter Public Key Fragment
class EnterPublicKeyFragment : Fragment() {
    private lateinit var binding: FragmentEnterPublicKeyBinding

    var isKeyboardShowing = false
        set(value) { field = value; handleIsKeyboardShowingChanged() }

    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(requireContext())!!
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEnterPublicKeyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            publicKeyEditText.imeOptions = EditorInfo.IME_ACTION_DONE or 16777216 // Always use incognito keyboard
            publicKeyEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
            publicKeyEditText.setOnEditorActionListener { v, actionID, _ ->
                if (actionID == EditorInfo.IME_ACTION_DONE) {
                    val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
            createPrivateChatButton.setOnClickListener { createPrivateChatIfPossible() }
        }
    }

    private fun handleIsKeyboardShowingChanged() {
        binding.optionalContentContainer.isVisible = !isKeyboardShowing
    }

    private fun copyPublicKey() {
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Bchat ID", hexEncodedPublicKey)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
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
        val activity = requireActivity() as CreatePrivateChatActivity
        activity.createPrivateChatIfPossible(hexEncodedPublicKey)
    }
}
// endregion
