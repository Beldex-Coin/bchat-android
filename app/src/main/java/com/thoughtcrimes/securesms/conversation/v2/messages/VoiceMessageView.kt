package com.thoughtcrimes.securesms.conversation.v2.messages

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.audio.AudioSlidePlayer
import com.thoughtcrimes.securesms.components.CornerMask
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewVoiceMessageBinding
import com.thoughtcrimes.securesms.conversation.v2.utilities.MessageBubbleUtilities
import com.thoughtcrimes.securesms.database.AttachmentDatabase
import com.thoughtcrimes.securesms.database.model.MmsMessageRecord
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.roundToLong
@AndroidEntryPoint
class VoiceMessageView : RelativeLayout, AudioSlidePlayer.Listener {

    @Inject lateinit var attachmentDb: AttachmentDatabase

    private val binding: ViewVoiceMessageBinding by lazy { ViewVoiceMessageBinding.bind(this) }
    private val cornerMask by lazy {
      CornerMask(
            this
        )
    }
    private var isPlaying = false
    set(value) {
        field = value
        renderIcon()
    }
    private var progress = 0.0
    private var duration = 0L
    private var player: AudioSlidePlayer? = null
    var delegate: VoiceMessageViewDelegate? = null
    var indexInAdapter = -1

    // region Lifecycle
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.voiceMessageViewDurationTextView.text = String.format("%01d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(0),
            TimeUnit.MILLISECONDS.toSeconds(0))
    }
    // endregion

    // region Updating
    fun bind(message: MmsMessageRecord, isStartOfMessageCluster: Boolean, isEndOfMessageCluster: Boolean) {
        val audio = message.slideDeck.audioSlide!!
        binding.voiceMessageViewLoader.isVisible = audio.isInProgress
        val cornerRadii = MessageBubbleUtilities.calculateRadii(context, isStartOfMessageCluster, isEndOfMessageCluster, message.isOutgoing)
        cornerMask.setTopLeftRadius(cornerRadii[0])
        cornerMask.setTopRightRadius(cornerRadii[1])
        cornerMask.setBottomRightRadius(cornerRadii[2])
        cornerMask.setBottomLeftRadius(cornerRadii[3])

        // only process audio if downloaded
        if (audio.isPendingDownload || audio.isInProgress) {
            this.player = null
            return
        }

        val player = AudioSlidePlayer.createFor(context, audio, this)
        this.player = player

        (audio.asAttachment() as? DatabaseAttachment)?.let { attachment ->
            attachmentDb.getAttachmentAudioExtras(attachment.attachmentId)?.let { audioExtras ->
                if (audioExtras.durationMs > 0) {
                    duration = audioExtras.durationMs
                    binding.voiceMessageViewDurationTextView.visibility = View.VISIBLE
                    binding.voiceMessageViewDurationTextView.text = String.format("%01d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(audioExtras.durationMs),
                            TimeUnit.MILLISECONDS.toSeconds(audioExtras.durationMs) % 60)
                    Log.d("Beldex","Voice msg time to mins ${TimeUnit.MICROSECONDS.toMinutes(audioExtras.durationMs)}")
                    Log.d("Beldex","Voice msg time to seconds ${TimeUnit.MICROSECONDS.toSeconds(audioExtras.durationMs)}")
                    Log.d("Beldex","Voice msg time text ${binding.voiceMessageViewDurationTextView.text}")

                }
            }
        }
    }

    override fun onPlayerStart(player: AudioSlidePlayer) {
        isPlaying = true
    }

    override fun onPlayerProgress(player: AudioSlidePlayer, progress: Double, unused: Long) {
        if (progress == 1.0) {
            togglePlayback()
            handleProgressChanged(0.0)
            delegate?.playVoiceMessageAtIndexIfPossible(indexInAdapter - 1)
        } else {
            handleProgressChanged(progress)
        }
    }

    private fun handleProgressChanged(progress: Double) {
        this.progress = progress
        binding.voiceMessageViewDurationTextView.text = String.format("%01d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration - (progress * duration.toDouble()).roundToLong()),
            TimeUnit.MILLISECONDS.toSeconds(duration - (progress * duration.toDouble()).roundToLong()) % 60)
        val layoutParams = binding.progressView.layoutParams as RelativeLayout.LayoutParams
        layoutParams.width = (width.toFloat() * progress.toFloat()).roundToInt()
        binding.progressView.layoutParams = layoutParams
    }

    override fun onPlayerStop(player: AudioSlidePlayer) {
        isPlaying = false
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        cornerMask.mask(canvas)
    }

    private fun renderIcon() {
        val iconID = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.voiceMessagePlaybackImageView.setImageResource(iconID)
    }

    // endregion

    // region Interaction
    fun togglePlayback() {
        val player = this.player ?: return
        isPlaying = !isPlaying
        Log.d("Beldex","player status,${isPlaying.toString()}")
        if (isPlaying) {
            TextSecurePreferences.setPlayerStatus(context,true)
            player.play(progress)
        } else {
            TextSecurePreferences.setPlayerStatus(context,false)
            player.stop()
        }
    }

    fun handleDoubleTap() {
        val player = this.player ?: return
        player.playbackSpeed = if (player.playbackSpeed == 1.0f) 1.5f else 1.0f
    }
    // endregion

}

interface VoiceMessageViewDelegate {

    fun playVoiceMessageAtIndexIfPossible(indexInAdapter: Int)
}


