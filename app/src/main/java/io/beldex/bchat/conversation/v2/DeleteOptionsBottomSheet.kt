package io.beldex.bchat.conversation.v2

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.recipients.Recipient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.compose_utils.BChatDimens
import io.beldex.bchat.compose_utils.OpenSans
import io.beldex.bchat.compose_utils.notchedCornerShape
import io.beldex.bchat.database.BchatContactDatabase
import io.beldex.bchat.util.UiModeUtilities
import javax.inject.Inject

@AndroidEntryPoint
class DeleteOptionsBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var contactDatabase: BchatContactDatabase

    lateinit var recipient: Recipient
    val contact by lazy {
        val senderId = recipient.address.serialize()
        // this dialog won't show for social group contacts
        contactDatabase.getContactWithBchatID(senderId)
            ?.displayName(Contact.ContactContext.REGULAR, requireContext())
    }

    var onDeleteForMeTapped: (() -> Unit?)? = null
    var onDeleteForEveryoneTapped: (() -> Unit)? = null
    var onCancelTapped: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!this::recipient.isInitialized) {
            dismiss()
            return View(requireContext())
        }
        val forMeText = if (recipient.isLocalNumber) resources.getString(R.string.delete)
        else resources.getString(R.string.delete_message_for_me)
        val everyoneText = if (!recipient.isGroupRecipient && !contact.isNullOrEmpty())
            resources.getString(R.string.delete_message_for_me_and_recipient, contact)
        else resources.getString(R.string.delete_message_for_everyone)
        val showEveryone = !recipient.isLocalNumber
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                io.beldex.bchat.compose_utils.BChatTheme(
                    darkTheme = UiModeUtilities.getUserSelectedUiMode(requireContext()) == io.beldex.bchat.util.UiMode.NIGHT
                ) {
                    io.beldex.bchat.compose_utils.DialogPanel(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                            modifier = androidx.compose.ui.Modifier.padding(horizontal = 15.dp, vertical = 18.dp)
                        ) {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Text(
                                    text = resources.getString(R.string.delete_message_title),
                                    color = androidx.compose.ui.graphics.Color(0xFFEBEBEB),
                                    fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = androidx.compose.ui.Modifier.weight(1f)
                                )
                                io.beldex.bchat.compose_utils.CloseTile(onClick = { onCancelTapped?.invoke() })
                            }
                            DeleteOptionCard(forMeText, R.drawable.ic_pop_delete_me, destructive = false) { onDeleteForMeTapped?.invoke() }
                            if (showEveryone) {
                                DeleteOptionCard(everyoneText, R.drawable.ic_pop_delete_everyone, destructive = true) { onDeleteForEveryoneTapped?.invoke() }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DeleteOptionCard(
        text: String,
        iconRes: Int,
        destructive: Boolean,
        onClick: () -> Unit
    ) {
        val shape = notchedCornerShape(BChatDimens.NotchMedium)
        val red = Color(0xFFFF3E3E)
        val accent = if (destructive) red else Color(0xFF444444)
        val content = if (destructive) red else Color(0xFFEBEBEB)
        val fill = if (destructive) red.copy(alpha = 0.10f) else Color(0xFF111111)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(shape)
                .background(fill)
                .border(if (destructive) 0.5.dp else 1.dp, accent, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = text,
                color = content,
                fontFamily = OpenSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        val isLightMode = UiModeUtilities.isDayUiMode(requireContext())
        window.setDimAmount(if (isLightMode) 0.1f else 0.75f)
        applyLandscapeSizing()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyLandscapeSizing()
    }

    private fun applyLandscapeSizing() {
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            bottomSheet.layoutParams.width = (resources.displayMetrics.widthPixels * 0.55).toInt()
        } else {
            bottomSheet.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        bottomSheet.requestLayout()
    }
}