package com.thoughtcrimes.securesms.onboarding.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.BChatOutlinedTextField
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.MultilineTextField
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun RestoreFromSeedScreen(
    navigateToPinCode: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        var restoreFromHeight by remember {
            mutableStateOf(false)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = "Display Name",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            BChatOutlinedTextField(
                value = "",
                onValueChange = {},
                label = "Enter name"
            )

            if (restoreFromHeight) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pick a restore height",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                BChatOutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = "Enter Block height to Restore",
                    keyboardType = KeyboardType.Number
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pick a Date",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                BChatOutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = "Enter Date",
                    trailingIcon = {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = ""
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                onClick = {
                    restoreFromHeight = !restoreFromHeight
                },
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = ""
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (!restoreFromHeight)
                        "Restore from Height"
                    else
                        "Restore from Date",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White
                    ),
                    modifier = Modifier
                        .padding(
                            vertical = 4.dp
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward),
                    contentDescription = ""
                )
            }
        }

        PrimaryButton(
            onClick = navigateToPinCode,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White
                ),
                modifier = Modifier
                    .padding(4.dp)
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun RestoreFromSeedScreenPreview() {
    BChatTheme() {
        Scaffold {
            RestoreFromSeedScreen(
                navigateToPinCode = {}
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RestoreFromSeedScreenPreviewLight() {
    BChatTheme() {
        Scaffold {
            RestoreFromSeedScreen(
                navigateToPinCode = {}
            )
        }
    }
}