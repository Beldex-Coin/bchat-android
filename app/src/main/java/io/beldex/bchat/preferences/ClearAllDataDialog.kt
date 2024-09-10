package io.beldex.bchat.preferences

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import io.beldex.bchat.R
import io.beldex.bchat.databinding.DialogClearAllDataBinding
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.conversation.v2.utilities.BaseDialog
import java.io.File
import io.beldex.bchat.util.Helper
import kotlinx.coroutines.launch

class ClearAllDataDialog : BaseDialog() {
    private lateinit var binding: DialogClearAllDataBinding

    enum class Steps {
        INFO_PROMPT_DEFAULT,
        INFO_PROMPT,
        NETWORK_PROMPT,
        DELETING
    }

    private var clearJob: Job? = null
        set(value) {
            field = value
        }

    private var step = Steps.INFO_PROMPT_DEFAULT
        set(value) {
            field = value
            updateUI()
        }

    //Important
/*    override fun setContentView(builder: AlertDialog.Builder) {
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
        val device = RadioOption(
            "deviceOnly",
            requireContext().getString(R.string.dialog_clear_all_data_clear_device_only_title),
            requireContext().getString(R.string.dialog_clear_all_data_clear_device_only_subtitle)
        )
        val network = RadioOption(
            "deviceAndNetwork",
            requireContext().getString(R.string.dialog_clear_all_data_clear_device_and_network_title),
            requireContext().getString(R.string.dialog_clear_all_data_clear_device_and_network_subtitle)
        )
        var selectedOption = device
        val optionAdapter = RadioOptionAdapter { selectedOption = it }
        binding.recyclerView.apply {
            itemAnimator = null
            adapter = optionAdapter
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
            setHasFixedSize(true)
        }
        optionAdapter.submitList(listOf(device, network))
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        binding.clearAllDataButton.setOnClickListener {
            when (step) {
                Steps.INFO_PROMPT_DEFAULT -> step = if (selectedOption == network) {
                    Steps.NETWORK_PROMPT
                } else {
                    Steps.INFO_PROMPT
                }

                Steps.INFO_PROMPT -> clearAllData(false)
                Steps.NETWORK_PROMPT -> clearAllData(true)
                Steps.DELETING -> { /* do nothing intentionally */
                }
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
                    binding.recyclerView.visibility = View.GONE
                    binding.dialogDescriptionText.visibility = View.VISIBLE
                    binding.clearAllDataButton.setText(R.string.clear)
                    binding.dialogDescriptionText.setText(R.string.dialog_clear_all_data_explanation)
                }

                Steps.NETWORK_PROMPT -> {
                    binding.recyclerView.visibility = View.GONE
                    binding.dialogDescriptionText.visibility = View.VISIBLE
                    binding.clearAllDataTitle.setText(R.string.dialog_clear_all_data_clear_device_and_network_title)
                    binding.dialogDescriptionText.setText(R.string.dialog_clear_all_data_network_explanation)
                }

                Steps.DELETING -> { /* do nothing intentionally */
                }

                else -> {
                    /* do nothing intentionally */
                }
            }

            binding.cancelButton.isVisible = !isLoading
            binding.clearAllDataButton.isVisible = !isLoading
            binding.progressBar.isVisible = isLoading

            it.setCanceledOnTouchOutside(!isLoading)
            isCancelable = !isLoading
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