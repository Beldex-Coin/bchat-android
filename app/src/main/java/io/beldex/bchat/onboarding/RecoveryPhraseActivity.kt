package io.beldex.bchat.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityRecoveryPhraseBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo


class RecoveryPhraseActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityRecoveryPhraseBinding
    var copiedSeed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Recovery Phrase", true)
        binding = ActivityRecoveryPhraseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding)
        {
            registerButton.setTextColor(ContextCompat.getColor(this@RecoveryPhraseActivity, R.color.disable_button_text_color))
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                registerButton.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@RecoveryPhraseActivity,
                        R.drawable.prominent_filled_button_medium_background_disable
                    )
                );
            } else {
                registerButton.background =
                    ContextCompat.getDrawable(
                        this@RecoveryPhraseActivity,
                        R.drawable.prominent_filled_button_medium_background_disable
                    );
            }
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
            recoveryPhraseCopyIcon.setOnClickListener() {
                copiedSeed = true
                copySeed()
            }
            shareBtn.setOnClickListener() {
                shareAddress()
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
        binding.hint?.visibility = View.GONE
        binding.registerButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            binding.registerButton.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this@RecoveryPhraseActivity,
                    R.drawable.prominent_filled_button_medium_background
                )
            );
        } else {
            binding.registerButton.background =
                ContextCompat.getDrawable(
                    this@RecoveryPhraseActivity,
                    R.drawable.prominent_filled_button_medium_background
                );
        }
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