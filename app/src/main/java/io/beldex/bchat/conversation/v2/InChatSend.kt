package io.beldex.bchat.conversation.v2

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.data.TxData
import io.beldex.bchat.model.PendingTransaction
import io.beldex.bchat.databinding.ActivityInChatSendBinding

class InChatSend(
    private val pendingTransaction: PendingTransaction,
    private val txData: TxData?,
    private val context: ConversationFragmentV2
) : BaseDialog() {

    private lateinit var binding: ActivityInChatSendBinding
    override fun setContentView(builder: AlertDialog.Builder) {
        binding = ActivityInChatSendBinding.inflate(LayoutInflater.from(requireContext()))

        with(binding) {
            if (txData != null) {
                dialogReceiverAddress.text = txData.destinationAddress
            }
            dialogTransactionFee.text =
                io.beldex.bchat.model.Wallet.getDisplayAmount(pendingTransaction.fee)
            dialogTransactionAmount.text =
                io.beldex.bchat.model.Wallet.getDisplayAmount(pendingTransaction.amount)

            cancelButton.setOnClickListener {
                context.pendingTransaction = null
                context.pendingTx = null
                context.transactionInProgress = false
                dismiss()
            }
            okButton.setOnClickListener {
                context.send()
                dismiss()
            }
            /* tvTxTotal.setText(
                 Wallet.getDisplayAmount(
                     pendingTransaction!!.fee + pendingTransaction!!.amount
                 )
             )*/
        }

        builder.setView(binding.root)
        isCancelable = false
    }
}