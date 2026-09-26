package com.beldex.libbchat.utilities

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Owns the persisted onion-routing hop count preference (the number of master nodes a message
 * passes through: 0 = off/direct connection, 1 = one hop, 3 = three hops).
 *
 * The value is meant to be read/written as an Int, but a legacy build stored it as a String
 * ("0" / "1" / "3") via a ListPreference. Reading an Int preference that actually holds a String
 * throws ClassCastException (see android.app.SharedPreferencesImpl#getInt), so [getPathCount]
 * reads the raw value, resolves it in a type-safe way, and migrates it back to an Int.
 */
object OnionRoutingPreferenceUtils {

    /** SharedPreferences key of the selected hop count. */
    const val KEY_ONION_ROUTING_PATH_COUNT = "pref_onion_routing_path_count"

    /**
     * Default hop count: a single hop. Applies both to fresh installs and to existing users on
     * upgrade. The previous release had no hop option (onion routing was hardcoded to three
     * hops), so after updating, users who never touched settings get the new one-hop default
     * (and at 1 hop, server traffic goes direct - see OnionRequestAPI.isServerOnionRoutingEnabled).
     */
    private const val DEFAULT_PATH_COUNT = 1

    @JvmStatic
    fun getPathCount(context: Context): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val storedValue = sharedPreferences.all[KEY_ONION_ROUTING_PATH_COUNT]
        val count = when (storedValue) {
            is Int -> storedValue
            is String -> storedValue.toIntOrNull() ?: DEFAULT_PATH_COUNT
            else -> DEFAULT_PATH_COUNT
        }
        if (storedValue !is Int) {
            // Migrate the legacy string value back to an Int so getInt() no longer crashes
            sharedPreferences.edit().putInt(KEY_ONION_ROUTING_PATH_COUNT, count).apply()
        }
        return count
    }

    @JvmStatic
    fun setPathCount(context: Context, count: Int) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putInt(KEY_ONION_ROUTING_PATH_COUNT, count)
            .apply()
    }
}