package io.beldex.bchat.my_account.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.util.QRCodeUtilities
import io.beldex.bchat.util.isValidString
import io.beldex.bchat.util.toPx
import io.beldex.bchat.R

@Composable
fun ShowQRDialog(
    title: String,
    uiState: MyAccountViewModel.UIState,
    onShare: () -> Unit,
    onDismissRequest: () -> Unit
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = {
            onDismissRequest()
        },
        containerColor = MaterialTheme.appColors.bnsDialogBackground
    ) {
        val context = LocalContext.current
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = BChatTypography.titleMedium.copy(
                    color = MaterialTheme.appColors.primaryButtonColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(700),
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                if (uiState.publicKey.isValidString()) {
                    val size = toPx(280, context.resources)
                    val bitMap = QRCodeUtilities.encode(
                        uiState.publicKey,
                        size,
                        isInverted = false,
                        hasTransparentBackground = false
                    )
                    Image(
                        bitmap = bitMap.asImageBitmap(),
                        contentDescription = "",
                        modifier =Modifier
                            .fillMaxWidth(0.5f)
                            .aspectRatio(1f)
                            .padding(
                                5.dp
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick={
                onShare()
            }, colors= ButtonDefaults.buttonColors(containerColor=MaterialTheme.appColors.primaryButtonColor)) {
                Text(text="Share", style=MaterialTheme.typography.bodyMedium.copy(color=Color.White, fontWeight=FontWeight.Bold), modifier=Modifier.padding(start = 10.dp, end = 5.dp))
                Icon(painter=painterResource(id= R.drawable.ic_baseline_share_24), contentDescription="Refresh", tint=Color.White, modifier = Modifier.size(14.dp))
            }

        }
    }
}