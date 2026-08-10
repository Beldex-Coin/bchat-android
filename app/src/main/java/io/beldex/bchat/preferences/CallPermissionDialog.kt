package io.beldex.bchat.preferences

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment
import io.beldex.bchat.R

class CallPermissionDialog : DialogFragment() {

    var onEnableClick: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("MissingInflatedId")
        val view: View = LayoutInflater.from(requireContext()).inflate(R.layout.enable_call_permission, null)
        val enableButton = view.findViewById<Button>(R.id.callPermissionEnableButton)
        val cancelButton = view.findViewById<Button>(R.id.callPermissionCancelButton)
        enableButton.setOnClickListener {
            onEnableClick?.invoke()
            dismiss()
        }
        cancelButton.setOnClickListener {
            dismiss()
        }
        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
            .apply {
                window?.setBackgroundDrawableResource(R.color.transparent)
            }
    }

    companion object {
        const val TAG = "CallPermissionDialog"
    }
}
