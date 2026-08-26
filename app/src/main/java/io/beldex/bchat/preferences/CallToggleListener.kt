package io.beldex.bchat.preferences

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setBooleanPreference
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setShownCallWarning
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.R

internal class CallToggleListener(
    private val context: Fragment,
    private val setCallback: (Boolean) -> Unit
) : Preference.OnPreferenceChangeListener {

    private fun requestMicrophonePermission() {
        Permissions.with(context)
            .request(Manifest.permission.RECORD_AUDIO)
            .onAllGranted {
                setBooleanPreference(
                    context.requireContext(),
                    TextSecurePreferences.CALL_NOTIFICATIONS_ENABLED,
                    true
                )
                setCallback.invoke(true)
            }
            .onAnyDenied { setCallback.invoke(false) }
            .execute()
    }

    fun reattachCallbackIfNeeded() {
        val dialog = context.childFragmentManager
            .findFragmentByTag(CallPermissionDialog.TAG) as? CallPermissionDialog
        if (dialog != null && dialog.onEnableClick == null) {
            dialog.onEnableClick = { requestMicrophonePermission() }
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (!(newValue as Boolean)) return true
        // check if we've shown the info dialog and check for microphone permissions
        if (setShownCallWarning(context.requireContext())) {
            val callPermissionDialog = CallPermissionDialog()
            callPermissionDialog.onEnableClick = { requestMicrophonePermission() }
            callPermissionDialog.show(context.childFragmentManager, CallPermissionDialog.TAG)
        } else {
            requestMicrophonePermission()
        }
        return false
    }
}