package io.beldex.bchat.my_account.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R

@Composable
fun PermissionSettingDialog(
    message: String,
    onDismissRequest: () -> Unit,
    gotoSettings: () -> Unit
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_permission_setting),
                contentDescription = "",
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.appColors.userDetailsCancelBackground,
                        shape = CircleShape
                    )
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.Permissions_permission_required),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.SemiBold
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = OpenSans
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.onboardingSecondaryButtonBackground
                    ),
                    border = BorderStroke(0.5.dp, MaterialTheme.appColors.onboardingSecondaryButtonBorder),
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.appColors.onboardingSecondaryButtonText,
                            fontFamily = OpenSans,
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = gotoSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground
                    ),
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.activity_settings_title),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = OpenSans,
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp,
                            color = MaterialTheme.appColors.onboardingPrimaryButtonText
                        ),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PermissionSettingDialogPreview() {
    PermissionSettingDialog(
        message = stringResource(R.string.library_access_required),
        onDismissRequest = {},
        gotoSettings = {}
    )
}