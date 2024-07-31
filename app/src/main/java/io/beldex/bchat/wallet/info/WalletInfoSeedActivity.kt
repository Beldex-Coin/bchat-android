package io.beldex.bchat.wallet.info

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.crypto.MnemonicCodec
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.hexEncodedPrivateKey
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.crypto.MnemonicUtilities
import io.beldex.bchat.home.HomeActivity
import io.beldex.bchat.onboarding.LandingActivity
import io.beldex.bchat.preferences.ClearAllDataDialog
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.util.Helper
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityRecoveryPhraseBinding
import io.beldex.bchat.databinding.ActivityWalletInfoSeedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WalletInfoSeedActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityWalletInfoSeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Seed", false)
        binding = ActivityWalletInfoSeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding)
        {
            walletInfoSeedContinueButton.setTextColor(
                ContextCompat.getColor(
                    this@WalletInfoSeedActivity,
                    R.color.disable_button_text_color
                )
            )
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                walletInfoSeedContinueButton.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        this@WalletInfoSeedActivity,
                        R.drawable.prominent_filled_button_medium_background_disable
                    )
                );
            } else {
                walletInfoSeedContinueButton.background =
                    ContextCompat.getDrawable(
                        this@WalletInfoSeedActivity,
                        R.drawable.prominent_filled_button_medium_background_disable
                    );
            }
            walletInfoSeedContinueButton.setOnClickListener() {
                if (!walletInfoSeedContinueButton.isEnabled) {
                    //walletInfoSeedContinueButton.isEnabled=true
                    Toast.makeText(
                        this@WalletInfoSeedActivity,
                        "Please copy and save your seed",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    confirmationAlertDialogBox()
                }
            }
            recoveryPhraseCopyIcon.setOnClickListener() {
                copySeed()
            }
            if (bChatSeedTextView != null) {
                bChatSeedTextView.text = seed
            }
        }
    }

    private fun confirmationAlertDialogBox() {
        val dialog = AlertDialog.Builder(this, R.style.BChatAlertDialog)
            .setMessage(R.string.confirmation_alert_dialog_box_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                binding.progressBar.isVisible = true
                binding.recoveryPhraseCopyIcon.isClickable=false
                binding.walletInfoSeedContinueButton.isClickable=false
                clearAllData()
            }.show()
        //New Line
        val textView: TextView = dialog.findViewById(android.R.id.message)
        val face: Typeface = Typeface.createFromAsset(assets, "fonts/open_sans_medium.ttf")
        textView.typeface = face
    }

    //New Line
    private val seed by lazy {
        var hexEncodedSeed = IdentityKeyUtil.retrieve(this, IdentityKeyUtil.BELDEX_SEED)
        if (hexEncodedSeed == null) {
            hexEncodedSeed =
                IdentityKeyUtil.getIdentityKeyPair(this).hexEncodedPrivateKey // Legacy account
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
        val seed = binding.bChatSeedTextView?.text.toString()
        val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Seed", seed)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        binding.walletInfoSeedContinueButton.isEnabled = true
        binding.hint?.visibility = View.GONE
        binding.walletInfoSeedContinueButton.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.white
            )
        )
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            binding.walletInfoSeedContinueButton.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this@WalletInfoSeedActivity,
                    R.drawable.prominent_filled_button_medium_background
                )
            );
        } else {
            binding.walletInfoSeedContinueButton.background =
                ContextCompat.getDrawable(
                    this@WalletInfoSeedActivity,
                    R.drawable.prominent_filled_button_medium_background
                );
        }
    }

    var clearJob: Job? = null
        set(value) {
            field = value
        }

    enum class Steps {
        INFO_PROMPT,
        NETWORK_PROMPT,
        DELETING
    }

    /*var step = Steps.INFO_PROMPT
        set(value) {
            field = value
            updateUI()
        }*/

    private fun clearAllData() {
        clearJob = lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                //step = Steps.DELETING
            }

            try {
                ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@WalletInfoSeedActivity)
                    .get()
            } catch (e: Exception) {
                Log.e("Beldex", "Failed to force sync", e)
            }

            //New Line
            removeWallet()

            ApplicationContext.getInstance(this@WalletInfoSeedActivity).clearAllData(false)
            withContext(Dispatchers.Main) {
            }
        }
    }

    private fun removeWallet() {
        val walletFolder: File = Helper.getWalletRoot(this)
        val walletName = TextSecurePreferences.getWalletName(this)
        val walletFile = File(walletFolder, walletName!!)
        val walletKeys = File(walletFolder, "$walletName.keys")
        val walletAddress = File(walletFolder, "$walletName.address.txt")
        if (walletFile.exists()) {
            walletFile.delete() // when recovering wallets, the cache seems corrupt - so remove it
        }
        if (walletKeys.exists()) {
            walletKeys.delete()
        }
        if (walletAddress.exists()) {
            walletAddress.delete()
        }
    }
}