package io.beldex.bchat.compose_utils

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.beldex.bchat.R

val OpenSans = FontFamily(
    Font(R.font.open_sans_bold, FontWeight.Bold),
    Font(R.font.open_sans_medium, FontWeight.Medium),
    Font(R.font.open_sans_regular, FontWeight.Normal),
    Font(R.font.open_sans_semi_bold, FontWeight.SemiBold)
)

// Roboto Mono is a variable font (single file, weight selected via the `wght` axis)
// used by the Revamp_2026 design as the secondary/body typeface alongside Open Sans.
@OptIn(ExperimentalTextApi::class)
val RobotoMono = FontFamily(
    Font(R.font.roboto_mono, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.roboto_mono, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.roboto_mono, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.roboto_mono, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.roboto_mono_italic, FontWeight.Normal, style = FontStyle.Italic, variationSettings = FontVariation.Settings(FontVariation.weight(400)))
)

private val defaultTypography = Typography()
val BChatTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontFamily = OpenSans,
        color = TextColor
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontFamily = OpenSans,
        color = TextColor
    )
)

val BChatTypographyLight = Typography(
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontFamily = OpenSans,
        color = TextColorLight
    )
)