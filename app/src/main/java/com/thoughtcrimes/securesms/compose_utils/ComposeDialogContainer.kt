package com.thoughtcrimes.securesms.compose_utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import com.thoughtcrimes.securesms.my_account.ui.dialogs.IgnoreRequestDialog
import com.thoughtcrimes.securesms.my_account.ui.dialogs.PermissionSettingDialog
import com.thoughtcrimes.securesms.my_account.ui.dialogs.ProfilePicturePopup
import com.thoughtcrimes.securesms.my_account.ui.dialogs.RequestBlockConfirmationDialog
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import io.beldex.bchat.R

enum class DialogType {
    PermissionDialog,
    UploadProfile,
    IgnoreRequest,
    DeleteRequest,
    BlockRequest
}

class ComposeDialogContainer(
    private val dialogType: DialogType,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit
): DialogFragment() {

    private var argument1: String? = null
    private var argument2: String? = null
    private var dismissAllowed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if(it.containsKey(EXTRA_ARGUMENT_1))
                argument1 = it.getString(EXTRA_ARGUMENT_1, null)

            if(it.containsKey(EXTRA_ARGUMENT_2))
                argument2 = it.getString(EXTRA_ARGUMENT_2, null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                when (dialogType) {
                    DialogType.PermissionDialog -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            PermissionSettingDialog(
                                message = "",
                                onDismissRequest = {

                                },
                                gotoSettings = {

                                }
                            )
                        }
                    }
                    DialogType.UploadProfile -> {
                        argument1 ?: return@setContent
                        argument2 ?: return@setContent
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            ProfilePicturePopup(
                                publicKey = argument1!!,
                                displayName = argument2!!,
                                onDismissRequest = {
                                    if (dismissAllowed)
                                        dismiss()
                                },
                                closePopUP = {
                                    dismiss()
                                },
                                removePicture = {
                                    onCancel()
                                    dismiss()
                                },
                                uploadPicture = {
                                    onConfirm()
                                    dismiss()
                                }
                            )
                        }
                    }
                    DialogType.IgnoreRequest -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            IgnoreRequestDialog(
                                onBlock = {
                                    dismiss()
                                    onConfirm()
                                },
                                onDelete = {
                                    dismiss()
                                    onCancel()
                                },
                                onDismissRequest = {
                                    dismiss()
                                }
                            )
                        }
                    }
                    DialogType.DeleteRequest -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            RequestBlockConfirmationDialog(
                                message = stringResource(id = R.string.message_requests_block_message),
                                actionTitle = stringResource(id = R.string.yes),
                                onConfirmation = {
                                    dismiss()
                                    onConfirm()
                                },
                                onDismissRequest = {
                                    dismiss()
                                    onCancel()
                                }
                            )
                        }
                    }
                    DialogType.BlockRequest -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            RequestBlockConfirmationDialog(
                                message = stringResource(id = R.string.message_requests_decline_messages),
                                actionTitle = stringResource(id = R.string.yes),
                                onConfirmation = {
                                    dismiss()
                                    onConfirm()
                                },
                                onDismissRequest = {
                                    dismiss()
                                    onCancel()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ARGUMENT_1 = "argument_1"
        const val EXTRA_ARGUMENT_2 = "argument_2"
        const val TAG = "ComposeDialogContainer"
    }

}