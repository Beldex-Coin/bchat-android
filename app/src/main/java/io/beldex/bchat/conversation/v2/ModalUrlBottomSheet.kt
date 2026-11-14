package io.beldex.bchat.conversation.v2

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import io.beldex.bchat.util.BChatToast
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.beldex.bchat.R
import io.beldex.bchat.util.UiModeUtilities
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogPanel
import io.beldex.bchat.compose_utils.PopupBody
import io.beldex.bchat.compose_utils.boldedMessage
import io.beldex.bchat.util.UiMode

class ModalUrlBottomSheet(private val url: String): BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BChatTheme(
                    darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        DialogPanel(modifier = Modifier.fillMaxWidth()) {
                            PopupBody(
                                title = stringResource(R.string.dialog_open_url_title),
                                message = boldedMessage(
                                    stringResource(R.string.dialog_open_url_explanation, url), url
                                ),
                                onClose = { dismiss() },
                                middleText = stringResource(R.string.copy),
                                onMiddle = { copy() },
                                middleIcon = painterResource(R.drawable.ic_pop_copy),
                                confirmText = stringResource(R.string.dialog_open_url_title).removeSuffix("?"),
                                confirmIcon = painterResource(R.drawable.ic_pop_open_link),
                                onConfirm = { open() }
                            )
                        }
                    }
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The Revamp_2026 panel draws its own surface; the sheet itself must be see-through.
        (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun open() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            requireContext().startActivity(intent)
        } catch (e: Exception) {
            BChatToast.error(context, R.string.invalid_url, Toast.LENGTH_SHORT).show()
        }
        dismiss()
    }

    private fun copy() {
        val clip = ClipData.newPlainText("URL", url)
        val manager = requireContext().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(clip)
        BChatToast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        val isLightMode = UiModeUtilities.isDayUiMode(requireContext())
        window.setDimAmount(if (isLightMode) 0.1f else 0.75f)

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null && bottomSheet.parent is CoordinatorLayout) {
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isHideable = true
            behavior.skipCollapsed = true
            if (isLandscape) {
                val displayHeight = resources.displayMetrics.heightPixels
                behavior.maxHeight = (displayHeight * 0.9).toInt()
            }
        }
    }
}
