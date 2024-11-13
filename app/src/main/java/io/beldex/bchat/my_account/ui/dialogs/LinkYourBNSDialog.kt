package io.beldex.bchat.my_account.ui.dialogs

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.beldex.bchat.compose_utils.BChatTypography
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.R
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.my_account.ui.MyAccountViewModel
import io.beldex.bchat.wallet.CheckOnline
import kotlinx.coroutines.delay
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import java.util.Locale

@Composable
fun LinkYourBNSDialog(
    state: MyAccountViewModel.UIState,
    onDismissRequest: (isBnsHolderStatus:Boolean) -> Unit
) {
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = {
            onDismissRequest(false)
        },
    ) {
        val context = LocalContext.current
        var bnsName by remember {
            mutableStateOf("")
        }
        var bnsLoader by remember {
            mutableStateOf(false)
        }
        var isVerified by remember {
            mutableStateOf(false)
        }
        var showErrorMessage by remember {
            mutableStateOf(false)
        }

        if(bnsLoader) {
            BnsNameVerifyingLoader(onDismiss = {
                bnsLoader = false
            })
        }

        fun verifyBNS(bnsName: String, publicKey: String, context: Context, result: (status: Boolean) -> Unit) {
            // This could be an BNS name
            bnsLoader = true
            MnodeAPI.getBchatID(bnsName).successUi { hexEncodedPublicKey ->
                bnsLoader = false
                if(hexEncodedPublicKey == publicKey){
                    result(true)
                }else{
                    result(false)
                    Toast.makeText(context, context.resources.getString(R.string.invalid_bns_warning_message), Toast.LENGTH_SHORT).show()
                }
            }.failUi { exception ->
                bnsLoader = false
                var message =
                    context.resources.getString(R.string.bns_name_warning_message)
                exception.localizedMessage?.let {
                    message = context.resources.getString(R.string.bns_name_warning_message)
                    Log.d("Beldex", "BNS exception $it")
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                result(false)
            }
        }

        fun verifyBNSName(bnsName:String):Boolean{
            return bnsName.isNotEmpty() && bnsName.length > 4 && bnsName.substring(bnsName.length-4) == ".bdx"
        }

        Column(
            modifier =Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {
            Text(
                text = "Link BNS",
                style = BChatTypography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight(700),
                    textAlign = TextAlign.Center
                ),
                modifier =Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom=15.dp)
            )
            Text(
                text = "Your BChat ID",
                style = BChatTypography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(600),
                    textAlign = TextAlign.Start
                ),
                modifier =Modifier
                        .align(Alignment.Start)
                        .padding(start=5.dp, bottom=5.dp)
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.appColors.linkBnsAddressBackground
                ),
                modifier =Modifier
                        .fillMaxWidth()
                        .padding(bottom=10.dp)
            ) {
                Text(
                    text = state.publicKey ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(
                            16.dp
                        )
                )
            }
            Text(
                text = "BNS Name",
                style = BChatTypography.titleMedium.copy(
                    color = MaterialTheme.appColors.editTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(600),
                    textAlign = TextAlign.Start
                ),
                modifier =Modifier
                        .align(Alignment.Start)
                        .padding(start=5.dp, bottom=5.dp)
            )
            TextField(
                value = bnsName,
                textStyle = TextStyle(
                    color = if(isVerified) MaterialTheme.appColors.negativeGreenButtonBorder else MaterialTheme.appColors.secondaryContentColor
                ),
                placeholder = {
                    Text(
                        text = stringResource(R.string.bns_textfield_hint),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.appColors.textFieldDescriptionColor
                        )
                    )
                },
                onValueChange = {
                    bnsName = it.trim().lowercase(Locale.US)
                    isVerified = false
                    showErrorMessage = false
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = if(showErrorMessage) {
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp)
                        .border(
                            1.dp,
                            color = MaterialTheme.appColors.negativeRedButtonBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                }else {
                    if (isVerified) {
                        Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp)
                        .border(
                            1.dp,
                            color = MaterialTheme.appColors.negativeGreenButtonBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }else {
                        Modifier
                        .fillMaxWidth()
                        .padding(bottom=15.dp)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.appColors.contactCardBackground,
                    focusedContainerColor = MaterialTheme.appColors.contactCardBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    selectionColors = TextSelectionColors(
                        MaterialTheme.appColors.textSelectionColor,
                        MaterialTheme.appColors.textSelectionColor
                    ),
                    cursorColor = MaterialTheme.appColors.secondaryContentColor
                )
            )
            Row(
                modifier =Modifier
                        .fillMaxWidth()
                        .padding(bottom=10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { onDismissRequest(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.negativeGreenButton,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.appColors.negativeGreenButtonBorder
                    ),
                    contentPadding = PaddingValues(
                        vertical = 14.dp
                    ),
                    modifier = Modifier.weight(1F)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.appColors.negativeGreenButtonText,
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .padding(
                                vertical = 4.dp
                            )
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                OutlinedButton(
                    onClick = {
                        if (CheckOnline.isOnline(context)) {
                            if (!isVerified) {
                                if (verifyBNSName(bnsName) && state.publicKey.isNotEmpty()) {
                                    verifyBNS(bnsName, state.publicKey, context, result={
                                        isVerified=it
                                        showErrorMessage=!it
                                    })
                                }
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.contactCardBackground,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (verifyBNSName(bnsName)) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.contactCardBackground
                    ),
                    contentPadding = PaddingValues(
                        vertical = 14.dp
                    ),
                    modifier = Modifier.weight(1F)
                ) {
                    Text(
                        text = if(isVerified) stringResource(id = R.string.verified) else stringResource(R.string.verify),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (verifyBNSName(bnsName)) if(isVerified) MaterialTheme.appColors.primaryButtonColor else MaterialTheme.appColors.secondaryContentColor else MaterialTheme.appColors.linkBnsDisabledButtonContent,
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .padding(
                                vertical = 4.dp
                            )
                    )
                    if(isVerified){
                        Image(painter = painterResource(id = R.drawable.ic_message_sent), contentDescription = "Bns verified", modifier =Modifier
                                .size(20.dp)
                                .align(Alignment.CenterVertically)
                                .padding(start=5.dp))
                    }
                }
            }
            PrimaryButton(
                onClick = {
                    if(isVerified) {
                        TextSecurePreferences.setIsBNSHolder(context, bnsName)
                        MessagingModuleConfiguration.shared.storage.setIsBnsHolder(state.publicKey,true)
                        onDismissRequest(true)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = isVerified,
                disabledContainerColor = MaterialTheme.appColors.contactCardBackground,
            ) {
                Text(
                    text = stringResource(R.string.link),
                    style = BChatTypography.titleMedium.copy(
                        color = if (isVerified) {
                            Color.White
                        } else {
                            MaterialTheme.appColors.linkBnsDisabledButtonContent
                        },
                        fontWeight = FontWeight(400),
                        fontSize = 16.sp
                    ),
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun BnsNameVerifyingLoader(onDismiss: () -> Unit) {

    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(CheckOnline.isOnline(context)) }
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            onDismiss()
        }
        while (isConnected) {
            delay(1000)
            isConnected=CheckOnline.isOnline(context)
        }
    }

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