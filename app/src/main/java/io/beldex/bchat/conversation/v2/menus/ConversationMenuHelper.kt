package io.beldex.bchat.conversation.v2.menus

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.AsyncTask
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.beldex.libbchat.messaging.sending_receiving.MessageSender
import com.beldex.libbchat.messaging.sending_receiving.leave
import com.beldex.libbchat.utilities.ExpirationUtil
import com.beldex.libbchat.utilities.GroupUtil.doubleDecodeGroupID
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.beldex.libsignal.utilities.Log
import com.beldex.libsignal.utilities.guava.Optional
import com.beldex.libsignal.utilities.toHexString
import io.beldex.bchat.*
import io.beldex.bchat.calls.WebRtcCallActivity
import io.beldex.bchat.contacts.SelectContactsActivity
import io.beldex.bchat.conversation.v2.utilities.NotificationUtils
import io.beldex.bchat.dependencies.DatabaseComponent
import io.beldex.bchat.groups.EditClosedGroupActivity
import io.beldex.bchat.groups.EditClosedGroupActivity.Companion.groupIDKey
import io.beldex.bchat.preferences.PrivacySettingsActivity
import io.beldex.bchat.service.WebRtcCallService
import io.beldex.bchat.util.BitmapUtil
import io.beldex.bchat.util.getColorWithID
import java.io.IOException
import android.content.*
import android.view.*
import io.beldex.bchat.R
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.beldex.bchat.conversation.v2.ConversationFragmentV2

object ConversationMenuHelper {

    fun onPrepareOptionsMenu(menu: Menu, inflater: MenuInflater, thread: Recipient, threadId: Long, context: Context,fragmentV2: ConversationFragmentV2, onOptionsItemSelected: (MenuItem) -> Unit) {
        // Prepare
        menu.clear()
        val isOpenGroup = thread.isOpenGroupRecipient
        // Base menu (options that should always be present)
        inflater.inflate(R.menu.menu_conversation, menu)
        // Expiring messages
        //New Line v32
        if (!isOpenGroup && (thread.hasApprovedMe() || thread.isClosedGroupRecipient)){
            if (thread.expireMessages > 0) {
                inflater.inflate(R.menu.menu_conversation_expiration_on, menu)
                val item = menu.findItem(R.id.menu_expiring_messages)
                val actionView = item.actionView
                val iconView = actionView?.findViewById<ImageView>(R.id.menu_badge_icon)
                val badgeView = actionView?.findViewById<TextView>(R.id.expiration_badge)
                @ColorInt val color = fragmentV2.resources.getColorWithID(R.color.text, context.theme)
                iconView?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
                badgeView?.text = ExpirationUtil.getExpirationAbbreviatedDisplayValue(context, thread.expireMessages)
                actionView?.setOnClickListener { onOptionsItemSelected(item) }
            } else {
                inflater.inflate(R.menu.menu_conversation_expiration_off, menu)
            }
        }
        // One-on-one chat menu (options that should only be present for one-on-one chats)
        if (thread.isContactRecipient && thread.hasApprovedMe() && !thread.isLocalNumber) {
            if (thread.isBlocked) {
                inflater.inflate(R.menu.menu_conversation_unblock, menu)
            } else {
                inflater.inflate(R.menu.menu_conversation_block, menu)
            }
        }
        // Secret group menu (options that should only be present in secret groups)
        if (thread.isClosedGroupRecipient) {
            val groupPublicKey = doubleDecodeGroupID(thread.address.toString()).toHexString()
            val isClosedGroup =
                DatabaseComponent.get(context).beldexAPIDatabase().isClosedGroup(groupPublicKey)
            if (isClosedGroup) {
                inflater.inflate(R.menu.menu_conversation_closed_group, menu)
            }
        }
        // Social group menu
        if (isOpenGroup) {
            inflater.inflate(R.menu.menu_conversation_open_group, menu)
        }
        // Muting
        if(thread.hasApprovedMe() && !thread.isLocalNumber) {
            if (thread.isMuted) {
                inflater.inflate(R.menu.menu_conversation_muted, menu)
            } else {
                inflater.inflate(R.menu.menu_conversation_unmuted, menu)
            }
        }

        if (thread.isGroupRecipient && !thread.isMuted) {
            inflater.inflate(R.menu.menu_conversation_notification_settings, menu)
        }

        //SteveJosephh21
        if (!thread.isGroupRecipient && thread.hasApprovedMe() && !thread.isLocalNumber) {
            inflater.inflate(R.menu.menu_conversation_call, menu)
        }

        // Search
        val searchViewItem = menu.findItem(R.id.menu_search)
        fragmentV2.searchViewItem = searchViewItem
        val searchView = searchViewItem.actionView as SearchView

        val queryListener = object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                fragmentV2.onSearchQueryUpdated(query)
                Log.d("Beldex","Search Query text change")
                return true
            }
        }
        searchViewItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                Log.d("Beldex","Search expand listener")
                searchView.setOnQueryTextListener(queryListener)
                fragmentV2.onSearchOpened()
                for (i in 0 until menu.size()) {
                    if (menu.getItem(i) != searchViewItem) {
                        menu.getItem(i).isVisible = false
                    }
                }
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                searchView.setOnQueryTextListener(null)
                fragmentV2.onSearchClosed()
                return true
            }
        })

    }

    fun onOptionItemSelected(
        context: Context,
        fragmentV2: ConversationFragmentV2,
        item: MenuItem,
        thread: Recipient,
        listenerCallback: ConversationFragmentV2.Listener?
    ): Boolean {
        when (item.itemId) {
            R.id.menu_view_all_media -> { showAllMedia(thread,listenerCallback) }
            R.id.menu_search -> { search(fragmentV2) }
            R.id.menu_add_shortcut -> { addShortcut(context, thread) }
            R.id.menu_expiring_messages -> { showExpiringMessagesDialog(fragmentV2, thread) }
            R.id.menu_expiring_messages_off -> { showExpiringMessagesDialog(fragmentV2, thread) }
            R.id.menu_unblock -> { unblock(fragmentV2, thread) }
            R.id.menu_block -> { block(fragmentV2, thread,deleteThread = false) }
            R.id.menu_copy_bchat_id -> { copyBchatID(fragmentV2, thread) }
            R.id.menu_edit_group -> { editClosedGroup(context, thread) }
            R.id.menu_leave_group -> { leaveClosedGroup(context, thread) }
            R.id.menu_invite_to_open_group -> { inviteContacts(context, thread) }
            R.id.menu_unmute_notifications -> { unmute(context, thread) }
            R.id.menu_mute_notifications -> { mute(context, thread) }
            R.id.menu_notification_settings -> { setNotifyType(context, thread) }
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




    fun showAllMedia(thread: Recipient,listenerCallback: ConversationFragmentV2.Listener?) {
        //SetDataAndType
        listenerCallback?.passSharedMessageToConversationScreen(thread)
    }

    private fun search(context: ConversationFragmentV2) {
        val searchViewModel = context.searchViewModel
        searchViewModel!!.onSearchOpened()
    }

    //New Line
    private fun call(context: Context, thread: Recipient) {

        if (!TextSecurePreferences.isCallNotificationsEnabled(context)) {
            /* AlertDialog.Builder(context)
                 .setTitle(R.string.ConversationActivity_call_title)
                 .setMessage(R.string.ConversationActivity_call_prompt)
                 .setPositiveButton(R.string.activity_settings_title) { _, _ ->
                     val intent = Intent(context, PrivacySettingsActivity::class.java)
                     context.startActivity(intent)
                 }
                 .setNeutralButton(R.string.cancel) { d, _ ->
                     d.dismiss()
                 }.show()*/
            //SteveJosephh22
            val factory = LayoutInflater.from(context)
            val callPermissionDialogView: View = factory.inflate(R.layout.call_permissions_dialog_box, null)
            val callPermissionDialog = AlertDialog.Builder(context).create()
            callPermissionDialog.setView(callPermissionDialogView)
            callPermissionDialogView.findViewById<TextView>(R.id.settingsDialogBoxButton).setOnClickListener{
                val intent = Intent(context, PrivacySettingsActivity::class.java)
                context.startActivity(intent)
                callPermissionDialog.dismiss()
            }
            callPermissionDialogView.findViewById<TextView>(R.id.cancelDialogBoxButton).setOnClickListener{
                callPermissionDialog.dismiss()
            }
            callPermissionDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
            callPermissionDialog.show()
            return
        }

        val service = WebRtcCallService.createCall(context, thread)
        context.startService(service)

        val activity = Intent(context, WebRtcCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(activity)

    }

    @SuppressLint("StaticFieldLeak")
    private fun addShortcut(context: Context, thread: Recipient) {
        object : AsyncTask<Void?, Void?, IconCompat?>() {

            override fun doInBackground(vararg params: Void?): IconCompat? {
                var icon: IconCompat? = null
                val contactPhoto = thread.contactPhoto
                if (contactPhoto != null) {
                    try {
                        var bitmap = BitmapFactory.decodeStream(contactPhoto.openInputStream(context))
                        bitmap = BitmapUtil.createScaledBitmap(bitmap, 300, 300)
                        icon = IconCompat.createWithAdaptiveBitmap(bitmap)
                    } catch (e: IOException) {
                        // Do nothing
                    }
                }
                if (icon == null) {
                    icon = IconCompat.createWithResource(context, if (thread.isGroupRecipient) R.drawable.ic_shortcut_group else R.drawable.ic_shortcut_person)
                }
                return icon
            }

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

    private fun showExpiringMessagesDialog(context: ConversationFragmentV2, thread: Recipient) {
        val listener = context as? ConversationMenuListener ?: return
        listener.showExpiringMessagesDialog(thread)
    }

    private fun unblock(context: ConversationFragmentV2, thread: Recipient) {
        if (!thread.isContactRecipient) { return }
        val listener = context as? ConversationMenuListener ?: return
        listener.unblock()
    }

    private fun block(context: ConversationFragmentV2, thread: Recipient, deleteThread: Boolean) {
        if (!thread.isContactRecipient) { return }
        val listener = context as? ConversationMenuListener ?: return
        listener.block(deleteThread)
    }

    private fun copyBchatID(context: ConversationFragmentV2, thread: Recipient) {
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

    private fun leaveClosedGroup(context: Context, thread: Recipient) {
        if (!thread.isClosedGroupRecipient) { return }
        val group = DatabaseComponent.get(context).groupDatabase().getGroup(thread.address.toGroupString()).orNull()
        val admins = group.admins
        val bchatID = TextSecurePreferences.getLocalNumber(context)
        val isCurrentUserAdmin = admins.any { it.toString() == bchatID }
        val message = if (isCurrentUserAdmin) {
            "Because you are the creator of this group it will be deleted for everyone. This cannot be undone."
        } else {
            context.resources.getString(R.string.ConversationActivity_are_you_sure_you_want_to_leave_this_group)
        }
        val builder = AlertDialog.Builder(context,R.style.BChatAlertDialog_Clear_All)
            .setTitle(context.resources.getString(R.string.ConversationActivity_leave_group))
            .setCancelable(true)
            .setMessage(message)
            .setPositiveButton(R.string.leave) { _, _ ->
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
                    } else {
                        Toast.makeText(context, R.string.ConversationActivity_error_leaving_group, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.ConversationActivity_error_leaving_group, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()

        //New Line
        val textView: TextView? = builder.findViewById(android.R.id.message)
        val face: Typeface = Typeface.createFromAsset(context.assets,"fonts/open_sans_medium.ttf")
        textView!!.typeface = face
    }

    private fun inviteContacts(context: Context, thread: Recipient) {
        if (!thread.isOpenGroupRecipient) { return }
        val intent = Intent(context, SelectContactsActivity::class.java)
        val activity = context as AppCompatActivity
        activity.startActivityForResult(intent, ConversationFragmentV2.INVITE_CONTACTS)
    }

    private fun unmute(context: Context, thread: Recipient) {
        DatabaseComponent.get(context).recipientDatabase().setMuted(thread, 0)
    }

    private fun mute(context: Context, thread: Recipient) {
        MuteDialog.show(context) { until: Long ->
            DatabaseComponent.get(context).recipientDatabase().setMuted(thread, until)
        }
    }

    private fun setNotifyType(context: Context, thread: Recipient) {
        NotificationUtils.showNotifyDialog(context, thread) { notifyType ->
            DatabaseComponent.get(context).recipientDatabase().setNotifyType(thread, notifyType)
        }
    }

    interface ConversationMenuListener {
        fun block(deleteThread: Boolean = false)
        fun unblock()
        fun copyBchatID(bchatId: String)
        fun showExpiringMessagesDialog(thread: Recipient)
    }

}

