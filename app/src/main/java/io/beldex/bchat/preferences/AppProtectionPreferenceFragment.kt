package io.beldex.bchat.preferences

import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import android.util.Log
import com.beldex.libbchat.mnode.OnionRequestAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getOnionRequestPathCount
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.getScreenLockTimeout
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.isPasswordDisabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setScreenLockEnabled
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setOnionRequestPathCount
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setOnionRoutingEnabled
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.BuildConfig
import io.beldex.bchat.R
import io.beldex.bchat.components.SwitchPreferenceCompat
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.service.KeyCachingService
import io.beldex.bchat.util.CallNotificationBuilder.Companion.areNotificationsEnabled
import io.beldex.bchat.util.IntentUtils
import java.util.concurrent.TimeUnit

class AppProtectionPreferenceFragment : ListSummaryPreferenceFragment() {

    private var callToggleListener: CallToggleListener? = null

    override fun onCreate(paramBundle: Bundle?) {
        super.onCreate(paramBundle)
        findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK)!!.onPreferenceChangeListener =
            ScreenLockListener()
        findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK_TIMEOUT)!!.onPreferenceClickListener =
            ScreenLockTimeoutListener()
        findPreference<Preference>(TextSecurePreferences.READ_RECEIPTS_PREF)!!.onPreferenceChangeListener =
            ReadReceiptToggleListener()
        findPreference<Preference>(TextSecurePreferences.TYPING_INDICATORS)!!.onPreferenceChangeListener =
            TypingIndicatorsToggleListener()
        findPreference<Preference>(TextSecurePreferences.LINK_PREVIEWS)!!.onPreferenceChangeListener =
            LinkPreviewToggleListener()

        findPreference<Preference>(TextSecurePreferences.USE_ONION_ROUTING)!!.onPreferenceClickListener =
            OnionRoutingClickListener()

        updateOnionRoutingSummary()

        //New Line
        callToggleListener = CallToggleListener(this) { setCall(it) }
        findPreference<Preference>(TextSecurePreferences.CALL_NOTIFICATIONS_ENABLED)!!.onPreferenceChangeListener =
            callToggleListener
        initializeVisibility()
    }

    //New Line
    private fun setCall(isEnabled: Boolean): Void? {
        /*if(isEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                fullScreenIntentPopup()
            }
        }*/
        (findPreference<Preference>(TextSecurePreferences.CALL_NOTIFICATIONS_ENABLED) as SwitchPreferenceCompat?)!!.isChecked =
            isEnabled
        if (isEnabled && !areNotificationsEnabled(requireActivity())) {
            // show a dialog saying that calls won't work properly if you don't have notifications on at a system level
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.activity_settings_notifications_button_title)
                .setMessage(R.string.CallNotificationBuilder_system_notification_message)
                .setPositiveButton(R.string.activity_settings_notifications_button_title) { d, w ->
                    val settingsIntent =
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(
                                Settings.EXTRA_APP_PACKAGE,
                                BuildConfig.APPLICATION_ID
                            )
                    if (IntentUtils.isResolvable(requireContext(), settingsIntent)) {
                        startActivity(settingsIntent)
                    }
                    d.dismiss()
                }
                .setNeutralButton(R.string.dismiss) { d, w ->
                    // do nothing, user might have broken notifications
                    d.dismiss()
                }
                .show()
        }
        return null
    }
   /* private fun fullScreenIntentPopup() {
        val factory=LayoutInflater.from(context)
        val fullScreenIntentDialogView : View=factory.inflate(R.layout.full_screen_intent_dialog, null)
        val fullScreenIntentDialog=AlertDialog.Builder(context).create()
        fullScreenIntentDialog.window?.setBackgroundDrawableResource(R.color.transparent)
        fullScreenIntentDialog.setView(fullScreenIntentDialogView)
        val enableButton=fullScreenIntentDialogView.findViewById<Button>(R.id.fullScreenIntentEnableButton)
        val cancelButton=fullScreenIntentDialogView.findViewById<Button>(R.id.fullScreenIntentCancelButton)
        enableButton.setOnClickListener {
            actionFullScreenIntent()
            fullScreenIntentDialog.dismiss()
        }
        cancelButton.setOnClickListener {
            fullScreenIntentDialog.dismiss()
        }
        fullScreenIntentDialog.show()
    }*/


    //Hales63
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_app_protection)
    }

    override fun onResume() {
        super.onResume()
        if (isPasswordDisabled(requireContext())) {
            initializeScreenLockTimeoutSummary()
        }
        updateOnionRoutingSummary()
        callToggleListener?.reattachCallbackIfNeeded()
    }

    private fun updateOnionRoutingSummary() {
        // The summary line is a fixed description (see preferences_app_protection.xml); the
        // selected hop option ("Off" / "1 hop" / "3 hops") is shown on the right hand side.
        val selectedHopCount = getOnionRequestPathCount(requireContext())
        findPreference<OnionRoutingPreference>(TextSecurePreferences.USE_ONION_ROUTING)
            ?.setHopCount(selectedHopCount)
    }

    private fun initializeScreenLockTimeoutSummary() {
        val timeoutSeconds = getScreenLockTimeout(requireContext())
        val hours = TimeUnit.SECONDS.toHours(timeoutSeconds)
        val minutes =
            TimeUnit.SECONDS.toMinutes(timeoutSeconds) - TimeUnit.SECONDS.toHours(timeoutSeconds) * 60
        val seconds =
            TimeUnit.SECONDS.toSeconds(timeoutSeconds) - TimeUnit.SECONDS.toMinutes(timeoutSeconds) * 60
        findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK_TIMEOUT)?.summary = if (timeoutSeconds <= 0) getString(R.string.AppProtectionPreferenceFragment_none) else String.format(
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    }

    private fun initializeVisibility() {
        if (isPasswordDisabled(requireContext())) {
            val keyguardManager =
                requireContext().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (!keyguardManager.isKeyguardSecure) {
                (findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK) as SwitchPreferenceCompat?)!!.isChecked =
                    false
                findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK)!!.isEnabled =
                    false
            }
        } else {
            findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK)!!.isVisible =
                false
            findPreference<Preference>(TextSecurePreferences.SCREEN_LOCK_TIMEOUT)!!.isVisible =
                false
        }
    }

    private inner class ScreenLockListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            val enabled = newValue as Boolean
            setScreenLockEnabled(context!!, enabled)
            val intent = Intent(context, KeyCachingService::class.java)
            intent.action = KeyCachingService.LOCK_TOGGLED_EVENT
            context!!.startService(intent)
            return true
        }
    }

    private inner class ScreenLockTimeoutListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
           /* TimeDurationPickerDialog(context, { view: TimeDurationPicker?, duration: Long ->
                if (duration == 0L) {
                    setScreenLockTimeout(context!!, 0)
                } else {
                    val timeoutSeconds =
                        TimeUnit.MILLISECONDS.toSeconds(duration)
                    setScreenLockTimeout(context!!, timeoutSeconds)
                }
                initializeScreenLockTimeoutSummary()
            }, 0).show()*/
            return true
        }
    }

    private inner class ReadReceiptToggleListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            return true
        }
    }

    private inner class TypingIndicatorsToggleListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            val enabled = newValue as Boolean
            if (!enabled) {
                ApplicationContext.getInstance(requireContext()).typingStatusRepository.clear()
            }
            return true
        }
    }

    private inner class LinkPreviewToggleListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            return true
        }
    }

    private inner class OnionRoutingClickListener : Preference.OnPreferenceClickListener {
        override fun onPreferenceClick(preference: Preference): Boolean {
            // Opens the hop-selection popup (0 hop / 1 hop / 3 hops) instead of using a toggle
            OnionRoutingHopSelectionDialogFragment(
                currentHopCount = getOnionRequestPathCount(requireContext()),
                onCancel = {},
                onConfirm = { selectedHopCount ->
                    Log.d("ONION_HOP", "User selected onion routing with $selectedHopCount hop(s)")
                    // 1) Persist the user-selected hop count (0 = off, 1 = one hop, 3 = three hops)
                    setOnionRequestPathCount(requireContext(), selectedHopCount)
                    if (selectedHopCount == 0) {
                        // 0-hop: route directly to the mnode (no onion layers)
                        setOnionRoutingEnabled(requireContext(), false)
                    } else {
                        // 1 or 3 hops: enable onion routing and apply immediately; this sets
                        // pathSize, wipes stale cached paths/guard mnodes, and proactively
                        // rebuilds so the status light and hops screen refresh right away.
                        setOnionRoutingEnabled(requireContext(), true)
                        OnionRequestAPI.setOnionRequestPathCount(selectedHopCount)
                        OnionRequestAPI.rebuildPathsIfNeeded()
                    }
                    updateOnionRoutingSummary()
                }
            ).show(childFragmentManager, "OnionRoutingHopSelection")
            return true
        }
    }
}
