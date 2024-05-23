package com.thoughtcrimes.securesms.conversation_v2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.compose_utils.BChatTheme
import com.thoughtcrimes.securesms.compose_utils.ui.ScreenContainer
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2
import com.thoughtcrimes.securesms.util.State
import com.thoughtcrimes.securesms.util.UiMode
import com.thoughtcrimes.securesms.util.UiModeUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R

enum class NewConversationType(val destination: String) {
    PrivateChat("private"),
    SecretGroup("secret_group"),
    PublicGroup("open_group")
}

@AndroidEntryPoint
class NewConversationActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination = intent?.getStringExtra(EXTRA_DESTINATION) ?: NewConversationType.PublicGroup.destination
        setContent {
            val context = this
            BChatTheme(
                darkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            ) {
                // A surface container using the 'background' color from the theme
                Surface {
                    Scaffold {
                        val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = destination,
                            modifier = Modifier
                                .padding(it)
                        ) {
                            composable(
                                route = NewConversationType.PrivateChat.destination
                            ) {
                                val contactViewModel: CreateSecretGroupViewModel = hiltViewModel()
                                val contacts by contactViewModel.recipients.collectAsState(initial = listOf())
                                val searchQuery by contactViewModel.searchQuery.collectAsState()
                                val selectedContact by contactViewModel.selectedRecipients.collectAsState()
                                ScreenContainer(
                                    title = stringResource(id = R.string.activity_create_private_chat_title),
                                    onBackClick = { finish() },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                ) {
                                    CreatePrivateChatScreen()
                                }
                            }

                            composable(
                                route = NewConversationType.SecretGroup.destination
                            ) {
                                val contactViewModel: CreateSecretGroupViewModel = hiltViewModel()
                                val contacts by contactViewModel.recipients.collectAsState(initial = listOf())
                                val searchQuery by contactViewModel.searchQuery.collectAsState()
                                val selectedContact by contactViewModel.selectedRecipients.collectAsState()
                                ScreenContainer(
                                    title = stringResource(id = R.string.home_screen_secret_groups_title),
                                    onBackClick = { finish() },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                ) {
                                    CreateSecretGroup(
                                        searchQuery = searchQuery,
                                        contacts = contacts,
                                        selectedContact = selectedContact,
                                        onEvent = contactViewModel::onEvent,
                                        context,
                                        onSearchClear = { contactViewModel.updateSearchQuery("") }
                                    )
                                }
                            }
                            composable(
                                route = NewConversationType.PublicGroup.destination
                            ) {
                                val viewModel: DefaultGroupsViewModel = hiltViewModel()
                                val lifecycleOwner = LocalLifecycleOwner.current
                                val groups = remember {
                                    mutableStateListOf<OpenGroupAPIV2.DefaultGroup>()
                                }
                                val uiState by viewModel.uiState.collectAsState()

                                LaunchedEffect(key1 = Unit) {
                                    viewModel.defaultRooms.observe(lifecycleOwner) { state ->
                                        when (state) {
                                            State.Loading -> {}
                                            is State.Error -> {}
                                            is State.Success -> {
                                                groups.clear()
                                                groups.addAll(state.value)
                                            }
                                        }
                                    }
                                }
                                ScreenContainer(
                                    title = stringResource(id = R.string.home_screen_social_groups_title),
                                    onBackClick = { finish() },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                ) {
                                    JoinSocialGroupScreen(
                                        uiState = uiState,
                                        groups = groups,
                                        onEvent = viewModel::onEvent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "io.beldex.bchat.DESTINATION"
    }
}

fun openConversationActivity(threadId: Long, recipient: Recipient, activity: Activity) {
    val returnIntent = Intent()
    returnIntent.putExtra(ConversationFragmentV2.THREAD_ID,threadId)
    returnIntent.putExtra(ConversationFragmentV2.ADDRESS,recipient.address)
    activity.setResult(PassphraseRequiredActionBarActivity.RESULT_OK, returnIntent)
}