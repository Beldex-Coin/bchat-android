package io.beldex.bchat.onboarding.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
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
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(BorderStroke(1.dp, MaterialTheme.appColors.newChatIconBackground))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_left_24),
                contentDescription = null,
                tint = MaterialTheme.appColors.onboardingInputText
            )
        }
        Text(
            text = title,
            color = MaterialTheme.appColors.onboardingInputText,
            fontFamily = OpenSans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        actions()
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

/**
 * Themed replacement for the old [android.app.ProgressDialog]-based blocking spinner
 * (`BaseComponentActivity.showProgressDialog`) shown while the onboarding wallet/account is
 * being created, after display-name entry, on both the create- and restore-account flows. No
 * dedicated Figma frame covers this transient state, so it follows the same spinner+message
 * pattern already used elsewhere in the app (e.g. `ClearDataDialog`'s deleting step) rather than
 * inventing a new look. Non-dismissible, matching the old dialog's `setCancelable(false)`.
 */
@Composable
fun OnboardingLoadingOverlay(message: String) {
    DialogContainer(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        onDismissRequest = {},
        wrapContentWidth = true
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.appColors.primaryButtonColor)

            Text(
                text = message,
                color = MaterialTheme.appColors.onboardingInputText,
                fontFamily = OpenSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
