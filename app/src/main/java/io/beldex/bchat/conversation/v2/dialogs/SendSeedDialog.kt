package io.beldex.bchat.conversation.v2.dialogs

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import io.beldex.bchat.databinding.DialogSendSeedBinding
import io.beldex.bchat.conversation.v2.utilities.BaseDialog

/** Shown if the user is about to send their recovery phrase to someone. */
class SendSeedDialog(private val proceed: (() -> Unit)? = null) : BaseDialog() {

    override fun setContentView(builder: AlertDialog.Builder) {
        val binding = DialogSendSeedBinding.inflate(LayoutInflater.from(requireContext()))
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.sendSeedButton.setOnClickListener { send() }
        builder.setView(binding.root)
    }

    private fun send() {
        proceed?.invoke()
        dismiss()
    }
}