package io.beldex.bchat.util

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import io.beldex.bchat.R

/**
 * Day/night UI mode related utilities.
 * @see <a href="https://developer.android.com/guide/topics/ui/look-and-feel/darktheme">Official Documentation</a>
 */
object UiModeUtilities {
    private const val PREF_KEY_SELECTED_UI_MODE = "SELECTED_UI_MODE"

    @JvmStatic
    fun setUserSelectedUiMode(context: Context, uiMode: UiMode) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
                .putString(PREF_KEY_SELECTED_UI_MODE, uiMode.name)
                .apply()
        AppCompatDelegate.setDefaultNightMode(uiMode.nightModeValue)
    }

    @JvmStatic
    fun getUserSelectedUiMode(context: Context): UiMode {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val selectedUiModeName = prefs.getString(PREF_KEY_SELECTED_UI_MODE, UiMode.NIGHT.name)!!
        val selectedUiMode: UiMode=try {
            UiMode.valueOf(selectedUiModeName)
        } catch (e: IllegalArgumentException) {
            // Cannot recognize UiMode constant from the given string.
            UiMode.NIGHT
        }
        return selectedUiMode
    }

    @JvmStatic
    fun setupUiModeToUserSelected(context: Context) {
        val selectedUiMode = getUserSelectedUiMode(context)
        setUserSelectedUiMode(context, selectedUiMode)
    }

    /**
     * Whether the application UI is in the light mode
     * (do not confuse with the user selected UiMode).
     */
    @JvmStatic
    fun isDayUiMode(context: Context): Boolean {
        val uiModeNightBit = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiModeNightBit == Configuration.UI_MODE_NIGHT_NO
    }
}

enum class UiMode(
        @StringRes
        val displayNameRes: Int,
        val nightModeValue: Int) {

    DAY(R.string.dialog_ui_mode_option_day, AppCompatDelegate.MODE_NIGHT_NO),
    NIGHT(R.string.dialog_ui_mode_option_night, AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM_DEFAULT(R.string.dialog_ui_mode_option_system_default, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
}