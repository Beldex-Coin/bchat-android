package com.thoughtcrimes.securesms.conversation.v2

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.databinding.ActivityInChatSendSuccessBinding

class InChatSendSuccess(
    private val context: ConversationFragmentV2
) : BaseDialog() {
    private lateinit var binding: ActivityInChatSendSuccessBinding


    override fun setContentView(builder: AlertDialog.Builder) {
        binding = ActivityInChatSendSuccessBinding.inflate(LayoutInflater.from(requireContext()))

        with(binding) {
            okButton.setOnClickListener {
                context.pendingTransaction = null
                context.pendingTx = null
                context.transactionInProgress = false
                dismiss()
            }
        }

        builder.setView(binding.root)
        isCancelable = false
    }
}