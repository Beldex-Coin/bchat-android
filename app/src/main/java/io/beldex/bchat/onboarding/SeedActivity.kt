package io.beldex.bchat.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivitySeedBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.util.getColorWithID

class SeedActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivitySeedBinding

    private val seed by lazy {
        var hexEncodedSeed = IdentityKeyUtil.retrieve(this, IdentityKeyUtil.BELDEX_SEED)
        if (hexEncodedSeed == null) {
            hexEncodedSeed = IdentityKeyUtil.getIdentityKeyPair(this).hexEncodedPrivateKey // Legacy account
        }
        val loadFileContents: (String) -> String = { fileName ->
            MnemonicUtilities.loadFileContents(this, fileName)
        }
        MnemonicCodec(loadFileContents).encode(hexEncodedSeed!!, MnemonicCodec.Language.Configuration.english)
    }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_seed_title)
        val seedReminderViewTitle = SpannableString("You're almost finished! 90%") // Intentionally not yet translated
        seedReminderViewTitle.setSpan(ForegroundColorSpan(resources.getColorWithID(R.color.accent, theme)), 24, 27, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        with(binding) {
            seedReminderView.title = seedReminderViewTitle
            seedReminderView.subtitle = resources.getString(R.string.view_seed_reminder_subtitle_2)
            seedReminderView.setProgress(90, false)
            seedReminderView.hideContinueButton()
            var redactedSeed = seed
            var index = 0
            for (character in seed) {
                if (character.isLetter()) {
                    redactedSeed = redactedSeed.replaceRange(index, index + 1, "▆")
                }
                index += 1
            }
            seedTextView.setTextColor(resources.getColorWithID(R.color.accent, theme))
            seedTextView.text = redactedSeed
            seedTextView.setOnLongClickListener { revealSeed(); true }
            revealButton.setOnLongClickListener { revealSeed(); true }
            copyButton.setOnClickListener { copySeed() }
        }
    }
    // endregion

    // region Updating
    private fun revealSeed() {
        val seedReminderViewTitle = SpannableString("Account secured! 100%") // Intentionally not yet translated
        seedReminderViewTitle.setSpan(ForegroundColorSpan(resources.getColorWithID(R.color.accent, theme)), 17, 21, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        with(binding) {
            seedReminderView.title = seedReminderViewTitle
            seedReminderView.subtitle = resources.getString(R.string.view_seed_reminder_subtitle_3)
            seedReminderView.setProgress(100, true)
            val seedTextViewLayoutParams = seedTextView.layoutParams as LinearLayout.LayoutParams
            seedTextViewLayoutParams.height = seedTextView.height
            seedTextView.layoutParams = seedTextViewLayoutParams
            seedTextView.setTextColor(resources.getColorWithID(R.color.text, theme))
            seedTextView.text = seed
        }
        TextSecurePreferences.setHasViewedSeed(this, true)
    }
    // endregion

    // region Interaction
    private fun copySeed() {
        revealSeed()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Seed", seed)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
    // endregion
}