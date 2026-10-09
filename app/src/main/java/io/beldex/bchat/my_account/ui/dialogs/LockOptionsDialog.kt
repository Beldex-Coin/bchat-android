package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import io.beldex.bchat.compose_utils.BChatRadioButton
import io.beldex.bchat.compose_utils.DialogCancelButton
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.notchedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.ui.NumberPicker
import io.beldex.bchat.compose_utils.ui.rememberPickerState
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
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.appColors.onboardingInputText,
                fontFamily = OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                itemsIndexed(options) { index, option ->
                    val selected = option == currentValue
                    val accent = MaterialTheme.appColors.userDetailsConfirmBackground
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(
                                if (selected) MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground
                                else MaterialTheme.appColors.backgroundColor,
                                notchedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) accent else MaterialTheme.appColors.dividerColor,
                                notchedCornerShape(12.dp)
                            )
                            .clickable { onValueChanged(option, index) }
                            .padding(horizontal = 24.dp)
                    ) {
                        BChatRadioButton(selected = selected, onClick = { onValueChanged(option, index) })
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option,
                            color = if (selected) accent else MaterialTheme.appColors.onboardingBodyColor,
                            fontFamily = RobotoMono,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            DialogCancelButton(
                text = stringResource(id = R.string.cancel),
                onClick = onDismiss,
                fontFamily = RobotoMono,
                textColor = MaterialTheme.appColors.onboardingInputText,
                modifier = Modifier.width(162.dp)
            )
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