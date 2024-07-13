package com.thoughtcrimes.securesms.conversation_v2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.PublicKeyValidation
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.compose_utils.ui.BChatPreviewContainer
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.dms.PrivateChatScanQRCodeActivity
import com.thoughtcrimes.securesms.my_account.ui.MyProfileActivity
import com.thoughtcrimes.securesms.wallet.CheckOnline
import io.beldex.bchat.R
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

@Composable
fun CreatePrivateChatScreen() {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var bChatId by remember {
        mutableStateOf("")
    }
    var bnsLoader by remember {
        mutableStateOf(false)
    }
    val gotoMyProfile: () -> Unit = {
        val intent = Intent(context, MyProfileActivity::class.java)
        intent.putExtra("profile_editable",false)
        context.startActivity(intent)
    }
    val privateChatScanQRCodeActivityResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val hexEncodedPublicKey = result.data!!.getStringExtra(ConversationFragmentV2.HEX_ENCODED_PUBLIC_KEY)
            val bnsName = result.data!!.getStringExtra(ConversationFragmentV2.BNS_NAME)
            if(hexEncodedPublicKey!=null) {
                createPrivateChat(hexEncodedPublicKey, context, bnsName.toString())
            }
        }
    }
     fun createPrivateChatIfPossible(bnsNameOrPublicKey: String, context: Context) {
        if (PublicKeyValidation.isValid(bnsNameOrPublicKey)) {
            createPrivateChat(bnsNameOrPublicKey, context, bnsNameOrPublicKey)
        } else {
            //Toast.makeText(context, R.string.invalid_bchat_id, Toast.LENGTH_SHORT).show()
            // This could be an BNS name
            bnsLoader = true
            MnodeAPI.getBchatID(bnsNameOrPublicKey).successUi { hexEncodedPublicKey ->
                bnsLoader = false
                createPrivateChat(hexEncodedPublicKey,context,bnsNameOrPublicKey)
            }.failUi { exception ->
                bnsLoader = false
                var message = context.resources.getString(R.string.fragment_enter_public_key_error_message)
                exception.localizedMessage?.let {
                    message = context.resources.getString(R.string.fragment_enter_public_key_error_message)
                    Log.d("Beldex","BNS exception $it")
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if(bnsLoader) {
        BnsLoadingPopUp(onDismiss = {
            bnsLoader = false

        })
    }

    Column(
        modifier =Modifier
                .fillMaxWidth()
                .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.activity_create_private_chat_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight(800)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.appColors.contactCardBackground
            )
        ) {
            Row(
                modifier =Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
            ) {
                TextField(
                    value = bChatId,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.enter_chat_id),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onValueChange = {
                        bChatId = it
                    },
                    modifier =Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(end=10.dp)
                            .weight(1f),
                    shape = RoundedCornerShape(16.dp),
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

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =Modifier
                            .height(100.dp)
                            .background(
                                    shape=RoundedCornerShape(16.dp),
                                    color=MaterialTheme.appColors.disabledButtonContainerColor
                            )
                            .padding(16.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_qr_code),
                        contentDescription = "",
                        colorFilter = ColorFilter.tint(
                            color = MaterialTheme.appColors.iconTint
                        ),
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(
                                    context,
                                    PrivateChatScanQRCodeActivity::class.java
                                )
                                privateChatScanQRCodeActivityResultLauncher.launch(intent)
                            }
                    )
                }
            }

            PrimaryButton(
                onClick = {
                    if (CheckOnline.isOnline(context)) {
                        if (bChatId.isEmpty()) {
                            Toast.makeText(
                                    context,
                                    "Please enter BChat ID",
                                    Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            createPrivateChatIfPossible(bChatId, context)
                            keyboardController?.hide()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier =Modifier
                        .fillMaxWidth()
                        .padding(start=16.dp, end=16.dp, bottom=16.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = bChatId.isNotEmpty(),
                disabledContainerColor = MaterialTheme.appColors.disabledButtonContainerColor,
            ) {
                Text(
                    text = stringResource(R.string.let_s_bchat),
                    style = BChatTypography.titleMedium.copy(
                        color = if (bChatId.isNotEmpty()) {
                            Color.White
                        } else {
                            MaterialTheme.appColors.disabledButtonContent
                        },
                        fontWeight = FontWeight(600)
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = gotoMyProfile,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.appColors.disabledButtonContainerColor,
            ),
            contentPadding = PaddingValues(
                vertical = 16.dp,
                horizontal = 24.dp
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.your_bchat_id),
                contentDescription = "",
                colorFilter = ColorFilter.tint(
                    color = MaterialTheme.appColors.iconTint
                )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(R.string.your_chat_id),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(
                        vertical = 4.dp
                    )
            )

            Spacer(modifier = Modifier.width(24.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_arrow_green),
                contentDescription = ""
            )
        }
    }
}

@Composable
fun BnsLoadingPopUp(onDismiss: () -> Unit) {
    DialogContainer(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            onDismissRequest = onDismiss,
    ) {

        OutlinedCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dialogBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier =Modifier
                    .fillMaxWidth()
                    .padding(15.dp)) {
                Box(
                        contentAlignment= Alignment.Center,
                        modifier =Modifier
                                .size(55.dp)
                                .background(color=MaterialTheme.appColors.circularProgressBarBackground, shape=CircleShape),
                ){
                    CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.appColors.primaryButtonColor,
                            strokeWidth = 2.dp
                    )
                }
                Text(text = stringResource(id = R.string.verify_bns), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight(800), color = MaterialTheme.appColors.primaryButtonColor), modifier = Modifier.padding(10.dp))
            }
        }
    }

}


//BNS disabled 16-01-2023
/*private fun createPrivateChatIfPossible(bnsNameOrPublicKey: String, context: Context) {
    if (PublicKeyValidation.isValid(bnsNameOrPublicKey)) {
        createPrivateChat(bnsNameOrPublicKey, context)
    } else {
        Toast.makeText(context, R.string.invalid_bchat_id, Toast.LENGTH_SHORT).show()

        //Important 02-06-2022 - 2.30 PM
        // This could be an BNS name
        /*showLoader()
            MnodeAPI.getBchatID(bnsNameOrPublicKey).successUi { hexEncodedPublicKey ->
            Log.d("PublicKeyValidation", "successUi")
            hideLoader()
            Log.d("Beldex", "value of Bchat id for BNS name $hexEncodedPublicKey")
            this.createPrivateChat(hexEncodedPublicKey)
        }.failUi { exception ->
            hideLoader()
            val message = resources.getString(R.string.fragment_enter_public_key_error_message)
            exception.localizedMessage?.let {
                *//*message = it*//*
                    Log.d("Beldex","BNS exception $it")
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }*/
    }
}*/

private fun createPrivateChat(hexEncodedPublicKey: String, context: Context,bnsName: String ) {
    val activity = (context as? Activity)
    val recipient = Recipient.from(context, Address.fromSerialized(hexEncodedPublicKey), false)
    val bundle = Bundle()
    val intent = Intent()
    bundle.putParcelable(ConversationFragmentV2.URI, intent.data)
    bundle.putString(ConversationFragmentV2.TYPE, intent.type)
    bundle.putString(ConversationFragmentV2.BNS_NAME,bnsName)
    val returnIntent = Intent()
    returnIntent.putExtra(ConversationFragmentV2.ADDRESS, recipient.address)
    //returnIntent.setDataAndType(intent.data, intent.type)
    val existingThread =
        DatabaseComponent.get(context).threadDatabase().getThreadIdIfExistsFor(recipient)
    returnIntent.putExtra(ConversationFragmentV2.THREAD_ID, existingThread)
    returnIntent.putExtras(bundle)
    activity?.setResult(ComponentActivity.RESULT_OK, returnIntent)
    activity?.finish()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CreateNewPrivateChatPreview() {
    BChatPreviewContainer {
        CreatePrivateChatScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun CreateNewPrivateChatPreviewLight() {
    BChatPreviewContainer {
        CreatePrivateChatScreen()
    }
}