package io.beldex.bchat.wallet.send

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.data.TxData
import io.beldex.bchat.model.PendingTransaction
import io.beldex.bchat.model.Wallet
import io.beldex.bchat.R
import io.beldex.bchat.databinding.DialogSendConfirmBinding

class SendConfirmDialog(
    private val pendingTransaction: PendingTransaction,
    private val txData: TxData?,
    private val context: SendFragment
) : BaseDialog() {
    private lateinit var binding:DialogSendConfirmBinding


    override fun setContentView(builder: AlertDialog.Builder) {
        binding = DialogSendConfirmBinding.inflate(LayoutInflater.from(requireContext()))

        with(binding){
            if (txData != null) {
                dialogReceiverAddress.text = txData.destinationAddress
            }
            dialogTransactionFee.text = Wallet.getDisplayAmount(pendingTransaction.fee)
            dialogTransactionAmount.text = Wallet.getDisplayAmount(pendingTransaction.amount)

            cancelButton.setOnClickListener {
                context.sendButtonEnabled()
                context.pendingTransaction = null
                context.pendingTx = null
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