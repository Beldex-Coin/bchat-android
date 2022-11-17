package com.thoughtcrimes.securesms.wallet.send

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.databinding.DialogSendSuccessBinding

class SendSuccessDialog(
    private val context: SendFragment
) : BaseDialog() {
    private lateinit var binding: DialogSendSuccessBinding


    override fun setContentView(builder: AlertDialog.Builder) {
        binding = DialogSendSuccessBinding.inflate(LayoutInflater.from(requireContext()))

        with(binding){
            okButton.setOnClickListener {
                context.transactionFinished()
                dismiss()
            }
        }

        builder.setView(binding.root)
        builder.setCancelable(false)
    }
}