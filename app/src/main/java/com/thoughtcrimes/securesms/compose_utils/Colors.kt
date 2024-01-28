package com.thoughtcrimes.securesms.compose_utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val TextColor = Color(0xFFEBEBEB)
val TextColorLight = Color(0xFF333333)
val BackgroundColor = Color(0xFF11111A)
val Primary = Color(0xFF1C1C26)
val PrimaryLight = Color(0xFFF0F0F0)
val OnPrimary = Color(0xFFFFFFFF)
val Background = Color(0xFF1C1C26)
val BackgroundLight = Color(0xFFF0F0F0)
val OnBackgroundLight = Color(0xFF333333)
val Outline = Color(0xFF4B4B64)
val OutlineLight = Color(0xFFA7A7BA)
val SurfaceLight = Color(0xFFF8F8F8)

class Colors(
    val primaryButtonColor: Color,
    val secondaryButtonColor: Color,
    val backgroundColor: Color,
    val secondaryContentColor: Color,
    val editTextPlaceholder: Color,
    val focusedEditTextColor: Color,
    val textFieldFocusedColor: Color,
    val textFieldUnfocusedColor: Color,
    val textFieldCursorColor: Color,
    val textFieldTextColor: Color,
    val tertiaryButtonColor: Color,
    val disabledPrimaryButtonContentColor: Color,
    val onMainContainerTextColor: Color,
    val secondaryTextColor: Color,
    val editTextBackground: Color,
    val editTextColor: Color,
    val editTextHint: Color,
    val titleTextColor: Color,
    val iconColor: Color,
    val beldexAddressColor: Color,
    val cardBackground: Color,
    val iconTint: Color,
    val changeLogColor: Color,
    val changeLogBackground: Color,
    val lockTimerColor: Color,
    val unCheckedSwitchThumb: Color,
    val switchTrackColor: Color,
    val settingsCardBackground: Color,
    val dialogBackground: Color,
    val restoreDescColor: Color,
    val seedInfoTextColor: Color,
    val actionIconBackground: Color,
    val createButtonBackground: Color,
    val contactCardBackground: Color,
    val contactCardBorder: Color
)

val lightColors = Colors(
    primaryButtonColor = Color(0xFF00BD40),
    secondaryButtonColor = Color(0xFFEFEFEF),
    backgroundColor = Color(0xFFEBEBEB),
    secondaryContentColor = Color(0xFF333333),
    editTextPlaceholder = Color(0xFFA7A7BA),
    focusedEditTextColor = Color(0xFF333333),
    textFieldFocusedColor = Color(0xFF00BD40),
    textFieldUnfocusedColor = Color(0xFFA7A7BA),
    textFieldCursorColor = Color(0x66222222),
    textFieldTextColor = Color(0xFF333333),
    tertiaryButtonColor = Color(0xFF0085FF),
    disabledPrimaryButtonContentColor = Color(0xFFF8F8F8),
    onMainContainerTextColor = Color(0xFF333333),
    secondaryTextColor = Color(0xFF8A8A9D),
    editTextBackground = Color(0xFFF8F8F8),
    editTextColor = Color(0xFF333333),
    editTextHint = Color(0xFFA7A7BA),
    titleTextColor = Color(0xFF333333),
    iconColor = Color(0xFF717194),
    beldexAddressColor = Color(0xFF00A3FF),
    cardBackground = Color(0xFFFFFFFF),
    iconTint = Color(0xFF333333),
    changeLogColor = Color(0xFF333333),
    changeLogBackground = Color(0xFFF4F4F4),
    lockTimerColor = Color(0xFF8A8A9D),
    unCheckedSwitchThumb = Color(0xFFA7A7BA),
    switchTrackColor = Color.White,
    settingsCardBackground = Color(0xFFF4F4F4),
    dialogBackground = Color(0xFFF8F8F8),
    restoreDescColor = Color(0xFFACACAC),
    seedInfoTextColor = Color(0xFFECAB0F),
    actionIconBackground = Color(0xFFFFFFFF),
    createButtonBackground = Color(0xFFF8F8F8),
    contactCardBackground = Color(0xFFECECEC),
    contactCardBorder = Color(0xFFA7A7BA)
)

val darkColors = Colors(
    primaryButtonColor = Color(0xFF00BD40),
    secondaryButtonColor = Color(0xFF282836),
    backgroundColor = Color(0xFF11111A),
    secondaryContentColor = Color(0xFFFFFFFF),
    editTextPlaceholder = Color(0xFFA7A7BA),
    focusedEditTextColor = Color(0x80FFFFFF),
    textFieldFocusedColor = Color(0xFF00BD40),
    textFieldUnfocusedColor = Color(0xFF4B4B64),
    textFieldCursorColor = Color(0x66FFFFFF),
    textFieldTextColor = Color(0x80FFFFFF),
    tertiaryButtonColor = Color(0xFF0085FF),
    disabledPrimaryButtonContentColor = Color(0xFF6C6C78),
    onMainContainerTextColor = Color(0xFFFFFFFF),
    secondaryTextColor = Color(0xFFA7A7BA),
    editTextBackground = Color(0xFF1C1C26),
    editTextColor = Color(0xFFEBEBEB),
    editTextHint = Color(0xFFA7A7BA),
    titleTextColor = Color(0xFFEBEBEB),
    iconColor = Color(0xFF717194),
    beldexAddressColor = Color(0xFF00A3FF),
    cardBackground = Color(0xFF282836),
    iconTint = Color(0xFFFFFFFF),
    changeLogColor = Color(0xFFEBEBEB),
    changeLogBackground = Color.Transparent,
    lockTimerColor = Color(0xFFACACAC),
    unCheckedSwitchThumb = Color(0xFF9595B5),
    switchTrackColor = Color(0xFF363645),
    settingsCardBackground = Color(0xFF1C1C26),
    dialogBackground = Color(0xFF111119),
    restoreDescColor = Color(0xFFACACAC),
    seedInfoTextColor = Color(0xFFF0AF13),
    actionIconBackground = Color(0xFF2C2C3B),
    createButtonBackground = Color(0xFF11111A),
    contactCardBackground = Color(0xFF1C1C26),
    contactCardBorder = Color(0xFF353544)
)



val LocalAppColors = staticCompositionLocalOf {
    darkColors
}

val MaterialTheme.appColors: Colors
    @Composable
    get() = LocalAppColors.current