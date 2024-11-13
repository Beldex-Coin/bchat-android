package io.beldex.bchat.conversation_v2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.dms.NewChatScreen

enum class OpenActivity(val destination: String) {
    NewChat("new_chat"),
    SecretGroup("secret_group"),
    PublicGroup("open_group"),
    NoteToSelf("note-to-self"),
    InviteAFriend("invite-a-friend"),
    Conversation("conversation")
}

@AndroidEntryPoint
class NewChatConversationActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = this
            val contactViewModel: NewChatScreenViewModel = hiltViewModel()
            val searchQuery by contactViewModel.searchQuery.collectAsState()
            val contacts by contactViewModel.recipients.collectAsState(initial = listOf())
            val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            BChatTheme(
                darkTheme = isDarkTheme
            ) {
                // A surface container using the 'background' color from the theme
                val activity = (context as? Activity)
                if (TextSecurePreferences.isScreenSecurityEnabled(context))
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                Surface {
                    NewChatScreen(
                        searchQuery = searchQuery,
                        contacts = contacts,
                        onEvent = contactViewModel::onEvent,
                        openActivity = { i ->
                            when(i){
                                OpenActivity.NewChat -> {
                                    openActivity(1, context)
                                }
                                OpenActivity.SecretGroup -> {
                                    openActivity(2, context)
                                }
                                OpenActivity.PublicGroup -> {
                                    openActivity(3, context)
                                }
                                OpenActivity.NoteToSelf -> {
                                    openActivity(4, context)
                                }
                                OpenActivity.InviteAFriend -> {
                                    openActivity(5, context)
                                }
                                else -> return@NewChatScreen
                            }
                        },
                        openConversation = {
                            val returnIntent = Intent()
                            returnIntent.putExtra(ConversationFragmentV2.ACTIVITY_TYPE,6)
                            returnIntent.putExtra(ConversationFragmentV2.ADDRESS,it.address)
                            context.setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
                            context.finish()
                        },
                        onBackPress = {
                            context.finish()
                        },isDarkTheme)
                }
            }
        }
    }
}

fun openActivity(activityType:Int, activity: Activity){
    val returnIntent = Intent()
    returnIntent.putExtra(ConversationFragmentV2.ACTIVITY_TYPE,activityType)
    activity.setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
    activity.finish()
}