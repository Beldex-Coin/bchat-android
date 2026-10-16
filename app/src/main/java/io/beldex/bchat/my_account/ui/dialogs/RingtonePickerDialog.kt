package io.beldex.bchat.my_account.ui.dialogs

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.beldex.bchat.R

/** One selectable notification sound. [uri] is null for "Silent". */
data class SoundOption(val title: String, val uri: Uri?)

/**
 * In-app replacement for the system ringtone picker (Revamp_2026 "Notification sounds" popup):
 * lists Silent, the default tone and every system notification sound in the shared option-card
 * dialog, and previews a sound when it is picked.
 */
@Composable
fun RingtonePickerDialog(
    currentUri: Uri?,
    onDismiss: () -> Unit,
    onSelected: (Uri?) -> Unit
) {
    val context = LocalContext.current
    val options = remember { loadSoundOptions(context) }
    val player = remember { PreviewHolder() }
    DisposableEffect(Unit) { onDispose { player.stop() } }

    val currentIndex = options.indexOfFirst { it.uri == currentUri }.let { if (it >= 0) it else 1 }
    LockOptionsDialog(
        title = stringResource(R.string.preferences__sound),
        options = options.map { it.title },
        currentValue = options[currentIndex].title,
        onDismiss = onDismiss,
        onValueChanged = { _, index ->
            val option = options[index]
            player.play(context, option.uri)
            onSelected(option.uri)
        }
    )
}

private class PreviewHolder {
    private var ringtone: Ringtone? = null
    fun play(context: Context, uri: Uri?) {
        stop()
        if (uri == null) return
        ringtone = RingtoneManager.getRingtone(context, uri)?.also { it.play() }
    }
    fun stop() {
        ringtone?.stop()
        ringtone = null
    }
}

private fun loadSoundOptions(context: Context): List<SoundOption> {
    val list = mutableListOf(
        SoundOption(context.getString(R.string.preferences__silent), null),
        SoundOption(context.getString(R.string.preferences__default), Settings.System.DEFAULT_NOTIFICATION_URI)
    )
    try {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_NOTIFICATION) }
        val cursor = manager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            list += SoundOption(title, manager.getRingtoneUri(cursor.position))
        }
    } catch (_: Exception) {
        // No readable sounds: Silent + Default are still offered.
    }
    return list
}
