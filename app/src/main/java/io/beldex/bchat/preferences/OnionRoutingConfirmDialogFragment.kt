package io.beldex.bchat.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.conversation.v2.dialogs.UnblockUserDialog
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

class OnionRoutingConfirmDialogFragment(
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme(
                    darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                ) {
                    UnblockUserDialog(
                        title = getString(R.string.preferences__onion_routing),
                        message = getString(R.string.onion_routing_turn_off_confirmation),
                        positiveButtonTitle = getString(R.string.ok),
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
