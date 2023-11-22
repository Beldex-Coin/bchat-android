package com.thoughtcrimes.securesms.calls

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color.green
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.beldex.libbchat.avatars.ProfileContactPhoto
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Log
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.mms.GlideApp
import com.thoughtcrimes.securesms.permissions.Permissions
import com.thoughtcrimes.securesms.service.WebRtcCallService
import com.thoughtcrimes.securesms.util.AvatarPlaceholderGenerator
import com.thoughtcrimes.securesms.webrtc.AudioManagerCommand
import com.thoughtcrimes.securesms.webrtc.CallViewModel
import com.thoughtcrimes.securesms.webrtc.CallViewModel.State.*
import com.thoughtcrimes.securesms.webrtc.audio.SignalAudioManager.AudioDevice.EARPIECE
import com.thoughtcrimes.securesms.webrtc.audio.SignalAudioManager.AudioDevice.SPEAKER_PHONE
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityWebRtcCallBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DurationFormatUtils


@AndroidEntryPoint
class WebRtcCallActivity : PassphraseRequiredActionBarActivity() {

        companion object {
            const val ACTION_PRE_OFFER = "pre-offer"
            const val ACTION_FULL_SCREEN_INTENT = "fullscreen-intent"
            const val ACTION_ANSWER = "answer"
            const val ACTION_END = "end-call"

            const val BUSY_SIGNAL_DELAY_FINISH = 5500L

            private const val CALL_DURATION_FORMAT = "HH:mm:ss"
        }

        private val viewModel by viewModels<CallViewModel>()
        private val glide by lazy { GlideApp.with(this) }
        private val glide1 by lazy { GlideApp.with(this) }
        private lateinit var binding: ActivityWebRtcCallBinding
        private var uiJob: Job? = null
        private var wantsToAnswer = false
            set(value) {
                field = value
                WebRtcCallService.broadcastWantsToAnswer(this, value)
            }
        private var hangupReceiver: BroadcastReceiver? = null

        //SteveJosephh21
        private var flipCamera:Boolean =true
        private var microPhoneEnable = false

        /*private val rotationListener by lazy {
            object : OrientationEventListener(this) {
                override fun onOrientationChanged(orientation: Int) {
                    if ((orientation + 15) % 90 < 30) {
                        viewModel.deviceRotation = orientation
//                    updateControlsRotation(orientation.quadrantRotation() * -1)
                    }
                }
            }
        }*/

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            if (item.itemId == android.R.id.home) {
                finish()
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        override fun onNewIntent(intent: Intent?) {
            super.onNewIntent(intent)
            if (intent?.action == ACTION_ANSWER) {
                val answerIntent = WebRtcCallService.acceptCallIntent(this)
                ContextCompat.startForegroundService(this, answerIntent)
            }
        }

        override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
            super.onCreate(savedInstanceState, ready)
            //rotationListener.enable()
            binding = ActivityWebRtcCallBinding.inflate(layoutInflater)
            setContentView(binding.root)
            TextSecurePreferences.setCallisActive(this, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
            volumeControlStream = AudioManager.STREAM_VOICE_CALL

            if (intent.action == ACTION_ANSWER) {
                answerCall()
            }
            if (intent.action == ACTION_PRE_OFFER) {
                wantsToAnswer = true
                answerCall() // this will do nothing, except update notification state
            }
            if (intent.action == ACTION_FULL_SCREEN_INTENT) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }

            binding.microphoneButton.setOnClickListener {
               /* microPhoneEnable = !microPhoneEnable

                if (microPhoneEnable) {
                    binding.microphoneButton.setColorFilter(
                        ContextCompat.getColor(
                            this@WebRtcCallActivity,
                            R.color.green
                        )
                    )
                } else {
                    binding.microphoneButton.setColorFilter(
                        ContextCompat.getColor(
                            this@WebRtcCallActivity,
                            R.color.text
                        )
                    )
                }*/
                val audioEnabledIntent =
                    WebRtcCallService.microphoneIntent(this, !viewModel.microphoneEnabled)
                startService(audioEnabledIntent)
            }

            binding.speakerPhoneButton.setOnClickListener {
                val command =
                    AudioManagerCommand.SetUserDevice(if (viewModel.isSpeaker) EARPIECE else SPEAKER_PHONE)
                WebRtcCallService.sendAudioManagerCommand(this, command)
            }

            binding.acceptCallButton.setOnClickListener {
                if (viewModel.currentCallState == CALL_PRE_INIT) {
                    wantsToAnswer = true
                    updateControls()
                }
                answerCall()
            }

            binding.declineCallButton.setOnClickListener {
                val declineIntent = WebRtcCallService.denyCallIntent(this)
                startService(declineIntent)
            }

            hangupReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    binding.dialingStatus.isVisible = false
                    if (!binding.callTime.isVisible) {
                        if (TextSecurePreferences.isRemoteHangup(this@WebRtcCallActivity)) {
                            TextSecurePreferences.setRemoteHangup(this@WebRtcCallActivity, false)
                            callRemoteFinishActivity(getString(R.string.call_ended))
                        }else{
                            callFinishActivity()
                        }
                    } else {
                        if(TextSecurePreferences.isRemoteCallEnded(this@WebRtcCallActivity)) {
                            TextSecurePreferences.setRemoteCallEnded(this@WebRtcCallActivity, false)
                           callRemoteFinishActivity(getString(R.string.call_ended));
                        }else {
                            callFinishActivity()
                        }
                    }
                }
            }

            LocalBroadcastManager.getInstance(this)
                .registerReceiver(hangupReceiver!!, IntentFilter(ACTION_END))

            binding.enableCameraButton.setOnClickListener {
                Permissions.with(this)
                    .request(Manifest.permission.CAMERA)
                    .onAllGranted {
                        val intent = WebRtcCallService.cameraEnabled(this, !viewModel.videoEnabled)
                        startService(intent)
                    }
                    .execute()
            }

            binding.switchCameraButton.setOnClickListener {
                if(binding.enableCameraButton.isSelected) {
                    binding.switchCameraButton.startAnimation(
                        AnimationUtils.loadAnimation(
                            this,
                            R.anim.flip_camera_anim
                        )
                    )
                }
                if(flipCamera && binding.enableCameraButton.isSelected){
                    //binding.switchCameraButton.setColorFilter(ContextCompat.getColor(this,R.color.green))
                    flipCamera=false
                }else{
                    binding.switchCameraButton.setColorFilter(ContextCompat.getColor(this,R.color.text))
                    flipCamera=true
                }
                startService(WebRtcCallService.flipCamera(this))
            }

            binding.endCallButton.setOnClickListener {
                startService(WebRtcCallService.hangupIntent(this))
            }
            binding.backArrow.setOnClickListener {
                onBackPressed()
            }

        }

        //SteveJosephh21
        private fun callFinishActivity(){
            binding.callDeclinedStatus.visibility = View.GONE
            finish()
        }

        private fun callRemoteFinishActivity(text: String) {
            binding.callDeclinedStatus.visibility = View.VISIBLE
            binding.callDeclinedStatus.text = text
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 1000)
        }

        override fun onDestroy() {
            super.onDestroy()
            TextSecurePreferences.setCallisActive(this,false)
            TextSecurePreferences.setMuteVide(this, false)
            hangupReceiver?.let { receiver ->
                LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
            }
            //rotationListener.disable()
        }

        private fun answerCall() {
            val answerIntent = WebRtcCallService.acceptCallIntent(this)
            ContextCompat.startForegroundService(this, answerIntent)
        }

        private fun updateControlsRotation(newRotation: Int) {
            with (binding) {
                val rotation = newRotation.toFloat()
                remoteRecipient.rotation = rotation
                speakerPhoneButton.rotation = rotation
                microphoneButton.rotation = rotation
                enableCameraButton.rotation = rotation
                switchCameraButton.rotation = rotation
                endCallButton.rotation = rotation
            }
        }

        private fun updateControls(state: CallViewModel.State? = null) {
            with(binding) {
                if (state == null) {
                    if (wantsToAnswer) {
                        controlGroup.isVisible = true
                        remoteLoadingView.isVisible = true
                        incomingControlGroup.isVisible = false
                    }
                } else {
                    controlGroup.isVisible = state in listOf(
                        CALL_CONNECTED,
                        CALL_OUTGOING,
                        CALL_INCOMING
                    ) || (state == CALL_PRE_INIT && wantsToAnswer)
                    remoteLoadingView.isVisible =
                        state !in listOf(CALL_CONNECTED,
                            CALL_RINGING, CALL_PRE_INIT
                        ) || wantsToAnswer
                    incomingControlGroup.isVisible =
                        state in listOf(CALL_RINGING,
                            CALL_PRE_INIT
                        ) && !wantsToAnswer
                    reconnectingText.isVisible = state == CALL_RECONNECTING
                    endCallButton.isVisible = endCallButton.isVisible || state == CALL_RECONNECTING
                    when {
                        incomingControlGroup.isVisible -> {
                            statusView.text = getString(R.string.incoming_call)
                        }
                    }
                    //SteveJosephh21
                    if(state == CALL_OUTGOING){
                        binding.statusView.text=getString(R.string.outgoing_call)
                    }
                    else if(state == CALL_INCOMING){
                        binding.statusView.text=getString(R.string.incoming_call)
                    }
                    if(reconnectingText.isVisible) {
                        statusView.text = getString(R.string.end_to_end_encrypted)
                    }
                }
            }
        }

        override fun onStart() {
            super.onStart()

            uiJob = lifecycleScope.launch {

                //SteveJosephh21 --
                launch{
                    viewModel.remoteAudioEnabledState.collect { isEnabled ->
                        if (!isEnabled) {
                            binding.callActivityAudioStatus.visibility=View.VISIBLE
                            if(binding.callActivityVideoStatus.isVisible){
                                binding.callActivityAudioStatus.text = ""
                            }else{
                                binding.callActivityAudioStatus.text = "Call Muted"
                            }
                           Log.d("Remote Audio Enabled","false")
                        }else{
                            binding.callActivityAudioStatus.visibility=View.GONE
                            binding.callActivityAudioStatus.text=""
                            Log.d("Remote Audio Enabled", "true")
                        }
                    }
                }

                //SteveJosephh21 --
                launch{
                    viewModel.remoteVideoStatusEnabledState.collect { isEnabled ->
                        if (isEnabled) {
                            binding.callActivityVideoStatus.visibility=View.VISIBLE
                            if(binding.callActivityAudioStatus.isVisible){
                                binding.callActivityVideoStatus.text = getString(R.string.video_paused_and_microphone_off)
                                binding.callActivityAudioStatus.text = ""
                            }else{
                                binding.callActivityVideoStatus.text = getString(R.string.video_paused)
                            }
                           Log.d("Remote Video Enabled","true")
                        }else{
                            binding.callActivityVideoStatus.visibility=View.GONE
                            binding.callActivityVideoStatus.text=""
                            if(binding.callActivityAudioStatus.isVisible){
                                binding.callActivityAudioStatus.text = getString(R.string.call_muted)
                            }
                            Log.d("Remote Video Enabled", "false")
                        }
                    }
                }

                launch {
                    viewModel.audioDeviceState.collect { state ->
                        val speakerEnabled = state.selectedDevice == SPEAKER_PHONE
                        // change drawable background to enabled or not
                        binding.speakerPhoneButton.isSelected = speakerEnabled
                        //SteveJosephh21
                        if(binding.speakerPhoneButton.isSelected){
                            binding.speakerPhoneButton.setColorFilter(ContextCompat.getColor(this@WebRtcCallActivity,R.color.green))
                        }
                        else{
                            binding.speakerPhoneButton.setColorFilter(ContextCompat.getColor(this@WebRtcCallActivity,R.color.text))
                        }
                    }
                }

                launch {
                    viewModel.callState.collect { state ->
                        Log.d("Beldex", "Consuming view model state $state")
                        when (state) {
                            CALL_RINGING -> {
                                if (wantsToAnswer) {
                                    answerCall()
                                    wantsToAnswer = false
                                }
                            }
                            CALL_OUTGOING -> {
                                //SteveJosephh21
                                binding.statusView.text=getString(R.string.outgoing_call)
                                binding.dialingStatus.isVisible = true
                            }
                            CALL_CONNECTED -> {
                                wantsToAnswer = false
                            }
                            else -> Unit
                        }
                        updateControls(state)
                    }
                }

                launch {
                    viewModel.recipient.collect { latestRecipient ->
                        if (latestRecipient.recipient != null) {
                            val publicKey = latestRecipient.recipient.address.serialize()
                            val displayName = getUserDisplayName(publicKey)
                            supportActionBar?.title = displayName
                            val signalProfilePicture = latestRecipient.recipient.contactPhoto
                            val avatar = (signalProfilePicture as? ProfileContactPhoto)?.avatarObject
                            val sizeInPX =
                                resources.getDimensionPixelSize(R.dimen.extra_large_profile_picture_size)
                            binding.remoteRecipientName.text = displayName
                            if (signalProfilePicture != null && avatar != "0" && avatar != "") {
                                glide.clear(binding.remoteRecipient)
                                glide.load(signalProfilePicture)
                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                    .circleCrop()
                                    .error(
                                        AvatarPlaceholderGenerator.generate(
                                            this@WebRtcCallActivity,
                                            sizeInPX,
                                            publicKey,
                                            displayName
                                        )
                                    )
                                    .into(binding.remoteRecipient)
                            } else {
                                glide.clear(binding.remoteRecipient)
                                glide.load(
                                    AvatarPlaceholderGenerator.generate(
                                        this@WebRtcCallActivity,
                                        sizeInPX,
                                        publicKey,
                                        displayName
                                    )
                                )
                                    .diskCacheStrategy(DiskCacheStrategy.ALL).circleCrop()
                                    .into(binding.remoteRecipient)
                            }
                        } else {
                            glide.clear(binding.remoteRecipient)
                        }
                    }
                }

                launch {
                    while (isActive) {
                        val startTime = viewModel.callStartTime
                        if (startTime == -1L) {
                            binding.callTime.isVisible = false
                            //SteveJosephh21
                            binding.microphoneButton.isClickable =false
                            binding.microphoneButton.alpha=0.1f
                            //SteveJosephh21 --
                            binding.callActivityAudioStatus.text = ""
                        } else {
                            binding.callTime.isVisible = true
                            //SteveJosephh21
                            binding.dialingStatus.isVisible = false
                            binding.microphoneButton.isClickable =true
                            binding.microphoneButton.alpha=1.0f

                            binding.callTime.text = DurationFormatUtils.formatDuration(
                                System.currentTimeMillis() - startTime,
                                CALL_DURATION_FORMAT
                            )
                            //SteveJosephh21
                            if(binding.remoteRecipientName.isVisible){
                                binding.statusView.text=getString(R.string.end_to_end_encrypted)
                            }
                        }

                        delay(1_000)
                    }
                }

                launch {
                    viewModel.localAudioEnabledState.collect { isEnabled ->
                        // change drawable background to enabled or not
                        binding.microphoneButton.isSelected = isEnabled
                        //SteveJosephh21
                            if (binding.microphoneButton.isSelected) {
                                binding.microphoneButton.setColorFilter(
                                    ContextCompat.getColor(
                                        this@WebRtcCallActivity,
                                        R.color.text
                                    )
                                )
                            } else {
                                binding.microphoneButton.setColorFilter(
                                    ContextCompat.getColor(
                                        this@WebRtcCallActivity,
                                        R.color.red
                                    )
                                )
                            }
                    }
                }

                launch {
                    viewModel.localVideoEnabledState.collect { isEnabled ->
                        binding.localRenderer.removeAllViews()
                        if (isEnabled) {
                            viewModel.localRenderer?.let { surfaceView ->
                                surfaceView.setZOrderOnTop(true)
                                binding.localRenderer.addView(surfaceView)
                            }
                        }
                        binding.localRenderer.isVisible = isEnabled
                        binding.enableCameraButton.isSelected = isEnabled
                        //SteveJosephh21
                        if(isEnabled){
                            binding.enableCameraButton.setColorFilter(ContextCompat.getColor(this@WebRtcCallActivity,R.color.green))
                            flipCamera =true
                        }
                        else{
                            binding.enableCameraButton.setColorFilter(ContextCompat.getColor(this@WebRtcCallActivity,R.color.text))
                            binding.switchCameraButton.setColorFilter(ContextCompat.getColor(this@WebRtcCallActivity,R.color.text))
                            flipCamera=false
                        }
                    }
                }

                launch {
                    viewModel.remoteVideoEnabledState.collect { isEnabled ->
                        binding.remoteRenderer.removeAllViews()
                        if (isEnabled) {
                            viewModel.remoteRenderer?.let { surfaceView ->
                                binding.remoteRenderer.addView(surfaceView)
                            }
                        }
                        binding.remoteRenderer.isVisible = isEnabled
                        binding.remoteRecipient.isVisible = !isEnabled

                        //SteveJosephh21
                        binding.remoteRecipientName.isVisible = !isEnabled
                        if(!binding.remoteRecipientName.isVisible){
                            binding.statusView.text=binding.remoteRecipientName.text.toString()
                        }
                    }
                }
            }
        }

        private fun getUserDisplayName(publicKey: String): String {
            val contact =
                DatabaseComponent.get(this).bchatContactDatabase().getContactWithBchatID(publicKey)
            return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
        }

        override fun onStop() {
            super.onStop()
            uiJob?.cancel()
            binding.remoteRenderer.removeAllViews()
            binding.localRenderer.removeAllViews()
        }
}