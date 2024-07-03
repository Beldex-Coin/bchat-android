package com.thoughtcrimes.securesms.preferences

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.databinding.DialogListPreferenceBinding

class ListPreferenceDialog(
    private val listPreference: ListPreference,
    private val dialogListener: () -> Unit
) : BaseDialog() {
    private lateinit var binding: DialogListPreferenceBinding

    override fun setContentView(builder: AlertDialog.Builder) {
        binding = DialogListPreferenceBinding.inflate(LayoutInflater.from(requireContext()))
        binding.messageTextView.text = listPreference.dialogMessage
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        val options = listPreference.entryValues.zip(listPreference.entries) { value, title ->
            NotificationContentRadioOption(value.toString(), title.toString())
        }
        val valueIndex = listPreference.findIndexOfValue(listPreference.value)
        val optionAdapter = NotificationContentRadioOptionAdapter(valueIndex) {
            listPreference.value = it.value
            dismiss()
            dialogListener.invoke()
        }
        binding.recyclerView.apply {
            adapter = optionAdapter
            setHasFixedSize(true)
        }
        optionAdapter.submitList(options)
        builder.setView(binding.root)
        builder.setCancelable(false)
    }

}