package com.thoughtcrimes.securesms.my_account.ui

sealed class MyAccountScreens(val route: String) {
    data object MyAccountScreen: MyAccountScreens("/my-account")
    data object SettingsScreen: MyAccountScreens("/settings")
    data object HopsScreen: MyAccountScreens("/hops")
    data object ChangeLogScreen: MyAccountScreens("/change-logs")
    data object AppLockScreen: MyAccountScreens("/app-lock")
    data object ChatSettingsScreen: MyAccountScreens("/chat-settings")
    data object BlockedContactScreen: MyAccountScreens("/blocked-contacts")
    data object MessageRequestsScreen: MyAccountScreens("/message-requests")
    data object RecoverySeedScreen: MyAccountScreens("/recovery-seed")
    data object AboutScreen: MyAccountScreens("/about")
    data object StartWalletInfoScreen: MyAccountScreens("/start-wallet-info")
}