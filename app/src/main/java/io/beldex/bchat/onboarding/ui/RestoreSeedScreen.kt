package io.beldex.bchat.onboarding.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.MultilineTextField
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors

@Composable
fun RestoreSeedScreen(
    navigateToNextScreen: () -> Unit
) {
    var seed by remember {
        mutableStateOf("")
    }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.appColors.editTextBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                MultilineTextField(
                    value = seed,
                    onValueChange = {
                        seed = it
                    },
                    hintText = "Enter your seed",
                    maxLines = 6,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.appColors.editTextColor,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val clipboardManager = LocalClipboardManager.current
            PrimaryButton(
                onClick = {
                    clipboardManager.getText()?.text?.let {
                        seed = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .align(Alignment.End)
            ) {
                Text(
                    text = "Paste Seed",
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
                    painterResource(id = R.drawable.ic_paste),
                    contentDescription = "paste"
                )
            }
        }

        PrimaryButton(
            onClick = navigateToNextScreen,
            enabled = seed.isNotEmpty(),
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

@Preview
@Composable
fun RestoreSeedScreenPreview() {
    BChatTheme() {
        RestoreSeedScreen(
            navigateToNextScreen = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun RestoreSeedScreenPreviewLight() {
    BChatTheme {
        RestoreSeedScreen(
            navigateToNextScreen = {}
        )
    }
}