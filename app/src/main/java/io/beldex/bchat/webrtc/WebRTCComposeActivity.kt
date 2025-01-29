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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.TextSecurePreferences
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
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


@AndroidEntryPoint
class WebRTCComposeActivity : ComponentActivity() {

    private var hangupReceiver: BroadcastReceiver? = null
    val viewModel:CallViewModel by viewModels()

    val wantsToAnswer :MutableState<Boolean> = mutableStateOf(false)
    private val isInPictureInPictureMode :MutableState<Boolean> = mutableStateOf(false)

    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }
    private var pipBuilderParams: PictureInPictureParams.Builder? = null

    private fun isSystemPipEnabledAndAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= 26 && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // switch to PiP mode if the user presses the home or recent button,
        if(isSystemPipEnabledAndAvailable()) {
            try {
                pipBuilderParams?.let { enterPictureInPictureMode(it.build()) }
            } catch (e: java.lang.Exception) {
                Log.w(TAG, "System lied about having PiP available.", e)
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
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
            BChatTheme(darkTheme=UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT) {
                Surface {
                    Scaffold(
                        containerColor=MaterialTheme.colorScheme.primary,
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_ANSWER) {
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
            mutableStateOf("Voice Call")
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
            return contact?.displayName(Contact.ContactContext.REGULAR) ?: publicKey
        }

        fun getUserDisplayNameOrShortestPublicKey(publicKey : String) : String {
            val contact=DatabaseComponent.get(context).bchatContactDatabase().getContactWithBchatID(publicKey)
            return contact?.displayName(Contact.ContactContext.REGULAR) ?: "${publicKey.take(4)}...${publicKey.takeLast(4)}"
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
                                "Call Muted"
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

                            isStatusCallDurationText=DurationFormatUtils.formatDuration(System.currentTimeMillis() - startTime, CALL_DURATION_FORMAT)
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
                            "Video Call"
                        } else {
                            "Voice Call"
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
        //UI started
        Column(
            verticalArrangement=Arrangement.Center,
            horizontalAlignment=Alignment.CenterHorizontally,
            modifier= Modifier
                .fillMaxSize()
                .paint(
                    if (isDarkTheme)
                        painterResource(id = R.drawable.call_background)
                    else
                        painterResource(id = R.drawable.call_background_white),
                    contentScale = ContentScale.FillBounds
                )
        ) {
            if(isInPictureInPictureMode){
                if(!remoteVideoView) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(60.dp)
                            .width(60.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.appColors.switchTrackColor,
                                shape = CircleShape
                            )
                            .aspectRatio(1f)
                            .align(Alignment.CenterHorizontally)
                            .background(
                                color = MaterialTheme.appColors.backgroundColor,
                                shape = CircleShape
                            ), contentAlignment = Alignment.Center
                    ) {

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .height(48.dp)
                                .width(48.dp)
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center,
                        ) {
                            ProfilePictureComponent(
                                publicKey = if (showLocalUserDetailsInFullScreen()) localUserPublicKey else recipientPublicKey,
                                displayName = getUserDisplayName(if (showLocalUserDetailsInFullScreen()) localUserPublicKey else recipientPublicKey),
                                containerSize = profileSize,
                                pictureMode = ProfilePictureMode.LargePicture
                            )
                        }
                    }
                } else {
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        isRemoteSurfaceView?.let {
                            VideoCallSurfaceView(
                                surfaceView = it
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {

                    if (!remoteVideoView) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {

                            Text(
                                text = isStatusText,
                                style = BChatTypography.titleMedium.copy(
                                    color = MaterialTheme.appColors.textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight(700)
                                ),
                                textAlign = TextAlign.Center,

                                )
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {

                                Icon(
                                    painter = painterResource(id = R.drawable.ic_lock_call),
                                    contentDescription = lockDescription,
                                    tint = MaterialTheme.appColors.textColor,
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.end_to_end_encrypted),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight(400)
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .height(194.dp)
                                    .width(194.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.appColors.switchTrackColor,
                                        shape = CircleShape
                                    )
                                    .aspectRatio(1f)
                                    .background(
                                        color = MaterialTheme.appColors.backgroundColor,
                                        shape = CircleShape
                                    ), contentAlignment = Alignment.Center
                            ) {

                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .height(152.dp)
                                        .width(152.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ProfilePictureComponent(
                                        publicKey = if (showLocalUserDetailsInFullScreen()) localUserPublicKey else recipientPublicKey,
                                        displayName = getUserDisplayName(if (showLocalUserDetailsInFullScreen()) localUserPublicKey else recipientPublicKey),
                                        containerSize = profileSize,
                                        pictureMode = ProfilePictureMode.LargePicture
                                    )
                                }
                            }

                            Text(
                                text = if (showLocalUserDetailsInFullScreen()) localUserName else isPersonNameText,
                                style = BChatTypography.titleMedium.copy(
                                    color = MaterialTheme.appColors.textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight(700)
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(all = 12.dp),

                                )
                        }


                        Column(
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isShowDialingStatus) {
                                Text(
                                    isStatusDialingText,
                                    Modifier
                                        .padding(all = 12.dp)
                                        .offset(y = (-50).dp),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 18.sp, fontWeight = FontWeight(400)
                                    )
                                )
                            }
                            if (isShowIncomingStatus) {
                                Text(
                                    isStatusInComingText,
                                    Modifier
                                        .padding(all = 12.dp)
                                        .offset(y = (-60).dp),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 18.sp, fontWeight = FontWeight(400)
                                    )
                                )
                            }
                            if (isShowCallAudioStatus) {
                                Text(
                                    isStatusCallAudioText,
                                    Modifier
                                        .padding(all = 12.dp)
                                        .offset(y = (-40).dp),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight(400)
                                    )
                                )
                            }
                            if (isShowReConnecting) {
                                Text(
                                    text = isStatusReConnectingText,
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight(400)
                                    ),
                                    modifier = Modifier.offset(y = (-80).dp)
                                )
                            }

                            if (isShowCallDurationStatus) {
                                Text(
                                    isStatusCallDurationText,
                                    Modifier
                                        .padding(all = 12.dp)
                                        .offset(y = (-50).dp),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight(400)
                                    )
                                )
                            }
                            if (callLoading) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .offset(y = (-70).dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LottieAnimation(
                                        composition,
                                        progress,
                                        modifier = Modifier.size(70.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color.Black,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            isRemoteSurfaceView?.let {
                                VideoCallSurfaceView(
                                    surfaceView = it
                                )
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isShowDialingStatus) {
                                Text(
                                    isStatusDialingText,
                                    Modifier
                                        .padding(all = 12.dp)
                                        .offset(y = (-50).dp),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 18.sp, fontWeight = FontWeight(400)
                                    )
                                )
                            }
                            if (isShowIncomingStatus) {
                                Text(
                                    isStatusInComingText,
                                    Modifier
                                        .padding(all = 12.dp)
                                        .offset(y = (-60).dp),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 18.sp, fontWeight = FontWeight(400)
                                    )
                                )
                            }
                            if (isShowCallAudioStatus) {
                                Text(
                                    isStatusCallAudioText,
                                    Modifier
                                        .padding(all = 12.dp)
                                        .offset(y = (-40).dp),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight(400)
                                    )
                                )
                            }
                            if (isShowReConnecting) {
                                Text(
                                    text = isStatusReConnectingText,
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight(400)
                                    ),
                                    modifier = Modifier.offset(y = (-80).dp)
                                )
                            }

                            Text(
                                text = if (isSwitchCameraFlipEnabled && videoSwapped) localUserName else isPersonNameText,
                                style = BChatTypography.titleMedium.copy(
                                    color = MaterialTheme.appColors.textColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight(700)
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                                    .offset(y = (-50).dp),
                            )

                            if (isShowCallDurationStatus) {
                                Text(
                                    isStatusCallDurationText,
                                    Modifier
                                        .padding(all = 12.dp)
                                        .offset(y = (-50).dp),
                                    style = BChatTypography.titleMedium.copy(
                                        color = MaterialTheme.appColors.textColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight(400)
                                    )
                                )
                            }
                            if (callLoading) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .offset(y = (-70).dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LottieAnimation(
                                        composition,
                                        progress,
                                        modifier = Modifier.size(70.dp)
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 16.dp)
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_back_call),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.appColors.editTextColor,
                            modifier = Modifier
                                .clickable {
                                    (context as ComponentActivity).finish()
                                }
                        )
                    }

                    if (localVideoView) {

                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .width(110.dp)
                                .padding(10.dp)
                                .align(Alignment.BottomEnd)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            if (showVideoCameOff) {
                                Box(
                                    modifier = Modifier
                                        .height(140.dp)
                                        .width(110.dp)
                                        .background(
                                            color = MaterialTheme.appColors.actionIconBackground,
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .clickable {
                                            callViewModel.swapVideos()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .height(54.dp)
                                            .width(54.dp)
                                            .border(
                                                width = 0.45.dp,
                                                color = Color(0xFF363645),
                                                shape = CircleShape
                                            )
                                            .aspectRatio(1f)
                                            .background(
                                                color = MaterialTheme.appColors.editTextBackground,
                                                shape = CircleShape
                                            ), contentAlignment = Alignment.Center
                                    ) {

                                        Box(
                                            modifier = Modifier
                                                .padding(3.dp)
                                                .height(40.dp)
                                                .width(40.dp)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            ProfilePictureComponent(
                                                publicKey = if (showLocalUserDetailsInSmallScreen()) localUserPublicKey else recipientPublicKey,
                                                displayName = getUserDisplayName(if (showLocalUserDetailsInSmallScreen()) localUserPublicKey else recipientPublicKey),
                                                containerSize = 40.dp,
                                                pictureMode = ProfilePictureMode.SmallPicture
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .height(140.dp)
                                        .width(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            color = Color.Black,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            callViewModel.swapVideos()
                                        },
                                ) {
                                    isLocalSurfaceView?.let {
                                        VideoCallSurfaceView(
                                            surfaceView = it
                                        )
                                    }
                                }
                            }

                        }

                    }

                    if (expanded) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.BottomStart)
                                .offset(x = (16).dp, y = (10).dp)
                                .background(
                                    color = MaterialTheme.appColors.callBottomBackground,
                                    shape = RoundedCornerShape(50.dp)
                                )
                        )
                        {
                            Image(
                                painter = painterResource(id = R.drawable.ic_bluetooth_call),
                                contentDescription = "endCallDescription",
                                colorFilter = ColorFilter.tint(
                                    color = (if (isBluetoothIsSelected) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.iconTint)
                                ),
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clickable {
                                        val command =
                                            AudioManagerCommand.SetUserDevice(if (callViewModel.isBluetooth) SignalAudioManager.AudioDevice.EARPIECE else SignalAudioManager.AudioDevice.BLUETOOTH)
                                        WebRtcCallService.sendAudioManagerCommand(
                                            context,
                                            command
                                        )
                                        expanded = false
                                    }
                            )
                            Image(
                                painter = painterResource(id = R.drawable.ic_speaker_call),
                                contentDescription = "endCallDescription",
                                colorFilter = ColorFilter.tint(
                                    color = (if (isSpeakerIsSelected) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.iconTint)
                                ),
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clickable {
                                        val command =
                                            AudioManagerCommand.SetUserDevice(if (callViewModel.isSpeaker) SignalAudioManager.AudioDevice.EARPIECE else SignalAudioManager.AudioDevice.SPEAKER_PHONE)
                                        WebRtcCallService.sendAudioManagerCommand(
                                            context,
                                            command
                                        )
                                        expanded = false
                                    }
                            )

                        }
                    }

                }
                if (isShowAnswerOption && isShowDeclineOption) {

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-50).dp)
                    ) {
                        if (isShowAnswerOption) {
                            Box(
                                modifier = Modifier
                                    .height(65.dp)
                                    .width(65.dp)
                                    .background(
                                        MaterialTheme.appColors.walletDashboardReceiveButtonBackground,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        if (callViewModel.currentCallState == CallViewModel.State.CALL_PRE_INIT) {
                                            wantsToAnswer = true
                                            updateControls()
                                        }
                                        answerCall(context)
                                    }, contentAlignment = Alignment.Center

                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_incoming_call),
                                    contentDescription = answerCallDescription,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        if (isShowDeclineOption) {
                            Box(
                                modifier = Modifier
                                    .height(65.dp)
                                    .width(65.dp)
                                    .background(
                                        MaterialTheme.appColors.errorMessageColor,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        val declineIntent =
                                            WebRtcCallService.denyCallIntent(context)
                                        context.startService(declineIntent)
                                    }, contentAlignment = Alignment.Center

                            ) {

                                Image(
                                    painter = painterResource(id = R.drawable.ic_decline_call),
                                    contentDescription = declineCallDescription,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center,
                ) {

                    if (isShowVideoOption || isShowSwitchCameraOption || isShowMuteOption || isShowSpeakerOption) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .paint(
                                    painterResource(id = R.drawable.call_bottom_background_white),
                                    contentScale = ContentScale.FillBounds,
                                    colorFilter = ColorFilter.tint(MaterialTheme.appColors.callBottomBackground)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp)
                            ) {
                                Row(

                                    modifier = Modifier.wrapContentWidth()

                                ) {
                                    if (isShowSpeakerOption) {
                                        Box(
                                            modifier = Modifier
                                                .height(42.dp)
                                                .width(42.dp)
                                                .background(
                                                    MaterialTheme.appColors.qrCodeBackground,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    if (isBluetoothIsConnected) {
                                                        expanded = !expanded
                                                    } else {
                                                        val command =
                                                            AudioManagerCommand.SetUserDevice(if (callViewModel.isSpeaker) SignalAudioManager.AudioDevice.EARPIECE else SignalAudioManager.AudioDevice.SPEAKER_PHONE)
                                                        WebRtcCallService.sendAudioManagerCommand(
                                                            context,
                                                            command
                                                        )
                                                    }


                                                }, contentAlignment = Alignment.Center

                                        ) {
                                            if (isBluetoothIsSelected && isBluetoothIsConnected) {

                                                Image(
                                                    painter = if (isDarkTheme) {
                                                        painterResource(id = R.drawable.ic_bluetooth_call)
                                                    } else {
                                                        painterResource(id = R.drawable.ic_bluetooth_call)
                                                    },
                                                    colorFilter = ColorFilter.tint(
                                                        color = MaterialTheme.appColors.primaryButtonColor
                                                    ),
                                                    contentDescription = speakerDescription,
                                                    modifier = Modifier.align(Alignment.Center)

                                                )
                                            } else {
                                                Image(
                                                    painter = if (isDarkTheme) {
                                                        painterResource(id = R.drawable.ic_speaker_call)
                                                    } else {
                                                        painterResource(id = R.drawable.ic_speaker_call_white)
                                                    },
                                                    colorFilter = ColorFilter.tint(
                                                        color = (if (isSpeakerIsSelected) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.iconTint)
                                                    ),
                                                    contentDescription = speakerDescription,
                                                    modifier = Modifier.align(Alignment.Center)

                                                )
                                            }

                                            if (isBluetoothIsConnected) {
                                                Image(
                                                    painter = if (isDarkTheme) {
                                                        painterResource(id = R.drawable.ic_switch_speaker_call)
                                                    } else {
                                                        painterResource(id = R.drawable.ic_switch_speaker_call_white)
                                                    },
                                                    contentDescription = speakerDescription,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .offset(x = ((0).dp), y = ((5).dp))
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(20.dp))

                                    if (isShowMuteOption) {

                                        Box(
                                            modifier = Modifier
                                                .height(42.dp)
                                                .width(42.dp)
                                                .background(
                                                    MaterialTheme.appColors.qrCodeBackground,
                                                    shape = CircleShape
                                                )
                                                .clickable(isMuteOptionClickable) { enableMuteOption() },
                                            contentAlignment = Alignment.Center

                                        ) {
                                            Image(
                                                painter =
                                                if (isMuteOptionIconChange) {
                                                    if (isDarkTheme) {
                                                        painterResource(id = R.drawable.ic_unmute_call)
                                                    } else {
                                                        painterResource(id = R.drawable.ic_unmute_call_white)
                                                    }
                                                } else {
                                                    if (isDarkTheme) {
                                                        painterResource(id = R.drawable.ic_mute_call)
                                                    } else {
                                                        painterResource(id = R.drawable.ic_mute_call_white)
                                                    }
                                                },
                                                contentDescription = muteDescription,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                                Row(
                                    //horizontalArrangement=Arrangement.SpaceAround,
                                    modifier = Modifier.wrapContentWidth()

                                ) {
                                    // Options within the second box
                                    if (isShowSwitchCameraOption) {
                                        Box(
                                            modifier = Modifier
                                                .height(42.dp)
                                                .width(42.dp)
                                                .background(
                                                    MaterialTheme.appColors.qrCodeBackground,
                                                    shape = CircleShape
                                                )
                                                .clickable(enabled = isSwitchCameraFlipEnabled) {
                                                    switchCamera()
                                                }, contentAlignment = Alignment.Center

                                        ) {

                                            Image(
                                                painter = painterResource(
                                                    id = if (isDarkTheme && isSwitchCameraFlipEnabled) R.drawable.ic_switch_camera_call
                                                    else if (!isDarkTheme && isSwitchCameraFlipEnabled) R.drawable.ic_switch_camera_call_white
                                                    else R.drawable.ic_switch_camera_disable_call
                                                ),
                                                contentDescription = switchCamDescription,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(20.dp))

                                    if (isShowVideoOption) {
                                        Box(
                                            modifier = Modifier
                                                .height(42.dp)
                                                .width(42.dp)
                                                .background(
                                                    MaterialTheme.appColors.qrCodeBackground,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    enableCamera()
                                                }, contentAlignment = Alignment.Center

                                        ) {

                                            Image(
                                                painter =
                                                if (isSelectedVideoOption) {
                                                    if (isDarkTheme) {
                                                        painterResource(id = R.drawable.ic_video_disabled_call)
                                                    } else {
                                                        painterResource(id = R.drawable.ic_video_disable_call_white)
                                                    }
                                                } else {
                                                    if (isDarkTheme) {
                                                        painterResource(id = R.drawable.ic_video_call)
                                                    } else {
                                                        painterResource(id = R.drawable.ic_video_call_white)
                                                    }
                                                },
                                                contentDescription = enableVideoDescription,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isShowEndCallOption) {
                                Box(
                                    modifier = Modifier
                                        .height(65.dp)
                                        .width(65.dp)
                                        .offset(y = (-43).dp)
                                        .background(
                                            MaterialTheme.appColors.errorMessageColor,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            context.startService(
                                                WebRtcCallService.hangupIntent(
                                                    context
                                                )
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_decline_call),
                                        contentDescription = endCallDescription
                                    )
                                }
                            }
                        }
                    }
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
            ), factory={ if(surfaceView.parent != null) (surfaceView.parent as ViewGroup).removeView(surfaceView)
            surfaceView }, update={ view ->
        })
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