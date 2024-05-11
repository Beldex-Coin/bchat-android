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
import androidx.compose.runtime.remember
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
import com.thoughtcrimes.securesms.my_account.ui.ScreenTimeoutOptions
import io.beldex.bchat.R

@Composable
fun LockOptionsDialog(
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
        val lockOptions = remember {
            ScreenTimeoutOptions.entries.map { it.displayValue }.toList()
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_inactivity_timeout),
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
                items = lockOptions,
                visibleItemsCount = 3,
                startIndex = lockOptions.indexOf(currentValue),
                textModifier = Modifier.padding(12.dp),
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                onClick = {
                    onValueChanged(valuesPickerState.selectedItem, lockOptions.indexOf(valuesPickerState.selectedItem))
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = stringResource(id = R.string.ok),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
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
        currentValue = "",
        onDismiss = {},
        onValueChanged = {_, _ -> }
    )
}