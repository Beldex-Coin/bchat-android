package io.beldex.bchat.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.cash.copper.flow.observeQuery
import com.beldex.libbchat.utilities.TextSecurePreferences
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.archivechats.ArchiveChatViewModel
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.conversation.v2.ConversationActivityV2
import io.beldex.bchat.database.DatabaseContentProviders
import io.beldex.bchat.database.GroupDatabase
import io.beldex.bchat.my_account.ui.ArchiveChatScreen
import io.beldex.bchat.my_account.ui.ArchiveChatScreenContainer
import io.beldex.bchat.my_account.ui.CardContainer
import io.beldex.bchat.preferences.ChatSettingsActivity
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities
import io.beldex.bchat.util.push
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ArchiveChatActivity : ComponentActivity() {

    @Inject
    lateinit var groupDatabase : GroupDatabase

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val isDarkTheme=
                UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            val view=LocalView.current
            val window=(view.context as Activity).window
            val statusBarColor=if (isDarkTheme) Color.Black else Color.White

            SideEffect {
                window.statusBarColor=statusBarColor.toArgb()
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars=!isDarkTheme
            }

            val archiveViewModel : ArchiveChatViewModel=hiltViewModel()
            val uiState by archiveViewModel.uiState.collectAsState()
            var expanded by remember { mutableStateOf(false) }
            var showSettings by remember { mutableStateOf(false) }

            fun showChatSettings() {
                val intent=Intent(this, ChatSettingsActivity::class.java)
                push(intent)
            }

            var keepArchiveChat by remember {
                mutableStateOf(TextSecurePreferences.getKeepArchiveChat(this@ArchiveChatActivity))
            }

            val lifecycleOwner=LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer=LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        keepArchiveChat=TextSecurePreferences
                            .getKeepArchiveChat(this@ArchiveChatActivity)
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                launch(Dispatchers.IO) {
                    contentResolver
                        .observeQuery(DatabaseContentProviders.ConversationList.CONTENT_URI)
                        .onEach { archiveViewModel.refreshContacts() }
                        .collect()
                }
            }

            BChatTheme(darkTheme=isDarkTheme) {
                Surface(
                    modifier=Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    Box(modifier=Modifier.fillMaxSize()) {
                        ArchiveChatScreenContainer(
                            title=stringResource(R.string.archive_chat),
                            onBackClick={ finish() },
                            actionItems={

                                Box {
                                    Icon(
                                        imageVector=Icons.Default.MoreVert,
                                        contentDescription="Menu",
                                        tint=MaterialTheme.appColors.editTextColor,
                                        modifier=Modifier.clickable { expanded=true }
                                    )

                                    DropdownMenu(
                                        expanded=expanded,
                                        onDismissRequest={ expanded=false }
                                    ) {
                                        DropdownMenuItem(
                                            text={ Text("Archive Settings") },
                                            onClick={
                                                expanded=false
                                                showSettings=true
                                            }
                                        )
                                    }
                                }
                            }

                        ) {
                            ArchiveChatScreen(
                                requestsList=uiState.archiveChats,
                                keepArchiveChat=keepArchiveChat,
                                onRequestClick={
                                    val intent=Intent(
                                        this@ArchiveChatActivity,
                                        ConversationActivityV2::class.java
                                    ).apply {
                                        putExtra(ConversationActivityV2.THREAD_ID, it.threadId)
                                    }
                                    startActivity(intent)
                                },
                                onTabChatSetting={
                                    showChatSettings()
                                },
                                archiveChatViewModel=archiveViewModel,
                                groupDatabase=groupDatabase,
                                modifier=Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }

                        if (showSettings) {
                            ArchiveSettingsScreen(
                                keepArchiveChat=keepArchiveChat,
                                onToggle={ checked ->
                                    keepArchiveChat=checked
                                    TextSecurePreferences.setKeepArchiveChat(
                                        this@ArchiveChatActivity,
                                        checked
                                    )
                                },
                                onClose={ showSettings=false }
                            )
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun ArchiveSettingsScreen(
        keepArchiveChat : Boolean,
        onToggle : (Boolean) -> Unit,
        onClose : () -> Unit
    ) {

        BackHandler {
            onClose()
        }

        Column(
            modifier=Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Row(
                verticalAlignment=Alignment.CenterVertically,
                modifier=Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Icon(
                    painterResource(id=R.drawable.ic_back_arrow),
                    contentDescription=stringResource(R.string.back),
                    tint=MaterialTheme.appColors.editTextColor,
                    modifier=Modifier
                        .clickable {
                            onClose()
                        }
                )
                Spacer(modifier=Modifier.width(16.dp))

                Text(
                    text=stringResource(R.string.archive_settings),
                    style=MaterialTheme.typography.titleLarge.copy(
                        color=MaterialTheme.appColors.editTextColor,
                        fontWeight=FontWeight.Bold,
                        fontSize=18.sp
                    ),
                    modifier=Modifier
                        .weight(1f)
                )
            }

            Spacer(modifier=Modifier.height(8.dp))

            CardContainer(
                modifier=Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Row(
                    modifier=Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    Column(
                        modifier=Modifier.weight(1f),
                        verticalArrangement=Arrangement.Top
                    ) {
                        Text(
                            text=stringResource(R.string.keep_chats_archived),
                            style=MaterialTheme.typography.titleLarge.copy(
                                color=MaterialTheme.appColors.editTextColor,
                                fontWeight=FontWeight.Normal,
                                fontSize=16.sp
                            )
                        )
                        Text(
                            text=stringResource(R.string.keep_chats_archived_subtitle),
                            style=MaterialTheme.typography.titleLarge.copy(
                                color=MaterialTheme.appColors.clearDataSubTitle,
                                fontSize=14.sp
                            ),
                            lineHeight=18.sp
                        )
                    }

                    Spacer(modifier=Modifier.width(12.dp))

                    Switch(
                        checked=keepArchiveChat,
                        onCheckedChange={ checked ->
                            onToggle(checked)
                        },
                        colors=SwitchDefaults.colors(
                            checkedThumbColor=MaterialTheme.appColors.primaryButtonColor,
                            uncheckedThumbColor=MaterialTheme.appColors.unCheckedSwitchThumb,
                            checkedTrackColor=MaterialTheme.appColors.switchTrackColor,
                            uncheckedTrackColor=MaterialTheme.appColors.switchTrackColor
                        ),
                        modifier=Modifier
                            .size(30.dp)
                            .padding(end=4.dp)
                    )
                }
            }
        }
    }
}
