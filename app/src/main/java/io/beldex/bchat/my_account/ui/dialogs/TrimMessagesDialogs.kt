package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.DialogCancelButton
import io.beldex.bchat.compose_utils.DialogConfirmButton
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors

/** Edit the per-conversation message limit (Chat Settings > Conversation length limit). */
@Composable
fun TrimLengthDialog(
    currentLength: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentLength.toString()) }
    val value = text.toIntOrNull()
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 18.dp)
        ) {
            Text(
                text = stringResource(id = R.string.preferences__conversation_length_limit),
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = RobotoMono,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.appColors.textSelectionColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.appColors.dialogBackground)
                    .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DialogCancelButton(
                    text = stringResource(id = R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                io.beldex.bchat.compose_utils.DialogPrimaryButton(
                    text = stringResource(id = R.string.ok),
                    onClick = { value?.let(onSave) },
                    enabled = value != null && value >= 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Confirmation for "Trim all conversations now". */
@Composable
fun TrimNowDialog(
    limit: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    io.beldex.bchat.compose_utils.IconConfirmDialog(
        icon = androidx.compose.ui.res.painterResource(id = R.drawable.ic_pop_trim_now),
        title = stringResource(id = R.string.ApplicationPreferencesActivity_delete_all_old_messages_now),
        message = androidx.compose.ui.text.AnnotatedString(
            androidx.compose.ui.res.pluralStringResource(
                id = R.plurals.ApplicationPreferencesActivity_this_will_immediately_trim_all_conversations_to_the_d_most_recent_messages,
                count = limit,
                limit
            )
        ),
        cancelText = stringResource(id = R.string.cancel),
        confirmText = stringResource(id = R.string.delete),
        onCancel = onDismiss,
        onConfirm = onConfirm,
        destructive = true
    )
}
