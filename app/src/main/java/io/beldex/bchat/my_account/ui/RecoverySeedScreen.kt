package io.beldex.bchat.my_account.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.copyToClipBoard

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
    val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT
    val activity = (context as? Activity)
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    val copyToClipBoard: () -> Unit = {
        context.copyToClipBoard("Seed", seed)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 64.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_recovery_seed_news),
            contentDescription = "",
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.appColors.onboardingHeadlineColor),
            modifier = Modifier.size(68.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(id = R.string.copy_your_recovery_seed),
            color = MaterialTheme.appColors.seedInfoTextColor,
            fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.appColors.onboardingInputBackground)
                .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder)
        ) {
            Text(
                text = seed,
                color = MaterialTheme.appColors.onboardingInputText,
                fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            onClick = copyToClipBoard,
            shape = io.beldex.bchat.compose_utils.notchedCornerShape(12.dp),
            containerColor = MaterialTheme.appColors.onboardingSecondaryButtonBackground,
            contentColor = MaterialTheme.appColors.onboardingInputText,
            border = BorderStroke(1.dp, MaterialTheme.appColors.onboardingSecondaryButtonBorder),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = stringResource(id = R.string.copy) + " " + stringResource(id = R.string.seed),
                fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "",
                tint = MaterialTheme.appColors.userDetailsConfirmBackground,
                modifier = Modifier
                    .size(14.dp)
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
            shape = androidx.compose.ui.graphics.RectangleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.appColors.dialogBackground
            ),
            border = BorderStroke(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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
                        fontWeight = FontWeight(700),
                        fontSize = 16.sp,
                        color = MaterialTheme.appColors.secondaryContentColor
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.never_share_your_seed_with_anyone),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.appColors.editTextColor,
                        fontWeight = FontWeight(700),
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.seed_permission_important_description),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.appColors.restoreDescColor,
                        fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.seed_permission_important_confirmTitle),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.appColors.editTextColor,
                        fontWeight = FontWeight(600),
                        fontSize = 12.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                io.beldex.bchat.compose_utils.PrimaryButton(
                    onClick = verifyPin,
                    shape = io.beldex.bchat.compose_utils.notchedCornerShape(10.dp),
                    containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
                    contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.seed_permission_important_confirmButton),
                        fontFamily = io.beldex.bchat.compose_utils.RobotoMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
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