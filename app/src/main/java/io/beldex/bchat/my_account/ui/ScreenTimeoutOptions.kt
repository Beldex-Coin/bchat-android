package io.beldex.bchat.my_account.ui

import android.content.Context
import androidx.annotation.StringRes
import io.beldex.bchat.R

enum class ScreenTimeoutOptions(
    val timeoutMillis: Long,
    @StringRes val labelRes: Int
) {
    None(0, R.string.none),
    ThirtySeconds(30000, R.string.thirty_seconds),
    OneMinute(60000, R.string.one_minute),
    TwoMinutes(120000, R.string.two_minutes),
    FiveMinutes(300000, R.string.five_minutes),
    FifteenMinutes(900000, R.string.fifteen_minutes),
    ThirtyMinutes(1800000, R.string.thirty_minutes);

    fun displayValue(context: Context): String {
        return context.getString(labelRes)
    }
}