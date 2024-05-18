package com.thoughtcrimes.securesms.onboarding.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.ui.BChatPreviewContainer
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import com.thoughtcrimes.securesms.util.copyToClipBoard
import io.beldex.bchat.R

@Composable
fun CopySeedScreen(
    seed: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT
    var continueEnabled by remember {
        mutableStateOf(false)
    }
    val copyToClipBoard: () -> Unit = {
        context.copyToClipBoard("Seed", seed)
        continueEnabled = true
    }
    Column(
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = 48.dp
                )
        ) {
            Image(
                painter = painterResource(id = if(isDarkTheme) R.drawable.ic_warning_lock else R.drawable.ic_warning_lock_light),
                contentDescription = ""
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.copy_your_recovery_seed),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.seedInfoTextColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.appColors.cardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = seed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                onClick = copyToClipBoard,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.copy),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                )
            }
        }

        Text(
            text = stringResource(R.string.copy_and_save_the_seed_to_continue),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.appColors.secondaryTextColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            onClick = copyToClipBoard,
            enabled = continueEnabled,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.continue_2),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (continueEnabled)
                        Color.White
                    else
                        MaterialTheme.appColors.disabledPrimaryButtonContentColor
                ),
                modifier = Modifier
                    .padding(8.dp)
            )
        }
    }
}

@Preview
@Composable
fun CopySeedScreenPreview() {
    BChatPreviewContainer {
        CopySeedScreen(
            seed = "Ut34co m56m 77odo8 6ve66ne natis023 3diam0id 5accum s3an3 6383ut7 purus eges tas34f acilisis is0233 diam0 id5acc ums3an36383ut7p",
            modifier = Modifier
                .fillMaxSize()
        )
    }
}