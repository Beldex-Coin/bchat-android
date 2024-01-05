package com.thoughtcrimes.securesms.groups

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.PublicKeyValidation
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.dms.CreateNewPrivateChat
import com.thoughtcrimes.securesms.my_account.ui.MyAccountViewModel
import com.thoughtcrimes.securesms.util.ConfigurationMessageUtilities
import com.thoughtcrimes.securesms.util.State
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class JoinSocialGroupScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BChatTheme() {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JoinSocialGroup()
                }
            }
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun JoinSocialGroup() {
    var socialGroupUrl by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current
    val activity = (context as? Activity)

    val viewModel: DefaultGroupsViewModel = hiltViewModel()

    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

  // var groups by viewModel.defaultGroups.collectAsState()

    var groups by remember {
        mutableStateOf(mutableListOf<OpenGroupAPIV2.DefaultGroup>())
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


    viewModel.defaultRooms.observe(lifecycleOwner) { state ->
        when (state) {
            State.Loading -> {
                println("default group loading called 1 $state")
            }

            is State.Error -> {
                println("default group error called 1 $state")
            }

            is State.Success -> {
                groups = state.value as MutableList<OpenGroupAPIV2.DefaultGroup>
            }
        }
    }


    val joinPublicChatScanQRCodeActivityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = Bundle()
            extras.putParcelable(
                ConversationFragmentV2.ADDRESS, result.data!!.getParcelableExtra(
                    ConversationFragmentV2.ADDRESS
                )
            )
            extras.putLong(
                ConversationFragmentV2.THREAD_ID, result.data!!.getLongExtra(
                    ConversationFragmentV2.THREAD_ID, -1
                )
            )
            val returnIntent = Intent()
            returnIntent.putExtras(extras)
            activity?.setResult(ComponentActivity.RESULT_OK, returnIntent)
            activity?.finish()
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.screen_background))

    ) {
        /*   if(showLoader) {
              Row(
                  modifier = Modifier
                      .fillMaxSize()
                      .background(color = Color.Transparent)

              ) {
                  LottieAnimation(
                      composition,
                      progress,
                      modifier = Modifier
                          .size(100.dp)
                          .align(Alignment.CenterVertically)
                  )

              }
          }*/

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = "Join Social Group",
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor
                ),
                fontSize = 22.sp
            )
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                colors = CardDefaults.cardColors(colorResource(id = R.color.card_color))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    TextField(
                        value = socialGroupUrl,
                        placeholder = { Text(text = "Enter Group URL") },
                        onValueChange = {
                            socialGroupUrl = it
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = colorResource(id = R.color.your_bchat_id_bg),
                            focusedContainerColor = colorResource(id = R.color.your_bchat_id_bg),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = colorResource(id = R.color.button_green)
                        ),
                        trailingIcon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_qr_code),
                                contentDescription = "",
                                modifier = Modifier.clickable {
                                    val intent = Intent(
                                        context,
                                        JoinPublicChatScanQRCodeActivity::class.java
                                    )
                                    joinPublicChatScanQRCodeActivityResultLauncher.launch(intent)
                                })
                        }
                    )

                }

                PrimaryButton(
                    onClick = {
                        joinPublicChatIfPossible(socialGroupUrl, lifecycleOwner, context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = socialGroupUrl.isNotEmpty(),
                    disabledContainerColor = colorResource(id = R.color.your_bchat_id_bg),
                    disabledContentColor = colorResource(id = R.color.text)
                ) {
                    Text(
                        text = "Next",
                        modifier = Modifier
                            .padding(8.dp),
                        style = BChatTypography.bodyLarge.copy(
                            color = Color.White
                        )
                    )
                }

            }
            Text(
                text = "Or Join",
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor
                ),
                fontSize = 22.sp
            )
            println("default groups $groups")

            if (groups.isEmpty()) {
                LottieAnimation(
                    composition,
                    progress,
                    modifier = Modifier
                        .size(70.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(75.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                    contentPadding = PaddingValues(all = 10.dp),
                    modifier = Modifier.clickable {
                    },
                    content = {
                        items(groups.size) { i ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .width(85.dp)
                                    .height(112.dp)
                                    .background(
                                        colorResource(id = R.color.your_bchat_id_bg),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        showLoader = true
                                        joinPublicChatIfPossible(
                                            groups[i].joinURL,
                                            lifecycleOwner,
                                            context
                                        )
                                    },
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,


                                ) {
                                groups[i].image?.let { convertImageByteArrayToBitmap(it).asImageBitmap() }
                                    ?.let {
                                        Image(
                                            bitmap = it,
                                            contentDescription = "",
                                            modifier = Modifier
                                                .width(52.dp)
                                                .height(52.dp)
                                                .clip(shape = RoundedCornerShape(6.dp)),
                                            alignment = Alignment.Center
                                        )
                                    }

                                Text(
                                    modifier = Modifier
                                        .padding(top = 5.dp),
                                    text = groups[i].name,
                                    textAlign = TextAlign.Center,
                                    style = BChatTypography.bodySmall.copy(
                                        color = colorResource(id = R.color.text),
                                        fontSize = 12.sp
                                    ),
                                )
                            }

                        }

                    })
            }
        }

    }
}

fun convertImageByteArrayToBitmap(imageData: ByteArray): Bitmap {
    println("default group image convert")
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
                openConversationActivity(threadID, recipient, activity)
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

fun openConversationActivity(threadId: Long, recipient: Recipient, activity: Activity?) {
    val returnIntent = Intent()
    returnIntent.putExtra(ConversationFragmentV2.THREAD_ID, threadId)
    returnIntent.putExtra(ConversationFragmentV2.ADDRESS, recipient.address)
    activity?.setResult(ComponentActivity.RESULT_OK, returnIntent)
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun JoinSocialGroupPreview() {
    BChatTheme() {
        JoinSocialGroup()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun JoinSocialGroupPreviewLight() {
    BChatTheme {
        JoinSocialGroup()
    }
}