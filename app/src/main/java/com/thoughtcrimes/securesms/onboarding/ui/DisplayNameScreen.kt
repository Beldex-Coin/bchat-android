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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thoughtcrimes.securesms.compose_utils.BChatOutlinedTextField
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.ui.BChatPreviewContainer
import io.beldex.bchat.R

@Composable
fun DisplayNameScreen(
    proceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = stringResource(id = R.string.display_name_screen_title_content),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            BChatOutlinedTextField(
                value = "",
                onValueChange = {},
                placeHolder = stringResource(id = R.string.enter_name),
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.activity_display_name_hint),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.textFieldDescriptionColor
                )
            )
        }

        PrimaryButton(
            onClick = proceed,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.continue_2),
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
fun DisplayNameScreenPreview() {
    BChatPreviewContainer() {
        Scaffold {
            DisplayNameScreen(
                proceed = {}
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun DisplayNameScreenPreviewDark() {
    BChatPreviewContainer() {
        Scaffold {
            DisplayNameScreen(
                proceed = {}
            )
        }
    }
}