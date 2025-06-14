package io.beldex.bchat.compose_utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.conversation.v2.dialogs.LeaveGroupDialog
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.home.NotificationSettingDialog
import io.beldex.bchat.my_account.ui.dialogs.DeleteChatConfirmationDialog
import io.beldex.bchat.my_account.ui.dialogs.PermissionSettingDialog
import io.beldex.bchat.my_account.ui.dialogs.ProfilePicturePopup
import io.beldex.bchat.my_account.ui.dialogs.WalletSyncingDialog
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.R

enum class DialogType {
    PermissionDialog,
    UploadProfile,
    WalletSyncing,
    DeleteChat,
    LeaveGroup,
    NotificationSettings,
    Settings
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
    var data:Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if(it.containsKey(EXTRA_ARGUMENT_1))
                argument1 = it.getString(EXTRA_ARGUMENT_1, null)

            if(it.containsKey(EXTRA_ARGUMENT_1))
                data = arguments?.getInt(EXTRA_ARGUMENT_1) ?: 0

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
                                message = argument1!!,
                                onDismissRequest = {
                                    dismiss()
                                },
                                gotoSettings = {
                                    dismiss()
                                    onConfirm()
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
                    DialogType.WalletSyncing -> {
                        BChatTheme(
                                darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            WalletSyncingDialog(
                                    onDismissRequest = {
                                        dismiss()
                                    },
                                    exit = {
                                        onConfirm()
                                        dismiss()
                                    }
                            )
                        }
                    }
                    DialogType.DeleteChat -> {
                        BChatTheme(
                                darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            DeleteChatConfirmationDialog(
                                    message = argument1?:context.getString(R.string.deleted_message),
                                    onConfirmation = {
                                        dismiss()
                                        onConfirm()
                                    },
                                    onDismissRequest = {
                                        dismiss()
                                        onCancel()
                                    },
                            )
                        }
                    }
                    DialogType.LeaveGroup -> {
                        val address = argument1?:""
                        val group = DatabaseComponent.get(context).groupDatabase().getGroup(address).orNull()
                        val admins = group.admins
                        val bchatID = TextSecurePreferences.getLocalNumber(context)
                        val isCurrentUserAdmin = admins.any { it.toString() == bchatID }
                        val message = if (isCurrentUserAdmin) {
                            "Because you are the creator of this group it will be deleted for everyone. This cannot be undone."
                        } else {
                            context.resources.getString(R.string.ConversationActivity_are_you_sure_you_want_to_leave_this_group)
                        }
                        BChatTheme(
                                darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            LeaveGroupDialog(
                                    title = stringResource(id = R.string.ConversationActivity_leave_group),
                                    message = message,
                                    positiveButtonTitle = stringResource(id = R.string.leave),
                                    onLeave = {
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
                    DialogType.NotificationSettings -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            val option  = context.resources.getStringArray(R.array.notify_types)
                            NotificationSettingDialog(
                                onDismiss = {
                                    dismiss()
                                    onConfirm()
                                },
                                onClick = {
                                    dismiss()
                                    onCancel()
                                },
                                options = option.toList(),
                                currentValue = option[data ?: 0],
                                onValueChanged = { _, index ->
                                    onConfirmWithData(index)
                                    dismiss()
                                }
                            )
                        }
                    }

                    else -> {}
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