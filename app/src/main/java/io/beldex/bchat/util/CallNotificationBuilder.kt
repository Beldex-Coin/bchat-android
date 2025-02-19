package io.beldex.bchat.util

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory.decodeStream
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Canvas
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.notifications.NotificationChannels
import io.beldex.bchat.preferences.PrivacySettingsActivity
import io.beldex.bchat.service.WebRtcCallService
import io.beldex.bchat.webrtc.WebRTCComposeActivity
import io.beldex.bchat.R


class CallNotificationBuilder {

    companion object {
        const val WEBRTC_NOTIFICATION = 313388

        const val TYPE_INCOMING_RINGING = 1
        const val TYPE_OUTGOING_RINGING = 2
        const val TYPE_ESTABLISHED = 3
        const val TYPE_INCOMING_CONNECTING = 4
        const val TYPE_INCOMING_PRE_OFFER = 5
        const val TYPE_SCREEN_ON = 6

        @JvmStatic
        fun areNotificationsEnabled(context: Context): Boolean {
            val notificationManager = NotificationManagerCompat.from(context)
            return when {
                !notificationManager.areNotificationsEnabled() -> false
                true -> {
                    notificationManager.notificationChannels.firstOrNull { channel ->
                        channel.importance == NotificationManager.IMPORTANCE_NONE
                    } == null
                }
                else -> true
            }
        }

        @JvmStatic
        fun getFirstCallNotification(context: Context): Notification {
            val contentIntent = Intent(context, PrivacySettingsActivity::class.java)

            val pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val text = context.getString(R.string.CallNotificationBuilder_first_call_message)

            val builder = NotificationCompat.Builder(context, NotificationChannels.CALLS)
                .setSound(null)
                .setSmallIcon(R.drawable.ic_baseline_call_24)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.CallNotificationBuilder_first_call_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)

            return builder.build()
        }

        @JvmStatic
        fun getCallInProgressNotification(context: Context, type: Int, recipient: Recipient?): Notification {
            val contentIntent = Intent(context, WebRTCComposeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val sizeInPX =context.resources.getDimensionPixelSize(R.dimen.extra_large_profile_picture_size)

            val contentView=RemoteViews(context.packageName, R.layout.custom_call_notification)
            val signalProfilePicture = recipient?.contactPhoto

            val bit=AvatarPlaceholderGenerator.generate(
                    context,
                    sizeInPX,
                    recipient?.address.toString(),
                    recipient?.name.toString()
            )
            if (signalProfilePicture != null) {
                val bitmap=decodeStream(signalProfilePicture.openInputStream(context))
                val output = Bitmap.createBitmap(
                    bitmap.width,
                    bitmap.height, Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(output)

                val paint = Paint()
                val rect = Rect(0, 0, bitmap.width, bitmap.height)

                paint.isAntiAlias = true
                canvas.drawARGB(0, 0, 0, 0)
                canvas.drawCircle(
                    (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(),
                    (bitmap.width / 2).toFloat(), paint
                )
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
                canvas.drawBitmap(bitmap, rect, rect, paint)
                contentView.setImageViewBitmap(R.id.image, output)
            } else {
                val output = Bitmap.createBitmap(
                    bit.bitmap.width,
                    bit.bitmap.height, Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(output)

                val paint = Paint()
                val rect = Rect(0, 0, bit.bitmap.width, bit.bitmap.height)

                paint.isAntiAlias = true
                canvas.drawARGB(0, 0, 0, 0)
                canvas.drawCircle(
                    (bit.bitmap.width / 2).toFloat(), (bit.bitmap.height / 2).toFloat(),
                    (bit.bitmap.width / 2).toFloat(), paint
                )
                paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
                canvas.drawBitmap(bit.bitmap, rect, rect, paint)
                contentView.setImageViewBitmap(R.id.image, output)
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val intent = Intent(context, WebRtcCallService::class.java)
                    .setAction(WebRtcCallService.ACTION_DENY_CALL)
            val hangUpIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val answerIntent = Intent(context, WebRTCComposeActivity::class.java)
                    .setAction( if (type == TYPE_INCOMING_PRE_OFFER) WebRTCComposeActivity.ACTION_PRE_OFFER else WebRTCComposeActivity.ACTION_ANSWER)

            val answerPendingIntent = PendingIntent.getActivity(context, 0, answerIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(context, NotificationChannels.CALLS)
                .setFullScreenIntent(getFullScreenPendingIntent(
                    context
                ), true)
                .setSound(null)
                .setColor(context.getColor(R.color.call_notification_background))
                .setSmallIcon(R.drawable.ic_baseline_call_24)
                .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setCustomContentView(contentView).build()

            recipient?.name?.let { name ->
                contentView.setTextViewText(R.id.title, name)
            }

            when (type) {
                TYPE_INCOMING_CONNECTING -> {
                    contentView.setTextViewText(R.id.text, context.getString(R.string.CallNotificationBuilder_connecting))
                    contentView.setViewVisibility(R.id.hangUpButton, View.GONE)
                    contentView.setViewVisibility(R.id.answerButton, View.GONE)
                }
                TYPE_INCOMING_PRE_OFFER,
                TYPE_INCOMING_RINGING -> {
                    contentView.setTextViewText(R.id.text, context.getString(R.string.NotificationBarManager__incoming_signal_call))
                    contentView.setOnClickPendingIntent(R.id.hangUpButton,hangUpIntent)
                    contentView.setOnClickPendingIntent(R.id.answerButton, answerPendingIntent)
                    //builder.setFullScreenIntent(pendingIntent)
                   /* builder.addAction(getServiceNotificationAction(
                        context,
                        WebRtcCallService.ACTION_DENY_CALL,
                        R.drawable.ic_close_grey600_32dp,
                        R.string.NotificationBarManager__deny_call
                    ))
                    // if notifications aren't enabled, we will trigger the intent from WebRtcCallService
                    builder.setFullScreenIntent(getFullScreenPendingIntent(
                        context
                    ), true)
                    builder.addAction(getActivityNotificationAction(
                        context,
                        if (type == TYPE_INCOMING_PRE_OFFER) WebRTCComposeActivity.ACTION_PRE_OFFER else WebRTCComposeActivity.ACTION_ANSWER,
                        R.drawable.ic_phone_grey600_32dp,
                        R.string.NotificationBarManager__answer_call
                    ))*/
                    builder.priority = Notification.PRIORITY_MAX
                }
                TYPE_OUTGOING_RINGING -> {
                    val intent = Intent(context, WebRtcCallService::class.java)
                            .setAction(WebRtcCallService.ACTION_LOCAL_HANGUP)

                    val establishCall = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    contentView.setTextViewText(R.id.text, context.getString(R.string.NotificationBarManager__establishing_signal_call))
                    contentView.setOnClickPendingIntent(R.id.text,establishCall )
                    contentView.setViewVisibility(R.id.hangUpButton, View.GONE)
                    contentView.setViewVisibility(R.id.answerButton, View.GONE)
                }
                TYPE_SCREEN_ON -> {
                    contentView.setTextViewText(R.id.text, context.getString(R.string.CallNotificationBuilder_screen_on))
                    contentView.setViewVisibility(R.id.hangUpButton, View.GONE)
                    contentView.setViewVisibility(R.id.answerButton, View.GONE)
                }
                else -> {
                    val intent = Intent(context, WebRtcCallService::class.java)
                            .setAction(WebRtcCallService.ACTION_LOCAL_HANGUP)

                    val establishCall = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    contentView.setTextViewText(R.id.text, context.getString(R.string.NotificationBarManager_call_in_progress))
                    contentView.setOnClickPendingIntent(R.id.text,establishCall)
                    contentView.setViewVisibility(R.id.hangUpButton, View.GONE)
                    contentView.setViewVisibility(R.id.answerButton, View.GONE)
                }
            }

            return builder
        }

        private fun getServiceNotificationAction(context: Context, action: String, iconResId: Int, titleResId: Int): NotificationCompat.Action {
            val intent = Intent(context, WebRtcCallService::class.java)
                .setAction(action)

            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            return NotificationCompat.Action(iconResId, context.getString(titleResId), pendingIntent)
        }

        private fun getFullScreenPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WebRTCComposeActivity::class.java)
                // When launching the call activity do NOT keep it in the history when finished, as it does not pass through CALL_DISCONNECTED
                // if the call was denied outright, and without this the "dead" activity will sit around in the history when the device is unlocked.
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                .setAction(WebRTCComposeActivity.ACTION_FULL_SCREEN_INTENT)

            return PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun getActivityNotificationAction(context: Context, action: String,
                                                  @DrawableRes iconResId: Int, @StringRes titleResId: Int): NotificationCompat.Action {
            val intent = Intent(context, WebRTCComposeActivity::class.java)
                .setAction(action)

            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            return NotificationCompat.Action(iconResId, context.getString(titleResId), pendingIntent)
        }

    }
}