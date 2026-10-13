package io.beldex.bchat.conversation_v2

import android.app.Activity
import android.content.Intent
import android.os.Build
import io.beldex.bchat.util.BChatToast
import androidx.compose.runtime.LaunchedEffect
import android.widget.Toast
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.conversation.v2.ConversationActivityV2
import io.beldex.bchat.dms.NewChatScreen
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

enum class OpenActivity(val destination: String) {
    NewChat("new_chat"),
    SecretGroup("secret_group"),
    PublicGroup("open_group"),
    NoteToSelf("note-to-self"),
    InviteAFriend("invite-a-friend"),
    Conversation("conversation")
}

@AndroidEntryPoint
class NewChatConversationActivity: BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = this
            val contactViewModel: NewChatScreenViewModel = hiltViewModel()
            val searchQuery by contactViewModel.searchQuery.collectAsState()
            val contacts by contactViewModel.recipients.collectAsState(initial = listOf())
            val isBnsLoading by contactViewModel.isBnsLoading.collectAsState()
            val chatIdError by contactViewModel.chatIdError.collectAsState()
            LaunchedEffect(Unit) {
                contactViewModel.events.collect { event ->
                    when (event) {
                        is NewChatScreenViewModel.Event.Message ->
                            BChatToast.makeText(context, event.resId, Toast.LENGTH_SHORT).show()
                        is NewChatScreenViewModel.Event.OpenChat -> {
                            val returnIntent = Intent().apply {
                                putExtra(ConversationActivityV2.ADDRESS, event.address)
                                putExtra(ConversationActivityV2.THREAD_ID, event.threadId)
                                putExtra(ConversationActivityV2.BNS_NAME, event.bnsName)
                                putExtra(ConversationActivityV2.URI, null as android.os.Parcelable?)
                                putExtra(ConversationActivityV2.TYPE, null as String?)
                            }
                            context.setResult(RESULT_OK, returnIntent)
                            context.finish()
                        }
                    }
                }
            }
            val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            val view = LocalView.current
            val window = (view.context as Activity).window
            val statusBarColor = if (isDarkTheme) Color.Black else Color.White
            SideEffect {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    window.statusBarColor = statusBarColor.toArgb()
                }
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }
            BChatTheme(
                darkTheme = isDarkTheme
            ) {
                // A surface container using the 'background' color from the theme
                Surface(color = Color.Transparent) {
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
                            returnIntent.putExtra(ConversationActivityV2.ACTIVITY_TYPE,6)
                            returnIntent.putExtra(ConversationActivityV2.ADDRESS,it.address)
                            context.setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
                            context.finish()
                        },
                        onBackPress = {
                            context.finish()
                        },
                        isDarkTheme = isDarkTheme,
                        isBnsLoading = isBnsLoading,
                        chatIdError = chatIdError,
                        onStartChat = contactViewModel::startChat,
                        onChatIdErrorCleared = contactViewModel::clearChatIdError,
                        onScannedChat = contactViewModel::openChat,
                        groupAvatarInfo = contactViewModel::groupAvatarInfo)
                }
            }
        }
    }
}

fun openActivity(activityType:Int, activity: Activity){
    val returnIntent = Intent()
    returnIntent.putExtra(ConversationActivityV2.ACTIVITY_TYPE,activityType)
    activity.setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
    activity.finish()
}