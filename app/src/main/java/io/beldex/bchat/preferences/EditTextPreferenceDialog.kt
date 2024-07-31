package io.beldex.bchat.preferences

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.R
import io.beldex.bchat.databinding.DialogEditTextPreferenceBinding

class EditTextPreferenceDialog(
        private val preference : Preference
) : BaseDialog() {
    private lateinit var binding : DialogEditTextPreferenceBinding

    override fun setContentView(builder : AlertDialog.Builder) {
        binding=DialogEditTextPreferenceBinding.inflate(LayoutInflater.from(requireContext()))
        binding.edit.setText(TextSecurePreferences.getThreadTrimLength(requireContext()).toString())
        binding.okButton.setOnClickListener {
            onPreferenceChange(preference, binding.edit.text.toString())
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        builder.setView(binding.root)
        builder.setCancelable(false)
    }

    private fun onPreferenceChange(preference : Preference, newValue : Any?) : Boolean {
        if (newValue == null || (newValue as String).trim { it <= ' ' }.isEmpty()) {
            return false
        }
        val value : Int=try {
            newValue.toInt()
        } catch (nfe : NumberFormatException) {
            Log.w(tag, nfe)
            return false
        }
        if (value < 1) {
            return false
        }
        preference.summary=resources.getQuantityString(R.plurals.ApplicationPreferencesActivity_messages_per_conversation, value, value)
        context?.let { TextSecurePreferences.setThreadTrimLength(it, value.toString()) }
        return true
    }
}

