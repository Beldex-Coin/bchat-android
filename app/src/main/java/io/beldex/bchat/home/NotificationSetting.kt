package io.beldex.bchat.home

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun NotificationSettingDialog(
        onDismiss : () -> Unit,
        onClick : (Int?) -> Unit,
        options : List<String>,
        currentValue : String,
        onValueChanged : (String, Int) -> Unit
) {

    var selectedItemIndex by remember {
        mutableIntStateOf(options.indexOf(currentValue))
    }

    DialogContainer(
            dismissOnBackPress=false,
            dismissOnClickOutside=false,
            onDismissRequest=onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.RecipientPreferenceActivity_notification_settings),
                    color = MaterialTheme.appColors.editTextColor,
                    fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .background(androidx.compose.ui.graphics.Color(0xCC222222))
                        .border(1.dp, MaterialTheme.appColors.dividerColor)
                        .clickable { onDismiss() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "",
                        tint = MaterialTheme.appColors.editTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEachIndexed { index, item ->
                    val selected = index == selectedItemIndex
                    val shape = io.beldex.bchat.compose_utils.notchedCornerShape(10.dp)
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                if (selected) MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground
                                else MaterialTheme.appColors.backgroundColor,
                                shape
                            )
                            .border(
                                1.dp,
                                if (selected) MaterialTheme.appColors.userDetailsConfirmBackground else MaterialTheme.appColors.dividerColor,
                                shape
                            )
                            .clickable { selectedItemIndex = index }
                    ) {
                        Text(
                            text = item,
                            color = if (selected) MaterialTheme.appColors.userDetailsConfirmBackground else MaterialTheme.appColors.onboardingBodyColor,
                            fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            io.beldex.bchat.compose_utils.PrimaryButton(
                onClick = {
                    if (selectedItemIndex in options.indices) {
                        onClick(selectedItemIndex)
                        onValueChanged(options[selectedItemIndex], selectedItemIndex)
                    } else {
                        onDismiss()
                    }
                },
                shape = io.beldex.bchat.compose_utils.notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium),
                containerColor = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
                contentColor = androidx.compose.ui.graphics.Color(0xFF0B0B0B),
                border = BorderStroke(1.4.dp, androidx.compose.ui.graphics.Color(0xFF0A370A)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(122.dp)
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.ok),
                    fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DisappearingDialogPreview() {
    BChatTheme {
        NotificationSettingDialog(
                onDismiss={},
                onClick={},
                options=listOf(),
                currentValue="",
                onValueChanged={ _, _ -> }
        )
    }
}

@Preview(uiMode=Configuration.UI_MODE_NIGHT_NO)
@Composable
fun DisappearingDialogLightPreview() {
    BChatTheme() {
        NotificationSettingDialog(
                onDismiss={},
                onClick={},
                options=listOf(),
                currentValue="",
                onValueChanged={ _, _ -> }
        )
    }
}