package com.thoughtcrimes.securesms.onboarding

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.Toast
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.Hex
import com.beldex.libsignal.utilities.KeyHelper
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import com.beldex.libsignal.utilities.hexEncodedPublicKey
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.crypto.KeyPairUtilities
import com.thoughtcrimes.securesms.crypto.MnemonicUtilities
import com.thoughtcrimes.securesms.seed.RecoveryGetSeedDetailsActivity
import com.thoughtcrimes.securesms.util.push
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityRecoveryPhraseRestoreBinding


class RecoveryPhraseRestoreActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityRecoveryPhraseRestoreBinding
    var filter: InputFilter?=null
    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Restore from Seed",true)
        TextSecurePreferences.apply {
            setHasViewedSeed(this@RecoveryPhraseRestoreActivity, true)
            setConfigurationMessageSynced(this@RecoveryPhraseRestoreActivity, false)
            setRestorationTime(this@RecoveryPhraseRestoreActivity, System.currentTimeMillis())
            setLastProfileUpdateTime(this@RecoveryPhraseRestoreActivity, System.currentTimeMillis())
        }
        binding = ActivityRecoveryPhraseRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mnemonicEditText.imeOptions = binding.mnemonicEditText.imeOptions or 16777216 // Always use incognito keyboard
        binding.restoreButton.setOnClickListener {
            if(binding.recoveryPhraseCountWord.text!=null && binding.recoveryPhraseCountWord.text=="25/25") {
                restore()
            }
            else{
                Toast.makeText(this,"Please enter valid seed",Toast.LENGTH_SHORT).show()
            }
        }

        binding.mnemonicEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                var numberOfInputWords = 0

                if(s.toString().isNotEmpty())
                    numberOfInputWords = s.toString().trim().split("\\s+".toRegex()).size
                binding.recoveryPhraseCountWord.text = "$numberOfInputWords/25"

                if (numberOfInputWords >= 25){
                    filter = InputFilter.LengthFilter(binding.mnemonicEditText.text.toString().length)
                    binding.mnemonicEditText.filters = arrayOf<InputFilter>(filter ?: return)
                }
                else if (filter != null) {
                    binding.mnemonicEditText.filters = arrayOfNulls(0)
                    filter = null
                }
            }
        })

        binding.clearButton.setOnClickListener {
            binding.mnemonicEditText.text.clear()
            binding.recoveryPhraseCountWord.text = "0/25"
        }

        binding.recoveryPhrasePasteIcon.setOnClickListener {
            val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            //since the clipboard contains plain text.
            if (clipboard.hasPrimaryClip()) {
                val item = clipboard.primaryClip!!.getItemAt(0)
                // Gets the clipboard as text.
                binding.mnemonicEditText.setText(item.text.toString())
            } else {
                Toast.makeText(this, R.string.no_copied_seed, Toast.LENGTH_SHORT)
                    .show()
            }

        }
    }

    // endregion

    // region Interaction
    private fun restore() {
        val mnemonic = binding.mnemonicEditText.text.toString().trimStart().trimEnd()
        try {
            val loadFileContents: (String) -> String = { fileName ->
                MnemonicUtilities.loadFileContents(this, fileName)
            }
            val hexEncodedSeed = MnemonicCodec(loadFileContents).decode(mnemonic)
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

    private fun openURL(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
        }
    }
    // endregion
}