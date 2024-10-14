package io.beldex.bchat.conversation_v2

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
import io.beldex.bchat.PassphraseRequiredActionBarActivity
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.ui.ScreenContainer
import io.beldex.bchat.conversation.v2.ConversationFragmentV2
import io.beldex.bchat.util.State
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R

enum class NewGroupConversationType(val destination: String) {
    SecretGroup("secret_group"),
    PublicGroup("open_group")
}

@AndroidEntryPoint
class NewGroupConversationActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination = intent?.getStringExtra(EXTRA_DESTINATION) ?: NewGroupConversationType.PublicGroup.destination
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
                                route = NewGroupConversationType.SecretGroup.destination
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
                                        context
                                    )
                                }
                            }
                            composable(
                                route = NewGroupConversationType.PublicGroup.destination
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