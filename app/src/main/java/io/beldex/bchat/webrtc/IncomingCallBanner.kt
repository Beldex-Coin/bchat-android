package io.beldex.bchat.webrtc

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beldex.libbchat.avatars.ProfileContactPhoto
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ViewIncomingCallBannerBinding
import io.beldex.bchat.service.WebRtcCallService
import io.beldex.bchat.util.AvatarPlaceholderGenerator
import kotlinx.coroutines.launch

object IncomingCallBanner {

    fun bind(
        activity: AppCompatActivity,
        banner: ViewIncomingCallBannerBinding,
        callViewModel: CallViewModel
    ) {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                callViewModel.callState.collect { state -> render(activity, banner, callViewModel, state) }
            }
        }
    }

    private fun render(
        activity: AppCompatActivity,
        banner: ViewIncomingCallBannerBinding,
        callViewModel: CallViewModel,
        state: CallViewModel.State
    ) {
        val incoming = state == CallViewModel.State.CALL_PRE_INIT || state == CallViewModel.State.CALL_RINGING
        val establishing = state == CallViewModel.State.CALL_OUTGOING || state == CallViewModel.State.CALL_INCOMING
        if (!incoming && !establishing) {
            banner.root.isVisible = false
            return
        }
        val recipient = callViewModel.currentRecipient
        val name = recipient?.name?.takeIf { it.isNotBlank() } ?: activity.getString(R.string.call_banner_person)
        banner.incomingCallName.text = name
        loadAvatar(activity, banner, recipient)

        if (incoming) {
            banner.incomingCallState.setText(R.string.NotificationBarManager__incoming_signal_call)
            banner.incomingCallHangup.setText(R.string.hang_up)
            banner.incomingCallAnswer.isVisible = true
            banner.incomingCallHangup.setOnClickListener {
                activity.startService(WebRtcCallService.denyCallIntent(activity))
                banner.root.isVisible = false
            }
            banner.incomingCallAnswer.setOnClickListener {
                val action = if (state == CallViewModel.State.CALL_PRE_INIT) {
                    WebRTCComposeActivity.ACTION_PRE_OFFER
                } else {
                    WebRTCComposeActivity.ACTION_ANSWER
                }
                activity.startActivity(
                    Intent(activity, WebRTCComposeActivity::class.java)
                        .setAction(action)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
            }
        } else {
            banner.incomingCallState.setText(R.string.NotificationBarManager__establishing_signal_call)
            banner.incomingCallHangup.setText(R.string.Permissions_deny)
            banner.incomingCallAnswer.isVisible = false
            banner.incomingCallHangup.setOnClickListener {
                activity.startService(WebRtcCallService.hangupIntent(activity))
                banner.root.isVisible = false
            }
        }
        // Tapping the card itself opens the full-screen call UI.
        banner.root.setOnClickListener {
            activity.startActivity(
                Intent(activity, WebRTCComposeActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
        banner.root.isVisible = true
    }

    private fun loadAvatar(
        activity: AppCompatActivity,
        banner: ViewIncomingCallBannerBinding,
        recipient: com.beldex.libbchat.utilities.recipients.Recipient?
    ) {
        val sizePx = (36 * activity.resources.displayMetrics.density).toInt()
        val placeholder: Drawable = AvatarPlaceholderGenerator.generate(
            activity, sizePx, recipient?.address?.toString() ?: "", recipient?.name
        )
        val photo = recipient?.contactPhoto
        val avatarObject = (photo as? ProfileContactPhoto)?.avatarObject
        val request = Glide.with(activity)
        if (photo != null && avatarObject != "0" && avatarObject != "") {
            request.load(photo)
                .placeholder(placeholder)
                .error(placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .circleCrop()
                .into(banner.incomingCallAvatar)
        } else {
            request.load(placeholder).circleCrop().into(banner.incomingCallAvatar)
        }
    }
}
