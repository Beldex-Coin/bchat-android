package io.beldex.bchat.onboarding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityDisplayNameBinding
import com.beldex.libbchat.utilities.SSKEnvironment.ProfileManagerProtocol
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.util.*
import io.beldex.bchat.util.setUpActionBarBchatLogo
import java.util.regex.Pattern


class DisplayNameActivity : BaseActionBarActivity() {
    private lateinit var binding: ActivityDisplayNameBinding

    private val namePattern = Pattern.compile("[A-Za-z0-9]+")


    //private ImageView ivGuntherLogo;
    //private TextView tvGuntherText;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpActionBarBchatLogo("Display Name",true)
        binding = ActivityDisplayNameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            displayNameEditText.imeOptions =
                    displayNameEditText.imeOptions or 16777216 // Always use incognito keyboard
            displayNameEditText.setOnEditorActionListener(
                    TextView.OnEditorActionListener { _, actionID, _ ->
                        if (actionID == EditorInfo.IME_ACTION_SEARCH ||
                                actionID == EditorInfo.IME_ACTION_DONE
                        ) {
                            register()
                            return@OnEditorActionListener true
                        }
                        false
                    })
            registerButton.setOnClickListener { register() }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.registerButton.isEnabled = true
    }



    private fun register() {
        val displayName = binding.displayNameEditText.text.toString().trim()
        if (displayName.isEmpty()) {
            return Toast.makeText(
                    this,
                    R.string.activity_display_name_display_name_missing_error,
                    Toast.LENGTH_SHORT
            ).show()
        }
        if (displayName.toByteArray().size > ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
            return Toast.makeText(
                    this,
                    R.string.activity_display_name_display_name_too_long_error,
                    Toast.LENGTH_SHORT
            ).show()
        }
        if (!displayName.matches(namePattern.toRegex())) {
            return Toast.makeText(
                    this,
                    R.string.display_name_validation,
                    Toast.LENGTH_SHORT
            ).show()
        }
        binding.registerButton.isEnabled = false
        showProgressDialog(R.string.generate_wallet_creating, 250)
        Handler().postDelayed({
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(binding.displayNameEditText.windowToken, 0)
            TextSecurePreferences.setProfileName(this, displayName)
            dismissProgressDialog()
            val intent = Intent(this, RegisterActivity::class.java)
            val b = Bundle()
            b.putString("type","accept")
            b.putString("displayName",binding.displayNameEditText.text.toString())
            intent.putExtras(b)
            push(intent)
        }, 1000)
    }
}