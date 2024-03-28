package com.thoughtcrimes.securesms.onboarding.ui

sealed class OnBoardingScreens(val route: String) {

    data object RestoreSeedScreen: OnBoardingScreens("/on-boarding/restore")
    data object RestoreFromSeedScreen: OnBoardingScreens("/on-boarding/restore-from-seed")
    data object EnterPinCode: OnBoardingScreens("/on-boarding/pin-code")
    data object DisplayNameScreen: OnBoardingScreens("/on-boarding/display-name")
    data object GenerateKeyScreen: OnBoardingScreens("/on-boarding/key-generation")
    data object CopyRestoreSeedScreen: OnBoardingScreens("/on-boarding/restore-seed")

}