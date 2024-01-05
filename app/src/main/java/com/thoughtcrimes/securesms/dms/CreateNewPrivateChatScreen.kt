package com.thoughtcrimes.securesms.dms

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.PublicKeyValidation
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.BChatTypography
import com.thoughtcrimes.securesms.compose_utils.PrimaryButton
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent
import com.thoughtcrimes.securesms.my_account.ui.MyAccountActivity
import com.thoughtcrimes.securesms.onboarding.ui.RestoreSeedScreen
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import io.beldex.bchat.R

class CreateNewPrivateChatScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BChatTheme() {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CreateNewPrivateChat()
                }
            }
        }
    }
}

@Composable
fun CreateNewPrivateChat() {
    var bchatID by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.screen_background))

    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .padding(10.dp)
                .weight(1f)
        ) {
            Text(
                text = "New Chat",
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor
                ),
                fontSize = 22.sp
            )

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp),
                colors = CardDefaults.cardColors(colorResource(id = R.color.card_color))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = bchatID,
                        placeholder = { Text(text = "Enter Chat ID") },
                        onValueChange = {
                            bchatID = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(end = 10.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = colorResource(id = R.color.your_bchat_id_bg),
                            focusedContainerColor = colorResource(id = R.color.your_bchat_id_bg),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = colorResource(id = R.color.button_green)
                        )
                    )

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorResource(id = R.color.your_bchat_id_bg), //Card background color
                        ),
                        modifier = Modifier
                            .height(100.dp)
                            .width(55.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_qr_code),
                                contentDescription = "",
                                modifier = Modifier.clickable {
                                }
                            )
                        }
                    }
                }

                PrimaryButton(
                    onClick = {
                        if (bchatID.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Please enter BChat ID",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            createPrivateChatIfPossible(bchatID, context)
                        }
                        // context.startActivity(Intent(context, OnBoardingActivity::class.java))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Let's BChat",
                        style = BChatTypography.bodyLarge.copy(
                            color = Color.White
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                    )
                }

            }

            OutlinedCard(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.your_bchat_id_bg),
                    contentColor = colorResource(id = R.color.text)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.5f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable {
                            val intent = Intent(context, MyAccountActivity::class.java)
                            context.startActivity(intent)
                        }
                ) {

                    Image(
                        painter = painterResource(id = R.drawable.your_bchat_id),
                        contentDescription = ""
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Your Chat ID",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = colorResource(id = R.color.text)
                        ),
                        modifier = Modifier
                            .padding(
                                vertical = 4.dp
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Image(
                        painter = painterResource(id = R.drawable.ic_arrow_green),
                        contentDescription = ""
                    )
                }
            }

        }

    }

}


//BNS disabled 16-01-2023
private fun createPrivateChatIfPossible(bnsNameOrPublicKey: String, context: Context) {
    if (PublicKeyValidation.isValid(bnsNameOrPublicKey)) {
        Log.d("PublicKeyValidation", "OK")
        createPrivateChat(bnsNameOrPublicKey, context)
    } else {
        Log.d("PublicKeyValidation", "Cancel")

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
}

private fun createPrivateChat(hexEncodedPublicKey: String, context: Context) {
    val activity = (context as? Activity)
    val recipient = Recipient.from(context, Address.fromSerialized(hexEncodedPublicKey), false)
    val bundle = Bundle()
    val intent = Intent()
    bundle.putParcelable(ConversationFragmentV2.URI, intent.data)
    bundle.putString(ConversationFragmentV2.TYPE, intent.type)
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
    BChatTheme() {
        CreateNewPrivateChat()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun CreateNewPrivateChatPreviewLight() {
    BChatTheme {
        CreateNewPrivateChat()
    }
}