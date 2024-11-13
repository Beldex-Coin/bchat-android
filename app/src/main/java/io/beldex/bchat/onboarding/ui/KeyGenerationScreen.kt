package io.beldex.bchat.onboarding.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.ui.BChatPreviewContainer
import io.beldex.bchat.R

@Composable
fun KeyGenerationScreen(
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
        var restoreFromHeight by remember {
            mutableStateOf(false)
        }
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

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.appColors.cardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.chatid),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.appColors.primaryButtonColor
                    ),
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "328632bdskhj839ehdnd92dddid83993ndasoaksjhpifyaoajscqitp98wkjhaiuahhashf9ahfsdfhasdf..",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.register_screen_chat_id_description_content),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.appColors.textFieldDescriptionColor
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.appColors.cardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.beldex_address),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.appColors.tertiaryButtonColor
                    ),
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "328632bdskhj839ehdnd92dddid83993ndasoaksjhpifyaoajscqitp98wkjhaiuahhashf9ahfsdfhasdf..",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.register_screen_chat_id_description_content),
                style = MaterialTheme.typography.bodySmall.copy(
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun KeyGenerationScreenPreview() {
    BChatPreviewContainer {
        KeyGenerationScreen(proceed = {})
    }
}

@Preview
@Composable
fun KeyGenerationScreenPreviewDark() {
    BChatPreviewContainer {
        KeyGenerationScreen(proceed = {})
    }
}