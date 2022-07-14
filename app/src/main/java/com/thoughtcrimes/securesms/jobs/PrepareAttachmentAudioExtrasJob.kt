package com.thoughtcrimes.securesms.jobs

import android.os.Build
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment
import com.beldex.libbchat.messaging.sending_receiving.attachments.AttachmentId
import org.greenrobot.eventbus.EventBus
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachmentAudioExtras
import com.beldex.libbchat.messaging.utilities.Data
import com.beldex.libbchat.utilities.DecodedAudio
import com.beldex.libbchat.utilities.InputStreamMediaDataSource
import com.beldex.libsignal.utilities.Log
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.jobmanager.Job
import com.thoughtcrimes.securesms.mms.PartAuthority
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Decodes the audio content of the related attachment entry
 * and caches the result with [DatabaseAttachmentAudioExtras] data.
 *
 * It only process attachments with "audio" mime types.
 *
 * Due to [DecodedAudio] implementation limitations, it only works for API 23+.
 * For any lower targets fake data will be generated.
 *
 * You can subscribe to [AudioExtrasUpdatedEvent] to be notified about the successful result.
 */
//TODO AC: Rewrite to WorkManager API when
class PrepareAttachmentAudioExtrasJob : BaseJob {

    companion object {
        private const val TAG = "AttachAudioExtrasJob"

        const val KEY = "PrepareAttachmentAudioExtrasJob"
        const val DATA_ATTACH_ID = "attachment_id"

        const val VISUAL_RMS_FRAMES = 32 // The amount of values to be computed for the visualization.
    }

    private val attachmentId: AttachmentId

    constructor(attachmentId: AttachmentId) : this(Parameters.Builder()
            .setQueue(KEY)
            .setLifespan(TimeUnit.DAYS.toMillis(1))
            .build(),
            attachmentId)

    private constructor(parameters: Parameters, attachmentId: AttachmentId) : super(parameters) {
        this.attachmentId = attachmentId
    }

    override fun serialize(): Data {
        return Data.Builder().putParcelable(DATA_ATTACH_ID, attachmentId).build();
    }

    override fun getFactoryKey(): String { return KEY
    }

    override fun onShouldRetry(e: Exception): Boolean {
        return false
    }

    override fun onCanceled() { }

    override fun onRun() {
        Log.v(TAG, "Processing attachment: $attachmentId")

        val attachDb = DatabaseComponent.get(context).attachmentDatabase()
        val attachment = attachDb.getAttachment(attachmentId)

        if (attachment == null) {
            throw IllegalStateException("Cannot find attachment with the ID $attachmentId")
        }
        if (!attachment.contentType.startsWith("audio/")) {
            throw IllegalStateException("Attachment $attachmentId is not of audio type.")
        }

        // Check if the audio extras already exist.
        if (attachDb.getAttachmentAudioExtras(attachmentId) != null) return

        fun extractAttachmentRandomSeed(attachment: Attachment): Int {
            return when {
                attachment.digest != null -> attachment.digest!!.sum()
                attachment.fileName != null -> attachment.fileName.hashCode()
                else -> attachment.hashCode()
            }
        }

        fun generateFakeRms(seed: Int, frames: Int = VISUAL_RMS_FRAMES): ByteArray {
            return ByteArray(frames).apply { Random(seed.toLong()).nextBytes(this) }
        }

        var rmsValues: ByteArray
        var totalDurationMs: Long = DatabaseAttachmentAudioExtras.DURATION_UNDEFINED

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Due to API version incompatibility, we just display some random waveform for older API.
            rmsValues = generateFakeRms(extractAttachmentRandomSeed(attachment))
        } else {
            try {
                @Suppress("BlockingMethodInNonBlockingContext")
                val decodedAudio = PartAuthority.getAttachmentStream(context, attachment.dataUri!!).use {
                    DecodedAudio.create(InputStreamMediaDataSource(it))
                }
                rmsValues = decodedAudio.calculateRms(VISUAL_RMS_FRAMES)
                totalDurationMs = (decodedAudio.totalDuration / 1000.0).toLong()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode sample values for the audio attachment \"${attachment.fileName}\".", e)
                rmsValues = generateFakeRms(extractAttachmentRandomSeed(attachment))
            }
        }

        attachDb.setAttachmentAudioExtras(DatabaseAttachmentAudioExtras(
                attachmentId,
                rmsValues,
                totalDurationMs
        ))

        EventBus.getDefault().post(AudioExtrasUpdatedEvent(attachmentId))
    }

    class Factory : Job.Factory<PrepareAttachmentAudioExtrasJob> {
        override fun create(parameters: Parameters, data: Data): PrepareAttachmentAudioExtrasJob {
            return PrepareAttachmentAudioExtrasJob(parameters, data.getParcelable(DATA_ATTACH_ID, AttachmentId.CREATOR))
        }
    }

    /** Gets dispatched once the audio extras have been updated. */
    data class AudioExtrasUpdatedEvent(val attachmentId: AttachmentId)
}