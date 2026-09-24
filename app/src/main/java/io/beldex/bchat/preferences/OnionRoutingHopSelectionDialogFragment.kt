package io.beldex.bchat.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
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

/**
 * Shows the "0 hop / 1 hop / 3 hops" radio-button popup for the Onion Routing setting.
 * The selected hop count is handed back via [onConfirm] and then persisted and applied by
 * the caller (0 = off/direct, 1 = one hop, 3 = three hops).
 */
class OnionRoutingHopSelectionDialogFragment(
    private val currentHopCount: Int,
    private val onConfirm: (Int) -> Unit,
    private val onCancel: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val entries = requireContext().resources.getStringArray(R.array.preferences__onion_routing_hops_entries).toList()
        val values = requireContext().resources.getStringArray(R.array.preferences__onion_routing_hops_values).mapNotNull { it.toIntOrNull() }
        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme(
                    darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == UiMode.NIGHT
                ) {
                    OnionRoutingHopSelectionDialog(
                        title = stringResource(id = R.string.preferences_onion_routing_hops),
                        message = stringResource(id = R.string.preferences_onion_routing_hops_summary),
                        entries = entries,
                        selectionValues = values,
                        currentValue = currentHopCount,
                        onConfirm = {
                            dismiss()
                            onConfirm(it)
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

@Composable
private fun OnionRoutingHopSelectionDialog(
    title: String,
    message: String,
    entries: List<String>,
    selectionValues: List<Int>,
    currentValue: Int,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = onCancel
    ) {
        var selectedValue by remember { mutableStateOf(selectionValues.firstOrNull { it == currentValue } ?: 1) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.secondaryContentColor,
                    fontWeight = FontWeight(700),
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.titleTextColor,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            selectionValues.forEachIndexed { index, value ->
                val entry = entries.getOrNull(index) ?: return@forEachIndexed
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedValue = value }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = selectedValue == value,
                        onClick = { selectedValue = value },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.appColors.negativeGreenButtonBorder,
                            unselectedColor = MaterialTheme.appColors.editTextColor
                        )
                    )
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.appColors.titleTextColor,
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.appColors.negativeGreenButtonText,
                            fontWeight = FontWeight(400),
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Button(
                    onClick = { onConfirm(selectedValue) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.negativeGreenButtonBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.ok),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight(400),
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}