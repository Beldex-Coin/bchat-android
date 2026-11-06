package io.beldex.bchat.conversation.v2.forward

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.BaseComponentActivity
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatTheme
import io.beldex.bchat.compose_utils.appColors
import io.beldex.bchat.compose_utils.bchatBackground
import io.beldex.bchat.conversation.v2.ConversationActivityV2
import io.beldex.bchat.database.model.MessageRecord
import io.beldex.bchat.util.BChatToast
import io.beldex.bchat.util.UiMode
import io.beldex.bchat.util.UiModeUtilities

/**
 * Forward screen: pick any number of chats (secret groups, social groups, contacts) and send the
 * forwarded message(s) to all of them with their text and every attachment intact.
 * Forwarding to a single chat opens that chat afterwards; several chats shows a confirmation.
 */
@AndroidEntryPoint
class ForwardMessageActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val isDarkTheme = UiModeUtilities.getUserSelectedUiMode(this) == UiMode.NIGHT
            val view = LocalView.current
            SideEffect {
                val window = (view.context as Activity).window
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    window.statusBarColor = (if (isDarkTheme) Color.Black else Color.White).toArgb()
                }
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }
            BChatTheme(darkTheme = isDarkTheme) {
                val viewModel: ForwardMessageViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                // Nothing that can be forwarded (e.g. media still downloading): explain and close.
                LaunchedEffect(state.loading, state.payloads, state.error) {
                    if (!state.loading && state.payloads.isEmpty()) {
                        val message = when (state.error) {
                            ForwardError.NOT_DOWNLOADED -> R.string.please_wait_until_file_downloaded
                            else -> R.string.forward_not_possible
                        }
                        BChatToast.makeText(this@ForwardMessageActivity, getString(message), Toast.LENGTH_SHORT).show()
                        finish()
                    } else if (!state.loading && state.error == ForwardError.NOT_DOWNLOADED) {
                        BChatToast.makeText(this@ForwardMessageActivity, getString(R.string.please_wait_until_file_downloaded), Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(
                    modifier = Modifier.bchatBackground(MaterialTheme.appColors.backgroundColor),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.systemBars
                ) { padding ->
                    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier.padding(padding).fillMaxSize()
                    ) {
                        ForwardMessageScreen(
                            state = state,
                            onBack = { finish() },
                            onQueryChanged = viewModel::onQueryChanged,
                            onToggle = viewModel::toggle,
                            onForward = { viewModel.forward(::onForwarded) },
                            modifier = if (isTablet) Modifier.widthIn(max = 480.dp) else Modifier
                        )
                    }
                }
            }
        }
    }

    private fun onForwarded(targets: List<ForwardTarget>) {
        if (targets.isEmpty()) {
            BChatToast.makeText(this, getString(R.string.forward_not_possible), Toast.LENGTH_SHORT).show()
            return
        }
        if (targets.size == 1) {
            val target = targets.first()
            startActivity(Intent(this, ConversationActivityV2::class.java).apply {
                putExtra(ConversationActivityV2.ADDRESS, target.recipient.address)
                putExtra(ConversationActivityV2.THREAD_ID, target.threadId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        } else {
            BChatToast.makeText(
                this,
                resources.getQuantityString(R.plurals.forwarded_to_chats, targets.size, targets.size),
                Toast.LENGTH_SHORT
            ).show()
        }
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_MESSAGE_IDS = "forward_message_ids"
        const val EXTRA_MESSAGE_IS_MMS = "forward_message_is_mms"
        const val EXTRA_PAYLOADS = "forward_payloads"

        /** Forward whole chat messages (text + all attachments). */
        @JvmStatic
        fun intent(context: Context, messages: Collection<MessageRecord>): Intent {
            val sorted = messages.sortedBy { it.dateSent }
            return Intent(context, ForwardMessageActivity::class.java)
                .putExtra(EXTRA_MESSAGE_IDS, sorted.map { it.id }.toLongArray())
                .putExtra(EXTRA_MESSAGE_IS_MMS, sorted.map { it.isMms }.toBooleanArray())
        }

        /** Forward a single stored attachment (e.g. from the media viewer), keeping its file name / caption. */
        @JvmStatic
        fun intent(context: Context, attachment: DatabaseAttachment): Intent {
            val payloads = arrayListOf<ForwardPayload>()
            ForwardAttachment.from(attachment)?.let { payloads += ForwardPayload(null, listOf(it)) }
            return Intent(context, ForwardMessageActivity::class.java)
                .putParcelableArrayListExtra(EXTRA_PAYLOADS, payloads)
        }
    }
}
