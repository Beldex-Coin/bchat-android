package io.beldex.bchat.preferences
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.TextSecurePreferences.Companion.isNotificationsEnabled
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.ApplicationContext
import io.beldex.bchat.R
import io.beldex.bchat.components.SwitchPreferenceCompat
import io.beldex.bchat.notifications.NotificationChannels
import io.beldex.bchat.notifications.PushRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
@AndroidEntryPoint
class NotificationsPreferenceFragment : ListSummaryPreferenceFragment() {
    @Inject
    lateinit var pushRegistry: PushRegistry
    @Inject
    lateinit var prefs: TextSecurePreferences
    override fun onCreate(paramBundle: Bundle?) {
        super.onCreate(paramBundle)
        // Set up FCM toggle
        val fcmKey = "pref_key_use_fcm"
        val fcmPreference: SwitchPreferenceCompat = findPreference(fcmKey)!!
        fcmPreference.isChecked = prefs.isPushEnabled()
        fcmPreference.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            prefs.setPushEnabled(newValue as Boolean)
            val job = pushRegistry.refresh(true)
            fcmPreference.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                job.join()
                withContext(Dispatchers.Main) {
                    fcmPreference.isEnabled = true
                }
            }
            true
        }
        val ledBlinkPref = findPreference<Preference>(TextSecurePreferences.LED_BLINK_PREF)
        ledBlinkPref!!.isVisible = false
        prefs.setNotificationRingtone(
            NotificationChannels.getMessageRingtone(requireContext()).toString()
        )
        prefs.setNotificationVibrateEnabled(
            NotificationChannels.getMessageVibrate(requireContext())
        )
        findPreference<Preference>(TextSecurePreferences.LED_COLOR_PREF)!!.onPreferenceChangeListener = LedColorChangeListener()
        findPreference<Preference>(TextSecurePreferences.RINGTONE_PREF)!!.onPreferenceChangeListener = RingtoneSummaryListener()
        //New Line
        /*   this.findPreference(TextSecurePreferences.NOTIFICATION_PREF)
            .setOnPreferenceChangeListener(new AllNotificationsListener());*/
        findPreference<Preference>(TextSecurePreferences.REPEAT_ALERTS_PREF)!!.onPreferenceChangeListener = ListSummaryListener()
        findPreference<Preference>(TextSecurePreferences.NOTIFICATION_PRIVACY_PREF)!!.onPreferenceChangeListener = NotificationPrivacyListener()
        findPreference<Preference>(TextSecurePreferences.NOTIFICATION_PRIORITY_PREF)!!.onPreferenceChangeListener = ListSummaryListener()
        findPreference<Preference>(TextSecurePreferences.VIBRATE_PREF)!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                NotificationChannels.updateMessageVibrate(requireContext(), newValue as Boolean)
                true
            }
        findPreference<Preference>(TextSecurePreferences.RINGTONE_PREF)!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val current = prefs.getNotificationRingtone()
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                intent.putExtra(
                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                    RingtoneManager.TYPE_NOTIFICATION
                )
                intent.putExtra(
                    RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    Settings.System.DEFAULT_NOTIFICATION_URI
                )
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
                startActivityForResult(intent, 1)
                true
            }
        initializeListSummary(findPreference<Preference>(TextSecurePreferences.LED_COLOR_PREF) as ListPreference?)
        initializeListSummary(findPreference<Preference>(TextSecurePreferences.REPEAT_ALERTS_PREF) as ListPreference?)
        findPreference<Preference>(TextSecurePreferences.NOTIFICATION_PRIVACY_PREF)!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference ->
                val listPreference = preference as ListPreference
                listPreference.setDialogMessage(R.string.preferences_notifications__show)
                ListPreferenceDialog(listPreference) {
                    initializeListSummary(findPreference(TextSecurePreferences.NOTIFICATION_PRIVACY_PREF))
                }
                true
            }
        initializeListSummary(findPreference<Preference>(TextSecurePreferences.NOTIFICATION_PRIVACY_PREF) as ListPreference?)
        findPreference<Preference>(TextSecurePreferences.NOTIFICATION_PRIORITY_PREF)!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                intent.putExtra(
                    Settings.EXTRA_CHANNEL_ID, NotificationChannels.getMessagesChannel(requireContext())
                )
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                startActivity(intent)
                true
            }
        initializeRingtoneSummary(findPreference(TextSecurePreferences.RINGTONE_PREF))
        initializeMessageVibrateSummary(findPreference<Preference>(TextSecurePreferences.VIBRATE_PREF) as SwitchPreferenceCompat?)
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_notifications)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            var uri = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (Settings.System.DEFAULT_NOTIFICATION_URI == uri) {
                NotificationChannels.updateMessageRingtone(requireContext(), uri)
                prefs.removeNotificationRingtone()
            } else {
                uri = uri ?: Uri.EMPTY
                NotificationChannels.updateMessageRingtone(requireContext(), uri)
                prefs.setNotificationRingtone(uri.toString())
            }
            initializeRingtoneSummary(findPreference(TextSecurePreferences.RINGTONE_PREF))
        }
    }
    private inner class RingtoneSummaryListener : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            val value = newValue as? Uri
            if (value == null || TextUtils.isEmpty(value.toString())) {
                preference.setSummary(R.string.preferences__silent)
            } else {
                RingtoneManager.getRingtone(activity, value)
                    ?.getTitle(activity)
                    ?.let { preference.summary = it }
            }
            return true
        }
    }
    private fun initializeRingtoneSummary(pref: Preference?) {
        val listener = pref!!.onPreferenceChangeListener as RingtoneSummaryListener?
        val uri = prefs.getNotificationRingtone()
        listener!!.onPreferenceChange(pref, uri)
    }
    private fun initializeMessageVibrateSummary(pref: SwitchPreferenceCompat?) {
        pref!!.isChecked = prefs.isNotificationVibrateEnabled()
    }
    private inner class NotificationPrivacyListener : ListSummaryListener() {
        @SuppressLint("StaticFieldLeak")
        override fun onPreferenceChange(preference: Preference, value: Any): Boolean {
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    ApplicationContext.getInstance(activity).messageNotifier.updateNotification(activity!!)
                    return null
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            return super.onPreferenceChange(preference, value)
        }
    }
    @SuppressLint("StaticFieldLeak")
    private inner class LedColorChangeListener : ListSummaryListener() {
        override fun onPreferenceChange(preference: Preference, value: Any): Boolean {
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg params: Void?): Void? {
                    NotificationChannels.updateMessagesLedColor(
                        activity!!,
                        (value as String)
                    )
                    return null
                }
            }.execute()
            return super.onPreferenceChange(preference, value)
        }
    }
    companion object {
        @Suppress("unused")
        private val TAG = NotificationsPreferenceFragment::class.java.simpleName
        fun getSummary(context: Context): CharSequence = when (isNotificationsEnabled(context)) {
            true -> R.string.ApplicationPreferencesActivity_On
            false -> R.string.ApplicationPreferencesActivity_Off
        }.let(context::getString)
    }
}