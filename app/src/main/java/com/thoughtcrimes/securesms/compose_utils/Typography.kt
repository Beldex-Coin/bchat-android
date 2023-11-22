package com.thoughtcrimes.securesms.compose_utils

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.beldex.bchat.R

val OpenSans = FontFamily(
    Font(R.font.open_sans_bold, FontWeight.Bold),
    Font(R.font.open_sans_medium, FontWeight.Medium),
    Font(R.font.open_sans_regular, FontWeight.Normal),
    Font(R.font.open_sans_semi_bold, FontWeight.SemiBold)
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