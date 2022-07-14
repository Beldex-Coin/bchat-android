package com.thoughtcrimes.securesms.preferences

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import io.beldex.bchat.R
import io.beldex.bchat.databinding.DialogClearAllDataBinding
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Log
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.util.ConfigurationMessageUtilities
import com.thoughtcrimes.securesms.conversation.v2.utilities.BaseDialog
import java.io.File
import com.thoughtcrimes.securesms.util.Helper
import kotlinx.coroutines.launch

class ClearAllDataDialog : BaseDialog() {
    private lateinit var binding: DialogClearAllDataBinding

    enum class Steps {
        INFO_PROMPT,
        NETWORK_PROMPT,
        DELETING
    }

    var clearJob: Job? = null
        set(value) {
            field = value
        }

    var step = Steps.INFO_PROMPT
        set(value) {
            field = value
            updateUI()
        }

    //Important
   /* override fun setContentView(builder: AlertDialog.Builder) {
        binding = DialogClearAllDataBinding.inflate(LayoutInflater.from(requireContext()))
        binding.cancelButton.setOnClickListener {
            if (step == Steps.NETWORK_PROMPT) {
                clearAllData(false)
            } else if (step != Steps.DELETING) {
                dismiss()
            }
        }
        binding.clearAllDataButton.setOnClickListener {
            when(step) {
                Steps.INFO_PROMPT -> step = Steps.NETWORK_PROMPT
                Steps.NETWORK_PROMPT -> {
                    clearAllData(true)
                }
                Steps.DELETING -> { *//* do nothing intentionally *//* }
            }
        }
        builder.setView(binding.root)
        builder.setCancelable(false)
    }*/

    override fun setContentView(builder: AlertDialog.Builder) {
        binding = DialogClearAllDataBinding.inflate(LayoutInflater.from(requireContext()))
        binding.cancelButton.setOnClickListener {
            if (step == Steps.NETWORK_PROMPT) {
                //clearAllData(false)
                dismiss()
            } else if (step != Steps.DELETING) {
                dismiss()
            }
        }
        binding.clearAllDataButton.setOnClickListener {
            when(step) {
                Steps.INFO_PROMPT -> step = Steps.NETWORK_PROMPT
                Steps.NETWORK_PROMPT -> {
                    clearAllData(false)
                    //clearAllData(true)
                }
                Steps.DELETING -> { /* do nothing intentionally */ }
            }
        }
        builder.setView(binding.root)
        builder.setCancelable(false)
    }

    private fun updateUI() {
        dialog?.let {
            val isLoading = step == Steps.DELETING

            when (step) {
                Steps.INFO_PROMPT -> {
                    binding.dialogDescriptionText.setText(R.string.dialog_clear_all_data_explanation)
                    binding.cancelButton.setText(R.string.cancel)
                    binding.clearAllDataButton.setText(R.string.delete)
                }
                else -> {
                    binding.dialogDescriptionText.setText(R.string.dialog_clear_all_data_network_explanation)
                   /* binding.cancelButton.setText(R.string.dialog_clear_all_data_local_only)
                    binding.clearAllDataButton.setText(R.string.dialog_clear_all_data_clear_network)*/
                    binding.cancelButton.setText(R.string.cancel)
                    binding.clearAllDataButton.setText(R.string.ok)
                }
            }

            binding.cancelButton.isVisible = !isLoading
            binding.clearAllDataButton.isVisible = !isLoading
            binding.progressBar.isVisible = isLoading

            it.setCanceledOnTouchOutside(!isLoading)
            isCancelable = !isLoading
        }
    }

    private fun removeWallet(){
        val walletFolder: File = Helper.getWalletRoot(context)
        val walletName = TextSecurePreferences.getWalletName(requireContext())
        val walletFile = File(walletFolder, walletName!!)
        val walletKeys =File(walletFolder, "$walletName.keys")
        val walletAddress = File(walletFolder,"$walletName.address.txt")
        if(walletFile.exists()) {
            walletFile.delete() // when recovering wallets, the cache seems corrupt - so remove it
        }
        if(walletKeys.exists()) {
            walletKeys.delete()
        }
        if(walletAddress.exists()) {
            walletAddress.delete()
        }
    }

    private fun clearAllData(deleteNetworkMessages: Boolean) {
        clearJob = lifecycleScope.launch(Dispatchers.IO) {
            val previousStep = step
            withContext(Dispatchers.Main) {
                step = Steps.DELETING
            }

            if (!deleteNetworkMessages) {
                try {
                    ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(requireContext()).get()
                } catch (e: Exception) {
                    Log.e("Beldex", "Failed to force sync", e)
                }

                //New Line
                removeWallet()

                ApplicationContext.getInstance(context).clearAllData(false)
                withContext(Dispatchers.Main) {
                    dismiss()
                }
            } else {
                // finish
                val result = try {
                    MnodeAPI.deleteAllMessages().get()
                } catch (e: Exception) {
                    null
                }

                if (result == null || result.values.any { !it } || result.isEmpty()) {
                    // didn't succeed (at least one)
                    withContext(Dispatchers.Main) {
                        step = previousStep
                    }
                } else if (result.values.all { it }) {
                    //New Line
                    removeWallet()
                    // don't force sync because all the messages are deleted?
                    ApplicationContext.getInstance(context).clearAllData(false)
                        withContext(Dispatchers.Main) {
                        dismiss()
                    }
                }
            }
        }
    }
}