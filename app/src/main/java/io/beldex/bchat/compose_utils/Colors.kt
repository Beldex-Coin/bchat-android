package io.beldex.bchat.compose_utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val TextColor = Color(0xFFEBEBEB)
val TextColorLight = Color(0xFF333333)
val Primary = Color(0xFF1C1C26)
val PrimaryLight = Color(0xFFF0F0F0)
val Outline = Color(0xFF4B4B64)
val OutlineLight = Color(0xFFA7A7BA)
val SurfaceLight = Color(0xFFF8F8F8)

// Revamp_2026 root design tokens (Figma "BChat New theme 2023" file, page Revamp_2026).
// Each has a light and dark variant; individual `Colors` fields below are remapped onto
// these per-screen as each Revamp_2026 phase is implemented.
val RootBaseLight = Color(0xFF06070B)
val RootBaseDark = Color(0xFFFBFBFC)
val RootFaintLight = Color(0xFF8A93A6)
val RootFaintDark = Color(0xFF8E939D)
val RootAccentLight = Color(0xFF0250EE)
val RootAccentDark = Color(0xFF2B68E3)
val RootCriticLight = Color(0xFFED5502)
val RootCriticDark = Color(0xFFC34C0B)
val RootWarningLight = Color(0xFFEDE202)
val RootWarningDark = Color(0xFFC9C111)
val RootSuccessLight = Color(0xFF02ED41)
val RootSuccessDark = Color(0xFF0DC53E)
val RootComplement1 = Color(0xFF6556D9)
val RootComplement2 = Color(0xFF00F0FF)

class Colors() {
    var primaryButtonColor: Color = Color.Unspecified
    var primaryButtonIconColor: Color = Color.Unspecified
    var backgroundColor: Color = Color.Unspecified
    var secondaryContentColor: Color = Color.Unspecified
    var editTextPlaceholder: Color = Color.Unspecified
    var textFieldFocusedColor: Color = Color.Unspecified
    var textFieldUnfocusedColor: Color = Color.Unspecified
    var textFieldCursorColor: Color = Color.Unspecified
    var textFieldTextColor: Color = Color.Unspecified
    var tertiaryButtonColor: Color = Color.Unspecified
    var disabledPrimaryButtonContentColor: Color = Color.Unspecified
    var secondaryTextColor: Color = Color.Unspecified
    var editTextBackground: Color = Color.Unspecified
    var editTextColor: Color = Color.Unspecified
    var iconColor: Color = Color.Unspecified
    var beldexAddressColor: Color = Color.Unspecified
    var cardBackground: Color = Color.Unspecified
    var lockTimerColor: Color = Color.Unspecified
    var switchTrackColor: Color = Color.Unspecified
    var dialogBackground: Color = Color.Unspecified
    var restoreDescColor: Color = Color.Unspecified
    var seedInfoTextColor: Color = Color.Unspecified
    var actionIconBackground: Color = Color.Unspecified
    var createButtonBackground: Color = Color.Unspecified
    var contactCardBackground: Color = Color.Unspecified
    var contactCardBorder: Color = Color.Unspecified
    var requestCountBackground: Color = Color.Unspecified
    var searchBackground: Color = Color.Unspecified
    var inputHintColor: Color = Color.Unspecified
    var disabledButtonContent: Color = Color.Unspecified
    var textFieldDescriptionColor: Color = Color.Unspecified
    var textColor: Color = Color.Unspecified
    var dividerColor: Color = Color.Unspecified
    var walletDashboardMainMenuCardBackground: Color = Color.Unspecified
    var walletDashboardReceiveButtonBackground: Color = Color.Unspecified
    var walletSyncingIcon: Color = Color.Unspecified
    var loaderBackground: Color = Color.Unspecified
    var callBottomBackground: Color = Color.Unspecified
    var circularProgressBarBackground : Color = Color.Unspecified
    var optionalTextFieldBackground: Color = Color.Unspecified
    var clearDataSubTitle: Color = Color.Unspecified
    var textSelectionColor: Color = Color.Unspecified
    var disabledNextButtonColor : Color = Color.Unspecified
    var listItemBackground : Color = Color.Unspecified
    var profileAddressCardBackground: Color = Color.Unspecified
    var profileCameraIconBackgroundWithBnsTag: Color = Color.Unspecified
    var profileCameraIconBackground: Color = Color.Unspecified
    var negativeGreenButton: Color = Color.Unspecified
    var negativeRedButton: Color = Color.Unspecified
    var negativeRedButtonBorder: Color = Color.Unspecified
    var scrollBarColor: Color = Color.Unspecified
    // Revamp_2026 onboarding (Phase 1)
    var onboardingHeadlineColor: Color = Color.Unspecified
    var onboardingBodyColor: Color = Color.Unspecified
    var onboardingPrimaryButtonBackground: Color = Color.Unspecified
    var onboardingSecondaryButtonBorder: Color = Color.Unspecified
    var onboardingPrimaryButtonDisabledBackground: Color = Color.Unspecified
    var onboardingPrimaryButtonDisabledText: Color = Color.Unspecified
    var pinBoxInactiveBorder: Color = Color.Unspecified
    var callEndBackground: Color = Color.Unspecified
    var onboardingInputHint: Color = Color.Unspecified
    var onboardingCaptionColor: Color = Color.Unspecified
    var onboardingKeypadKeyBackground: Color = Color.Unspecified
    // Revamp_2026 Home / conversation list (Phase 2)
    var homeBannerText: Color = Color.Unspecified
    var homeFabBackground: Color = Color.Unspecified
    var homeFabIconColor: Color = Color.Unspecified
    // Revamp_2026 contact-details bottom sheet (Phase 3)
    var userDetailsSheetBackground: Color = Color.Unspecified
    var userDetailsCancelBackground: Color = Color.Unspecified
    var userDetailsCancelIconColor: Color = Color.Unspecified
    var userDetailsConfirmBackground: Color = Color.Unspecified
    var userDetailsBchatIdText: Color = Color.Unspecified
    // Revamp_2026 New Chat / Groups (Phase 4)
}

val lightColors = Colors().apply {
    primaryButtonColor = Color(0xFF078720)
    primaryButtonIconColor = Color(0xFFFFFFFF)
    backgroundColor = Color(0xFFEBEBEB)
    secondaryContentColor = Color(0xFF333333)
    editTextPlaceholder = Color(0xFFA7A7BA)
    textFieldFocusedColor = Color(0xFF00BD40)
    textFieldUnfocusedColor = Color(0xFFA7A7BA)
    textFieldCursorColor = Color(0x66222222)
    textFieldTextColor = Color(0xFF333333)
    tertiaryButtonColor = Color(0xFF0085FF)
    disabledPrimaryButtonContentColor = Color(0xFFF8F8F8)
    secondaryTextColor = Color(0xFF8A8A9D)
    editTextBackground = Color(0xFFF8F8F8)
    editTextColor = Color(0xFF333333)
    iconColor = Color(0xFF717194)
    beldexAddressColor = Color(0xFF00A3FF)
    cardBackground = Color(0xFFFFFFFF)
    lockTimerColor = Color(0xFF8A8A9D)
    switchTrackColor = Color.White
    dialogBackground = Color(0xFFF8F8F8)
    restoreDescColor = Color(0xFFACACAC)
    seedInfoTextColor = Color(0xFFECAB0F)
    actionIconBackground = Color(0xFFFFFFFF)
    createButtonBackground = Color(0xFFF8F8F8)
    contactCardBackground = Color(0xFFECECEC)
    contactCardBorder = Color(0xFFA7A7BA)
    requestCountBackground = Color(0xFFFFFFFF)
    searchBackground = Color(0xFFF8F8F8)
    inputHintColor = Color(0xFF8D8D8D)
    disabledButtonContent = Color(0xFF6E6E7C)
    textFieldDescriptionColor = Color(0xFF333333)
    textColor = Color(0xFF111111)
    dividerColor = Color(0XFFA7A7BA)
    walletDashboardMainMenuCardBackground = Color(0xffF0F0F0)
    walletDashboardReceiveButtonBackground = Color(0xff078720)
    walletSyncingIcon = Color(0xffA7A7BA)
    loaderBackground = Color(0xA4000000)
    callBottomBackground = Color(0xFFE8E8E8)
    circularProgressBarBackground = Color(0xffEBEBEB)
    optionalTextFieldBackground = Color(0xFFF4F4F4)
    clearDataSubTitle = Color(0XFF6A6A77)
    textSelectionColor= Color(0xFF008D06)
    disabledNextButtonColor = Color(0xFFA7A7BA)
    profileAddressCardBackground = Color(0xFFFFFFFF)
    profileCameraIconBackgroundWithBnsTag = Color(0xFFEBEBEB)
    profileCameraIconBackground = Color(0xFFF8F8F8)
    negativeGreenButton = Color(0x0D078727)
    negativeRedButton = Color(0x0DFF3E3E)
    negativeRedButtonBorder = Color(0xFFFF3E3E)
    scrollBarColor = Color(0xFFCDCDCD)
    onboardingHeadlineColor = Color(0xFF333333)
    onboardingBodyColor = Color(0xFF6E6E7C)
    onboardingPrimaryButtonBackground = Color(0xFF0B0B0B)
    onboardingSecondaryButtonBorder = Color(0xFFA7A7BA)
    onboardingPrimaryButtonDisabledBackground = Color(0xFFF8F8F8)
    onboardingPrimaryButtonDisabledText = Color(0xFF6E6E7C)
    pinBoxInactiveBorder = Color(0xFFD9D9D9)
    callEndBackground = Color(0xFFFBE9E9)
    onboardingInputHint = Color(0xFFA7A7BA)
    onboardingCaptionColor = Color(0xFF8A8A9D)
    onboardingKeypadKeyBackground = Color(0xFFECECEC)
    homeBannerText = Color(0xFF6A6A77)
    homeFabBackground = Color(0xFF00BC33)
    homeFabIconColor = Color(0xFF0A0A0A)
    userDetailsSheetBackground = Color(0xFFF8F8F8)
    userDetailsCancelBackground = Color(0xFFECECEC)
    userDetailsCancelIconColor = Color(0xFF6E6E7C)
    userDetailsConfirmBackground = Color(0xFF078720)
    userDetailsBchatIdText = Color(0xFF078720)
}

val darkColors = Colors().apply {
    primaryButtonColor = Color(0xFF078720)
    primaryButtonIconColor = Color(0xFFFFFFFF)
    backgroundColor = Color(0xFF0B0B0B)
    secondaryContentColor = Color(0xFFFFFFFF)
    editTextPlaceholder = Color(0xFFA7A7BA)
    textFieldFocusedColor = Color(0xFF00BD40)
    textFieldUnfocusedColor = Color(0xFF4B4B64)
    textFieldCursorColor = Color(0x66FFFFFF)
    textFieldTextColor = Color(0x80FFFFFF)
    tertiaryButtonColor = Color(0xFF0085FF)
    disabledPrimaryButtonContentColor = Color(0xFF6C6C78)
    secondaryTextColor = Color(0xFFA7A7BA)
    editTextBackground = Color(0xFF1C1C26)
    editTextColor = Color(0xFFEBEBEB)
    iconColor = Color(0xFF717194)
    beldexAddressColor = Color(0xFF00A3FF)
    cardBackground = Color(0xFF282836)
    lockTimerColor = Color(0xFFACACAC)
    switchTrackColor = Color(0xFF363645)
    dialogBackground = Color(0xFF111111)
    restoreDescColor = Color(0xFFACACAC)
    seedInfoTextColor = Color(0xFFF0AF13)
    actionIconBackground = Color(0xFF2C2C3B)
    createButtonBackground = Color(0xFF0B0B0F)
    contactCardBackground = Color(0xFF1C1C26)
    contactCardBorder = Color(0xFF353544)
    requestCountBackground = Color(0xFFEBEBEB)
    searchBackground = Color(0xFF282836)
    inputHintColor = Color(0xFFB9B9B9)
    disabledButtonContent = Color(0xFF6E6E7C)
    textFieldDescriptionColor = Color(0xFFA7A7BA)
    textColor = Color(0xFFFFFFFF)
    dividerColor = Color(0XFF4B4B64)
    walletDashboardMainMenuCardBackground = Color(0xff1C1C26)
    walletDashboardReceiveButtonBackground = Color(0xff078720)
    walletSyncingIcon = Color(0xff65656E)
    loaderBackground = Color(0xA4000000)
    callBottomBackground = Color(0xFF11111A)
    circularProgressBarBackground = Color(0xff30303F)
    optionalTextFieldBackground = Color(0xFF1C1C26)
    clearDataSubTitle = Color(0XFF6A6A77)
    textSelectionColor= Color(0xFF008D06)
    disabledNextButtonColor = Color(0xFF6C6C78)
    profileAddressCardBackground = Color(0xFF11111A)
    profileCameraIconBackgroundWithBnsTag = Color(0xFF11111A)
    profileCameraIconBackground = Color(0xFF111119)
    negativeGreenButton = Color(0x1A078727)
    negativeRedButton = Color(0x1AFF3E3E)
    negativeRedButtonBorder = Color(0xFFFF3E3E)
    scrollBarColor = Color(0xFF282836)
    onboardingHeadlineColor = Color(0xFFACACAC)
    onboardingBodyColor = Color(0xFF8D8D8D)
    onboardingPrimaryButtonBackground = Color(0xFFEBEBEB)
    onboardingSecondaryButtonBorder = Color(0xFF8D8D8D)
    onboardingPrimaryButtonDisabledBackground = Color(0xFF1A1A1A)
    onboardingPrimaryButtonDisabledText = Color(0xFF737373)
    pinBoxInactiveBorder = Color(0xFF333333)
    callEndBackground = Color(0xFF290F0F)
    onboardingInputHint = Color(0xFF737373)
    onboardingCaptionColor = Color(0xFF737373)
    onboardingKeypadKeyBackground = Color(0xFF1A1A1A)
    homeBannerText = Color(0xFFACACAC)
    homeFabBackground = Color(0xFF00BC33)
    homeFabIconColor = Color(0xFF0A0A0A)
    userDetailsSheetBackground = Color(0xFF151515)
    userDetailsCancelBackground = Color(0xFF222222)
    userDetailsCancelIconColor = Color(0xFFACACAC)
    userDetailsConfirmBackground = Color(0xFF00BC33)
    userDetailsBchatIdText = Color(0xFF00BD40)
}



val LocalAppColors = staticCompositionLocalOf {
    darkColors
}

val MaterialTheme.appColors: Colors
    @Composable
    get() = LocalAppColors.current