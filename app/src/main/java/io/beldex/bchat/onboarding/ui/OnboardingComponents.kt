package io.beldex.bchat.onboarding.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.notchedCornerShape

/**
 * Shared Revamp_2026 onboarding top bar: a back arrow + step title, matching the header row
 * seen across the onboarding frames (e.g. `display_name` 7546:1015/1036).
 */
@Composable
fun OnboardingTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_left_24),
                contentDescription = null,
                tint = MaterialTheme.appColors.onboardingHeadlineColor
            )
        }
        Text(
            text = title,
            color = MaterialTheme.appColors.onboardingHeadlineColor,
            fontFamily = OpenSans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        )
    }
}

/**
 * Shared Revamp_2026 notched primary CTA button with a disabled state, matching the bottom-
 * pinned "Continue"/"Create Account" buttons seen across the onboarding frames.
 */
@Composable
fun OnboardingPrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryButton(
        onClick = onClick,
        enabled = enabled,
        shape = notchedCornerShape(14.5.dp),
        containerColor = MaterialTheme.appColors.onboardingPrimaryButtonBackground,
        contentColor = MaterialTheme.appColors.onboardingPrimaryButtonText,
        disabledContainerColor = MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground,
        disabledContentColor = MaterialTheme.appColors.onboardingPrimaryButtonDisabledText,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(text = text, fontFamily = OpenSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}
