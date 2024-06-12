package com.thoughtcrimes.securesms.home

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.appColors
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
        OutlinedCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.appColors.dialogBackground),
                elevation=CardDefaults.cardElevation(defaultElevation=4.dp),
                modifier=Modifier.fillMaxWidth()) {

            Column(
                    Modifier
                            .fillMaxWidth()
                            .padding(start=20.dp, end=20.dp, top=25.dp, bottom=25.dp), Arrangement.Center, Alignment.CenterHorizontally) {

                Row(modifier=Modifier.padding(bottom=20.dp)) {
                    Text(text=stringResource(id=R.string.RecipientPreferenceActivity_notification_settings),
                            style=MaterialTheme.typography.titleMedium.copy(
                                    fontSize=18.sp,
                                    fontWeight=FontWeight(700),
                                    color=MaterialTheme.appColors.primaryButtonColor),
                            textAlign=TextAlign.Center,
                            modifier=Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically))

                    Icon(
                            painter=painterResource(id=R.drawable.ic_close),
                            contentDescription="",
                            tint=MaterialTheme.appColors.editTextColor,
                            modifier=Modifier
                                    .clickable {
                                        onDismiss()
                                    }
                    )
                }
                LazyColumn(
                        verticalArrangement=Arrangement.spacedBy(10.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        modifier=Modifier
                                .fillMaxWidth()

                ) {
                    itemsIndexed(options) { index, item ->
                        Card(
                                colors=CardDefaults.cardColors(
                                        containerColor=MaterialTheme.appColors.changeLogBackground
                                ),
                                border=BorderStroke(
                                        width=2.dp,
                                        color=if (index == selectedItemIndex) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.textFiledBorderColor
                                ),
                                elevation=CardDefaults.cardElevation(
                                        defaultElevation=0.dp
                                ),
                                modifier=Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedItemIndex=index
                                            onClick(selectedItemIndex)
                                            onValueChanged(options[selectedItemIndex], selectedItemIndex)
                                        },
                                shape=RoundedCornerShape(16.dp)
                        ) {

                            Column(
                                    modifier=Modifier
                                            .fillMaxSize()
                                            .padding(vertical=8.dp),
                                    verticalArrangement=Arrangement.Center,
                                    horizontalAlignment=Alignment.CenterHorizontally,
                            ) {

                                Text(text=item, style=MaterialTheme.typography.titleMedium.copy(
                                        color=if (index == selectedItemIndex) MaterialTheme.appColors.secondaryContentColor else MaterialTheme.appColors.secondaryTextColor, fontSize=18.sp, fontWeight=FontWeight(600)
                                ), modifier=Modifier.padding(10.dp))
                            }
                        }
                    }
                }
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