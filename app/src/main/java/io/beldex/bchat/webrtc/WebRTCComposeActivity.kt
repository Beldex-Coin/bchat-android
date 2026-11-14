package io.beldex.bchat.webrtc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.hexEncodedPublicKey
import com.google.android.gms.common.util.CollectionUtils.listOf
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.bchatBackground
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.crypto.IdentityKeyUtil
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.permissions.Permissions
import io.beldex.bchat.service.WebRtcCallService
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.webrtc.audio.SignalAudioManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DurationFormatUtils
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import io.beldex.bchat.compose_utils.BChatDimens


@AndroidEntryPoint
class WebRTCComposeActivity : BaseComponentActivity() {

    private var hangupReceiver: BroadcastReceiver? = null
    val viewModel:CallViewModel by viewModels()

    val wantsToAnswer :MutableState<Boolean> = mutableStateOf(false)
    private val isInPictureInPictureMode :MutableState<Boolean> = mutableStateOf(false)
    private val isLandscapeState: MutableState<Boolean> = mutableStateOf(false)

    private val hexEncodedPublicKey: String
        get() {
            return getUserLocalPublicKey()
        }

    private fun getUserLocalPublicKey(): String {
        TextSecurePreferences.getLocalNumber(this)?.takeIf { it.isNotBlank() }?.let {
            return it
        }

        val userPublicKey = IdentityKeyUtil.getIdentityKeyPair(this).hexEncodedPublicKey
        TextSecurePreferences.setLocalNumber(this, userPublicKey)
        return userPublicKey
    }

    private var pipBuilderParams: PictureInPictureParams.Builder? = null

    private fun isSystemPipEnabledAndAvailable(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // switch to PiP mode if the user presses the home or recent button,
        if(isSystemPipEnabledAndAvailable()) {
            try {
                pipBuilderParams?.let { enterPictureInPictureMode(it.build()) }
            } catch (e: Exception) {
                Log.w(TAG, "System lied about having PiP available.", e)
            }
        }
    }

    private fun isScreenLandscape(config: Configuration) =
        config.screenWidthDp >= config.screenHeightDp

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isLandscapeState.value = isScreenLandscape(newConfig)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (lifecycle.currentState == Lifecycle.State.CREATED) {
            //user clicked on close button of PiP window
            finishAndRemoveTask()
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        isLandscapeState.value = isScreenLandscape(resources.configuration)
        if (isSystemPipEnabledAndAvailable()) {
            pipBuilderParams = PictureInPictureParams.Builder()
            pipBuilderParams!!.setAspectRatio(Rational(9,16))
        }
        addOnPictureInPictureModeChangedListener { info: PictureInPictureModeChangedInfo ->
            isInPictureInPictureMode.value = info.isInPictureInPictureMode
        }
        if (intent.action == ACTION_ANSWER) {
            answerCall()
        }
        if (intent.action == ACTION_PRE_OFFER) {
            wantsToAnswer.value = true
            answerCall() // this will do nothing, except update notification state
        }
        if (intent.action == ACTION_FULL_SCREEN_INTENT) {
            this.actionBar?.setDisplayHomeAsUpEnabled(false)
        }
        setContent {
            val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            val view = LocalView.current
            val window = (view.context as Activity).window
            val statusBarColor = if (isDarkTheme) Color.Black else Color.White
            SideEffect {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    window.statusBarColor = statusBarColor.toArgb()
                }
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }
            BChatTheme(darkTheme= isDarkTheme) {
                Surface(
                    modifier=Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(WindowInsets.systemBars.asPaddingValues()),
                ) {
                    Scaffold(
                        containerColor=Color.Transparent,
                    ) {
                        WebRtcCallScreen(wantsToAnswer.value, hexEncodedPublicKey,isInPictureInPictureMode.value)
                    }
                }
            }
        }
    }

    private fun answerCall() {
        val answerIntent = WebRtcCallService.acceptCallIntent(this)
        answerIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        ContextCompat.startForegroundService(this, answerIntent)
    }

    companion object {
        const val ACTION_PRE_OFFER="pre-offer"
        const val ACTION_FULL_SCREEN_INTENT="fullscreen-intent"
        const val ACTION_ANSWER="answer"
        const val ACTION_END="end-call"
        const val CALL_DURATION_FORMAT="HH:mm:ss"
        var CALL_DURATION="0"
        const val TAG = "WebRTCComposeActivity"

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_ANSWER) {
            answerCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TextSecurePreferences.setCallisActive(this,false)
        TextSecurePreferences.setMuteVide(this, false)
        hangupReceiver?.let { receiver ->
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        }
    }

    @Composable
    fun WebRtcCallScreen(wantToAnswer: Boolean = false, localUser: String = "", isInPictureInPictureMode: Boolean) {
        val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT
        val callViewModel : CallViewModel=hiltViewModel()
        val context=LocalContext.current
        val lifecycleOwner=LocalLifecycleOwner.current
        val isLandscape=isLandscapeState.value
        val profilePictureSize=if (isLandscape) 112.dp else 194.dp
        val profilePictureInnerSize=if (isLandscape) 92.dp else 132.dp
        val profileSize=132.dp

        var wantsToAnswer by remember {
            mutableStateOf(wantToAnswer)
        }
        val lockDescription="end to end encryption"
        val answerCallDescription="answer call"
        val declineCallDescription="decline call"
        val endCallDescription="end call"
        val enableVideoDescription="enable video"
        val switchCamDescription="switch camera"
        val muteDescription="mute call"
        val speakerDescription="speak call"
        val bluetoothDescription="bluetooth call"

        var surfaceView : SurfaceView?=null

        var isRemoteSurfaceView by remember {
            mutableStateOf(surfaceView)
        }
        var isLocalSurfaceView by remember {
            mutableStateOf(surfaceView)
        }

        val localUserPublicKey by remember {
            mutableStateOf(localUser)
        }

        var videoSwapped by remember {
            mutableStateOf(false)
        }

        var isShowAnswerOption by remember {
            mutableStateOf(false)
        }
        var isShowDeclineOption by remember {
            mutableStateOf(false)
        }
        var isShowEndCallOption by remember {
            mutableStateOf(true)
        }
        var isCallEnding by remember {
            mutableStateOf(false)
        }
        var isShowVideoOption by remember {
            mutableStateOf(false)
        }
        var isSelectedVideoOption by remember {
            mutableStateOf(false)
        }
        var isShowSwitchCameraOption by remember {
            mutableStateOf(false)
        }
        var flipCamera by remember {
            mutableStateOf(true)
        }
        var isShowMuteOption by remember {
            mutableStateOf(false)
        }
        var isMuteOptionIconChange by remember {
            mutableStateOf(false)
        }
        var isMuteOptionClickable by remember {
            mutableStateOf(false)
        }
        var isSelectedMuteOption by remember {
            mutableStateOf(false)
        }
        var isShowSpeakerOption by remember {
            mutableStateOf(false)
        }
        var isSpeakerOptionColorChange by remember {
            mutableStateOf(false)
        }
        var isSpeakerIsSelected by remember {
            mutableStateOf(false)
        }
        var isShowCallConnecting by remember {
            mutableStateOf(false)
        }
        var isShowReConnecting by remember {
            mutableStateOf(false)
        }
        var isStatusReConnectingText by remember {
            mutableStateOf(context.getString(R.string.WebRtcCallActivity_Reconnecting))
        }
        var isStatusText by remember {
            mutableStateOf(context.getString(R.string.voice_call))
        }

        var isShowDialingStatus by remember {
            mutableStateOf(false)
        }
        var isStatusDialingText by remember {
            mutableStateOf("")
        }

        var isShowIncomingStatus by remember {
            mutableStateOf(false)
        }
        val isStatusInComingText by remember {
            mutableStateOf(context.getString(R.string.incoming_call))
        }
        var isShowCallDurationStatus by remember {
            mutableStateOf(false)
        }
        var isStatusCallDurationText by remember {
            mutableStateOf("")
        }

        var isShowCallDeclineStatus by remember {
            mutableStateOf(false)
        }
        var isStatusCallDeclineText by remember {
            mutableStateOf("")
        }

        var isShowCallAudioStatus by remember {
            mutableStateOf(false)
        }
        var isStatusCallAudioText by remember {
            mutableStateOf("")
        }

        var isShowCallVideoStatus by remember {
            mutableStateOf(false)
        }
        var isStatusCallVideoText by remember {
            mutableStateOf("")
        }
        var isPersonNameText by remember {
            mutableStateOf("")
        }
        var localUserName by remember {
            mutableStateOf("")
        }
        var recipientPublicKey by remember {
            mutableStateOf("")
        }

        var remoteVideoView by remember {
            mutableStateOf(false)
        }
        var localVideoView by remember {
            mutableStateOf(false)
        }
        var showVideoCameOff by remember {
            mutableStateOf(false)
        }
        var callLoading by remember {
            mutableStateOf(false)
        }
        var isSwitchCameraFlipEnabled by remember{
            mutableStateOf(false)
        }

        val composition by rememberLottieComposition(
            LottieCompositionSpec
                .RawRes(R.raw.call_connect)
        )
        val isPlaying by remember {
            mutableStateOf(true)
        }
        // for speed
        val speed by remember {
            mutableFloatStateOf(1f)
        }

        val progress by animateLottieCompositionAsState(
            composition,
            iterations = LottieConstants.IterateForever,
            isPlaying = isPlaying,
            speed = speed,
            restartOnPlay = false
        )


        var expanded by remember {
            mutableStateOf(false)
        }
        var isBluetoothIsSelected by remember{
            mutableStateOf(false)
        }
        var isBluetoothIsConnected by remember{
            mutableStateOf(false)
        }
        hangupReceiver=remember {
            object : BroadcastReceiver() {
                override fun onReceive(p0 : Context?, p1 : Intent?) {
                    isCallEnding=true
                    isShowDialingStatus=false
                    if (!isShowCallDurationStatus) {
                        if (TextSecurePreferences.isRemoteHangup(context)) {
                            TextSecurePreferences.setRemoteHangup(context, false)
                            isShowCallDeclineStatus=true
                            isStatusCallDeclineText=context.getString(R.string.call_ended)
                            Handler(Looper.getMainLooper()).postDelayed({
                                finish()
                            }, 1000)
                        } else {
                            isShowCallDeclineStatus=false
                            (context as Activity).finish()
                        }
                    } else {
                        if (TextSecurePreferences.isRemoteCallEnded(context)) {
                            TextSecurePreferences.setRemoteCallEnded(context, false)
                            isShowCallDeclineStatus=true
                            isStatusCallDeclineText=context.getString(R.string.call_ended)
                            Handler(Looper.getMainLooper()).postDelayed({
                                finish()
                            }, 1000)
                        } else {
                            isShowCallDeclineStatus=false
                            finish()
                        }
                    }
                }


            }
        }

        fun outgoingControl(isVisible : Boolean) {
            isShowVideoOption=isVisible
            isShowSwitchCameraOption=isVisible
            isShowMuteOption=isVisible
            isShowSpeakerOption=isVisible
            isShowEndCallOption=isVisible

        }

        fun incomingControl(isVisible : Boolean) {
            isShowAnswerOption=isVisible
            isShowDeclineOption=isVisible
            isShowIncomingStatus=isVisible
            isShowEndCallOption=false

        }


        fun updateControls(state : CallViewModel.State?=null) {
            if (state == null) {
                if (wantsToAnswer) {
                    outgoingControl(true)
                    isShowCallConnecting=true
                    incomingControl(false)
                }
            } else {
                isShowVideoOption=state in listOf(CallViewModel.State.CALL_CONNECTED, CallViewModel.State.CALL_OUTGOING, CallViewModel.State.CALL_INCOMING) || (state == CallViewModel.State.CALL_PRE_INIT && wantsToAnswer)
                callLoading=state !in listOf(CallViewModel.State.CALL_CONNECTED, CallViewModel.State.CALL_RINGING, CallViewModel.State.CALL_PRE_INIT) || wantsToAnswer
                isShowSwitchCameraOption=state in listOf(CallViewModel.State.CALL_CONNECTED, CallViewModel.State.CALL_OUTGOING, CallViewModel.State.CALL_INCOMING) || (state == CallViewModel.State.CALL_PRE_INIT && wantsToAnswer)
                isShowMuteOption=state in listOf(CallViewModel.State.CALL_CONNECTED, CallViewModel.State.CALL_OUTGOING, CallViewModel.State.CALL_INCOMING) || (state == CallViewModel.State.CALL_PRE_INIT && wantsToAnswer)
                isShowSpeakerOption=state in listOf(CallViewModel.State.CALL_CONNECTED, CallViewModel.State.CALL_OUTGOING, CallViewModel.State.CALL_INCOMING) || (state == CallViewModel.State.CALL_PRE_INIT && wantsToAnswer)
                isShowCallConnecting=state !in listOf(CallViewModel.State.CALL_CONNECTED, CallViewModel.State.CALL_RINGING, CallViewModel.State.CALL_PRE_INIT) || wantsToAnswer
                isShowAnswerOption=state in listOf(CallViewModel.State.CALL_RINGING, CallViewModel.State.CALL_PRE_INIT) && !wantsToAnswer
                isShowDeclineOption=state in listOf(CallViewModel.State.CALL_RINGING, CallViewModel.State.CALL_PRE_INIT) && !wantsToAnswer
                isShowIncomingStatus=state in listOf(CallViewModel.State.CALL_RINGING, CallViewModel.State.CALL_PRE_INIT) && !wantsToAnswer
                isShowReConnecting=state == CallViewModel.State.CALL_RECONNECTING
                isShowEndCallOption=!(state in listOf(CallViewModel.State.CALL_RINGING, CallViewModel.State.CALL_PRE_INIT) && !wantsToAnswer)
                isShowEndCallOption=isShowEndCallOption == true || state == CallViewModel.State.CALL_RECONNECTING
                isBluetoothIsConnected  = state == CallViewModel.State.CALL_DISCONNECTED
            }
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(hangupReceiver as BroadcastReceiver, IntentFilter(ACTION_END))

        fun enableCamera() {
            Permissions.with(context as Activity).request(Manifest.permission.CAMERA).onAllGranted {
                val intent=WebRtcCallService.cameraEnabled(context, !callViewModel.videoState.value.userVideoEnabled)
                context.startService(intent)
            }.execute()
        }

        fun switchCamera() {
            flipCamera = !(flipCamera && isShowVideoOption)
            context.startService(WebRtcCallService.flipCamera(context))
        }

        fun enableMuteOption() {
            val audioEnabledIntent=WebRtcCallService.microphoneIntent(context, !callViewModel.microphoneEnabled)
            context.startService(audioEnabledIntent)
        }

        fun getUserDisplayName(publicKey : String) : String {
            val contact=DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
            return contact?.displayName(Contact.ContactContext.REGULAR, context) ?: publicKey
        }

        fun getUserDisplayNameOrShortestPublicKey(publicKey : String) : String {
            val contact=DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
            return contact?.displayName(Contact.ContactContext.REGULAR, context) ?: "${publicKey.take(4)}...${publicKey.takeLast(4)}"
        }

        fun showLocalUserDetailsInFullScreen() : Boolean {
            return !isSwitchCameraFlipEnabled && videoSwapped && localVideoView
        }

        fun showLocalUserDetailsInSmallScreen() : Boolean {
            return !isSwitchCameraFlipEnabled && !videoSwapped
        }

        val scope=rememberCoroutineScope()

        LaunchedEffect("key") {
            scope.launch {
                launch {
                    callViewModel.remoteAudioEnabledState.collect { isEnabled ->
                        if (!isEnabled) {
                            isShowCallAudioStatus=true
                            isStatusCallAudioText=if (isShowCallVideoStatus) {
                                ""
                            } else {
                                getString(R.string.call_muted)
                            }
                        } else {
                            isShowCallAudioStatus=false
                            isStatusCallAudioText=""
                        }
                    }
                }
                launch {
                    callViewModel.remoteVideoStatusEnabledState.collect { isEnabled ->
                        if (isEnabled) {
                            isShowCallVideoStatus=true
                            if (isShowCallAudioStatus) {
                                isStatusCallVideoText=context.getString(R.string.video_paused_and_microphone_off)
                                isStatusCallAudioText=""
                            } else {
                                isStatusCallVideoText=context.getString(R.string.video_paused)
                            }
                        } else {
                            isShowCallVideoStatus=false
                            isStatusCallVideoText=""
                            if (isShowCallAudioStatus) {
                                isStatusCallAudioText=context.getString(R.string.call_muted)
                            }
                        }
                    }
                }
                launch {
                    callViewModel.audioDeviceState.collect { state ->
                        val speakerEnabled=state.selectedDevice == SignalAudioManager.AudioDevice.SPEAKER_PHONE
                        isSpeakerIsSelected=speakerEnabled
                        if (isSpeakerIsSelected) {
                            isSpeakerOptionColorChange=true
                        } else {
                            isSpeakerOptionColorChange=true
                        }
                    }
                }
                launch {
                    callViewModel.audioBluetoothDeviceState.collect{ state ->
                        isBluetoothIsSelected = state.selectedDevice == SignalAudioManager.AudioDevice.BLUETOOTH
                    }
                }

                launch {
                    callViewModel.bluetoothConnectionState.observe(lifecycleOwner) { newValue ->
                        isBluetoothIsConnected=newValue
                    }
                }

                launch {
                    callViewModel.callState.collect { state ->
                        when (state) {
                            CallViewModel.State.CALL_RINGING -> {
                                if (wantsToAnswer) {
                                    answerCall(context)
                                    wantsToAnswer=false
                                }
                            }

                            CallViewModel.State.CALL_OUTGOING -> {
                                isShowDialingStatus=true
                                isStatusDialingText=context.getString(R.string.calling)
                            }

                            CallViewModel.State.CALL_CONNECTED -> {
                                wantsToAnswer=false
                            }

                            else -> Unit
                        }
                        updateControls(state)
                    }
                }

                launch {
                    callViewModel.recipient.collect { latestRecipient ->
                        if (latestRecipient.recipient != null) {
                            recipientPublicKey=latestRecipient.recipient.address.serialize()
                            val displayName=getUserDisplayNameOrShortestPublicKey(recipientPublicKey)
                            isPersonNameText=displayName
                            localUserName = getUserDisplayNameOrShortestPublicKey(localUserPublicKey)
                            val signalProfilePicture=latestRecipient.recipient.contactPhoto
                        }
                    }
                }

                launch {
                    while (isActive) {
                        val startTime=callViewModel.callStartTime
                        if(callViewModel.bluetoothConnectionStatus){
                            callViewModel.setBooleanValue(callViewModel.bluetoothConnectionStatus)
                        }else{
                            callViewModel.setBooleanValue(callViewModel.bluetoothConnectionStatus)
                        }
                        if (startTime == -1L) {
                            isShowCallDurationStatus=false
                            isMuteOptionClickable=false
                            isStatusCallAudioText=""
                        } else {
                            isShowCallDurationStatus=true
                            isShowDialingStatus=false
                            isMuteOptionClickable=true

                            val elapsedMs=System.currentTimeMillis() - startTime
                            isStatusCallDurationText=DurationFormatUtils.formatDuration(elapsedMs, if (elapsedMs >= 3_600_000L) "H:mm:ss" else "m:ss")
                            CALL_DURATION=isStatusCallDurationText
                        }
                        delay(1_000)
                    }
                }

                launch {
                    callViewModel.localAudioEnabledState.collect { isEnabled ->
                        isSelectedMuteOption=isEnabled//Need to change microPhone color to Red
                        isMuteOptionIconChange=!isSelectedMuteOption
                    }
                }

                // handle video state
                launch {
                    callViewModel.videoState.collect { state ->
                        videoSwapped = state.swapped
                        localVideoView= state.userVideoEnabled || state.remoteVideoEnabled
                        isSwitchCameraFlipEnabled=state.userVideoEnabled
                        isStatusText=if (state.userVideoEnabled) {
                            context.getString(R.string.video_call)
                        } else {
                            context.getString(R.string.voice_call)
                        }
                        if (state.showFloatingVideo()) {
                            callViewModel.floatingRenderer?.let { sfView ->
                                isLocalSurfaceView?.setZOrderOnTop(false)
                                isLocalSurfaceView?.setZOrderMediaOverlay(true)
                                isLocalSurfaceView=sfView
                            }
                            showVideoCameOff = false
                        } else {
                            showVideoCameOff = true
                        }
                        isSelectedVideoOption=state.userVideoEnabled
                        flipCamera = state.userVideoEnabled

                        remoteVideoView=state.showFullscreenVideo()
                        if (state.showFullscreenVideo()) {
                            callViewModel.fullscreenRenderer?.let { sfView ->
                                isRemoteSurfaceView=sfView
                            }
                        }
                    }
                }
            }
        }
        //UI started (Revamp_2026 call screens: Figma Call / Call_1 / Call_2 / Video_call_2)
        val onBackClick: () -> Unit = { (context as ComponentActivity).finish() }
        val displayedKey = if (showLocalUserDetailsInFullScreen()) localUserPublicKey else recipientPublicKey
        val displayedName = if (showLocalUserDetailsInFullScreen()) localUserName else isPersonNameText
        val statusLine = when {
            isCallEnding -> if (isShowCallDeclineStatus) isStatusCallDeclineText else context.getString(R.string.call_ended)
            isShowReConnecting -> isStatusReConnectingText
            isShowCallDurationStatus -> isStatusCallDurationText
            isShowDialingStatus -> isStatusDialingText
            isShowIncomingStatus -> isStatusInComingText
            isShowCallConnecting -> context.getString(R.string.CallNotificationBuilder_connecting)
            else -> ""
        }
        val avatarFrameSize = if (isLandscape) profilePictureSize else if (isShowCallDurationStatus) 194.dp else 174.dp
        val toggleSpeaker: () -> Unit = {
            if (isBluetoothIsConnected) {
                expanded = !expanded
            } else {
                WebRtcCallService.sendAudioManagerCommand(
                    context,
                    AudioManagerCommand.SetUserDevice(
                        if (callViewModel.isSpeaker) SignalAudioManager.AudioDevice.EARPIECE else SignalAudioManager.AudioDevice.SPEAKER_PHONE
                    )
                )
            }
        }
        val hangUp: () -> Unit = {
            isCallEnding = true
            context.startService(WebRtcCallService.hangupIntent(context))
        }
        val answer: () -> Unit = {
            if (callViewModel.currentCallState == CallViewModel.State.CALL_PRE_INIT) {
                wantsToAnswer = true
                updateControls()
            }
            answerCall(context)
        }
        val decline: () -> Unit = {
            isCallEnding = true
            context.startService(WebRtcCallService.denyCallIntent(context))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .bchatBackground(MaterialTheme.appColors.backgroundColor)
        ) {
            if (isInPictureInPictureMode) {
                if (!remoteVideoView) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CallAvatarFrame(
                            publicKey = displayedKey,
                            displayName = getUserDisplayName(displayedKey),
                            outerSize = 60.dp,
                            innerSize = 48.dp,
                            audioOff = false
                        )
                    }
                } else {
                    Surface(
                        color = Color.Black,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        isRemoteSurfaceView?.let {
                            VideoCallSurfaceView(surfaceView = it)
                        }
                    }
                }
            } else {
                CallTopBar(
                    title = isStatusText,
                    onBack = onBackClick,
                    overVideo = remoteVideoView
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (!remoteVideoView) {
                        if (isLandscape) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CallAvatarFrame(
                                        publicKey = displayedKey,
                                        displayName = getUserDisplayName(displayedKey),
                                        outerSize = avatarFrameSize,
                                        innerSize = profilePictureInnerSize,
                                        audioOff = isShowCallAudioStatus
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    EncryptedPill(lockDescription)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CallPersonName(displayedName)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CallStatusText(statusLine)
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))
                                EncryptedPill(lockDescription)
                                Spacer(modifier = Modifier.height(24.dp))
                                CallAvatarFrame(
                                    publicKey = displayedKey,
                                    displayName = getUserDisplayName(displayedKey),
                                    outerSize = avatarFrameSize,
                                    innerSize = profilePictureInnerSize,
                                    audioOff = isShowCallAudioStatus
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                CallPersonName(displayedName)
                                Spacer(modifier = Modifier.weight(1f))
                                CallStatusText(statusLine)
                                Spacer(modifier = Modifier.height(28.dp))
                            }
                        }
                    } else {
                        // Remote video fills the body; name and duration sit over the lower part of it.
                        Surface(
                            color = Color.Black,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            isRemoteSurfaceView?.let {
                                VideoCallSurfaceView(surfaceView = it)
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        ) {
                            CallPersonName(displayedName, overVideo = true)
                            Spacer(modifier = Modifier.height(4.dp))
                            CallStatusText(statusLine, overVideo = true)
                        }
                    }

                    if (localVideoView) {
                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .width(116.dp)
                                .padding(10.dp)
                                .align(Alignment.BottomEnd)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            if (showVideoCameOff) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = MaterialTheme.appColors.actionIconBackground,
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .clickable { callViewModel.swapVideos() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    ProfilePictureComponent(
                                        publicKey = if (showLocalUserDetailsInSmallScreen()) localUserPublicKey else recipientPublicKey,
                                        displayName = getUserDisplayName(if (showLocalUserDetailsInSmallScreen()) localUserPublicKey else recipientPublicKey),
                                        containerSize = 40.dp,
                                        pictureMode = ProfilePictureMode.SmallPicture
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color = Color.Black, shape = RoundedCornerShape(12.dp))
                                        .clickable { callViewModel.swapVideos() },
                                ) {
                                    isLocalSurfaceView?.let {
                                        VideoCallSurfaceView(surfaceView = it)
                                    }
                                }
                            }
                        }
                    }

                    if (expanded) {
                        AudioDeviceCard(
                            bluetoothSelected = isBluetoothIsSelected,
                            onBluetooth = {
                                WebRtcCallService.sendAudioManagerCommand(
                                    context,
                                    AudioManagerCommand.SetUserDevice(SignalAudioManager.AudioDevice.BLUETOOTH)
                                )
                                expanded = false
                            },
                            onMobileSpeaker = {
                                WebRtcCallService.sendAudioManagerCommand(
                                    context,
                                    AudioManagerCommand.SetUserDevice(SignalAudioManager.AudioDevice.SPEAKER_PHONE)
                                )
                                expanded = false
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }

                if (!isCallEnding && isShowAnswerOption && isShowDeclineOption) {
                    // Incoming: Decline on the left, Accept on the right, each with its label (Figma Call_2).
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 68.dp, end = 68.dp, bottom = 48.dp)
                    ) {
                        CallActionButton(
                            label = stringResource(id = R.string.decline),
                            icon = R.drawable.ic_fig_call_decline,
                            fill = MaterialTheme.appColors.callDeclineBackground,
                            stroke = MaterialTheme.appColors.callDeclineBorder,
                            tint = Color(0xFFFF3E3E),
                            contentDescription = declineCallDescription,
                            onClick = decline
                        )
                        CallActionButton(
                            label = stringResource(id = R.string.accept),
                            icon = R.drawable.ic_fig_call_accept,
                            fill = MaterialTheme.appColors.callAcceptBackground,
                            stroke = MaterialTheme.appColors.callAcceptBorder,
                            tint = MaterialTheme.appColors.userDetailsBchatIdText,
                            contentDescription = answerCallDescription,
                            onClick = answer
                        )
                    }
                } else if (!isCallEnding && (isShowVideoOption || isShowSwitchCameraOption || isShowMuteOption || isShowSpeakerOption || isShowReConnecting)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.appColors.callBottomBackground)
                            .border(width = 0.5.dp, color = MaterialTheme.appColors.callTopBarBorder)
                            .padding(horizontal = 23.dp, vertical = 14.dp)
                    ) {
                        if (isShowSpeakerOption) {
                            CallControlButton(
                                icon = if (isBluetoothIsSelected && isBluetoothIsConnected) R.drawable.ic_fig_call_bluetooth else R.drawable.ic_fig_call_speaker,
                                tint = if (isSpeakerIsSelected || (isBluetoothIsSelected && isBluetoothIsConnected)) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.callContentColor,
                                contentDescription = speakerDescription,
                                onClick = toggleSpeaker
                            )
                        }
                        if (isShowMuteOption) {
                            CallControlButton(
                                icon = R.drawable.ic_fig_call_mic,
                                tint = if (isMuteOptionIconChange) MaterialTheme.appColors.negativeRedButtonBorder else MaterialTheme.appColors.callContentColor,
                                contentDescription = muteDescription,
                                enabled = isMuteOptionClickable,
                                onClick = { enableMuteOption() }
                            )
                        }
                        if (isShowSwitchCameraOption) {
                            CallControlButton(
                                icon = R.drawable.ic_fig_call_flip,
                                tint = MaterialTheme.appColors.callContentColor,
                                contentDescription = switchCamDescription,
                                enabled = isSwitchCameraFlipEnabled,
                                onClick = { switchCamera() }
                            )
                        }
                        if (isShowVideoOption) {
                            CallControlButton(
                                icon = R.drawable.ic_fig_call_video,
                                tint = if (isSelectedVideoOption) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.callContentColor,
                                contentDescription = enableVideoDescription,
                                onClick = { if (isShowCallDurationStatus) enableCamera() }
                            )
                        }
                        if (isShowEndCallOption) {
                            CallControlButton(
                                icon = R.drawable.ic_fig_call_end,
                                tint = Color(0xFFFF3E3E),
                                fill = MaterialTheme.appColors.callEndBackground,
                                stroke = MaterialTheme.appColors.negativeRedButtonBorder,
                                contentDescription = endCallDescription,
                                onClick = hangUp
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CallTopBar(title: String, onBack: () -> Unit, overVideo: Boolean) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (overVideo) MaterialTheme.appColors.messageRequestItemBackground else MaterialTheme.appColors.messageRequestItemBackground.copy(alpha = 0.9f))
                .border(width = 0.5.dp, color = MaterialTheme.appColors.callTopBarBorder)
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back_arrow),
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.appColors.callContentColor,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = MaterialTheme.appColors.callContentColor,
                fontFamily = OpenSans,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }

    @Composable
    private fun EncryptedPill(lockDescription: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.appColors.onboardingClearButtonBackground, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_fig_call_lock),
                contentDescription = lockDescription,
                tint = MaterialTheme.appColors.callEncryptedText,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(id = R.string.end_to_end_encrypted),
                color = MaterialTheme.appColors.callEncryptedText,
                fontFamily = RobotoMono,
                fontSize = 11.sp
            )
        }
    }

    /**
     * Notched profile frame (Figma 194x194 with a 132 photo). When the other person mutes their
     * microphone, a round "muted" badge sits on the bottom edge of the photo (Figma Call / Call_1).
     * It replaces the old "Call muted" status text; video-paused has no badge.
     */
    @Composable
    private fun CallAvatarFrame(
        publicKey: String,
        displayName: String,
        outerSize: Dp,
        innerSize: Dp,
        audioOff: Boolean
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(outerSize)
                .background(MaterialTheme.appColors.callAvatarBackground, notchedCornerShape(BChatDimens.NotchCard))
                .border(1.dp, MaterialTheme.appColors.callAvatarBorder, notchedCornerShape(BChatDimens.NotchCard))
        ) {
            ProfilePictureComponent(
                publicKey = publicKey,
                displayName = displayName,
                containerSize = innerSize,
                pictureMode = ProfilePictureMode.LargePicture
            )
            if (audioOff) {
                val badgeSize = (innerSize * 0.27f).coerceIn(20.dp, 36.dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        // centre the badge just above the photo's bottom edge
                        .offset(y = innerSize / 2 - badgeSize / 2 + 2.dp)
                        .size(badgeSize)
                        .background(MaterialTheme.appColors.callBadgeBackground, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_fig_call_badge_volume_off),
                        contentDescription = stringResource(R.string.call_muted),
                        tint = MaterialTheme.appColors.callContentColor,
                        modifier = Modifier.size(badgeSize * 0.56f)
                    )
                }
            }
        }
    }

    @Composable
    private fun CallPersonName(name: String, overVideo: Boolean = false) {
        Text(
            text = name,
            color = if (overVideo) Color.White else MaterialTheme.appColors.callContentColor,
            fontFamily = OpenSans,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = TextStyle(
                shadow = if (overVideo) Shadow(Color(0x99000000), Offset(0f, 2f), 6f) else null
            )
        )
    }

    @Composable
    private fun CallStatusText(text: String, overVideo: Boolean = false) {
        if (text.isEmpty()) return
        Text(
            text = text,
            color = if (overVideo) Color.White else MaterialTheme.appColors.callContentColor,
            fontFamily = RobotoMono,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 4.dp),
            style = TextStyle(
                shadow = if (overVideo) Shadow(Color(0x99000000), Offset(0f, 2f), 6f) else null
            )
        )
    }

    @Composable
    private fun CallControlButton(
        icon: Int,
        tint: Color,
        contentDescription: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        fill: Color = MaterialTheme.appColors.callControlBackground,
        stroke: Color = MaterialTheme.appColors.callControlBorder
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 52.dp, height = 52.dp)
                .callControlSurface(fill, stroke)
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = 0.4f),
                modifier = Modifier.size(26.dp)
            )
        }
    }

    @Composable
    private fun CallActionButton(
        label: String,
        icon: Int,
        fill: Color,
        stroke: Color,
        tint: Color,
        contentDescription: String,
        onClick: () -> Unit
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .callControlSurface(fill, stroke)
                    .clickable(onClick = onClick)
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                color = MaterialTheme.appColors.callContentColor,
                fontFamily = RobotoMono,
                fontSize = 14.sp
            )
        }
    }

    /** Audio output chooser (Figma "Bluetooth Device" / "Mobile Speaker"): the selected row is the light one. */
    @Composable
    private fun AudioDeviceCard(
        bluetoothSelected: Boolean,
        onBluetooth: () -> Unit,
        onMobileSpeaker: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.appColors.messageRequestItemBackground)
                .border(0.5.dp, MaterialTheme.appColors.callDeviceCardBorder)
                .padding(14.dp)
        ) {
            AudioDeviceRow(
                icon = R.drawable.ic_fig_call_bluetooth,
                label = stringResource(id = R.string.bluetooth_device),
                selected = bluetoothSelected,
                onClick = onBluetooth
            )
            AudioDeviceRow(
                icon = R.drawable.ic_fig_call_mobile_speaker,
                label = stringResource(id = R.string.mobile_speaker),
                selected = !bluetoothSelected,
                onClick = onMobileSpeaker
            )
        }
    }

    @Composable
    private fun AudioDeviceRow(icon: Int, label: String, selected: Boolean, onClick: () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = MaterialTheme.appColors.callContentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            val shape = notchedCornerShape(BChatDimens.NotchSmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(if (selected) MaterialTheme.appColors.onboardingPrimaryButtonBackground else MaterialTheme.appColors.messageRequestItemBackground, shape)
                    .border(1.dp, if (selected) MaterialTheme.appColors.onboardingPrimaryButtonBackground else MaterialTheme.appColors.callDeviceRowBorder, shape)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 18.dp)
            ) {
                Text(
                    text = label,
                    color = if (selected) MaterialTheme.appColors.dialogPositiveButtonText else MaterialTheme.appColors.callContentColor,
                    fontFamily = OpenSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Text(text = "✓", color = MaterialTheme.appColors.dialogPositiveButtonText, fontSize = 14.sp)
                } else {
                    Box(Modifier.size(5.dp).background(MaterialTheme.appColors.callDeviceDot, CircleShape))
                }
            }
        }
    }

    @Composable
    fun VideoCallSurfaceView(modifier : Modifier=Modifier, surfaceView : SurfaceView) {
        AndroidView(modifier= modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = Color.Black,
                shape = RoundedCornerShape(12.dp)
            )
            .onSizeChanged { size ->
                forceSurfaceSize(surfaceView, size.width, size.height)
            }
            .onGloballyPositioned { coords ->
                forceSurfaceSize(surfaceView, coords.size.width, coords.size.height)
            }, factory={
            if(surfaceView.parent != null) {
                (surfaceView.parent as ViewGroup).removeView(surfaceView)
            }
            surfaceView
        }, update={ view ->
            if (view is SurfaceViewRenderer) {
                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                forceSurfaceSize(surfaceView, view.width, view.height)
            }
        })
    }

    private fun forceSurfaceSize(surfaceView: SurfaceView, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        surfaceView.layout(0, 0, width, height)
        surfaceView.holder?.setFixedSize(width, height)
    }

    private fun answerCall(context : Context) {
        val answerIntent=WebRtcCallService.acceptCallIntent(context)
        ContextCompat.startForegroundService(context, answerIntent)
    }

    @Preview
    @Composable
    fun WebRtcCallScreenPreview() {
        WebRtcCallScreen(wantsToAnswer.value, hexEncodedPublicKey, isInPictureInPictureMode.value)
    }

    @Preview(uiMode=Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun WebRtcCallScreenPreviewDark() {
        WebRtcCallScreen(wantsToAnswer.value, hexEncodedPublicKey, isInPictureInPictureMode.value)
    }
}

@Composable
private fun Modifier.callControlSurface(fill: Color, stroke: Color): Modifier = this
    .background(fill, notchedCornerShape(BChatDimens.NotchMedium))
    .border(1.dp, stroke, notchedCornerShape(BChatDimens.NotchMedium))
