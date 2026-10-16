package io.beldex.bchat.conversation.v2

import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.FragmentDeleteMessageBottomSheetBinding
import com.beldex.libbchat.messaging.contacts.Contact
import com.beldex.libbchat.utilities.recipients.Recipient
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

    @androidx.compose.runtime.Composable
    private fun DeleteOptionCard(text: String, iconRes: Int, destructive: Boolean, onClick: () -> Unit) {
        val shape = io.beldex.bchat.compose_utils.notchedCornerShape(io.beldex.bchat.compose_utils.BChatDimens.NotchMedium)
        val accent = if (destructive) androidx.compose.ui.graphics.Color(0xFFFF3E3E) else androidx.compose.ui.graphics.Color(0xFF444444)
        val content = if (destructive) androidx.compose.ui.graphics.Color(0xFFFF3E3E) else androidx.compose.ui.graphics.Color(0xFFEBEBEB)
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(
                    if (destructive) accent.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color(0xFF111111),
                    shape
                )
                .border(if (destructive) 0.5.dp else 1.dp, accent, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(iconRes),
                contentDescription = null,
                tint = content,
                modifier = androidx.compose.ui.Modifier.size(22.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(14.dp))
            androidx.compose.material3.Text(
                text = text,
                color = content,
                fontFamily = io.beldex.bchat.compose_utils.OpenSans,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
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