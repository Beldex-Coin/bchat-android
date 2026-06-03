package io.beldex.bchat.conversation.v2.messages

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.core.view.isVisible
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.audio.AudioSlidePlayer
import io.beldex.bchat.components.CornerMask
import io.beldex.bchat.conversation.v2.utilities.MessageBubbleUtilities
import io.beldex.bchat.database.AttachmentDatabase
import io.beldex.bchat.database.model.MmsMessageRecord
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.messages.VisibleMessageContentView.Companion.getTimeTextColor
import io.beldex.bchat.databinding.ViewVoiceMessageBinding
import io.beldex.bchat.util.DateUtils
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
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
    var delegate: VisibleMessageViewDelegate? = null
    var indexInAdapter = -1
    private var isSelectionMode = false

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

    // region Updating
    fun bind(
        message : MmsMessageRecord,
        isStartOfMessageCluster : Boolean,
        isEndOfMessageCluster : Boolean,
        delegate : VisibleMessageViewDelegate,
        isSelectionMode : Boolean
    ) {
        resetViewState()
        this.isSelectionMode = isSelectionMode
        this.delegate = delegate
        binding.voiceMessageTime.text = DateUtils.getTimeStamp(context, Locale.getDefault(), message.timestamp)
        binding.voiceMessageTime.setTextColor(getTimeTextColor(context, message.isOutgoing))
        val audio = message.slideDeck.audioSlide!!
        binding.voiceMessageViewLoader.isVisible = audio.isInProgress
        binding.voiceMessagePlaybackImageView.isVisible = !audio.isInProgress
        //The duration value is displayed only for the voice message loader
        if(!message.isSent && message.isPending) {
            binding.voiceMessageViewDurationTextView.text = context.getString(R.string.zero_time_durationMs)
        }
        val cornerRadii = MessageBubbleUtilities.calculateRadii(context, isStartOfMessageCluster, isEndOfMessageCluster, message.isOutgoing)
        cornerMask.setTopLeftRadius(cornerRadii[0])
        cornerMask.setTopRightRadius(cornerRadii[1])
        cornerMask.setBottomRightRadius(cornerRadii[2])
        cornerMask.setBottomLeftRadius(cornerRadii[3])
        if(message.isOutgoing) {
            binding.voiceMessageViewLoader.indeterminateTintList = ColorStateList.valueOf(context.getColor(R.color.white))
            binding.seekbarAudio.progressBackgroundTintList = ColorStateList.valueOf(context.getColor(R.color.outgoingMessageProgressBackgroundTintColor))
            binding.seekbarAudio.progressTintList = ColorStateList.valueOf(context.getColor(R.color.white))
            binding.seekbarAudio.thumbTintList = ColorStateList.valueOf(context.getColor(R.color.white))
            binding.voiceMessageViewDurationTextView.setTextColor(context.getColor(R.color.white))
            binding.voiceMessagePlaybackImageView.setColorFilter(context.getColor(R.color.white))
            binding.viewVoiceMessageCard.setCardBackgroundColor(context.getColor(R.color.outgoing_call_background))
        }else{
            binding.voiceMessageViewLoader.indeterminateTintList = ColorStateList.valueOf(context.getColor(R.color.icon_tint))
            binding.seekbarAudio.progressBackgroundTintList = ColorStateList.valueOf(context.getColor(R.color.incomingMessageProgressBackgroundTintColor))
            binding.seekbarAudio.progressTintList = ColorStateList.valueOf(context.getColor(R.color.outgoingMessageProgressBackgroundTintColor))
            binding.seekbarAudio.thumbTintList = ColorStateList.valueOf(context.getColor(R.color.outgoingMessageProgressBackgroundTintColor))
            binding.voiceMessageViewDurationTextView.setTextColor(context.getColor(R.color.view_message_view_duration))
            binding.voiceMessagePlaybackImageView.setColorFilter(context.getColor(R.color.icon_tint))
            binding.viewVoiceMessageCard.setCardBackgroundColor(context.getColor(R.color.received_call_card_background))
        }

        // only process audio if downloaded
        if (audio.isPendingDownload || audio.isInProgress) {
            this.player = null
            return
        }

        val player = AudioSlidePlayer.createFor(context.applicationContext, audio, this)
        this.player = player

        (audio.asAttachment() as? DatabaseAttachment)?.let { attachment ->
            attachmentDb.getAttachmentAudioExtras(attachment.attachmentId)?.let { audioExtras ->
                if (audioExtras.durationMs > 0) {
                    duration = audioExtras.durationMs
                    binding.voiceMessageViewDurationTextView.visibility = View.VISIBLE
                    binding.voiceMessageViewDurationTextView.text = String.format("%01d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(audioExtras.durationMs),
                        TimeUnit.MILLISECONDS.toSeconds(audioExtras.durationMs) % 60)
                }
            }
        }

        binding.seekbarAudio.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val progressValue = progress / 100.0

                    this@VoiceMessageView.progress = progressValue

                    player.seekTo(progressValue)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val progressValue = if (seekBar.progress in 1..99) {
                    "0.${seekBar.progress}"
                } else if (seekBar.progress > 99) {
                    "1.0"
                } else {
                    "0.0"
                }

                player.seekTo(progressValue.toDouble())
            }
        })
        binding.voiceMessagePlaybackImageView.setOnClickListener {
            togglePlayback(isSelectionMode)
        }
    }

    override fun onPlayerStart(player: AudioSlidePlayer) {
        if (player != this.player) return

        if (isSelectionMode) {
            player.pause()
            return
        }
        isPlaying = true

        delegate?.isAudioPlaying(true, indexInAdapter)
    }

    override fun onPlayerProgress(
        player: AudioSlidePlayer,
        progress: Double,
        unused: Long
    ) {
        // Ignore stale callbacks
        if (player != this.player) return

        // Ignore callbacks after pause/stop
        if (!isPlaying) return

        // Ignore during selection mode
        if (isSelectionMode) return

        binding.seekbarAudio.progress = (progress * 100).toInt()

        if (progress >= 0.99) {

            isPlaying = false

            handleProgressChanged(0.0)

            binding.seekbarAudio.progress = 0

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
    }

    override fun onPlayerStop(player: AudioSlidePlayer) {
        if (player != this.player) return

        isPlaying = false

        binding.seekbarAudio.progress = 0

        progress = 0.0

        binding.voiceMessageViewDurationTextView.text =
            formatDuration(duration)

        delegate?.isAudioPlaying(false, indexInAdapter)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        cornerMask.mask(canvas)
    }

    private fun renderIcon() {
        val iconID = if (isPlaying) R.drawable.ic_pause_audio else R.drawable.ic_play_audio
        binding.voiceMessagePlaybackImageView.setImageResource(iconID)
    }

    fun togglePlayback(isSelectionMode: Boolean) {

        val player = this.player ?: return

        if (TextSecurePreferences.getRecordingStatus(context)) {
            Toast.makeText(
                context,
                "Unable to play audio while recording",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isSelectionMode) return

        if (isPlaying) {

            player.pause()

            isPlaying = false

        } else {

            isPlaying = true

            if (progress >= 1.0 || progress <= 0.0) {
                player.play(0.0)
            } else {
                player.resume()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        recycle()
    }

    fun handleDoubleTap() {
        val player = this.player ?: return
        player.playbackSpeed = if (player.playbackSpeed == 1.0f) 1.5f else 1.0f
    }

    private fun formatDuration(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format(Locale.ROOT, "%01d:%02d", minutes, seconds)
    }

    fun recycle() {

        isPlaying = false

        progress = 0.0

        binding.seekbarAudio.progress = 0

        player?.stop()
        player = null
    }

    private fun resetViewState() {

        // reset player state
        isPlaying = false

        progress = 0.0

        binding.seekbarAudio.progress = 0

        duration = 0L

        binding.voiceMessageViewDurationTextView.text =
            formatDuration(duration)

        // Remove old player
        player?.stop()
        player = null
    }

    fun stoppedVoiceMessage() {

        val player = this.player ?: return

        if (!isPlaying) return

        player.stop()
    }
    // endregion

}


