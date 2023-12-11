package com.thoughtcrimes.securesms.my_account.ui

sealed class MyAccountScreens(val route: String) {
    data object MyAccountScreen: MyAccountScreens("/my-account")
    data object SettingsScreen: MyAccountScreens("/settings")
}