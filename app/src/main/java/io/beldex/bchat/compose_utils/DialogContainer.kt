package io.beldex.bchat.compose_utils

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.beldex.bchat.R

@Composable
fun DialogContainer(
    dismissOnBackPress : Boolean = false,
    dismissOnClickOutside : Boolean = false,
    onDismissRequest: () -> Unit,
    containerColor: Color = MaterialTheme.appColors.dialogBackground,
    wrapContentWidth: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    showBorder: Boolean = true,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            border = if (showBorder) BorderStroke(
                width = 1.dp,
                color = MaterialTheme.appColors.pinBoxInactiveBorder
            ) else null,
            modifier = Modifier
                .then(
                    if (wrapContentWidth)
                        Modifier.wrapContentWidth().padding(horizontal = 16.dp)
                    else
                        Modifier.fillMaxWidth(if (isLandscape) 0.5f else 0.9f)
                )
                .heightIn(max = 560.dp)
                .widthIn(max = if (isLandscape) 320.dp else 400.dp)
        ) {
            content()
        }
    }
}

// Revamp_2026 success popup (Figma 7546:22102): approval badge, title, compact OK button.
@Composable
fun SuccessPopup(
    title: String,
    onDismiss: () -> Unit,
    okEnabled: Boolean = true
) {
    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = onDismiss,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_success_approval),
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                textAlign = TextAlign.Center,
                color = MaterialTheme.appColors.onboardingInputText,
                fontFamily = OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(17.dp))
            PrimaryButton(
                onClick = onDismiss,
                enabled = okEnabled,
                shape = notchedCornerShape(12.dp),
                containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
                contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
                modifier = Modifier
                    .width(158.dp)
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.ok),
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Revamp_2026 dialog buttons (Figma 7546:5067): 162x52 notched, dark neutral "cancel" and a
// tinted "confirm" (red tint for destructive actions, green tint otherwise).
@Composable
fun DialogCancelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: androidx.compose.ui.text.font.FontFamily = OpenSans,
    textColor: Color = MaterialTheme.appColors.onboardingSecondaryButtonText
) {
    PrimaryButton(
        onClick = onClick,
        shape = notchedCornerShape(12.dp),
        containerColor = MaterialTheme.appColors.onboardingSecondaryButtonBackground,
        contentColor = textColor,
        border = BorderStroke(1.dp, MaterialTheme.appColors.onboardingSecondaryButtonBorder),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier.height(52.dp)
    ) {
        Text(text = text, fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp)
    }
}

@Composable
fun DialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    val accent = if (destructive) MaterialTheme.appColors.negativeRedButtonBorder
    else MaterialTheme.appColors.userDetailsConfirmBackground
    PrimaryButton(
        onClick = onClick,
        enabled = enabled,
        shape = notchedCornerShape(12.dp),
        containerColor = accent.copy(alpha = 0.1f),
        contentColor = MaterialTheme.appColors.onboardingInputText,
        border = BorderStroke(0.5.dp, accent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier.height(52.dp)
    ) {
        Text(text = text, fontFamily = OpenSans, fontWeight = FontWeight.Normal, fontSize = 14.sp)
    }
}

/**
 * Revamp_2026 icon popup (Figma "Voice/Video Calls", "Permission Required"): a notched dark icon
 * tile on the left of the title/message, then a Cancel + primary pair.
 */
@Composable
fun IconConfirmDialog(
    icon: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    message: androidx.compose.ui.text.AnnotatedString,
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    dismissOnOutside: Boolean = true,
    destructive: Boolean = false
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = dismissOnOutside,
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 18.dp)
        ) {
            androidx.compose.foundation.layout.Row {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(58.dp)
                        .background(MaterialTheme.appColors.userDetailsCancelBackground, notchedCornerShape(14.dp))
                ) {
                    androidx.compose.material3.Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = MaterialTheme.appColors.onboardingInputText,
                        modifier = Modifier.size(30.dp)
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.appColors.onboardingInputText,
                        fontFamily = OpenSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.appColors.onboardingInputText,
                        fontFamily = RobotoMono,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DialogCancelButton(
                    text = cancelText,
                    onClick = onCancel,
                    fontFamily = RobotoMono,
                    textColor = MaterialTheme.appColors.onboardingInputText,
                    modifier = Modifier.weight(1f)
                )
                if (destructive) {
                    DialogConfirmButton(
                        text = confirmText,
                        onClick = onConfirm,
                        destructive = true,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                PrimaryButton(
                    onClick = onConfirm,
                    shape = notchedCornerShape(12.dp),
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color(0xFF0B0B0B),
                    border = BorderStroke(1.dp, Color(0xFF0A370A)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text(
                        text = confirmText,
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                }
            }
        }
    }
}

