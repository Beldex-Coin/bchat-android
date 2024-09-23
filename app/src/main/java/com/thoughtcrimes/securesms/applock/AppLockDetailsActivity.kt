package com.thoughtcrimes.securesms.applock

import android.os.Bundle
import io.beldex.bchat.databinding.ActivityAppLockDetailsBinding
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo
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
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.crypto.IdentityKeyUtil
import com.thoughtcrimes.securesms.service.KeyCachingService
import com.thoughtcrimes.securesms.util.push
import java.util.*
import java.util.concurrent.TimeUnit


class AppLockDetailsActivity : PassphraseRequiredActionBarActivity() {
    private lateinit var binding: ActivityAppLockDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityAppLockDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("App Lock")
        with(binding) {
            appLockDetailsScreenLockInActivityTimeOut.text = IdentityKeyUtil.retrieve(
                this@AppLockDetailsActivity,
                IdentityKeyUtil.SCREEN_TIMEOUT_VALUES_KEY
            ) ?: "None"
            appLockDetailsChangePasswordCard.setOnClickListener {

            }
            appLockDetailsScreenLockInActivityTimeOutCard.setOnClickListener {
                if (IdentityKeyUtil.retrieve(
                        this@AppLockDetailsActivity,
                        IdentityKeyUtil.SCREEN_TIMEOUT_KEY
                    ) != null
                ) {
                    numberPickerDialog()
                } else {
                    IdentityKeyUtil.save(
                        this@AppLockDetailsActivity,
                        IdentityKeyUtil.SCREEN_TIMEOUT_KEY,
                        "0"
                    )
                    IdentityKeyUtil.save(
                        this@AppLockDetailsActivity,
                        IdentityKeyUtil.SCREEN_TIMEOUT_VALUES_KEY,
                        "None"
                    )
                    numberPickerDialog()
                }
            }
            changePassword.setOnClickListener()
            {
                val intent = Intent(this@AppLockDetailsActivity, ChangePasswordActivity::class.java)
                push(intent)
            }
        }
    }

    private fun numberPickerDialog() {
        /* val builder = AlertDialog.Builder(this)
         builder.setTitle(this.getString(R.string.ExpirationDialog_disappearing_messages))
         builder.setView(view)
         builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
             val selected =
                 (view.findViewById<View>(R.id.expiration_number_picker) as NumberPickerView).value
             listener.onClick(
                 context.getResources().getIntArray(R.array.expiration_times).get(selected)
             )
         }
         builder.setNegativeButton(android.R.string.cancel, null)
         builder.show()*/
        val d: AlertDialog.Builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.number_picker_dialog, null)

        d.setView(dialogView)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.dialog_number_picker)
        val numberPickerCancel = dialogView.findViewById<TextView>(R.id.number_picker_dialog_cancel)
        val numberPickerOk = dialogView.findViewById<TextView>(R.id.number_picker_dialog_ok)
        val screenTimeOutArray: Array<out String> =
            this.resources.getStringArray(R.array.screen_lock_timeout)
        val screenTimeOutValues = arrayOfNulls<String>(screenTimeOutArray.size)
        val currentIndex: Int =
           IdentityKeyUtil.retrieve(this, IdentityKeyUtil.SCREEN_TIMEOUT_KEY).toInt()
        var selectedIndex: Int = currentIndex
        Log.d("selected Index", "$selectedIndex")
        Log.d("ScreenTimeOut", screenTimeOutArray[currentIndex])

        for (i in screenTimeOutArray.indices) {
            screenTimeOutValues[i] =
                ExpirationUtil.getTimeOutDisplayValue(this, screenTimeOutArray.get(i).toString())
            if (i == currentIndex) {
                selectedIndex = i
            }
        }
        numberPicker.displayedValues = screenTimeOutValues
        numberPicker.minValue = 0
        numberPicker.maxValue = screenTimeOutArray.size - 1
        numberPicker.wrapSelectorWheel = true
        val listener =
            NumberPicker.OnValueChangeListener { picker: NumberPicker, oldVal: Int, newVal: Int ->
                if (newVal == 0) {
                    //textView.setText(R.string.ExpirationDialog_your_messages_will_not_expire)
                } else {
                    /* textView.setText(
                         context.getString(
                             R.string.ExpirationDialog_your_messages_will_disappear_s_after_they_have_been_seen,
                             picker.displayedValues[newVal]
                         )
                     )*/
                    selectedIndex = newVal
                    Log.d("selected Index", "$selectedIndex")
                }
            }

        numberPicker.setOnValueChangedListener(listener)
        Log.d("selected Index", "$selectedIndex")
        numberPicker.value = selectedIndex
        listener.onValueChange(numberPicker, selectedIndex, selectedIndex)

        /*d.setPositiveButton("Ok",
            DialogInterface.OnClickListener { dialogInterface, i ->
                IdentityKeyUtil.save(
                    this,
                    IdentityKeyUtil.SCREEN_TIMEOUT_KEY,
                    numberPicker.value.toString()
                )
                IdentityKeyUtil.save(
                    this,
                    IdentityKeyUtil.SCREEN_TIMEOUT_VALUES_KEY,
                    screenTimeOutArray[numberPicker.value]
                )
                Log.d(
                    "TAG",
                    "onClick: " + numberPicker.value
                )
                binding.appLockDetailsScreenLockInActivityTimeOut.text =
                    screenTimeOutArray[numberPicker.value]
            })
        d.setNegativeButton("Cancel",
            DialogInterface.OnClickListener { dialogInterface, i -> })*/
        val alertDialog: AlertDialog = d.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()
        numberPickerCancel.setOnClickListener {
            alertDialog.cancel()
        }
        numberPickerOk.setOnClickListener {
            IdentityKeyUtil.save(
                this,
                IdentityKeyUtil.SCREEN_TIMEOUT_KEY,
                numberPicker.value.toString()
            )
            IdentityKeyUtil.save(
                this,
                IdentityKeyUtil.SCREEN_TIMEOUT_VALUES_KEY,
                screenTimeOutArray[numberPicker.value]
            )
            Log.d("TAG", "onClick: " + numberPicker.value)
            binding.appLockDetailsScreenLockInActivityTimeOut.text =
                screenTimeOutArray[numberPicker.value]

            //New Line
            val enabled = true

            setScreenLockEnabled(this, enabled)

            val intent = Intent(this, KeyCachingService::class.java)
            intent.action = KeyCachingService.LOCK_TOGGLED_EVENT
            this.startService(intent)
            Log.d("numberPicker Value 0 ", numberPicker.value.toString())
            if(screenTimeOutArray[numberPicker.value] == "None") {
                setScreenLockTimeout(this, 950400)
                Log.d("numberPicker Value 1 ", numberPicker.value.toString())
            } else if (screenTimeOutArray[numberPicker.value] == "30 seconds") {
                Log.d("numberPicker Value 2 ", numberPicker.value.toString())

                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(30000)
                Log.d("numberPicker Value 2i ", timeoutSeconds.toString())
                setScreenLockTimeout(this, timeoutSeconds)
            } else if (screenTimeOutArray[numberPicker.value] == "1 Minute") {
                Log.d("numberPicker Value 3 ", numberPicker.value.toString())

                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(60000)
                setScreenLockTimeout(this, timeoutSeconds)
            } else if (screenTimeOutArray[numberPicker.value] == "2 Minutes") {
                Log.d("numberPicker Value 4 ", numberPicker.value.toString())

                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(120000)
                setScreenLockTimeout(this, timeoutSeconds)
            } else if (screenTimeOutArray[numberPicker.value] == "5 Minutes") {
                Log.d("numberPicker Value 5 ", numberPicker.value.toString())

                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(300000)
                setScreenLockTimeout(this, timeoutSeconds)
            } else if (screenTimeOutArray[numberPicker.value] == "15 Minutes") {
                Log.d("numberPicker Value 6 ", numberPicker.value.toString())

                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(900000)
                setScreenLockTimeout(this, timeoutSeconds)
            } else if (screenTimeOutArray[numberPicker.value] == "30 Minutes") {
                Log.d("numberPicker Value 7 ", numberPicker.value.toString())

                val timeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(1800000)
                setScreenLockTimeout(this, timeoutSeconds)
            }

            // initializeScreenLockTimeoutSummary()

            alertDialog.cancel()
        }
    }

    /*private fun initializeVisibility() {
        if (isPasswordDisabled(this)) {
            val keyguardManager = this.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (!keyguardManager.isKeyguardSecure) {
                (this.findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK) as SwitchPreferenceCompat).isChecked =
                    false
                findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK).setEnabled(false)
            }
        } else {
            findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK).setVisible(false)
            findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK_TIMEOUT).setVisible(false)
        }
    }*/
}