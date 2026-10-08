package io.beldex.bchat.preferences

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

private val BUTTON_ROW_HEIGHT = 40.dp

/**
 * Shows the "0 hop / 1 hop / 3 hops" radio-button popup for the Onion Routing setting.
 *
 * The hop count currently applied by the app is passed in via [newInstance] (stored in the
 * fragment arguments) and the chosen hop count is handed back through the fragment result API
 * (see [REQUEST_KEY] / [RESULT_HOP_COUNT]). Both the arguments and the pending in-dialog
 * selection are restored on configuration change, which is why this class deliberately has a
 * no-argument constructor: a fragment that takes constructor parameters cannot be recreated
 * by the FragmentManager on rotation and crashes with
 * Fragment.InstantiationException: "could not find Fragment constructor".
 */
class OnionRoutingHopSelectionDialogFragment : DialogFragment() {

    /** The hop selection the user has made in the dialog, kept in sync with the composable. */
    private var selectedHopCount: Int = DEFAULT_HOP_COUNT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentHopCount = arguments?.getInt(ARG_CURRENT_HOP_COUNT, DEFAULT_HOP_COUNT)
            ?: DEFAULT_HOP_COUNT
        selectedHopCount = savedInstanceState?.getInt(STATE_SELECTED_HOP_COUNT, currentHopCount)
            ?: currentHopCount
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SELECTED_HOP_COUNT, selectedHopCount)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val entries = requireContext().resources.getStringArray(R.array.preferences__onion_routing_hops_entries).toList()
        val values = requireContext().resources.getStringArray(R.array.preferences__onion_routing_hops_values).mapNotNull { it.toIntOrNull() }
        val messageHint = requireContext().resources.getStringArray(R.array.preferences__onion_routing_hops_description).toList()
        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme(
                    darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                ) {
                    OnionRoutingHopSelectionDialog(
                        title = stringResource(id = R.string.activity_path_title),
                        messageHint = messageHint,
                        entries = entries,
                        selectionValues = values,
                        selectedValue = selectedHopCount,
                        onValueChange = { selectedHopCount = it },
                        onConfirm = { hopCount ->
                            parentFragmentManager.setFragmentResult(
                                REQUEST_KEY,
                                Bundle().apply { putInt(RESULT_HOP_COUNT, hopCount) }
                            )
                            dismiss()
                        },
                        onCancel = { dismiss() }
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "OnionRoutingHopSelection"

        /** Key of the fragment result carrying the confirmed hop count. */
        const val REQUEST_KEY = "io.beldex.bchat.preferences.ONION_ROUTING_HOP_COUNT"

        /** Int key of the confirmed hop count inside the result bundle. */
        const val RESULT_HOP_COUNT = "hop_count"

        private const val ARG_CURRENT_HOP_COUNT = "current_hop_count"
        private const val STATE_SELECTED_HOP_COUNT = "selected_hop_count"

        /** Same fallback as the composable used before: one hop. */
        private const val DEFAULT_HOP_COUNT = 3

        fun newInstance(currentHopCount: Int): OnionRoutingHopSelectionDialogFragment =
            OnionRoutingHopSelectionDialogFragment().apply {
                arguments = Bundle().apply { putInt(ARG_CURRENT_HOP_COUNT, currentHopCount) }
            }
    }
}

@Composable
private fun OnionRoutingHopSelectionDialog(
    title: String,
    messageHint: List<String>,
    entries: List<String>,
    selectionValues: List<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = onCancel
    ) {
        // Seeded from the fragment so a rotation restores what the user had tapped, while the
        // state itself lives here to keep the radio buttons recomposing on tap.
        var selection by remember(selectedValue) { mutableStateOf(selectedValue) }
        val selectedIndex = selectionValues.indexOf(selection).coerceAtLeast(0)

        // Landscape windows are only ~330dp tall, so the vertical rhythm is tightened there and
        // the dialog is expected to fit without scrolling. The scroll stays as a safety net for
        // large font scales / very short windows, and the Cancel/OK row sits outside of it so it
        // can never be pushed under the bottom of the screen.
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val contentPadding = if (isLandscape) 12.dp else 16.dp
        val listSpacing = if (isLandscape) 8.dp else 12.dp
        val optionSpacing = if (isLandscape) 6.dp else 10.dp
        val optionPadding = if (isLandscape) 8.dp else 14.dp
        val footerSpacing = if (isLandscape) 10.dp else 16.dp

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val listMaxHeight = (
                maxHeight - contentPadding * 2 - BUTTON_ROW_HEIGHT - footerSpacing
                ).coerceAtLeast(80.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = listMaxHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.appColors.secondaryContentColor,
                            fontWeight = FontWeight(700),
                            fontSize = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(listSpacing))

                    selectionValues.forEachIndexed { index, value ->
                        val entry = entries.getOrNull(index) ?: return@forEachIndexed
                        val isSelected = selection == value
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    color = MaterialTheme.appColors.listItemBackground,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.appColors.negativeGreenButtonBorder
                                        } else {
                                            MaterialTheme.appColors.textFiledBorderColor
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selection = value
                                    onValueChange(value)
                                }
                                .padding(horizontal = 12.dp, vertical = optionPadding)
                        ) {
                            OnionRoutingSelectionCircle(isSelected)
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.appColors.secondaryContentColor,
                                    fontWeight = FontWeight(400),
                                    fontSize = 16.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(optionSpacing))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_info_outline_dark),
                            contentDescription = "info icon for routing",
                            tint = MaterialTheme.appColors.titleTextColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = messageHint.getOrElse(selectedIndex) { "" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.appColors.titleTextColor,
                                fontWeight = FontWeight(400),
                                fontSize = 12.sp
                            ),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.height(footerSpacing))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.appColors.negativeGreenButton
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.appColors.negativeGreenButtonBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.appColors.negativeGreenButtonText,
                                fontWeight = FontWeight(400),
                                fontSize = 12.sp
                            )
                        )
                    }

                    Button(
                        onClick = { onConfirm(selection) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.appColors.negativeGreenButtonBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.ok),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight(400),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnionRoutingSelectionCircle(isSelected: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
modifier = Modifier
            .size(13.dp)
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.appColors.negativeGreenButtonBorder
                    } else {
                        MaterialTheme.appColors.textFiledBorderColor
                    }
                ),
                shape = CircleShape
            )
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(MaterialTheme.appColors.negativeGreenButtonBorder, CircleShape)
            )
        }
    }
}