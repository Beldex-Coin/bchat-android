package io.beldex.bchat.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import com.beldex.libbchat.utilities.ExpirationUtil
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.conversation.v2.dialogs.ClearChatDialog
import io.beldex.bchat.conversation.v2.dialogs.SelectedDeleteMessage
import io.beldex.bchat.conversation.v2.dialogs.UnblockUserDialog
import io.beldex.bchat.database.model.ThreadRecord
import io.beldex.bchat.my_account.ui.dialogs.DeleteChatConfirmationDialog
import io.beldex.bchat.my_account.ui.dialogs.IgnoreRequestDialog
import io.beldex.bchat.my_account.ui.dialogs.LockOptionsDialog
import io.beldex.bchat.my_account.ui.dialogs.RequestBlockConfirmationDialog
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.serializable
import io.beldex.bchat.R

enum class HomeDialogType {
    /*DeleteRequest,
    BlockRequest,*/
    UnblockUser,
    MuteChat,
    BlockUser,
    NotificationSettings,
    DeleteChat,
    IgnoreRequest,
    SelectedMessageDelete,
    DisappearingTimer,
    ClearChat,
    AcceptRequest,
    DeclineRequest
}

class ConversationActionDialog: DialogFragment() {

    private var argument1: String? = null
    private var argument2: String? = null
    private var dismissAllowed: Boolean = false
    var data:Int? = null
    var longData: Long? = null
    private var dialogType: HomeDialogType = HomeDialogType.DeleteChat
    private var listener: ConversationActionDialogListener? = null
    private var threadRecord: ThreadRecord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if(it.containsKey(EXTRA_DIALOG_TYPE))
                dialogType = it.serializable(EXTRA_DIALOG_TYPE) ?: HomeDialogType.DeleteChat

            if(it.containsKey(EXTRA_ARGUMENT_1))
                argument1 = it.getString(EXTRA_ARGUMENT_1, null)

            if(it.containsKey(EXTRA_ARGUMENT_1))
                data = arguments?.getInt(EXTRA_ARGUMENT_1) ?: 0

            if(it.containsKey(EXTRA_ARGUMENT_2))
                argument2 = it.getString(EXTRA_ARGUMENT_2, null)

            if(it.containsKey(EXTRA_ARGUMENT_LONG))
                longData = it.getLong(EXTRA_ARGUMENT_LONG)

            if(it.containsKey(EXTRA_THREAD_RECORD))
                threadRecord = it.serializable(EXTRA_THREAD_RECORD)
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
                    /*HomeDialogType.DeleteRequest -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            RequestBlockConfirmationDialog(
                                message = stringResource(id = R.string.message_requests_block_message),
                                actionTitle = stringResource(id = R.string.yes),
                                onConfirmation = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onDismissRequest = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                }
                            )
                        }
                    }
                    HomeDialogType.BlockRequest -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            RequestBlockConfirmationDialog(
                                message = stringResource(id = R.string.message_requests_decline_messages),
                                actionTitle = stringResource(id = R.string.yes),
                                onConfirmation = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onDismissRequest = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                }
                            )
                        }
                    }*/
                    HomeDialogType.UnblockUser -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            UnblockUserDialog(
                                title = stringResource(id = R.string.unblock_contact),
                                message = stringResource(id = R.string.unblock_user_confirmation),
                                positiveButtonTitle = stringResource(id = R.string.unblock),
                                onAccept = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                }
                            )
                        }
                    }
                    HomeDialogType.MuteChat -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            val timesOption = context.resources.getStringArray(R.array.mute_durations)
                            LockOptionsDialog(
                                title = stringResource(R.string.mute_notification),
                                options = timesOption.toList(),
                                currentValue = timesOption[argument1?.toInt() ?: 0],
                                onDismiss = {
                                    dismiss()
                                },
                                onValueChanged = { _, index ->
                                    listener?.onConfirmationWithData(dialogType, index, threadRecord)
                                    dismiss()
                                }
                            )
                        }
                    }
                    HomeDialogType.BlockUser -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            UnblockUserDialog(
                                title = stringResource(id = R.string.block_contact),
                                message = stringResource(id = R.string.block_user_confirmation),
                                positiveButtonTitle = stringResource(id = R.string.yes),
                                onAccept = {
                                    dismiss()
                                    listener?.onConfirmationWithData(dialogType, data, threadRecord)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                }
                            )
                        }
                    }
                    HomeDialogType.NotificationSettings -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            val option  = context.resources.getStringArray(R.array.notify_types)
                            NotificationSettingDialog(
                                onDismiss = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onClick = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                },
                                options = option.toList(),
                                currentValue = option[data ?: 0],
                                onValueChanged = { _, index ->
                                    listener?.onConfirmationWithData(dialogType, index, threadRecord)
                                    dismiss()
                                }
                            )
                        }
                    }
                    HomeDialogType.DeleteChat -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            DeleteChatConfirmationDialog(
                                message = argument1?:context.getString(R.string.deleted_message),
                                onConfirmation = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onDismissRequest = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                },
                            )
                        }
                    }
                    HomeDialogType.IgnoreRequest -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            threadRecord =  arguments?.serializable(EXTRA_THREAD_RECORD)
                            IgnoreRequestDialog(
                                onBlock = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onDelete = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                },
                                onDismissRequest = {
                                    dismiss()
                                }
                            )
                        }
                    }
                    HomeDialogType.SelectedMessageDelete -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            val messageCount = data ?:0
                            val title = resources.getQuantityString(
                                R.plurals.ConversationFragment_delete_selected_messages,
                                messageCount,
                                messageCount
                            )
                            val message = resources.getQuantityString(
                                R.plurals.ConversationFragment_this_will_permanently_delete_all_n_selected_messages,
                                messageCount,
                                messageCount
                            )
                            SelectedDeleteMessage(
                                title = title,
                                message = message,
                                positiveButtonTitle = stringResource(id = R.string.delete),
                                onAccept = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                }
                            )
                        }
                    }
                    HomeDialogType.DisappearingTimer -> {
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
                                title = stringResource(R.string.disappearing_messages),
                                options = options,
                                currentValue = options[timesOption.indexOf(argument1?.toInt() ?: 0)],
                                onDismiss = {
                                    dismiss()
                                },
                                onValueChanged = { _, index ->
                                    dismiss()
                                    listener?.onConfirmationWithData(dialogType, timesOption[index], threadRecord)
                                }
                            )
                        }
                    }
                    HomeDialogType.ClearChat -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            ClearChatDialog(
                                onAccept = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                }
                            )
                        }
                    }
                    HomeDialogType.AcceptRequest -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            UnblockUserDialog(
                                title = stringResource(id = R.string.accept_request),
                                message = stringResource(id = R.string.message_requests_accept_message),
                                positiveButtonTitle = stringResource(id = R.string.accept),
                                onAccept = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                }
                            )
                        }
                    }
                    HomeDialogType.DeclineRequest -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            UnblockUserDialog(
                                title = stringResource(id = R.string.decline_request),
                                message = stringResource(id = R.string.message_requests_decline_message),
                                positiveButtonTitle = stringResource(id = R.string.decline),
                                onAccept = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    fun setListener(listener: ConversationActionDialogListener) {
        this.listener = listener
    }

    interface ConversationActionDialogListener {
        fun onConfirm(dialogType: HomeDialogType, threadRecord: ThreadRecord?)
        fun onCancel(dialogType: HomeDialogType, threadRecord: ThreadRecord?)
        fun onConfirmationWithData(dialogType: HomeDialogType, data: Any?, threadRecord: ThreadRecord?)
    }

    companion object {
        const val EXTRA_DIALOG_TYPE = "dialog_type"
        const val EXTRA_ARGUMENT_1 = "argument_1"
        const val EXTRA_ARGUMENT_2 = "argument_2"
        const val EXTRA_ARGUMENT_LONG = "argument_long_type"
        const val EXTRA_THREAD_RECORD = "thread_record"
        const val TAG = "ComposeDialogContainer"
    }
}