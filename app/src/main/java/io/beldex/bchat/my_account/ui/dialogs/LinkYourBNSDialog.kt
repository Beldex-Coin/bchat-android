package io.beldex.bchat.my_account.ui.dialogs

import io.beldex.bchat.util.BChatToast
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import io.beldex.bchat.compose_utils.DialogCancelButton
import io.beldex.bchat.compose_utils.RobotoMono
import io.beldex.bchat.compose_utils.OpenSans
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
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
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.R
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import io.beldex.bchat.compose_utils.PrimaryButton
import io.beldex.bchat.my_account.ui.MyAccountViewModel
import io.beldex.bchat.CheckOnline
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
                    BChatToast.makeText(context, context.resources.getString(R.string.invalid_bns_warning_message), Toast.LENGTH_SHORT).show()
                }
            }.failUi { exception ->
                bnsLoader = false
                var message =
                    context.resources.getString(R.string.bns_name_warning_message)
                exception.localizedMessage?.let {
                    message = context.resources.getString(R.string.bns_name_warning_message)
                    Log.d("Beldex", "BNS exception $it")
                }
                BChatToast.makeText(context, message, Toast.LENGTH_SHORT).show()
                result(false)
            }
        }

        fun verifyBNSName(bnsName: String): Boolean {
            return bnsName.isNotEmpty() &&
                    bnsName.length > 4 &&
                    (bnsName.endsWith(".bdx") || bnsName.endsWith(".BDX"))
        }

        val configuration = LocalConfiguration.current
        val isLandscape =
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val dialogMaxHeight = (configuration.screenHeightDp * 0.9f).dp
        val fieldBorder = when {
            showErrorMessage -> MaterialTheme.appColors.negativeRedButtonBorder
            isVerified -> MaterialTheme.appColors.userDetailsConfirmBackground
            else -> MaterialTheme.appColors.pinBoxInactiveBorder
        }
        val lightButton = Color(0xFFE0E0E0)
        val darkText = Color(0xFF0B0B0B)
        val disabledText = MaterialTheme.appColors.onboardingCaptionColor
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = dialogMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 15.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.link_bns),
                    color = MaterialTheme.appColors.onboardingInputText,
                    fontFamily = OpenSans,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(0xCC222222))
                        .border(1.dp, MaterialTheme.appColors.dividerColor)
                        .clickable { onDismissRequest(false) }
                ) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = null,
                        tint = MaterialTheme.appColors.onboardingInputText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.your_bchat_id),
                color = MaterialTheme.appColors.onboardingInputText,
                fontFamily = OpenSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.appColors.onboardingInputBackground)
                    .border(1.dp, MaterialTheme.appColors.pinBoxInactiveBorder)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = state.publicKey ?: "",
                    color = MaterialTheme.appColors.onboardingInputText,
                    fontFamily = RobotoMono,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.bns_name),
                color = MaterialTheme.appColors.onboardingInputText,
                fontFamily = OpenSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.appColors.onboardingInputBackground)
                    .border(1.dp, fieldBorder)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                if (bnsName.isEmpty()) {
                    Text(
                        text = stringResource(R.string.bns_textfield_hint).uppercase(),
                        color = MaterialTheme.appColors.onboardingInputHint,
                        fontFamily = RobotoMono,
                        fontSize = 12.sp
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = bnsName,
                    onValueChange = {
                        bnsName = it
                        isVerified = false
                        showErrorMessage = false
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    textStyle = TextStyle(
                        color = if (isVerified) MaterialTheme.appColors.userDetailsConfirmBackground else MaterialTheme.appColors.onboardingInputText,
                        fontFamily = RobotoMono,
                        fontSize = 12.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.appColors.textSelectionColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogCancelButton(
                    text = stringResource(R.string.cancel),
                    onClick = { onDismissRequest(false) },
                    fontFamily = RobotoMono,
                    textColor = MaterialTheme.appColors.onboardingInputText,
                    modifier = Modifier.weight(1f)
                )
                val canVerify = verifyBNSName(bnsName)
                PrimaryButton(
                    onClick = {
                        if (CheckOnline.isOnline(context)) {
                            if (!isVerified) {
                                if (verifyBNSName(bnsName) && state.publicKey.isNotEmpty()) {
                                    verifyBNS(bnsName.trim().lowercase(Locale.US), state.publicKey, context, result = {
                                        isVerified = it
                                        showErrorMessage = !it
                                    })
                                }
                            }
                        } else {
                            BChatToast.makeText(context, context.getString(R.string.please_check_your_internet_connection), Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = notchedCornerShape(12.dp),
                    containerColor = if (canVerify) lightButton else MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground,
                    contentColor = if (canVerify) darkText else disabledText,
                    border = if (canVerify) BorderStroke(1.dp, MaterialTheme.appColors.userDetailsConfirmBackground.copy(alpha = 0.5f)) else null,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text(
                        text = if (isVerified) stringResource(id = R.string.verified) else stringResource(R.string.verify),
                        fontFamily = RobotoMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        androidx.compose.material3.Icon(
                            painter = painterResource(id = R.drawable.ic_toast_check),
                            contentDescription = "Bns verified",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            PrimaryButton(
                onClick = {
                    if (isVerified) {
                        TextSecurePreferences.setIsBNSHolder(context, bnsName)
                        MessagingModuleConfiguration.shared.storage.setIsBnsHolder(state.publicKey, true)
                        onDismissRequest(true)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = notchedCornerShape(12.dp),
                enabled = isVerified,
                containerColor = lightButton,
                contentColor = darkText,
                disabledContainerColor = MaterialTheme.appColors.onboardingPrimaryButtonDisabledBackground,
                disabledContentColor = disabledText,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = stringResource(R.string.link),
                    fontFamily = RobotoMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
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

        io.beldex.bchat.compose_utils.BChatLoaderContent(text = stringResource(id = R.string.verify_bns))
    }

}