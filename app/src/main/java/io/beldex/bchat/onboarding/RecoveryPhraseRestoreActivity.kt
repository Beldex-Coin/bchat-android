package io.beldex.bchat.onboarding

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.Hex
import com.beldex.libsignal.utilities.KeyHelper
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import com.beldex.libsignal.utilities.hexEncodedPublicKey
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.KeyPairUtilities
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.onboarding.ui.RecoveryPhraseRestoreScreen
import io.beldex.bchat.seed.RecoveryGetSeedDetailsActivity
import io.beldex.bchat.util.push
import io.beldex.bchat.R


class RecoveryPhraseRestoreActivity : BaseComponentActivity() {
    private var mnemonic by mutableStateOf("")

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        TextSecurePreferences.apply {
            setHasViewedSeed(this@RecoveryPhraseRestoreActivity, true)
            setConfigurationMessageSynced(this@RecoveryPhraseRestoreActivity, false)
            setRestorationTime(this@RecoveryPhraseRestoreActivity, System.currentTimeMillis())
            setLastProfileUpdateTime(this@RecoveryPhraseRestoreActivity, System.currentTimeMillis())
        }

        setContent {
            BChatTheme {
                RecoveryPhraseRestoreScreen(
                    mnemonic = mnemonic,
                    onMnemonicChange = { mnemonic = it },
                    wordCount = wordCount(mnemonic),
                    onPasteClick = { pasteFromClipboard() },
                    onClearClick = { mnemonic = "" },
                    onContinueClick = { onContinueClick() },
                    onBackClick = { finish() }
                )
            }
        }
    }
    // endregion

    private fun wordCount(text: String): Int =
        if (text.isEmpty()) 0 else text.trim().split("\\s+".toRegex()).size

    // region Interaction
    private fun onContinueClick() {
        if (wordCount(mnemonic) == 25) {
            restore()
        } else {
            Toast.makeText(this, getString(R.string.please_enter_valid_seed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val item = clipboard.primaryClip!!.getItemAt(0)
            if (item.text != null) {
                mnemonic = item.text.toString()
            }
        } else {
            Toast.makeText(this, R.string.no_copied_seed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun restore() {
        val trimmedMnemonic = mnemonic.trimStart().trimEnd()
        try {
            val loadFileContents: (String) -> String = { fileName ->
                MnemonicUtilities.loadFileContents(this, fileName)
            }
            val hexEncodedSeed = MnemonicCodec(loadFileContents).decode(trimmedMnemonic)
            val seed = Hex.fromStringCondensed(hexEncodedSeed)
            val keyPairGenerationResult = KeyPairUtilities.generate(seed)
            val x25519KeyPair = keyPairGenerationResult.x25519KeyPair
            KeyPairUtilities.store(this, seed, keyPairGenerationResult.ed25519KeyPair, x25519KeyPair)
            val userHexEncodedPublicKey = x25519KeyPair.hexEncodedPublicKey
            val registrationID = KeyHelper.generateRegistrationId(false)
            TextSecurePreferences.setLocalRegistrationId(this, registrationID)
            TextSecurePreferences.setLocalNumber(this, userHexEncodedPublicKey)
            val intent = Intent(this, RecoveryGetSeedDetailsActivity::class.java)
            intent.putExtra("seed",seed1)
            push(intent)
            finish()
            // Important
            /*val intent = Intent(this, DisplayNameActivity::class.java)
            push(intent)*/
        } catch (e: Exception) {
            val message = if (e is MnemonicCodec.DecodingError) e.description else MnemonicCodec.DecodingError.Generic.description
            return Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val seed1 by lazy {
        var hexEncodedSeed = IdentityKeyUtil.retrieve(this, IdentityKeyUtil.BELDEX_SEED)
        if (hexEncodedSeed == null) {
            hexEncodedSeed = IdentityKeyUtil.getIdentityKeyPair(this).hexEncodedPrivateKey // Legacy account
        }
        val loadFileContents: (String) -> String = { fileName ->
            MnemonicUtilities.loadFileContents(this, fileName)
        }
        MnemonicCodec(loadFileContents).encode(hexEncodedSeed!!, MnemonicCodec.Language.Configuration.english)
    }
    // endregion
}
