package io.beldex.bchat.onboarding

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityCheckPasswordBinding
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.BaseActionBarActivity
import io.beldex.bchat.keyboard.CustomKeyboardView
import io.beldex.bchat.seed.ShowSeedActivity
import io.beldex.bchat.util.push
import io.beldex.bchat.util.setUpActionBarBchatLogo

class CheckPasswordActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityCheckPasswordBinding
    private lateinit var keyboard: CustomKeyboardView
    private var page: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Password")

        page = intent.extras!!.getInt("page")


        with(binding) {
            when (page) {
                1 -> {
                    checkPasswordDescriptionTextView.text =
                        resources.getString(R.string.check_password_description_content_seed)
                    checkPasswordDescriptionsTextView.typeface= Typeface.DEFAULT_BOLD
                }
                2 -> {
                    checkPasswordDescriptionTextView.text =
                        resources.getString(R.string.checkPasswordDescriptionTextView)
                    checkPasswordDescriptionsTextView.typeface= Typeface.DEFAULT_BOLD
                    checkPasswordDescriptionsTextView.visibility=View.VISIBLE
                }
                else -> {
                    checkPasswordDescriptionTextView.text =
                        resources.getString(R.string.check_password_description_content_keys)
                }
            }
            userPinEditTxt.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validatePassword(s.toString(),false)
                }
            })

            binding.userPinEditTxt.requestFocus()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            val ic: InputConnection = binding.userPinEditTxt.onCreateInputConnection(EditorInfo())
            binding.keyboard1?.setInputConnection(ic)

            binding.userPinEditTxt.setOnTouchListener { _: View, event: MotionEvent ->
                binding.userPinEditTxt.onTouchEvent(event) // call native handler

                true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // API 21
                binding.userPinEditTxt.showSoftInputOnFocus = false
            } else { // API 11-20
                binding.userPinEditTxt.setTextIsSelectable(true)
            }

            binding.keyboard1.buttonEnter!!.setOnClickListener() {
                validatePassword(userPinEditTxt.text.toString(),true)
            }
        }
    }

    private fun validatePassword(pin: String,validation:Boolean) {
        val userPassword = TextSecurePreferences.getMyPassword(this@CheckPasswordActivity)
        when{
            pin.isEmpty() -> {
                binding.userPinEditTxtLayout.isErrorEnabled = false
                if (validation) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_enter_your_four_digit_pin),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            pin.length < 4 -> {
                if(validation) {
                    binding.userPinEditTxtLayout.isErrorEnabled=true
                    binding.userPinEditTxtLayout.error = getString(R.string.invalid_password)
                }else{
                    binding.userPinEditTxtLayout.isErrorEnabled=false
                }
            }
            userPassword != pin -> {
                binding.userPinEditTxtLayout.isErrorEnabled = true
                binding.userPinEditTxtLayout.error = getString(R.string.invalid_password)
            }
            userPassword == pin -> {
                validateSuccess(page)
            }
            else -> {}
        }
    }

    private fun validateSuccess(page: Int) {
        when (page) {
            1 -> {
                val intent = Intent(this, ShowSeedActivity::class.java)
                push(intent)
                finish()
            }
            2 -> {
                /*val intent = Intent(this, WalletInfoSeedActivity::class.java)
                push(intent)
                finish()*/
            }
            else -> {
            }
        }

    }
}