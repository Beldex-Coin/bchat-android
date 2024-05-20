package com.thoughtcrimes.securesms.compose_utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import com.beldex.libbchat.utilities.ExpirationUtil
import com.thoughtcrimes.securesms.conversation.v2.dialogs.ClearChatDialog
import com.thoughtcrimes.securesms.conversation.v2.dialogs.UnblockUserDialog
import com.thoughtcrimes.securesms.my_account.ui.dialogs.IgnoreRequestDialog
import com.thoughtcrimes.securesms.my_account.ui.dialogs.LockOptionsDialog
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
    BlockRequest,
    ClearChat,
    UnblockUser,
    DisappearingTimer,
    MuteChat,
    BlockUser
}

class ComposeDialogContainer(
    private val dialogType: DialogType,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit,
    private val onConfirmWithData: (Any?) -> Unit = {}
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
                    DialogType.ClearChat -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            ClearChatDialog(
                                onAccept = {
                                    dismiss()
                                    onConfirm()
                                },
                                onCancel = {
                                    dismiss()
                                    onCancel()
                                }
                            )
                        }
                    }
                    DialogType.UnblockUser -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            UnblockUserDialog(
                                title = stringResource(id = R.string.unblock_contact),
                                message = stringResource(id = R.string.unblock_user_confirmation),
                                positiveButtonTitle = stringResource(id = R.string.unblock),
                                onAccept = {
                                    dismiss()
                                    onConfirm()
                                },
                                onCancel = {
                                    dismiss()
                                    onCancel()
                                }
                            )
                        }
                    }
                    DialogType.DisappearingTimer -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            val timesOption = context.resources.getIntArray(R.array.expiration_times)
                            val options = remember {
                                timesOption.map {
                                    ExpirationUtil.getExpirationDisplayValue(
                                        context,
                                        it
                                    )
                                }
                            }
                            LockOptionsDialog(
                                title = stringResource(R.string.conversation_expiring_off__disappearing_messages),
                                options = options,
                                currentValue = options[timesOption.indexOf(argument1?.toInt() ?: 0)],
                                onDismiss = {
                                    dismiss()
                                },
                                onValueChanged = { _, index ->
                                    dismiss()
                                    onConfirmWithData(timesOption[index])
                                }
                            )
                        }
                    }
                    DialogType.MuteChat -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            val timesOption = context.resources.getStringArray(R.array.mute_durations)
                            LockOptionsDialog(
                                title = stringResource(R.string.conversation_unmuted__mute_notifications),
                                options = timesOption.toList(),
                                currentValue = argument1 ?: "0",
                                onDismiss = {
                                    dismiss()
                                },
                                onValueChanged = { _, index ->
                                    dismiss()
                                    onConfirmWithData(timesOption[index])
                                }
                            )
                        }
                    }
                    DialogType.BlockUser -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            UnblockUserDialog(
                                title = stringResource(id = R.string.block_contact),
                                message = stringResource(id = R.string.block_user_confirmation),
                                positiveButtonTitle = stringResource(id = R.string.yes),
                                onAccept = {
                                    dismiss()
                                    onConfirm()
                                },
                                onCancel = {
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