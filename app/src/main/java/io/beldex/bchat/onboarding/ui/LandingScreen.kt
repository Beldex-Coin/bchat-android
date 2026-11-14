package io.beldex.bchat.onboarding.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.bchatBackground
import io.beldex.bchat.compose_utils.notchedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import io.beldex.bchat.compose_utils.BChatDimens

@Composable
fun LandingScreen(
    onCreateAccountClick: () -> Unit,
    onRestoreAccountClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .bchatBackground(MaterialTheme.appColors.backgroundColor, centerAlpha = 0.3f, dotAlpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 40.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_landing_logo_icon),
                        contentDescription = null,
                        modifier = Modifier.size(width = 36.dp, height = 38.dp)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_landing_logo_wordmark),
                        contentDescription = stringResource(R.string.app_name),
                        colorFilter = ColorFilter.tint(MaterialTheme.appColors.editTextColor),
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .size(width = 151.dp, height = 38.dp)
                    )
                }

                val headlineParts = stringResource(R.string.landing_headline).uppercase()
                    .split("\n").filter { it.isNotBlank() }
                headlineParts.forEachIndexed { index, part ->
                    Text(
                        text = part,
                        color = MaterialTheme.appColors.onboardingHeadlineColor,
                        fontFamily = OpenSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        modifier = Modifier.padding(top = if (index == 0) 44.dp else 12.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.landing_screen_content),
                    color = MaterialTheme.appColors.onboardingBodyColor,
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            LandingChatSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .graphicsLayer(alpha = 0.5f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    onClick = onCreateAccountClick,
                    shape = notchedCornerShape(BChatDimens.NotchLarge),
                    containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
                    contentColor = MaterialTheme.appColors.backgroundColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = stringResource(R.string.activity_landing_register_button_title),
                        fontFamily = OpenSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                PrimaryButton(
                    onClick = onRestoreAccountClick,
                    shape = notchedCornerShape(BChatDimens.NotchLarge),
                    containerColor = MaterialTheme.appColors.dialogBackground,
                    contentColor = MaterialTheme.appColors.editTextColor,
                    border = BorderStroke(1.dp, MaterialTheme.appColors.onboardingSecondaryButtonBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = stringResource(R.string.activity_landing_restore_button_title),
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = stringResource(R.string.terms_and_conditions),
                    color = MaterialTheme.appColors.onboardingCaptionColor,
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTermsClick
                        )
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun LandingScreenPreview() {
    BChatTheme {
        LandingScreen(onCreateAccountClick = {}, onRestoreAccountClick = {}, onTermsClick = {})
    }
}

// Figma `Frame 2765` (Landing 7546:934): faded chat-row skeleton drawn between the intro text and
// the buttons. Coordinates are Figma px on the 390-wide frame, treated as dp; it bleeds off the right edge.
@Composable
private fun LandingChatSkeleton(modifier: Modifier = Modifier) {
    val circle = MaterialTheme.appColors.cardTileBackground
    val lineColor = MaterialTheme.appColors.pinBoxInactiveBorder
    val cardStart = MaterialTheme.appColors.userDetailsCancelBackground
    val cardEnd = MaterialTheme.appColors.selectedItemBackground
    Canvas(modifier = modifier) {
        val contentH = (636f - 369f).dp.toPx()
        val s = minOf(size.height / contentH, 1f)
        val top = (size.height - contentH * s) / 2f
        fun dp(v: Float) = v.dp.toPx() * s
        fun x(v: Float) = v.dp.toPx() * s
        fun y(v: Float) = (v - 369f).dp.toPx() * s + top
        fun card(cx: Float, cy: Float, w: Float, h: Float, tl: Float, tr: Float) {
            val r = dp(19f)
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = x(cx), top = y(cy), right = x(cx + w), bottom = y(cy + h),
                        topLeftCornerRadius = CornerRadius(dp(tl)),
                        topRightCornerRadius = CornerRadius(dp(tr)),
                        bottomRightCornerRadius = CornerRadius(r),
                        bottomLeftCornerRadius = CornerRadius(r)
                    )
                )
            }
            drawPath(path, brush = Brush.horizontalGradient(listOf(cardStart, cardEnd), startX = x(cx), endX = x(cx + w)))
        }
        fun line(lx: Float, ly: Float, w: Float) =
            drawLine(lineColor, Offset(x(lx), y(ly)), Offset(x(lx + w), y(ly)), strokeWidth = dp(4.75f), cap = StrokeCap.Round)

        // row 1
        drawCircle(circle, radius = dp(27f), center = Offset(x(93f), y(404f)))
        card(139f, 369f, 325f, 69f, tl = 2.4f, tr = 19f)
        line(168f, 393f, 267f); line(168f, 415f, 267f)
        // row 2
        card(139f, 457f, 253f, 91f, tl = 19f, tr = 2.4f)
        line(168f, 481f, 195f); line(168f, 503f, 195f); line(168f, 525f, 112f)
        drawCircle(circle, radius = dp(27f), center = Offset(x(438f), y(503f)))
        // row 3
        drawCircle(circle, radius = dp(27f), center = Offset(x(93f), y(602f)))
        card(139f, 567f, 325f, 69f, tl = 2.4f, tr = 19f)
        line(168f, 591f, 267f); line(168f, 613f, 267f)
    }
}
