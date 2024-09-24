package io.beldex.bchat.groups

import android.content.Context
import androidx.annotation.WorkerThread
import org.greenrobot.eventbus.EventBus
import com.beldex.libbchat.messaging.open_groups.OpenGroupAPIV2
import com.beldex.libbchat.utilities.GroupUtil
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.dependencies.DatabaseComponent
import java.util.*

//TODO Refactor so methods declare specific type of checked exceptions and not generalized Exception.
object OpenGroupUtilities {

    private const val TAG = "OpenGroupUtilities"

    /**
     * Pulls the general public chat data from the server and updates related records.
     * Fires [GroupInfoUpdatedEvent] on [EventBus] upon success.
     *
     * Consider using [io.beldex.bchat.beldex.api.PublicChatInfoUpdateWorker] for lazy approach.
     */
    @JvmStatic
    @WorkerThread
    @Throws(Exception::class)
    fun updateGroupInfo(context: Context, server: String, room: String) {
        val groupId = GroupUtil.getEncodedOpenGroupID("$server.$room".toByteArray())
        if (!DatabaseComponent.get(context).groupDatabase().hasGroup(groupId)) {
            throw IllegalStateException("Attempt to update social group info for non-existent DB record: $groupId")
        }

        val info = OpenGroupAPIV2.getInfo(room, server).get() // store info again?
        OpenGroupAPIV2.getMemberCount(room, server).get()

        EventBus.getDefault().post(GroupInfoUpdatedEvent(server, room = room))
    }

    /**
     * Return a generated name for users in the style of `$name (...$hex.takeLast(8))` for public groups
     */
    @JvmStatic
    fun getDisplayName(recipient: Recipient): String {
        return String.format(Locale.ROOT, PUBLIC_GROUP_STRING_FORMAT, recipient.name, recipient.address.serialize().takeLast(8))
    }

    const val PUBLIC_GROUP_STRING_FORMAT = "%s (...%s)"

    data class GroupInfoUpdatedEvent(val url: String, val channel: Long = -1, val room: String = "")
}