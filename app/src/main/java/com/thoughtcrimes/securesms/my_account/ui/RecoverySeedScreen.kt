package com.thoughtcrimes.securesms.my_account.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.util.copyToClipBoard
import io.beldex.bchat.R

@Composable
fun RecoverySeedScreen(
    markedAsSafe: Boolean,
    verifyPin: () -> Unit,
    modifier: Modifier = Modifier,
    seed: String? = null
) {
    if (markedAsSafe) {
        RecoverySeedView(
            seed = seed ?: "",
            modifier = modifier
        )
    } else {
        RecoveryWarningView(
            verifyPin = verifyPin,
            modifier = modifier
        )
    }
}

@Composable
fun RecoverySeedView(
    seed: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val copyToClipBoard: () -> Unit = {
        context.copyToClipBoard("Seed", seed)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(
                top = 48.dp
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_warning_lock),
            contentDescription = ""
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.copy_your_recovery_seed),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.appColors.seedInfoTextColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = seed,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.primaryButtonColor
                ),
                modifier = Modifier
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            onClick = copyToClipBoard,
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
}

@Composable
private fun RecoveryWarningView(
    verifyPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        OutlinedCard(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.appColors.dialogBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 24.dp
                    )
            ) {
                OutlinedCard(
                    shape = CircleShape,
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFF0AF13)
                    ),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_important),
                        contentDescription = "",
                        modifier = Modifier
                            .size(72.dp)
                            .padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.important),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight(800)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.never_share_your_seed_with_anyone),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(id = R.string.seed_permission_important_description),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.appColors.restoreDescColor
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.seed_permission_important_confirmTitle),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = verifyPin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.primaryButtonColor
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.seed_permission_important_confirmButton),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun RecoverySeedScreenPreview() {
    RecoverySeedScreen(
        seed = "Lorem ipsum dolor sit amet consectetur adipiscing elit Ut et massa mi. Aliquam in hendrerit urna. Pellentesque sit amet sapien fringilla, mattis ligula consectetur,.",
        markedAsSafe = true,
        verifyPin = {},
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}