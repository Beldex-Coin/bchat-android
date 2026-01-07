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
    DeclineRequest,
    GifSetting
}

class ConversationActionDialog: DialogFragment() {

    private var argument1: String? = null
    private var argument2: String? = null
    private var argument3: Int = 0
    private var dismissAllowed: Boolean = false
    var longData: Long? = null
    private var dialogType: HomeDialogType = HomeDialogType.DeleteChat
    private var listener: ConversationActionDialogListener? = null
    private var threadRecord: ThreadRecord? = null
    private var threadPosition: Int=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if(it.containsKey(EXTRA_DIALOG_TYPE))
                dialogType = it.serializable(EXTRA_DIALOG_TYPE) ?: HomeDialogType.DeleteChat

            if(it.containsKey(EXTRA_ARGUMENT_1))
                argument1 = it.getString(EXTRA_ARGUMENT_1, null)

            if(it.containsKey(EXTRA_ARGUMENT_3))
                argument3 = it.getInt(EXTRA_ARGUMENT_3)

            if(it.containsKey(EXTRA_ARGUMENT_2))
                argument2 = it.getString(EXTRA_ARGUMENT_2, null)

            if(it.containsKey(EXTRA_ARGUMENT_LONG))
                longData = it.getLong(EXTRA_ARGUMENT_LONG)

            if(it.containsKey(EXTRA_THREAD_RECORD))
                threadRecord = it.serializable(EXTRA_THREAD_RECORD)

            if(it.containsKey(EXTRA_THREAD_POSITION))
                threadPosition = it.getInt(EXTRA_THREAD_POSITION)
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
                                    listener?.onConfirm(dialogType, threadRecord, threadPosition)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
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
                                currentValue = timesOption[argument3],
                                onDismiss = {
                                    dismiss()
                                },
                                onValueChanged = { _, index ->
                                    listener?.onConfirmationWithData(dialogType, index, threadRecord, threadPosition)
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
                                    listener?.onConfirmationWithData(dialogType, argument3, threadRecord, threadPosition)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
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
                                    listener?.onConfirm(dialogType, threadRecord, threadPosition)
                                },
                                onClick = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
                                },
                                options = option.toList(),
                                currentValue = option[argument3],
                                onValueChanged = { _, index ->
                                    listener?.onConfirmationWithData(dialogType, index, threadRecord, threadPosition)
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
                                    listener?.onConfirm(dialogType, threadRecord, threadPosition)
                                },
                                onDismissRequest = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
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
                                    listener?.onConfirm(dialogType, threadRecord, threadPosition)
                                },
                                onDelete = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
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
                            val messageCount = argument3
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
                                    listener?.onConfirm(dialogType, threadRecord, threadPosition)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
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
                                currentValue = options[timesOption.indexOf(argument3)],
                                onDismiss = {
                                    dismiss()
                                },
                                onValueChanged = { _, index ->
                                    dismiss()
                                    if (options[timesOption.indexOf(argument3)] != options[index]) {
                                        listener?.onConfirmationWithData(dialogType, timesOption[index], threadRecord, threadPosition)
                                    }
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
                                    listener?.onConfirm(dialogType, threadRecord, threadPosition)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
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
                                    listener?.onConfirm(dialogType, threadRecord, threadPosition)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
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
                                    listener?.onConfirm(dialogType, threadRecord, threadPosition)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
                                }
                            )
                        }
                    }
                    HomeDialogType.GifSetting -> {
                        BChatTheme(
                            darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                        ) {
                            UnblockUserDialog(
                                title = stringResource(id = R.string.gif_dialog_title),
                                message = stringResource(id = R.string.gif_dialog_message),
                                positiveButtonTitle = stringResource(id = R.string.ok),
                                onAccept = {
                                    dismiss()
                                    listener?.onConfirm(dialogType, threadRecord, threadPosition)
                                },
                                onCancel = {
                                    dismiss()
                                    listener?.onCancel(dialogType, threadRecord, threadPosition)
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
        fun onConfirm(dialogType: HomeDialogType, threadRecord: ThreadRecord?, position : Int)
        fun onCancel(dialogType: HomeDialogType, threadRecord: ThreadRecord?, position : Int)
        fun onConfirmationWithData(dialogType: HomeDialogType, data: Any?, threadRecord: ThreadRecord?, position : Int)
    }

    companion object {
        const val EXTRA_DIALOG_TYPE = "dialog_type"
        const val EXTRA_ARGUMENT_1 = "argument_1"
        const val EXTRA_ARGUMENT_2 = "argument_2"
        const val EXTRA_ARGUMENT_3 = "argument_3"
        const val EXTRA_ARGUMENT_LONG = "argument_long_type"
        const val EXTRA_THREAD_RECORD = "thread_record"
        const val EXTRA_THREAD_POSITION = "thread_position"
        const val TAG = "ComposeDialogContainer"
    }
}