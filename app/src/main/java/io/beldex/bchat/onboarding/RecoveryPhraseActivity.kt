package io.beldex.bchat.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.onboarding.ui.RecoveryPhraseScreen
import io.beldex.bchat.util.push
import io.beldex.bchat.R


class RecoveryPhraseActivity : BaseComponentActivity() {
    var copiedSeed by mutableStateOf(false)
    private var shareButtonLastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        copiedSeed = savedInstanceState?.getBoolean(SEED_COPIED_KEY, false)
            ?: TextSecurePreferences.isCopiedSeed(this)

        setContent {
            BChatTheme {
                RecoveryPhraseScreen(
                    title = stringResource(R.string.activity_settings_recovery_phrase_button_title),
                    seed = seed,
                    seedCopied = copiedSeed,
                    onCopySeedClick = { copySeed() },
                    onSaveClick = { shareAddressThrottled() },
                    onContinueClick = { onContinueClick() },
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SEED_COPIED_KEY, copiedSeed)
    }

    private fun onContinueClick() {
        if (!copiedSeed) {
            Toast.makeText(this, R.string.please_copy_and_save_your_seed, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.please_copy_the_seed_and_save_it, Toast.LENGTH_SHORT).show()
            homepage()
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
        TextSecurePreferences.setCopiedSeed(this, true)
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Seed", seed)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        copiedSeed = true
    }

    private fun shareAddressThrottled() {
        if (SystemClock.elapsedRealtime() - shareButtonLastClickTime >= 1000) {
            shareButtonLastClickTime = SystemClock.elapsedRealtime()
            shareAddress()
        }
    }

    private fun shareAddress() {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, seed)
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, getString(R.string.share))
        startActivity(chooser)
    }

    companion object {
        private const val SEED_COPIED_KEY = "seed_copied"
    }
}
