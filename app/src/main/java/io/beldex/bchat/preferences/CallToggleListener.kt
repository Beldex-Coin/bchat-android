package io.beldex.bchat.preferences

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setBooleanPreference
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.setShownCallWarning
import io.beldex.bchat.R
import io.beldex.bchat.permissions.Permissions

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

    @SuppressLint("MissingInflatedId")
    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (!(newValue as Boolean)) return true
        // check if we've shown the info dialog and check for microphone permissions
        if (setShownCallWarning(context.requireContext())) {
            val factory = LayoutInflater.from(context.requireContext())
            val callPermissionDialogView: View= factory.inflate(R.layout.enable_call_permission, null)
            val callPermissionDialog = AlertDialog.Builder(context.requireContext()).create()
            callPermissionDialog.window?.setBackgroundDrawableResource(R.color.transparent)
            callPermissionDialog.setView(callPermissionDialogView)
            val enableButton = callPermissionDialogView.findViewById<Button>(R.id.callPermissionEnableButton)
            val cancelButton = callPermissionDialogView.findViewById<Button>(R.id.callPermissionCancelButton)
            enableButton.setOnClickListener {
                requestMicrophonePermission()
                callPermissionDialog.dismiss()
            }
            cancelButton.setOnClickListener {
                callPermissionDialog.dismiss()
            }
            callPermissionDialog.show()
        } else {
            requestMicrophonePermission()
        }
        return false
    }
}