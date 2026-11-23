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
import com.beldex.libbchat.utilities.recipients.Recipient
import androidx.lifecycle.LifecycleEventObserver
import java.util.concurrent.atomic.AtomicInteger
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object IncomingCallBanner {

    /** Number of started screens (Home, chat) that can show this in-app call card. */
    private val visibleHosts = AtomicInteger(0)

    /**
     * True while a screen that shows the in-app call card is on screen. The call notification then
     * stays quiet (no heads-up, no full-screen intent) so the incoming call is not shown twice.
     */
    val isHostVisible: Boolean
        get() = visibleHosts.get() > 0

    fun bind(
        activity: AppCompatActivity,
        banner: ViewIncomingCallBannerBinding,
        callViewModel: CallViewModel
    ) {
        floatOverScreen(activity, banner.root)
        activity.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> visibleHosts.incrementAndGet()
                Lifecycle.Event.ON_STOP -> visibleHosts.decrementAndGet()
                else -> Unit
            }
        })
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                callViewModel.callState.collect { state -> render(activity, banner, callViewModel, state) }
            }
        }
    }

    /**
     * Shows the card like a heads-up notification: it floats over the top of the screen, just below
     * the status bar, instead of sitting in the screen's layout and pushing the toolbar down.
     */
    private fun floatOverScreen(activity: AppCompatActivity, card: View) {
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (card.parent === content) return
        (card.parent as? ViewGroup)?.removeView(card)
        val density = activity.resources.displayMetrics.density
        val sideMargin = (14 * density).toInt()
        val gap = (8 * density).toInt()
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ).apply {
            marginStart = sideMargin
            marginEnd = sideMargin
            topMargin = gap
        }
        content.addView(card, params)
        card.elevation = 8 * density
        // Keep it under the status bar. Some screens already pad the content view by the status
        // bar height (chat), others leave it edge-to-edge (Home), so only add what is missing.
        ViewCompat.setOnApplyWindowInsetsListener(card) { view, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val lp = view.layoutParams as FrameLayout.LayoutParams
            lp.topMargin = (statusBarTop - content.paddingTop).coerceAtLeast(0) + gap
            view.layoutParams = lp
            insets
        }
        ViewCompat.requestApplyInsets(card)
    }

    private fun render(
        activity: AppCompatActivity,
        banner: ViewIncomingCallBannerBinding,
        callViewModel: CallViewModel,
        state: CallViewModel.State
    ) {
        val incoming = state == CallViewModel.State.CALL_PRE_INIT || state == CallViewModel.State.CALL_RINGING
        if (!incoming) {
            banner.root.isVisible = false
            return
        }
        val recipient = callViewModel.currentRecipient
        val name = recipient?.name?.takeIf { it.isNotBlank() } ?: activity.getString(R.string.call_banner_person)
        banner.incomingCallName.text = name
        loadAvatar(activity, banner, recipient)

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
            banner.root.isVisible = false
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
        recipient: Recipient?
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
