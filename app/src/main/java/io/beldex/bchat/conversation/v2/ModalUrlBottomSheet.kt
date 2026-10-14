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
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentModalUrlBottomSheetBinding
import io.beldex.bchat.util.UiModeUtilities

class ModalUrlBottomSheet(private val url: String): BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                io.beldex.bchat.compose_utils.BChatTheme(
                    darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == io.beldex.bchat.util.UiMode.NIGHT
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        io.beldex.bchat.compose_utils.DialogPanel(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                            io.beldex.bchat.compose_utils.PopupBody(
                                title = androidx.compose.ui.res.stringResource(R.string.dialog_open_url_title),
                                message = io.beldex.bchat.compose_utils.boldedMessage(
                                    androidx.compose.ui.res.stringResource(R.string.dialog_open_url_explanation, url), url
                                ),
                                cancelText = androidx.compose.ui.res.stringResource(R.string.cancel),
                                onCancel = { dismiss() },
                                middleText = androidx.compose.ui.res.stringResource(R.string.copy),
                                onMiddle = { copy() },
                                confirmText = androidx.compose.ui.res.stringResource(R.string.dialog_open_url_title).removeSuffix("?"),
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
        (view.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
