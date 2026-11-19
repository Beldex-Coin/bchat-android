package io.beldex.bchat.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatDimens
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.bchatBackground
import io.beldex.bchat.compose_utils.notchedCornerShape

class ScanQRCodePlaceholderFragment: Fragment() {
    var delegate: ScanQRCodePlaceholderFragmentDelegate? = null

    override fun onCreateView(layoutInflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BChatTheme {
                    CameraAccessPlaceholder(onGrantAccess = { delegate?.requestCameraAccess() })
                }
            }
        }
    }
}

/**
 * Shown on the scan screens until camera permission is granted: an empty scan frame with green
 * corner brackets, "Allow camera access" title + mono explanation, and the notched light CTA.
 * Built from theme colours so it follows both dark and white themes.
 */
@Composable
private fun CameraAccessPlaceholder(onGrantAccess: () -> Unit) {
    val colors = MaterialTheme.appColors
    val isDark = colors.backgroundColor.luminance() < 0.5f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .bchatBackground(colors.backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp)
    ) {
        Spacer(modifier = Modifier.height(75.dp))
        ScanFrame()
        Spacer(modifier = Modifier.height(80.dp))
        Text(
            text = stringResource(id = R.string.camera_access_title),
            color = colors.editTextColor,
            fontFamily = OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.camera_access_description),
            color = colors.onboardingBodyColor,
            fontFamily = RobotoMono,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(50.dp))
        val buttonShape = notchedCornerShape(BChatDimens.NotchLarge)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 230.dp, height = 52.dp)
                .clip(buttonShape)
                .background(colors.onboardingPrimaryButtonBackground, buttonShape)
                // Soft centre highlight (the "silver" look in dark; a faint sheen on the black
                // white-theme button).
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDark) 0.55f else 0.14f),
                            Color.Black.copy(alpha = if (isDark) 0.10f else 0f)
                        )
                    ),
                    buttonShape
                )
                .clickable(onClick = onGrantAccess)
        ) {
            Text(
                text = stringResource(id = R.string.fragment_scan_qr_code_grant_camera_access_button_title),
                color = colors.backgroundColor,
                fontFamily = RobotoMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/** Square scan window: bordered gradient panel, inner well and green corner brackets. */
@Composable
private fun ScanFrame() {
    val colors = MaterialTheme.appColors
    val bracket = colors.primaryButtonColor
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                Brush.radialGradient(
                    listOf(colors.dialogPanelGradientStart, colors.dialogPanelGradientEnd)
                )
            )
            .border(1.dp, colors.optionBorder)
            .padding(31.dp)
            .background(colors.backgroundColor)
            .drawBehind {
                val len = 52.dp.toPx()
                val stroke = 1.dp.toPx()
                val h = stroke / 2
                val w = size.width
                val ht = size.height
                // top-left
                drawLine(bracket, Offset(h, h), Offset(len, h), stroke)
                drawLine(bracket, Offset(h, h), Offset(h, len), stroke)
                // top-right
                drawLine(bracket, Offset(w - len, h), Offset(w - h, h), stroke)
                drawLine(bracket, Offset(w - h, h), Offset(w - h, len), stroke)
                // bottom-left
                drawLine(bracket, Offset(h, ht - h), Offset(len, ht - h), stroke)
                drawLine(bracket, Offset(h, ht - len), Offset(h, ht - h), stroke)
                // bottom-right
                drawLine(bracket, Offset(w - len, ht - h), Offset(w - h, ht - h), stroke)
                drawLine(bracket, Offset(w - h, ht - len), Offset(w - h, ht - h), stroke)
            }
    )
}

interface ScanQRCodePlaceholderFragmentDelegate {

    fun requestCameraAccess()
}
