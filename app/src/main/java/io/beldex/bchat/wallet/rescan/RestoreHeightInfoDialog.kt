package io.beldex.bchat.wallet.rescan

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import io.beldex.bchat.databinding.RestoreHeightInfoDialogBinding

class RestoreHeightInfoDialog: BaseDialog() {
    private lateinit var binding:RestoreHeightInfoDialogBinding


    override fun setContentView(builder: AlertDialog.Builder) {
        binding = RestoreHeightInfoDialogBinding.inflate(LayoutInflater.from(requireContext()))

        with(binding){
            okButton.setOnClickListener {
               /* val urlIntent = Intent(Intent.ACTION_VIEW)
                urlIntent.data = Uri.parse("https://explorer.beldex.io/")
                startActivity(urlIntent)*/
                dismiss()
            }
        }
        builder.setView(binding.root)
    }


}