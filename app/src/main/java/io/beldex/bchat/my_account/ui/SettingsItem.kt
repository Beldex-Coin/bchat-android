package io.beldex.bchat.my_account.ui

import io.beldex.bchat.compose_utils.BChatSwitch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp

@Composable
fun SettingsItem(
    settingTitle: String,
    settingIcon: Painter,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = false,
    containsSwitch: Boolean = true,
    settingDesc: String? = null,
    trailingText: String? = null,
    onSwitchChanged: (Boolean) -> Unit = {},
    descTopPadding: Dp = 2.dp,
    iconTint: Color? = null,
    iconAlignTop: Boolean = false,
    /** When false the whole row is dimmed and its switch cannot be toggled. */
    rowEnabled: Boolean = true,
    /** Optional second line under [trailingText], e.g. "(Default)" (Figma Onion Routing row). */
    trailingSubText: String? = null,
    /** Shows the small grey chevron after the trailing value (Figma Onion Routing row). */
    showChevron: Boolean = false,
) {
    Row(
        verticalAlignment = if (iconAlignTop) Alignment.Top else Alignment.CenterVertically,
        modifier = modifier.then(if (rowEnabled) Modifier else Modifier.alpha(0.4f))
    ) {
        Icon(
            painter = settingIcon,
            contentDescription = "",
            tint = iconTint ?: MaterialTheme.appColors.secondaryContentColor
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = settingTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = OpenSans,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp
                ),
            )

            settingDesc?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.appColors.onboardingBodyColor,
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight(400),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.padding(top = descTopPadding),
                )
            }
        }

        trailingText?.let { value ->
            Spacer(modifier = Modifier.width(8.dp))

            if (trailingSubText != null || showChevron) {
                // Figma: right-aligned value, "(Default)" on its own line, then a 4x8 chevron.
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = value,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.appColors.editTextColor,
                            fontFamily = OpenSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                    )
                    trailingSubText?.let { sub ->
                        Text(
                            text = sub,
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.appColors.onboardingBodyColor,
                                fontFamily = OpenSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                        )
                    }
                }
                if (showChevron) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_settings_row_chevron),
                        contentDescription = null,
                    )
                }
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.appColors.lockTimerColor,
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight(600),
                        fontSize = 14.sp
                    ),
                )
            }
        }

        if (containsSwitch) {
            Spacer(modifier = Modifier.width(8.dp))

            BChatSwitch(
                checked = isEnabled,
                onCheckedChange = if (rowEnabled) onSwitchChanged else { _ -> }
            )
        }
    }
}
