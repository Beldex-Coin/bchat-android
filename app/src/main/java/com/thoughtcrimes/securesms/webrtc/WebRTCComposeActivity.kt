package com.thoughtcrimes.securesms.webrtc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureComponent
import com.thoughtcrimes.securesms.compose_utils.ProfilePictureMode
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.permissions.Permissions
import com.thoughtcrimes.securesms.service.WebRtcCallService
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import com.thoughtcrimes.securesms.webrtc.audio.SignalAudioManager
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DurationFormatUtils


@AndroidEntryPoint
class WebRTCComposeActivity : ComponentActivity() {

    private var hangupReceiver: BroadcastReceiver? = null

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BChatTheme(darkTheme=UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT) {
                Surface {
                    Scaffold(
                            containerColor=MaterialTheme.colorScheme.primary,
                    ) {
                        WebRtcCallScreen()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_PRE_OFFER="pre-offer"
        const val ACTION_FULL_SCREEN_INTENT="fullscreen-intent"
        const val ACTION_ANSWER="answer"
        const val ACTION_END="end-call"
        const val CALL_DURATION_FORMAT="HH:mm:ss"
        var CALL_DURATION="0"

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
    fun WebRtcCallScreen() {
        val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(LocalContext.current) == UiMode.NIGHT
        val callViewModel : CallViewModel=hiltViewModel()
        val context=LocalContext.current
        val profileSize=132.dp

        var wantsToAnswer=false
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
        var isShowDisabledVideoOption by remember {
            mutableStateOf(false)
        }
        var isShowSwitchCameraOption by remember {
            mutableStateOf(false)
        }
        var isSwitchCameraOptionColorChange by remember {
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
        var isShowStatus by remember {
            mutableStateOf(false)
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
        var isShowPersonNameStatus by remember {
            mutableStateOf(false)
        }
        var isPersonNameText by remember {
            mutableStateOf("")
        }
        var isShowVideoCallLocalView by remember {
            mutableStateOf(false)
        }
        var isShowVideoCallRemoteView by remember {
            mutableStateOf(false)
        }
        var isShowRemoteRecipientView by remember {
            mutableStateOf(false)
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
        var callLoading by remember {
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
                val intent=WebRtcCallService.cameraEnabled(context, !callViewModel.videoEnabled)
                context.startService(intent)
            }.execute()
        }

        fun switchCamera() {
            if (flipCamera && isShowVideoOption) {
                flipCamera=false
            } else {
                isSwitchCameraOptionColorChange=true
                flipCamera=true
            }
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
                            val displayName=getUserDisplayName(recipientPublicKey)
                            isPersonNameText=displayName
                            val signalProfilePicture=latestRecipient.recipient.contactPhoto
                            isPersonNameText=displayName
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

                launch {
                    callViewModel.localVideoEnabledState.collect { isEnabled ->
                        isShowVideoCallLocalView=false
                        localVideoView=isEnabled
                        isStatusText=if (isEnabled) {
                            "Video Call"
                        } else {
                            "Voice Call"
                        }
                        if (isEnabled) {
                            callViewModel.localRenderer?.let { sfView ->
                                isLocalSurfaceView?.setZOrderOnTop(true)
                                isLocalSurfaceView=sfView
                            }
                        }
                        isShowVideoCallLocalView=isEnabled
                        isSelectedVideoOption=isEnabled
                        if (isEnabled) {
                            isShowDisabledVideoOption=true
                            flipCamera=true
                        } else {
                            isShowDisabledVideoOption=true
                            isSwitchCameraOptionColorChange=true
                            flipCamera=false
                        }
                    }
                }

                launch {
                    callViewModel.remoteVideoEnabledState.collect { isEnabled ->
                        isShowVideoCallRemoteView=false
                        remoteVideoView=isEnabled
                        if (isEnabled) {
                            callViewModel.remoteRenderer?.let { sfView ->
                                isRemoteSurfaceView=sfView
                            }
                        }
                        isShowVideoCallRemoteView=isEnabled
                        isShowRemoteRecipientView=!isEnabled
                        isShowPersonNameStatus=!isEnabled
                    }
                }
            }
        }
        //UI started
        Column(
                verticalArrangement=Arrangement.Center,
                horizontalAlignment=Alignment.CenterHorizontally,
                modifier=Modifier
                        .fillMaxSize()
                        .paint(if (isDarkTheme)
                            painterResource(id=R.drawable.call_background)
                        else
                            painterResource(id=R.drawable.call_background_white),
                                contentScale=ContentScale.FillBounds)
        ) {
            Row(
                    verticalAlignment=Alignment.CenterVertically,
                    modifier=Modifier
                            .fillMaxWidth()
                            .padding(top=16.dp, start=16.dp, end=16.dp, bottom=0.dp)
            ) {
                Icon(
                        painterResource(id=R.drawable.ic_back_arrow),
                        contentDescription=stringResource(R.string.back),
                        tint=MaterialTheme.appColors.editTextColor,
                        modifier=Modifier
                                .clickable {
                                    (context as ComponentActivity).finish()
                                }
                )
            }

            Column(
                    modifier=Modifier
            ) {
                Box(modifier=Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {

                    if (!remoteVideoView) {

                        Column(horizontalAlignment=Alignment.CenterHorizontally,
                                modifier=Modifier.fillMaxWidth()) {

                            Text(
                                    text=isStatusText,
                                    style=BChatTypography.titleMedium.copy(color=MaterialTheme.appColors.textColor, fontSize=24.sp, fontWeight=FontWeight(700)),
                                    textAlign=TextAlign.Center,

                                    )
                            Row(
                                    modifier=Modifier
                                            .padding(vertical=5.dp),
                                    verticalAlignment=Alignment.CenterVertically,
                                    horizontalArrangement=Arrangement.Center
                            ) {

                                Icon(
                                        painter=painterResource(id=R.drawable.ic_lock_call),
                                        contentDescription=lockDescription,
                                        tint = MaterialTheme.appColors.textColor,
                                        modifier=Modifier.padding(horizontal=5.dp)
                                )
                                Text(
                                        text=stringResource(id=R.string.end_to_end_encrypted),
                                        style=BChatTypography.titleMedium.copy(
                                                color=MaterialTheme.appColors.textColor,
                                                fontSize=12.sp,
                                                fontWeight=FontWeight(400)),
                                        modifier=Modifier.padding(horizontal=5.dp))
                            }
                            Box(modifier=Modifier
                                    .padding(4.dp)
                                    .height(194.dp)
                                    .width(194.dp)
                                    .border(width=1.dp, color=MaterialTheme.appColors.switchTrackColor, shape=CircleShape)
                                    .aspectRatio(1f)
                                    .background(color=MaterialTheme.appColors.backgroundColor, shape=CircleShape), contentAlignment=Alignment.Center) {

                                Box(
                                        modifier=Modifier
                                                .padding(4.dp)
                                                .height(152.dp)
                                                .width(152.dp)
                                                .fillMaxWidth(),
                                        contentAlignment=Alignment.Center,
                                ) {
                                    ProfilePictureComponent(
                                            publicKey=recipientPublicKey,
                                            displayName=getUserDisplayName(recipientPublicKey),
                                            containerSize=profileSize,
                                            pictureMode=ProfilePictureMode.LargePicture)
                                }
                            }

                            Text(
                                    text=isPersonNameText,
                                    style=BChatTypography.titleMedium.copy(color=MaterialTheme.appColors.textColor, fontSize=24.sp, fontWeight=FontWeight(700)),
                                    textAlign=TextAlign.Center,
                                    modifier=Modifier.padding(all=12.dp),

                                    )
                        }


                        Column(verticalArrangement=Arrangement.Bottom, horizontalAlignment=Alignment.CenterHorizontally, modifier=Modifier.fillMaxSize()) {
                            if (isShowDialingStatus) {
                                Text(isStatusDialingText,
                                        Modifier.padding(all=12.dp).offset(y=(-50).dp),
                                        style=BChatTypography.titleMedium.copy(
                                                color=MaterialTheme.appColors.textColor,
                                                fontSize=18.sp, fontWeight=FontWeight(400)))
                            }
                            if(isShowIncomingStatus){
                                Text(isStatusInComingText,
                                        Modifier.padding(all=12.dp).offset(y=(-60).dp),
                                        style=BChatTypography.titleMedium.copy(
                                                color=MaterialTheme.appColors.textColor,
                                                fontSize=18.sp, fontWeight=FontWeight(400)))
                            }
                            if (isShowCallAudioStatus) {
                                Text(isStatusCallAudioText, Modifier.padding(all=12.dp), style=BChatTypography.titleMedium.copy(color=MaterialTheme.appColors.textColor, fontSize=12.sp, fontWeight=FontWeight(400)))
                            }
                            if (isShowReConnecting) {
                                Text(
                                        text=isStatusReConnectingText,
                                        style=BChatTypography.titleMedium.copy(
                                                color=MaterialTheme.appColors.textColor,
                                                fontSize=18.sp,
                                                fontWeight=FontWeight(400)),
                                        modifier=Modifier.offset(y=(-80).dp)
                                )
                            }

                            if (isShowCallDurationStatus) {
                                Text(isStatusCallDurationText,
                                        Modifier
                                                .padding(all=12.dp)
                                                .offset(y=(-50).dp),
                                        style=BChatTypography.titleMedium.copy(
                                                color=MaterialTheme.appColors.textColor,
                                                fontSize=18.sp,
                                                fontWeight=FontWeight(400)))
                            }
                            if (callLoading) {
                                Box(
                                        modifier=Modifier
                                                .wrapContentSize()
                                                .offset(y=(-70).dp),
                                        contentAlignment=Alignment.Center
                                ) {
                                    LottieAnimation(
                                            composition,
                                            progress,
                                            modifier=Modifier.size(70.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(color=Color.Black,
                                shape=RoundedCornerShape(12.dp)) {
                            isRemoteSurfaceView?.let {
                                VideoCallSurfaceView(
                                        surfaceView=it)
                            }
                        }
                    }

                    if (localVideoView) {

                        Box(modifier=Modifier
                                .height(140.dp)
                                .width(110.dp)
                                .padding(10.dp)
                                .align(Alignment.BottomEnd)
                                .clip(RoundedCornerShape(12.dp)),
                                contentAlignment=Alignment.TopEnd) {
                            Surface(color=Color.Black,
                                    shape=RoundedCornerShape(12.dp)) {
                                isLocalSurfaceView?.let {
                                    VideoCallSurfaceView(
                                            surfaceView=it)
                                }
                            }

                        }

                    }

                    if(expanded) {

                        Column(
                                horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.SpaceBetween,
                                modifier=Modifier
                                        .padding(16.dp)
                                        .align(Alignment.BottomEnd)
                                        .offset(x=(-26).dp, y=(10).dp)
                                        .background(color=MaterialTheme.appColors.callBottomBackground, shape=RoundedCornerShape(50.dp))
                        )
                        {
                            Image(
                                    painter=painterResource(id=R.drawable.ic_bluetooth_call),
                                    contentDescription="endCallDescription",
                                    colorFilter = ColorFilter.tint(
                                            color = (if(isBluetoothIsSelected) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.iconTint)
                                    ),
                                    modifier =Modifier
                                            .padding(16.dp)
                                            .clickable {
                                                val command=AudioManagerCommand.SetUserDevice(if (callViewModel.isBluetooth) SignalAudioManager.AudioDevice.EARPIECE else SignalAudioManager.AudioDevice.BLUETOOTH)
                                                WebRtcCallService.sendAudioManagerCommand(context, command)
                                                expanded=false
                                            }
                            )
                            Image(
                                    painter=painterResource(id=R.drawable.ic_speaker_call),
                                    contentDescription="endCallDescription",
                                    colorFilter = ColorFilter.tint(
                                            color = (if(isSpeakerIsSelected) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.iconTint)
                                    ),
                                     modifier =Modifier
                                             .padding(16.dp)
                                             .clickable {
                                                 val command=AudioManagerCommand.SetUserDevice(if (callViewModel.isSpeaker) SignalAudioManager.AudioDevice.EARPIECE else SignalAudioManager.AudioDevice.SPEAKER_PHONE)
                                                 WebRtcCallService.sendAudioManagerCommand(context, command)
                                                 expanded=false
                                             }
                            )

                        }
                    }

                }
                if (isShowAnswerOption && isShowDeclineOption) {

                    Row(horizontalArrangement=Arrangement.SpaceEvenly, modifier=Modifier
                            .fillMaxWidth()
                            .offset(y=(-50).dp)
                    ) {
                        if (isShowAnswerOption) {
                            Box(modifier=Modifier
                                    .height(65.dp)
                                    .width(65.dp)
                                    .background(MaterialTheme.appColors.walletDashboardReceiveButtonBackground, shape=CircleShape)
                                    .clickable {
                                        if (callViewModel.currentCallState == CallViewModel.State.CALL_PRE_INIT) {
                                            wantsToAnswer=true
                                            updateControls()
                                        }
                                        answerCall(context)
                                    }, contentAlignment=Alignment.Center

                            ) {
                                Image(painter=painterResource(id=R.drawable.ic_incoming_call), contentDescription=answerCallDescription, modifier=Modifier.padding(10.dp))
                            }
                        }
                        if (isShowDeclineOption) {
                            Box(modifier=Modifier
                                    .height(65.dp)
                                    .width(65.dp)
                                    .background(MaterialTheme.appColors.errorMessageColor, shape=CircleShape)
                                    .clickable {
                                        val declineIntent=WebRtcCallService.denyCallIntent(context)
                                        context.startService(declineIntent)
                                    }, contentAlignment=Alignment.Center

                            ) {

                                Image(painter=painterResource(id=R.drawable.ic_decline_call), contentDescription=declineCallDescription, modifier=Modifier.padding(10.dp))
                            }
                        }
                    }
                }

                Box(
                        modifier=Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.BottomCenter),
                        contentAlignment=Alignment.Center,
                ) {

                    if (isShowVideoOption || isShowSwitchCameraOption || isShowMuteOption || isShowSpeakerOption) {
                        Box(
                                modifier=Modifier
                                        .wrapContentSize()
                                        .paint(painterResource(id=R.drawable.call_bottom_background_white),
                                                contentScale=ContentScale.FillBounds,
                                                colorFilter=ColorFilter.tint(MaterialTheme.appColors.callBottomBackground)),
                                contentAlignment=Alignment.Center
                        ) {
                            Row(
                                    horizontalArrangement=Arrangement.SpaceAround,
                                    modifier=Modifier
                                            .fillMaxWidth()
                                            .padding(vertical=20.dp)
                            ) {
                                Row(
                                        //horizontalArrangement=Arrangement.SpaceAround,
                                        modifier=Modifier.wrapContentWidth()

                                ) {
                                    // Options within the second box
                                    if (isShowVideoOption) {
                                        Box(modifier=Modifier
                                                .height(42.dp)
                                                .width(42.dp)
                                                .background(MaterialTheme.appColors.qrCodeBackground, shape=CircleShape)
                                                .clickable {
                                                    enableCamera()
                                                }, contentAlignment=Alignment.Center

                                        ) {

                                            Image(painter=
                                            if (isSelectedVideoOption) {
                                                if (isDarkTheme) {
                                                    painterResource(id=R.drawable.ic_video_disabled_call)
                                                } else {
                                                    painterResource(id=R.drawable.ic_video_disable_call_white)
                                                }
                                            } else {
                                                if (isDarkTheme) {
                                                    painterResource(id=R.drawable.ic_video_call)
                                                } else {
                                                    painterResource(id=R.drawable.ic_video_call_white)
                                                }
                                            },
                                                    contentDescription=enableVideoDescription, modifier=Modifier.padding(10.dp))
                                        }
                                    }

                                    Spacer(modifier=Modifier.width(20.dp))

                                    if (isShowSwitchCameraOption) {
                                        Box(modifier=Modifier
                                                .height(42.dp)
                                                .width(42.dp)
                                                .background(MaterialTheme.appColors.qrCodeBackground, shape=CircleShape)
                                                .clickable() {
                                                    switchCamera()
                                                }, contentAlignment=Alignment.Center

                                        ) {

                                            Image(
                                                    painter=painterResource(
                                                            id=if (isDarkTheme) R.drawable.ic_switch_camera_call
                                                            else R.drawable.ic_switch_camera_call_white),
                                                    contentDescription=switchCamDescription,
                                                    modifier=Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                                Row(

                                        modifier=Modifier.wrapContentWidth()

                                ) {
                                    if (isShowMuteOption) {

                                        Box(modifier=Modifier
                                                .height(42.dp)
                                                .width(42.dp)
                                                .background(MaterialTheme.appColors.qrCodeBackground, shape=CircleShape)
                                                .clickable(isMuteOptionClickable) { enableMuteOption() }, contentAlignment=Alignment.Center

                                        ) {
                                            Image(painter=
                                            if (isMuteOptionIconChange) {
                                                if (isDarkTheme) {
                                                    painterResource(id=R.drawable.ic_unmute_call)
                                                } else {
                                                    painterResource(id=R.drawable.ic_unmute_call_white)
                                                }
                                            } else {
                                                if (isDarkTheme) {
                                                    painterResource(id=R.drawable.ic_mute_call)
                                                } else {
                                                    painterResource(id=R.drawable.ic_mute_call_white)
                                                }
                                            },
                                                    contentDescription=muteDescription, modifier=Modifier.padding(10.dp))
                                        }
                                    }

                                    Spacer(modifier=Modifier.width(20.dp))

                                    if (isShowSpeakerOption) {

                                        Box(modifier=Modifier
                                                .height(42.dp)
                                                .width(42.dp)
                                                .background(MaterialTheme.appColors.qrCodeBackground, shape=CircleShape)
                                                .clickable {
                                                    if(isBluetoothIsConnected) {
                                                        expanded=!expanded
                                                    }else{
                                                         val command=AudioManagerCommand.SetUserDevice(if (callViewModel.isSpeaker) SignalAudioManager.AudioDevice.EARPIECE else SignalAudioManager.AudioDevice.SPEAKER_PHONE)
                                                    WebRtcCallService.sendAudioManagerCommand(context, command)
                                                }, contentAlignment=Alignment.Center

                                        ) {
                                            if(isBluetoothIsSelected && isBluetoothIsConnected) {

                                                Image(painter=if (isDarkTheme) {
                                                    painterResource(id=R.drawable.ic_bluetooth_call)
                                                } else {
                                                    painterResource(id=R.drawable.ic_bluetooth_call)
                                                },
                                                        colorFilter=ColorFilter.tint(
                                                                color=MaterialTheme.appColors.primaryButtonColor
                                                        ),
                                                        contentDescription=speakerDescription,
                                                        modifier=Modifier.align(Alignment.Center)

                                                )
                                            }else{
                                                Image(painter= if(isDarkTheme){
                                                    painterResource(id=R.drawable.ic_speaker_call)
                                                }else{
                                                    painterResource(id=R.drawable.ic_speaker_call_white)
                                                },
                                                        colorFilter = ColorFilter.tint(
                                                                color = (if(isSpeakerIsSelected) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.iconTint)
                                                        ),
                                                        contentDescription=speakerDescription,
                                                        modifier=Modifier.align(Alignment.Center)

                                                )
                                            }

                                            Image(
                                                    painter= if(isDarkTheme) {
                                                        painterResource(id=R.drawable.ic_switch_speaker_call)
                                                    }else{
                                                        painterResource(id=R.drawable.ic_switch_speaker_call_white)
                                                    },
                                                    contentDescription=speakerDescription,
                                                    modifier=Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .offset(x=((5).dp), y=((5).dp))
                                            )

                                        }
                                    }
                                }
                            }

                            if (isShowEndCallOption) {
                                Box(
                                        modifier=Modifier
                                                .height(65.dp)
                                                .width(65.dp)
                                                .offset(y=(-43).dp)
                                                .background(MaterialTheme.appColors.errorMessageColor, shape=CircleShape)
                                                .clickable {
                                                    context.startService(WebRtcCallService.hangupIntent(context))
                                                },
                                        contentAlignment=Alignment.Center
                                ) {
                                    Image(
                                            painter=painterResource(id=R.drawable.ic_decline_call),
                                            contentDescription=endCallDescription
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
        AndroidView(modifier=modifier, factory={ surfaceView }, update={ view ->
        })
    }

    private fun answerCall(context : Context) {
        val answerIntent=WebRtcCallService.acceptCallIntent(context)
        ContextCompat.startForegroundService(context, answerIntent)
    }

    @Preview
    @Composable
    fun WebRtcCallScreenPreview() {
        WebRtcCallScreen()
    }

    @Preview(uiMode=Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun WebRtcCallScreenPreviewDark() {
        WebRtcCallScreen()
    }
}