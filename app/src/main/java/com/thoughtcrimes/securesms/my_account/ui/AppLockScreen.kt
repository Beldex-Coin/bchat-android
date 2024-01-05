package com.thoughtcrimes.securesms.my_account.ui

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.onboarding.ui.OnBoardingActivity
import io.beldex.bchat.R

@Composable
fun AppLockScreen() {
    val context = LocalContext.current
    val changePin: () -> Unit  = {
        val intent = Intent(Intent.ACTION_VIEW, "onboarding://change_pin?finish=true".toUri(), context, OnBoardingActivity::class.java)
        context.startActivity(intent)
    }
    var showLockOptionsDialog by remember {
        mutableStateOf(false)
    }
    if (showLockOptionsDialog) {
        LockOptionsDialog(
            onDismiss = {
                showLockOptionsDialog = false
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    changePin()
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_change_password),
                    contentDescription = "",
                    tint = MaterialTheme.appColors.iconTint
                )

                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = stringResource(id = R.string.change_password),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showLockOptionsDialog = true
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_screen_lock),
                    contentDescription = "",
                    tint = MaterialTheme.appColors.iconTint
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.screenlock_inactivity_timeout),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "None",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.appColors.lockTimerColor
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun AppLockScreenPreview() {
    BChatTheme {
        AppLockScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AppLockScreenPreviewDark() {
    BChatTheme {
        AppLockScreen()
    }
}