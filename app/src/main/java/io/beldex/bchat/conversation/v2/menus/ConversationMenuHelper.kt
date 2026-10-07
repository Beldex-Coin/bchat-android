package io.beldex.bchat.conversation.v2.menus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.fragment.app.FragmentManager
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.leave
import com.beldex.libbchat.utilities.ExpirationUtil
import com.beldex.libbchat.utilities.GroupUtil.doubleDecodeGroupID
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.guava.Optional
import com.beldex.libsignal.utilities.toHexString
import io.beldex.bchat.ShortcutLauncherActivity
import io.beldex.bchat.compose_utils.ComposeDialogContainer
import io.beldex.bchat.compose_utils.DialogType
import io.beldex.bchat.contacts.SelectContactsActivity
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.groups.EditClosedGroupActivity
import io.beldex.bchat.groups.EditClosedGroupActivity.Companion.groupIDKey
import io.beldex.bchat.util.BitmapUtil
import io.beldex.bchat.R
import io.beldex.bchat.conversation.v2.ConversationActivityV2
import java.io.IOException

/**
 * One entry in the themed Compose overflow dropdown (see `ConversationOverflowMenu.kt`). Mirrors
 * a single clickable row of the old system-rendered options menu this replaces.
 */
data class ConversationMenuEntry(val id: Int, val label: String)

object ConversationMenuHelper {

    /**
     * Compose-dropdown equivalent of [onPrepareOptionsMenu] below — same conditions, same item
     * ids, but returns a plain list instead of inflating XML menu resources into a system [Menu].
     * Keep these two functions' conditions in sync; [onPrepareOptionsMenu] is still used to size
     * the now-empty system options menu (so no auto overflow icon renders) but no longer the
     * source of truth for what's shown to the user.
     */
    fun buildMenuEntries(context: Context, activityContext: ConversationActivityV2, thread: Recipient): List<ConversationMenuEntry> {
        val entries = mutableListOf<ConversationMenuEntry>()
        val isOpenGroup = thread.isOpenGroupRecipient
        val isBlockedContact = thread.isBlocked

        entries += ConversationMenuEntry(R.id.menu_view_all_media, context.getString(R.string.MediaRepository_all_media))
        entries += ConversationMenuEntry(R.id.menu_search, context.getString(R.string.SearchToolbar_search))
        entries += ConversationMenuEntry(R.id.menu_add_shortcut, context.getString(R.string.conversation__menu_add_shortcut))

        if (!isOpenGroup && (thread.hasApprovedMe() || thread.isClosedGroupRecipient) && !isBlockedContact) {
            if (thread.expireMessages > 0) {
                val badge = ExpirationUtil.getExpirationAbbreviatedDisplayValue(context, thread.expireMessages)
                entries += ConversationMenuEntry(R.id.menu_expiring_messages, "${context.getString(R.string.menu_conversation_expiring_on__messages_expiring)} ($badge)")
            } else {
                val showOff = (thread.isGroupRecipient && activityContext.isSecretGroupIsActive()) || !thread.isGroupRecipient
                if (showOff) {
                    entries += ConversationMenuEntry(R.id.menu_expiring_messages_off, context.getString(R.string.ExpirationDialog_disappearing_messages))
                }
            }
        }

        if (thread.isContactRecipient && thread.hasApprovedMe() && !thread.isLocalNumber) {
            entries += if (thread.isBlocked) {
                ConversationMenuEntry(R.id.menu_unblock, context.getString(R.string.ConversationActivity_unblock))
            } else {
                ConversationMenuEntry(R.id.menu_block, context.getString(R.string.RecipientPreferenceActivity_block))
            }
        }

        if (thread.isClosedGroupRecipient && activityContext.isSecretGroupIsActive()) {
            entries += ConversationMenuEntry(R.id.menu_edit_group, context.getString(R.string.conversation__menu_edit_group))
            entries += ConversationMenuEntry(R.id.menu_leave_group, context.getString(R.string.conversation__menu_leave_group))
        }

        if (isOpenGroup) {
            entries += ConversationMenuEntry(R.id.menu_invite_to_open_group, context.getString(R.string.ConversationActivity_invite_to_open_group))
        }

        if (thread.hasApprovedMe() && !thread.isLocalNumber) {
            entries += if (thread.isMuted) {
                ConversationMenuEntry(R.id.menu_unmute_notifications, context.getString(R.string.conversation_muted__unmute))
            } else {
                ConversationMenuEntry(R.id.menu_mute_notifications, context.getString(R.string.conversation_unmuted__mute_notifications))
            }
        }

        if (thread.isGroupRecipient && !thread.isMuted && activityContext.isSecretGroupIsActive()) {
            entries += ConversationMenuEntry(R.id.menu_notification_settings, context.getString(R.string.RecipientPreferenceActivity_notification_settings))
        }

        return entries
    }

    /**
     * The chat screen's overflow/call icons are now rendered by a themed Compose
     * [io.beldex.bchat.conversation.v2.menus.ConversationOverflowMenu] (see [buildMenuEntries]
     * above), not the system options menu. This is kept as a no-op purely so the system never
     * populates a (differently-styled) overflow icon of its own on top of it — an empty [Menu]
     * means `Toolbar`/`ActionBar` renders no overflow button at all. The real menu-item
     * conditions now live in [buildMenuEntries]; keep the two in sync if either changes.
     */
    fun onPrepareOptionsMenu(menu: Menu, inflater: MenuInflater, thread: Recipient, threadId: Long, context: Context, activityContext: ConversationActivityV2, onOptionsItemSelected: (MenuItem) -> Unit) {
        menu.clear()
    }

    fun onOptionItemSelected(
            context : Context,
            activityContext : ConversationActivityV2,
            itemId : Int,
            thread : Recipient,
            childFragmentManager : FragmentManager
    ): Boolean {
        when (itemId) {
            R.id.menu_view_all_media -> { showAllMedia(thread, activityContext) }
            R.id.menu_search -> { search(activityContext) }
            R.id.menu_add_shortcut -> { addShortcut(context, thread) }
            R.id.menu_expiring_messages -> { showExpiringMessagesDialog(activityContext, thread) }
            R.id.menu_expiring_messages_off -> { showExpiringMessagesDialog(activityContext, thread) }
            R.id.menu_unblock -> { unblock(activityContext, thread) }
            R.id.menu_block -> { block(activityContext, thread,deleteThread = false) }
            R.id.menu_copy_bchat_id -> { copyBchatID(activityContext, thread) }
            R.id.menu_edit_group -> { editClosedGroup(context, thread) }
            R.id.menu_leave_group -> { leaveClosedGroup(context, thread,activityContext,childFragmentManager) }
            R.id.menu_invite_to_open_group -> { inviteContacts(context, thread) }
            R.id.menu_unmute_notifications -> { unmute(context, thread) }
            R.id.menu_mute_notifications -> { mute(activityContext, thread) }
            R.id.menu_notification_settings -> { setNotifyType(context, thread, childFragmentManager) }
        }
        return true
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }

    private fun showAllMedia(thread: Recipient, listenerCallback: ConversationActivityV2) {
        listenerCallback.showAllMediaView(thread)
    }

    private fun search(context: ConversationActivityV2) {
        val listener = context as? ConversationMenuListener ?: return
        listener.openSearch()
    }

    @SuppressLint("StaticFieldLeak")
    private fun addShortcut(context: Context, thread: Recipient) {
        object : AsyncTask<Void?, Void?, IconCompat?>() {

            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg params: Void?): IconCompat? {
                var icon: IconCompat? = null
                val contactPhoto = thread.contactPhoto
                if (contactPhoto != null) {
                    try {
                        val inputStream = contactPhoto.openInputStream(context, true)
                        var bitmap = inputStream?.let { BitmapFactory.decodeStream(it) }
                        if(bitmap != null) {
                            bitmap = BitmapUtil.createScaledBitmap(bitmap, 300, 300)
                            icon = IconCompat.createWithAdaptiveBitmap(bitmap)
                        } else {
                            icon = defaultIcon(context, thread)
                        }
                    } catch (e: IOException) {
                        // Do nothing
                    }
                } else {
                    icon = defaultIcon(context, thread)
                }
                return icon
            }

            @Deprecated("Deprecated in Java")
            override fun onPostExecute(icon: IconCompat?) {
                val name = Optional.fromNullable<String>(thread.name)
                    .or(Optional.fromNullable<String>(thread.profileName))
                    .or(thread.toShortString())
                val shortcutInfo = ShortcutInfoCompat.Builder(context, thread.address.serialize() + '-' + System.currentTimeMillis())
                    .setIcon(icon)
                    .setShortLabel(name)
                    .setIntent(ShortcutLauncherActivity.createIntent(context, thread.address))
                    .build()
                if (ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)) {
                    Toast.makeText(context, context.resources.getString(R.string.ConversationActivity_added_to_home_screen), Toast.LENGTH_LONG).show()
                }
            }
        }.execute()
    }

    private fun defaultIcon(context: Context, thread: Recipient):IconCompat {
        return IconCompat.createWithResource(context, if (thread.isGroupRecipient) R.drawable.ic_shortcut_group else R.drawable.ic_shortcut_person)
    }

    private fun showExpiringMessagesDialog(context: ConversationActivityV2, thread: Recipient) {
        val listener = context as? ConversationMenuListener ?: return
        listener.showExpiringMessagesDialog(thread)
    }

    private fun unblock(context: ConversationActivityV2, thread: Recipient) {
        if (!thread.isContactRecipient) { return }
        val listener = context as? ConversationMenuListener ?: return
        listener.unblock()
    }

    private fun block(context: ConversationActivityV2, thread: Recipient, deleteThread: Boolean) {
        if (!thread.isContactRecipient) { return }
        val listener = context as? ConversationMenuListener ?: return
        listener.block(deleteThread)
    }

    private fun copyBchatID(context: ConversationActivityV2, thread: Recipient) {
        if (!thread.isContactRecipient) { return }
        val listener = context as? ConversationMenuListener ?: return
        listener.copyBchatID(thread.address.toString())
    }

    private fun editClosedGroup(context: Context, thread: Recipient) {
        if (!thread.isClosedGroupRecipient) { return }
        val intent = Intent(context, EditClosedGroupActivity::class.java)
        val groupID: String = thread.address.toGroupString()
        intent.putExtra(groupIDKey, groupID)
        context.startActivity(intent)
    }

    private fun leaveClosedGroup(
            context : Context,
            thread : Recipient,
            activityContext : ConversationActivityV2,
            childFragmentManager : FragmentManager,

            ) {
        if (!thread.isClosedGroupRecipient) { return }

        val dialog = ComposeDialogContainer(
                dialogType = DialogType.LeaveGroup,
                onConfirm = {
                    var groupPublicKey: String?
                    var isClosedGroup: Boolean
                    try {
                        groupPublicKey = doubleDecodeGroupID(thread.address.toString()).toHexString()
                        isClosedGroup = DatabaseComponent.get(context).beldexAPIDatabase().isClosedGroup(groupPublicKey)
                    } catch (e: IOException) {
                        groupPublicKey = null
                        isClosedGroup = false
                    }
                    try {
                        if (isClosedGroup) {
                            MessageSender.leave(groupPublicKey!!, true)
                            activityContext.backToHome()
                        } else {
                            Toast.makeText(context, R.string.ConversationActivity_error_leaving_group, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, R.string.ConversationActivity_error_leaving_group, Toast.LENGTH_LONG).show()
                    }
                },
                onCancel = {},
                onConfirmWithData = { index -> }
        )
        dialog.apply {
            arguments = Bundle().apply {
                putString(ComposeDialogContainer.EXTRA_ARGUMENT_1,thread.address.toGroupString())
            }
        }
        dialog.show(childFragmentManager, ComposeDialogContainer.TAG)
    }

    private fun inviteContacts(context: Context, thread: Recipient) {
        if (!thread.isOpenGroupRecipient) { return }
        val intent = Intent(context, SelectContactsActivity::class.java)
        val activity = context as AppCompatActivity
        activity.startActivityForResult(intent, ConversationActivityV2.INVITE_CONTACTS)
    }

    private fun unmute(context: Context, thread: Recipient) {
        DatabaseComponent.get(context).recipientDatabase().setMuted(thread, 0)
    }

    private fun mute(context: ConversationActivityV2, thread: Recipient) {
//        MuteDialog.show(context) { until: Long ->
//            DatabaseComponent.get(context).recipientDatabase().setMuted(thread, until)
//        }
        val listener = context as? ConversationMenuListener ?: return
        listener.showMuteOptionDialog(thread)
    }

    private fun setNotifyType(context : Context, thread : Recipient, fragmentManager : FragmentManager) {
        val dialog = ComposeDialogContainer(
                dialogType = DialogType.NotificationSettings,
                onConfirm = {

                },
                onCancel = {},
                onConfirmWithData = { index ->
                    DatabaseComponent.get(context).recipientDatabase().setNotifyType(thread, index.toString().toInt())
                }
        )
        dialog.apply {
            arguments = Bundle().apply {
                putInt(ComposeDialogContainer.EXTRA_ARGUMENT_3,thread.notifyType)
            }
        }
        dialog.show(fragmentManager, ComposeDialogContainer.TAG)
    }

    interface ConversationMenuListener {
        fun block(deleteThread: Boolean = false)
        fun unblock()
        fun copyBchatID(bchatId: String)
        fun showExpiringMessagesDialog(thread: Recipient)
        fun showMuteOptionDialog(thread: Recipient)
        fun openSearch()
    }

}


