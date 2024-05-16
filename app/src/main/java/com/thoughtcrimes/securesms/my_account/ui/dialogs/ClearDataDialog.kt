package com.thoughtcrimes.securesms.my_account.ui.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beldex.libbchat.mnode.MnodeAPI
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Log
import com.thoughtcrimes.securesms.ApplicationContext
import com.thoughtcrimes.securesms.compose_utils.BChatRadioButton
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.DialogContainer
import com.thoughtcrimes.securesms.compose_utils.appColors
import com.thoughtcrimes.securesms.util.ConfigurationMessageUtilities
import com.thoughtcrimes.securesms.util.Helper
import io.beldex.bchat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Steps {
    INFO_PROMPT_DEFAULT,
    INFO_PROMPT,
    NETWORK_PROMPT,
    DELETING
}

enum class DeleteOption {
    Device,
    Network
}

@Composable
fun ClearDataDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val resource = context.resources
    var step by remember {
        mutableStateOf(Steps.INFO_PROMPT_DEFAULT)
    }
    fun removeWallet(){
        val walletFolder: File = Helper.getWalletRoot(context)
        val walletName = TextSecurePreferences.getWalletName(context)
        val walletFile = File(walletFolder, walletName!!)
        val walletKeys = File(walletFolder, "$walletName.keys")
        val walletAddress = File(walletFolder,"$walletName.address.txt")
        if(walletFile.exists()) {
            walletFile.delete() // when recovering wallets, the cache seems corrupt - so remove it
        }
        if(walletKeys.exists()) {
            walletKeys.delete()
        }
        if(walletAddress.exists()) {
            walletAddress.delete()
        }
    }
    fun clearAllData(deleteNetworkMessages: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            val previousStep = step
            withContext(Dispatchers.Main) {
                step = Steps.DELETING
            }

            if (!deleteNetworkMessages) {
                try {
                    ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(context).get()
                } catch (e: Exception) {
                    Log.e("Beldex", "Failed to force sync", e)
                }

                //New Line
                removeWallet()

                ApplicationContext.getInstance(context).clearAllData(false)
                withContext(Dispatchers.Main) {
                    onDismissRequest()
                }
            } else {
                // finish
                val result = try {
                    MnodeAPI.deleteAllMessages().get()
                } catch (e: Exception) {
                    null
                }

                if (result == null || result.values.any { !it } || result.isEmpty()) {
                    // didn't succeed (at least one)
                    withContext(Dispatchers.Main) {
                        step = previousStep
                    }
                } else if (result.values.all { it }) {
                    //New Line
                    removeWallet()
                    // don't force sync because all the messages are deleted?
                    ApplicationContext.getInstance(context).clearAllData(false)
                    withContext(Dispatchers.Main) {
                        onDismissRequest()
                    }
                }
            }
        }
    }

    val buttonTitle by remember(step) {
        mutableStateOf(
            value = when (step) {
                Steps.INFO_PROMPT_DEFAULT-> {
                    resource.getString(R.string.ok)
                }
                Steps.NETWORK_PROMPT -> {
                     resource.getString(R.string.delete)
                }
                Steps.INFO_PROMPT -> {
                    resource.getString(R.string.clear)
                }
                Steps.DELETING -> {

                }
            }
        )
    }
    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = {
            if (step != Steps.DELETING) {
                onDismissRequest()
            }
        }
    ) {
        var selectedOption by remember {
            mutableStateOf(DeleteOption.Device)
        }
        val buttonClick: () -> Unit =  {
            when (step) {
                Steps.INFO_PROMPT_DEFAULT -> {
                    step = if (selectedOption == DeleteOption.Network) {
                        Steps.NETWORK_PROMPT
                    } else {
                        Steps.INFO_PROMPT
                    }
                }
                Steps.INFO_PROMPT -> {
                    clearAllData(false)
                }
                Steps.NETWORK_PROMPT -> {
                    clearAllData(true)
                }
                Steps.DELETING -> {

                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = when (step) {
                    Steps.INFO_PROMPT_DEFAULT,
                    Steps.INFO_PROMPT -> {
                        stringResource(id = R.string.dialog_clear_all_data_title)
                    }
                    Steps.NETWORK_PROMPT -> {
                        stringResource(id = R.string.dialog_clear_all_data_clear_device_and_network_title)
                    }
                    Steps.DELETING -> {
                        ""
                    }
                },
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                Steps.INFO_PROMPT_DEFAULT -> {
                    DeleteOption(
                        isSelected = selectedOption == DeleteOption.Device,
                        title = stringResource(id = R.string.clear_data_from_device),
                        description = stringResource(id = R.string.delete_data_on_this_device),
                        onClicked = {
                            selectedOption = DeleteOption.Device
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DeleteOption(
                        isSelected = selectedOption == DeleteOption.Network,
                        title = stringResource(id = R.string.delete_entire_account),
                        description = stringResource(id = R.string.delete_data_from_the_network),
                        onClicked = {
                            selectedOption = DeleteOption.Network
                        }
                    )
                }
                Steps.INFO_PROMPT -> {
                    Text(
                        text = stringResource(id = R.string.dialog_clear_all_data_explanation),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Steps.NETWORK_PROMPT -> {
                    Text(
                        text = stringResource(id = R.string.dialog_clear_all_data_network_explanation),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Steps.DELETING -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.appColors.primaryButtonColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (step != Steps.DELETING) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.appColors.secondaryButtonColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = buttonClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if(buttonTitle == stringResource(R.string.clear) || buttonTitle == stringResource(
                                    id = R.string.delete
                                ))MaterialTheme.appColors.walletDashboardMainMenuCardBackground else MaterialTheme.appColors.primaryButtonColor,
                            contentColor = if(buttonTitle == stringResource(R.string.clear) || buttonTitle == stringResource(
                                    id = R.string.delete
                                )) Color.Red else Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text = buttonTitle.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if(buttonTitle == stringResource(R.string.clear) || buttonTitle == stringResource(
                                        id = R.string.delete
                                    )) Color.Red else Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteOption(
    isSelected: Boolean,
    title: String,
    description: String,
    onClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClicked()
            }
    ) {
        BChatRadioButton(
            selected = isSelected,
            onClick = {
                onClicked()
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = 8.dp
                )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.appColors.secondaryTextColor
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
    showBackground = true
)
@Composable
fun ClearDataDialogPreview() {
    BChatTheme {
        ClearDataDialog(
            onDismissRequest = {}
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
    showBackground = true
)
@Composable
fun ClearDataDialogPreviewDark() {
    BChatTheme {
        ClearDataDialog(
            onDismissRequest = {}
        )
    }
}