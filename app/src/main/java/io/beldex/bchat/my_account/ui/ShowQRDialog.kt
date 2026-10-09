package io.beldex.bchat.my_account.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.RectangleShape
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.notchedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = {
            onDismissRequest()
        },
        wrapContentWidth = isLandscape,
        containerColor = Color(0xA60B0B0B),
        shape = notchedCornerShape(20.dp),
        showBorder = false
    ) {
        val context = LocalContext.current
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .then(
                    if (isLandscape)
                        Modifier.fillMaxWidth(0.7F).widthIn(max = 320.dp)
                    else
                        Modifier.fillMaxWidth().widthIn(max = 260.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = BChatTypography.titleMedium.copy(
                    color = MaterialTheme.appColors.onboardingInputText,
                    fontFamily = OpenSans,
                    fontSize = 16.sp,
                    fontWeight = FontWeight(700),
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                if (uiState.publicKey.isValidString()) {
                    val size = toPx(220, context.resources)
                    val bitMap = QRCodeUtilities.encode(
                        uiState.publicKey,
                        size,
                        isInverted = false,
                        hasTransparentBackground = false
                    )
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = bitMap.asImageBitmap(),
                            contentDescription = "",
                            modifier = Modifier
                                .sizeIn(maxWidth = 180.dp, maxHeight = 180.dp)
                                .padding(5.dp)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF222222), CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_bchat_logo),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            PrimaryButton(
                onClick = { onShare() },
                shape = notchedCornerShape(12.dp),
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color(0xFF0B0B0B),
                border = BorderStroke(1.4.dp, Color(0xFF0A370A)),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.width(122.dp).height(52.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_share_24),
                    contentDescription = "Share",
                    tint = Color(0xFF0B0B0B),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.share),
                    fontFamily = OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp)
                )
            }

        }
    }
}