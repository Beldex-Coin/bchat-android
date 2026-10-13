package io.beldex.bchat.compose_utils

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Revamp_2026 spinner: a faint ring with a green sweep. Replaces the old Lottie / Material spinners. */
@Composable
fun BChatLoader(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val transition = rememberInfiniteTransition(label = "bchat-loader")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val accent = MaterialTheme.appColors.userDetailsConfirmBackground
    val track = MaterialTheme.appColors.pinBoxInactiveBorder
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )
        drawArc(
            color = accent,
            startAngle = rotation,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/** Loader tile + optional caption, used inside [DialogContainer] popups. */
@Composable
fun BChatLoaderContent(text: String?, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .background(MaterialTheme.appColors.userDetailsCancelBackground, notchedCornerShape(14.dp))
        ) {
            BChatLoader(size = 30.dp)
        }
        if (text != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                color = MaterialTheme.appColors.editTextColor,
                fontFamily = RobotoMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
