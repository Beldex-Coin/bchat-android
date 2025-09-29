package io.beldex.bchat.conversation.v2.contact_sharing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.DialogContainer
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.ProfilePictureComponent
import io.beldex.bchat.compose_utils.ProfilePictureMode
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.ui.ScreenContainer
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.conversation.v2.ConversationFragmentV2.Companion.THREAD_ID
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.util.BaseFragment
import io.beldex.bchat.wallet.OnBackPressedListener


class ViewAllContactFragment : BaseFragment(), OnBackPressedListener {

    private var contact : ContactModel?=null

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            contact=arguments?.getParcelable(CONTACTMODEL)
        }
    }

    override fun onCreateView(
        inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?
    ) : View {

        return ComposeView(requireContext()).apply {
            setContent {
                BChatTheme {
                    Surface(
                        modifier=Modifier.fillMaxSize(),
                        color=MaterialTheme.appColors.cardBackground
                    ) {
                        ScreenContainer(
                            title=stringResource(id=R.string.view_contacts),
                            onBackClick={ requireActivity().onBackPressedDispatcher.onBackPressed() },
                            modifier=Modifier
                                .fillMaxSize()
                                .background(
                                    color=MaterialTheme.appColors.walletDashboardMainMenuCardBackground
                                )
                        ) {
                            contact?.let {
                                ViewContactScreen(it)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val CONTACTMODEL="contact_model"
    }

    override fun onBackPressed() : Boolean {
        return false
    }

}

@Composable
fun ViewContactScreen(contact : ContactModel) {

    var showPopup by remember { mutableStateOf(false) }
    var selectedName by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

    if(showPopup){
        ChatWithContactPopUp(selectedName,selectedAddress, onDismiss = {
            showPopup = false
            selectedName = ""
            selectedAddress = ""
        })
    }


    LazyColumn(
        verticalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.padding(12.dp)
    ) {
        items(flattenData(contact.name).zip(flattenData(contact.address.serialize()))) { (name, address) ->
            ViewContactItem(
                name=name, address=address, modifier=Modifier.fillMaxWidth(), onChatTo={
                    showPopup = true
                    selectedName = name
                    selectedAddress = address
                }
            )
        }
    }
}

fun String.capitalizeFirstLetter() : String {
    return this.replaceFirstChar { it.uppercase() }
}

fun formatAddresses(address: String): String {
    val first = address.take(15)
    val last = address.takeLast(7)
    return "$first.........$last"
}

@Composable
private fun ViewContactItem(
    name : String,
    address : String,
    modifier : Modifier,
    onChatTo : () -> Unit,
) {

    OutlinedCard(
        shape=RoundedCornerShape(50), border=BorderStroke(
            width=1.dp, color=MaterialTheme.appColors.contactCardBorder
        ), colors=CardDefaults.cardColors(
            containerColor=MaterialTheme.appColors.contactCardBackground
        ), modifier=modifier.padding(bottom=4.dp)
    ) {
        Row(
            verticalAlignment=Alignment.CenterVertically,
            modifier=Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(modifier=Modifier) {
                ProfilePictureComponent(
                    publicKey=address,
                    displayName=name,
                    containerSize=36.dp,
                    pictureMode=ProfilePictureMode.SmallPicture
                )
            }

            Column(
                modifier=Modifier
                    .weight(1f)
                    .padding(start = 8.dp,end=16.dp)
            ) {
                Text(
                    name.capitalizeFirstLetter().ifEmpty { formatAddresses(address).capitalizeFirstLetter() },
                    color=MaterialTheme.appColors.textColor,
                    fontFamily=OpenSans,
                    fontWeight=FontWeight(600),
                    fontSize=16.sp,
                    maxLines=1,
                    overflow=TextOverflow.Ellipsis
                )

                Text(
                    formatAddresses(address),
                    maxLines=1,
                    fontFamily=OpenSans,
                    fontWeight=FontWeight(400),
                    fontSize=12.sp,
                    color=MaterialTheme.appColors.secondaryTextColor,
                    overflow=TextOverflow.Ellipsis
                )
            }

            Image(
                painter=painterResource(id=R.drawable.ic_chat_to_contact),
                contentDescription="chat to contact",
                modifier=Modifier
                    .padding(start=4.dp, end=4.dp)
                    .clickable {
                        onChatTo()
                    })
        }
    }
}

@Composable
fun ChatWithContactPopUp(name: String, address : String, onDismiss: () -> Unit){
    val nameOfContact by remember {
        mutableStateOf(name)
    }
    val context=LocalContext.current
    val activityCompact=context as AppCompatActivity

    fun moveToChat(address : String) {
        val addressForThread=Address.fromSerialized(address)
        val recipient=Recipient.from(context, addressForThread, true)
        val threadID=DatabaseComponent.get(context).threadDatabase()
            .getOrCreateThreadIdFor(recipient)
        val extras=Bundle()
        extras.putLong(THREAD_ID, threadID)
        val fragment=ConversationFragmentV2().apply {
            arguments=extras
        }
        activityCompact.supportFragmentManager.beginTransaction()
            .replace(R.id.activity_home_frame_layout_container, fragment).commit()
    }

    DialogContainer(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = nameOfContact.capitalizeFirstLetter().ifEmpty { formatAddresses(address) },
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.appColors.secondaryContentColor,
                    fontWeight = FontWeight(700),
                    fontSize = 16.sp
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.chat_with_contact_confirmation),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.appColors.titleTextColor,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.negativeGreenButton
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.appColors.negativeGreenButtonBorder),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.appColors.negativeGreenButtonText,
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(
                            vertical = 8.dp
                        )
                    )
                }

                Button(
                    onClick = {
                        moveToChat(address)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.appColors.negativeGreenButtonBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text= stringResource(id = R.string.message),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight(400),
                            fontSize = 14.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(
                            vertical = 8.dp
                        )
                    )
                }
            }
        }
    }
}