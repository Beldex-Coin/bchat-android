package io.beldex.bchat.conversation.v2

import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.databinding.ActivityTransactionLoadingBarBinding

class TransactionLoadingBar : BaseDialog() {
    private lateinit var binding: ActivityTransactionLoadingBarBinding

    override fun setContentView(builder: AlertDialog.Builder) {
        binding =
            ActivityTransactionLoadingBarBinding.inflate(LayoutInflater.from(requireContext()))
        binding.transactionProgressText.text = fromHtml("Please <b>don\'t close this window</b> or attend calls or navigate to another app until the transaction gets initiated")
        builder.setView(binding.root)
        isCancelable = false
    }

    private fun fromHtml(source: String?): Spanned? {
        return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
    }
}