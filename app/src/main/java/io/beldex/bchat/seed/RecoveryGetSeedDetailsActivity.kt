package io.beldex.bchat.seed
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.onboarding.CreatePasswordActivity
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo
import io.beldex.bchat.R
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.databinding.ActivityRecoveryGetSeedDetailsBinding
import io.beldex.bchat.util.RandomAddressGenerate
import java.util.regex.Pattern

class RecoveryGetSeedDetailsActivity :  BaseActionBarActivity() {
    private lateinit var binding:ActivityRecoveryGetSeedDetailsBinding

    private var getSeed:String?=null
    private val namePattern = Pattern.compile("[A-Za-z0-9]+")
    private val myFormat = "yyyy-MM-dd" // mention the format you need


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoveryGetSeedDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Restore from Seed", true)
        getSeed = intent.extras?.getString("seed")
        with(binding){
            restoreSeedWalletName.imeOptions = restoreSeedWalletName.imeOptions or 16777216 // Always use incognito keyboard
            restoreSeedWalletName.setOnEditorActionListener(
                TextView.OnEditorActionListener { _, actionID, _ ->
                    if (actionID == EditorInfo.IME_ACTION_SEARCH ||
                        actionID == EditorInfo.IME_ACTION_DONE
                    ) {
                        register()
                        return@OnEditorActionListener true
                    }
                    false
                })
            restoreSeedRestoreButton.setOnClickListener {
                showProgressDialog(R.string.generate_wallet_creating, 250)
                register()
            }
        }
        //Random Address
        val walletAddress = RandomAddressGenerate().randomAddress()
        IdentityKeyUtil.save(this, IdentityKeyUtil.IDENTITY_W_ADDRESS_PREF, walletAddress)
        TextSecurePreferences.setSenderAddress(this,walletAddress)
    }

    private fun register() {
        val displayName = binding.restoreSeedWalletName.text.toString().trim()
        if (displayName.isEmpty()) {
            return Toast.makeText(this, R.string.activity_display_name_display_name_missing_error, Toast.LENGTH_SHORT).show()
        }
        if (displayName.toByteArray().size > SSKEnvironment.ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
            return Toast.makeText(this, R.string.activity_display_name_display_name_too_long_error, Toast.LENGTH_SHORT).show()
        }

        if (!displayName.matches(namePattern.toRegex())) {
            return Toast.makeText(
                    this,
                    R.string.display_name_validation,
                    Toast.LENGTH_SHORT
            ).show()
        }

        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.restoreSeedWalletName.windowToken, 0)
        TextSecurePreferences.setProfileName(this, displayName)
        updateKeyPair()
        dismissProgressDialog()
    }
    // region Updating
    private fun updateKeyPair() {
        val intent = Intent(this, CreatePasswordActivity::class.java)
        intent.putExtra("callPage",2)
        push(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
    }
}