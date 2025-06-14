package io.beldex.bchat.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityRecoveryPhraseBinding


class RecoveryPhraseActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityRecoveryPhraseBinding
    var copiedSeed = false
    private var shareButtonLastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo(getString(R.string.activity_settings_recovery_phrase_button_title), false)
        binding = ActivityRecoveryPhraseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
        with(binding)
        {
            if(isDarkTheme) restoreSeedHintIcon.setImageResource(R.drawable.ic_restore_seed_dark) else restoreSeedHintIcon.setImageResource(R.drawable.ic_restore_seed_white)
            registerButton.setTextColor(ContextCompat.getColor(this@RecoveryPhraseActivity, R.color.disable_button_text_color))
            registerButton.background =
                ContextCompat.getDrawable(
                    this@RecoveryPhraseActivity,
                    R.drawable.prominent_filled_button_medium_background_disable
                )
            registerButton.setOnClickListener() {
                if (!copiedSeed) {
                    Toast.makeText(
                        this@RecoveryPhraseActivity,
                       R.string.please_copy_and_save_your_seed,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@RecoveryPhraseActivity,
                        R.string.please_copy_the_seed_and_save_it,
                        Toast.LENGTH_SHORT
                    ).show()
                    homepage()
                }
            }
            copyButton.setOnClickListener() {
                copiedSeed = true
                copySeed()
            }
            shareButton.setOnClickListener() {
                if (SystemClock.elapsedRealtime() - shareButtonLastClickTime >= 1000) {
                    shareButtonLastClickTime = SystemClock.elapsedRealtime()
                    shareAddress()
                }
            }
            if (bChatSeedTextView != null) {
                bChatSeedTextView.text = seed
            }
        }
    }

    //New Line
    private val seed by lazy {
        var hexEncodedSeed = IdentityKeyUtil.retrieve(this, IdentityKeyUtil.BELDEX_SEED)
        if (hexEncodedSeed == null) {
            hexEncodedSeed = IdentityKeyUtil.getIdentityKeyPair(this).hexEncodedPrivateKey // Legacy account
        }
        val loadFileContents: (String) -> String = { fileName ->
            MnemonicUtilities.loadFileContents(this, fileName)
        }
        MnemonicCodec(loadFileContents).encode(
            hexEncodedSeed!!,
            MnemonicCodec.Language.Configuration.english
        )
    }

    private fun homepage() {
        // for testing
        TextSecurePreferences.setHasSeenWelcomeScreen(this, true)
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        push(intent)
        /* finish()*/
    }

    private fun copySeed() {
        TextSecurePreferences.setCopiedSeed(this,true)
        val seed = binding.bChatSeedTextView?.text.toString()
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Seed", seed)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        binding.registerButton.isEnabled = true
        binding.hint.visibility = View.GONE
        binding.registerButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.registerButton.background =
            ContextCompat.getDrawable(
                this@RecoveryPhraseActivity,
                R.drawable.prominent_filled_button_medium_background
            )
    }

    private fun shareAddress() {
        val seed = binding.bChatSeedTextView?.text.toString()
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, seed)
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, getString(R.string.share))
        startActivity(chooser)
    }
}