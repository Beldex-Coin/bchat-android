package io.beldex.bchat.seed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityShowSeedBinding
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.util.setUpActionBarBchatLogo

class ShowSeedActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityShowSeedBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowSeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Seed")
        with(binding) {
            showSeedCopyButton.setOnClickListener {
                copySeed()
            }
            showSeedShareButton.setOnClickListener {
                shareAddress()
            }
            showSeedText.text = seed
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

    private fun copySeed() {
        val seed = binding.showSeedText.text.toString()
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Seed", seed)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun shareAddress() {
        val seed = binding.showSeedText.text.toString()
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, seed)
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, getString(R.string.share))
        startActivity(chooser)
    }
}