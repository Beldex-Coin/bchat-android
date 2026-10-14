package io.beldex.bchat.webrtc

import android.content.Context
import io.beldex.bchat.R
import org.apache.commons.lang3.time.DurationFormatUtils

/** Text for the "Person in call" banner shown on Home and in a chat (Figma Call_progress). */
object CallBannerUtils {
    /** Figma shows `0:48`; hours only appear once the call is over an hour long. */
    fun formatDuration(elapsedMs: Long): String =
        DurationFormatUtils.formatDuration(elapsedMs, if (elapsedMs >= 3_600_000L) "H:mm:ss" else "m:ss")

    fun label(context: Context, viewModel: CallViewModel): String {
        val name = viewModel.currentRecipient?.name?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.call_banner_person)
        val video = viewModel.videoState.value.let { it.userVideoEnabled || it.remoteVideoEnabled }
        return context.getString(if (video) R.string.call_banner_in_video_call else R.string.call_banner_in_call, name)
    }
}
