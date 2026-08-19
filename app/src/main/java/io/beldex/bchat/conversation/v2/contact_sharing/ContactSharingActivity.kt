package io.beldex.bchat.conversation.v2.contact_sharing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

@AndroidEntryPoint
class ContactSharingActivity: ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
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
            BChatTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier=Modifier
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Scaffold(
                        containerColor=MaterialTheme.colorScheme.primary,
                        contentWindowInsets = WindowInsets.systemBars,
                    ) {
                        val viewModel : ContactsViewModel=hiltViewModel()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        val sendResult : () -> Unit={
                            val resultIntent=Intent()
                            resultIntent.putExtra(
                                RESULT_CONTACT_TO_SHARE,
                                ArrayList(state.selectedContacts.map { contact ->
                                    ContactModel(
                                        address=contact.recipient.address,
                                        name=contact.recipient.name ?: ""
                                    )
                                })
                            )
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }

                        val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600

                        Box(
                            modifier = Modifier
                                .padding(it)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            ContactsScreen(
                                searchQuery=state.searchQuery,
                                contacts=state.filteredContacts,
                                selectedContacts=state.selectedContacts,
                                onQueryChanged=viewModel::postQuery,
                                onSend={ _ ->
                                    sendResult()
                                },
                                onBack={
                                    finish()
                                },
                                contactChanged=viewModel::onContactSelected,
                                modifier = if (isTablet)
                                    Modifier.widthIn(max = 480.dp)
                                else
                                    Modifier
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val RESULT_CONTACT_TO_SHARE = "io.beldex.bchat.CONTACTS_TO_SHARE"
    }

}