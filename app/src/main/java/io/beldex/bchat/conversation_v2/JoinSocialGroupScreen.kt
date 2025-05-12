package io.beldex.bchat.conversation_v2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.PublicKeyValidation
import io.beldex.bchat.compose_utils.BChatOutlinedTextField
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.ui.BChatPreviewContainer
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.groups.GroupManager
import io.beldex.bchat.groups.JoinPublicChatScanQRCodeActivity
import io.beldex.bchat.groups.OpenGroupManager
import io.beldex.bchat.util.ConfigurationMessageUtilities
import io.beldex.bchat.util.parcelable
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun JoinSocialGroupScreen(
    uiState: DefaultGroupsViewModel.UIState,
    groups: List<OpenGroupAPIV2.DefaultGroup>,
    onEvent: (OpenGroupEvents) -> Unit
) {
    val context = LocalContext.current
    val activity = (context as? Activity)
    val lifecycleOwner = LocalLifecycleOwner.current
    if (TextSecurePreferences.isScreenSecurityEnabled(context))
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    var showLoader by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition(
        LottieCompositionSpec
            .RawRes(R.raw.load_animation)
    )
    val isPlaying by remember {
        mutableStateOf(true)
    }
    // for speed
    val speed by remember {
        mutableStateOf(1f)
    }

    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isPlaying,
        speed = speed,
        restartOnPlay = false
    )

    val joinPublicChatScanQRCodeActivityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(ConversationFragmentV2.ADDRESS, result.data!!.parcelable(ConversationFragmentV2.ADDRESS))
            extras.putLong(ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(ConversationFragmentV2.THREAD_ID, -1))
            val returnIntent = Intent()
            returnIntent.putExtras(extras)
            activity?.setResult(ComponentActivity.RESULT_OK, returnIntent)
            activity?.finish()
        }
    }

    Column(
        modifier =Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        OutlinedCard(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.appColors.contactCardBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.activity_join_public_chat_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.secondaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(400)
                ),
                modifier =Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            )

            TextField(
                    value=uiState.groupUrl,
                    placeholder={
                        Text(text=stringResource(R.string.fragment_enter_chat_url_edit_text_hint),
                                style=MaterialTheme.typography.bodyMedium
                        )
                    },
                onValueChange = { url ->
                    onEvent(OpenGroupEvents.GroupUrlChanged(url))
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                trailingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_qr_code),
                        contentDescription = "",
                        colorFilter = ColorFilter.tint(
                            color = MaterialTheme.appColors.iconTint
                        ),
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                context,
                                JoinPublicChatScanQRCodeActivity::class.java
                            )
                            joinPublicChatScanQRCodeActivityResultLauncher.launch(intent)
                        })
                },
                modifier =Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                        colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                    focusedContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    selectionColors = TextSelectionColors(MaterialTheme.appColors.textSelectionColor, MaterialTheme.appColors.textSelectionColor),
                    cursorColor = colorResource(id = R.color.button_green)
            )
            )

            PrimaryButton(
                onClick = {
                    joinPublicChatIfPossible(uiState.groupUrl, lifecycleOwner, context)
                },
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.groupUrl.isNotEmpty(),
                disabledContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
                modifier =Modifier
                        .fillMaxWidth()
                        .padding(
                                start=16.dp,
                                end=16.dp,
                                bottom=16.dp
                        )
            ) {
                Text(
                    text = stringResource(id = R.string.next),
                    modifier = Modifier
                        .padding(8.dp),
                    style = BChatTypography.titleMedium.copy(
                        fontWeight = FontWeight(400),
                        fontSize = 16.sp,
                        color = if (uiState.groupUrl.isNotEmpty()) {
                            Color.White
                        } else {
                            MaterialTheme.appColors.disabledButtonContent
                        }
                    )
                )
            }

        }

        Text(
            text = stringResource(id = R.string.or_join),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.appColors.secondaryTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight(400)
            ),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (groups.isEmpty()) {
            LottieAnimation(
                composition,
                progress,
                modifier =Modifier
                        .size(70.dp)
                        .align(Alignment.CenterHorizontally)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                contentPadding = PaddingValues(all = 8.dp),
                content = {
                    items(groups.size) { i ->
                        Column(
                            modifier =Modifier
                                    .background(
                                            color=MaterialTheme.appColors.disabledButtonContainerColor,
                                            shape=RoundedCornerShape(8.dp)
                                    )
                                    .padding(start=16.dp, end=16.dp, top=14.dp, bottom=14.dp)
                                    .clickable {
                                        showLoader=true
                                        joinPublicChatIfPossible(
                                                groups[i].joinURL,
                                                lifecycleOwner,
                                                context
                                        )
                                    },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                            groups[i].image?.let {
                                convertImageByteArrayToBitmap(it).asImageBitmap()
                            }?.let { bitmap ->
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "",
                                    modifier =Modifier
                                            .width(52.dp)
                                            .height(52.dp)
                                            .clip(shape=RoundedCornerShape(6.dp)),
                                    alignment = Alignment.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = groups[i].name,
                                textAlign = TextAlign.Center,
                                style = BChatTypography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.appColors.textFieldTextColor
                                )
                            )
                        }
                    }
                }
            )
        }
    }
}

fun convertImageByteArrayToBitmap(imageData: ByteArray): Bitmap {
    return BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
}

fun joinPublicChatIfPossible(url: String, lifecycleOwner: LifecycleOwner, context: Context) {
    val activity = (context as? Activity)
    // Add "http" if not entered explicitly
    val stringWithExplicitScheme = if (!url.startsWith("http")) "http://$url" else url
    val url = stringWithExplicitScheme.toHttpUrlOrNull() ?: return Toast.makeText(
        context,
        R.string.invalid_url,
        Toast.LENGTH_SHORT
    ).show()
    val room = url.pathSegments.firstOrNull()
    val publicKey = url.queryParameter("public_key")
    val isV2OpenGroup = !room.isNullOrEmpty()
    if (isV2OpenGroup && (publicKey == null || !PublicKeyValidation.isValid(
            publicKey,
            64,
            false
        ))
    ) {
        return Toast.makeText(context, R.string.invalid_public_key, Toast.LENGTH_SHORT).show()
    }
    //showLoader()
    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
        try {
            val (threadID, groupID) = if (isV2OpenGroup) {
                val server = HttpUrl.Builder().scheme(url.scheme).host(url.host).apply {
                    if (url.port != 80 || url.port != 443) {
                        this.port(url.port)
                    } // Non-standard port; add to server
                }.build()
                val sanitizedServer = server.toString().removeSuffix("/")
                val openGroupID = "$sanitizedServer.${room!!}"
                OpenGroupManager.add(sanitizedServer, room, publicKey!!, context)
                MessagingModuleConfiguration.shared.storage.onOpenGroupAdded(
                    stringWithExplicitScheme
                )
                val threadID = GroupManager.getOpenGroupThreadID(openGroupID, context)
                val groupID = GroupUtil.getEncodedOpenGroupID(openGroupID.toByteArray())
                threadID to groupID
            } else {
                throw Exception("No longer supported.")
            }
            ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(context)
            withContext(Dispatchers.Main) {
                val recipient = Recipient.from(context, Address.fromSerialized(groupID), false)
                openConversationActivity(threadID, recipient, activity!!)
                activity?.finish()
            }
        } catch (e: Exception) {
            Log.e("Beldex", "Couldn't join social group.", e)
            withContext(Dispatchers.Main) {
                // hideLoader()
                Toast.makeText(
                    context,
                    R.string.activity_join_public_chat_error,
                    Toast.LENGTH_SHORT
                ).show()
            }
            return@launch
        }
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun JoinSocialGroupPreview() {
    BChatPreviewContainer {
        JoinSocialGroupScreen(
            uiState = DefaultGroupsViewModel.UIState(),
            groups = emptyList(),
            onEvent = {}
        )
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun JoinSocialGroupPreviewLight() {
    BChatPreviewContainer {
        JoinSocialGroupScreen(
            uiState = DefaultGroupsViewModel.UIState(),
            groups = emptyList(),
            onEvent = {}
        )
    }
}