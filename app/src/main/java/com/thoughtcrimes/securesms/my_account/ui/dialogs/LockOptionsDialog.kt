package com.thoughtcrimes.securesms.my_account.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.ui.NumberPicker
import com.thoughtcrimes.securesms.compose_utils.ui.rememberPickerState
import io.beldex.bchat.R

@Composable
fun LockOptionsDialog(
    title: String,
    options: List<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onValueChanged: (String, Int) -> Unit
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismiss,
    ) {
        val valuesPickerState = rememberPickerState()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.primaryButtonColor,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            NumberPicker(
                state = valuesPickerState,
                items = options,
                visibleItemsCount = 3,
                startIndex = options.indexOf(currentValue),
                textModifier = Modifier.padding(12.dp),
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                onClick = {
                    onValueChanged(valuesPickerState.selectedItem, options.indexOf(valuesPickerState.selectedItem))
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = stringResource(id = R.string.ok),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp
                        )
                )
            }
        }
    }
}

@Preview
@Composable
fun LockOptionsDialogPreview() {
    LockOptionsDialog(
        title = "",
        options = listOf(),
        currentValue = "",
        onDismiss = {},
        onValueChanged = {_, _ -> }
    )
}