package com.thoughtcrimes.securesms.wallet.jetpackcomposeUI.settings

sealed class WalletSettingScreens(val route: String) {

    data object MyWalletSettingsScreen: WalletSettingScreens("/my-wallet-setting")

    /*data object NodeScreen: WalletSettingScreens("/node-screen")*/

    data object AddressBookScreen: WalletSettingScreens("/address-book")

    data object ChangePasswordScreen: WalletSettingScreens("/change-password")

}