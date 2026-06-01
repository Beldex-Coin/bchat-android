package io.beldex.bchat.applock

import android.os.Bundle
import io.beldex.bchat.databinding.ActivityAppLockDetailsBinding
import io.beldex.bchat.util.setUpActionBarBchatLogo
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.beldex.libbchat.utilities.ExpirationUtil
import io.beldex.bchat.R
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setScreenLockEnabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setScreenLockTimeout
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.my_account.ui.ScreenTimeoutOptions
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.util.push
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.core.graphics.drawable.toDrawable


class AppLockDetailsActivity : io.beldex.bchat.PassphraseRequiredActionBarActivity() {
    private lateinit var binding: ActivityAppLockDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityAppLockDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo(getString(R.string.activity_settings_app_lock_button_title))
        with(binding) {
            val currentIndex =
                IdentityKeyUtil.retrieve(
                    this@AppLockDetailsActivity,
                    IdentityKeyUtil.SCREEN_TIMEOUT_KEY
                )?.toIntOrNull() ?: 0

            val currentOption = ScreenTimeoutOptions.entries.getOrElse(currentIndex) {
                    ScreenTimeoutOptions.None
                }

            appLockDetailsScreenLockInActivityTimeOut.text =
                currentOption.displayValue(this@AppLockDetailsActivity)
            appLockDetailsChangePasswordCard.setOnClickListener {

            }
            appLockDetailsScreenLockInActivityTimeOutCard.setOnClickListener {

                if (IdentityKeyUtil.retrieve(
                        this@AppLockDetailsActivity,
                        IdentityKeyUtil.SCREEN_TIMEOUT_KEY
                    ) == null
                ) {
                    IdentityKeyUtil.save(
                        this@AppLockDetailsActivity,
                        IdentityKeyUtil.SCREEN_TIMEOUT_KEY,
                        "0"
                    )
                }

                numberPickerDialog()
            }
            changePassword.setOnClickListener()
            {
                val intent = Intent(this@AppLockDetailsActivity, ChangePasswordActivity::class.java)
                push(intent)
            }
        }
    }

    private fun numberPickerDialog() {

        val dialogView = layoutInflater.inflate(
            R.layout.number_picker_dialog,
            null
        )

        val numberPicker =
            dialogView.findViewById<NumberPicker>(R.id.dialog_number_picker)

        val cancel =
            dialogView.findViewById<TextView>(R.id.number_picker_dialog_cancel)

        val ok =
            dialogView.findViewById<TextView>(R.id.number_picker_dialog_ok)

        val options = ScreenTimeoutOptions.entries

        val displayedValues = options.map {
            it.displayValue(this)
        }.toTypedArray()

        val currentIndex =
            IdentityKeyUtil.retrieve(
                this,
                IdentityKeyUtil.SCREEN_TIMEOUT_KEY
            )?.toIntOrNull() ?: 0

        numberPicker.minValue = 0
        numberPicker.maxValue = displayedValues.lastIndex
        numberPicker.displayedValues = displayedValues
        numberPicker.value = currentIndex
        numberPicker.wrapSelectorWheel = true

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(
            Color.TRANSPARENT.toDrawable()
        )

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        ok.setOnClickListener {

            val selectedIndex = numberPicker.value
            val selectedOption = options[selectedIndex]

            IdentityKeyUtil.save(
                this,
                IdentityKeyUtil.SCREEN_TIMEOUT_KEY,
                selectedIndex.toString()
            )

            binding.appLockDetailsScreenLockInActivityTimeOut.text =
                selectedOption.displayValue(this)

            setScreenLockEnabled(this, true)

            val intent = Intent(
                this,
                KeyCachingService::class.java
            ).apply {
                action = KeyCachingService.LOCK_TOGGLED_EVENT
            }

            startService(intent)

            val timeoutSeconds =
                if (selectedOption == ScreenTimeoutOptions.None) {
                    950400
                } else {
                    TimeUnit.MILLISECONDS.toSeconds(
                        selectedOption.timeoutMillis
                    )
                }

            setScreenLockTimeout(this, timeoutSeconds)

            dialog.dismiss()
        }

        dialog.show()
    }
}