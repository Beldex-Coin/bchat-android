package io.beldex.bchat.conversation.v2.utilities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogPanel
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

/**
 * Base for the app's small dialogs. Renders [Content] in the shared Revamp_2026 popup panel, so
 * every subclass gets the same look as the Compose popups. Subclasses only implement [Content].
 */
open class BaseDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(if (UiModeUtilities.isDayUiMode(requireContext())) 0.1f else 0.75f)
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            BChatTheme(darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    DialogPanel(modifier = Modifier.fillMaxWidth()) { Content() }
                }
            }
        }
    }

    /** The popup body, e.g. a [io.beldex.bchat.compose_utils.PopupBody]. */
    @Composable
    open fun Content() {
    }
}
