package com.thoughtcrimes.securesms.wallet.send

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.databinding.DialogSendFailedBinding


class SendFailedDialog (private val errorString :String
) : BaseDialog() {
    private lateinit var binding: DialogSendFailedBinding


    override fun setContentView(builder: AlertDialog.Builder) {
        binding = DialogSendFailedBinding.inflate(LayoutInflater.from(requireContext()))

        with(binding){
            errorStringValue.text = errorString
            okButton.setOnClickListener {
                dismiss()
            }
        }

        builder.setView(binding.root)
        builder.setCancelable(false)
    }
}
