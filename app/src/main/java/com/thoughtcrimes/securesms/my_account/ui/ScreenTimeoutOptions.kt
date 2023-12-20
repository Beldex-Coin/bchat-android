package com.thoughtcrimes.securesms.my_account.ui

enum class ScreenTimeoutOptions(val time: Long, val displayValue: String) {
    None(0, "None"),
    ThirtySeconds(30000, "30 Seconds"),
    OneMinute(60000, "1 Minute"),
    TwoMinute(120000, "2 Minutes"),
    FiveMinute(300000, "5 Minutes"),
    FifteenMinute(900000, "15 Minutes"),
    ThirtyMinute(1800000, "30 Minutes"),
}