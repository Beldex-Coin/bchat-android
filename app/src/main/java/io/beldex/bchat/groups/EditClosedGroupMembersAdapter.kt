package io.beldex.bchat.groups

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.beldex.libbchat.utilities.Address
import io.beldex.bchat.contacts.UserView
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.bumptech.glide.RequestManager

class EditClosedGroupMembersAdapter(
        private val context: Context,
        private val glide: RequestManager,
        private val admin: Boolean,
        private val memberClickListener: ((String) -> Unit)? = null,
        private val groupAdmin: String
) : RecyclerView.Adapter<EditClosedGroupMembersAdapter.ViewHolder>() {

    private val members = ArrayList<String>()
    private val zombieMembers = ArrayList<String>()

    fun setMembers(members: Collection<String>) {
        this.members.clear()
        this.members.addAll(members)
        notifyDataSetChanged()
    }

    fun setZombieMembers(members: Collection<String>) {
        this.zombieMembers.clear()
        this.zombieMembers.addAll(members)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = members.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = UserView(context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val member = members[position]

        val unlocked = admin && member != TextSecurePreferences.getLocalNumber(context)

        viewHolder.view.bind(
            Recipient.from(
            context,
            Address.fromSerialized(member), false),
            glide,
            if (unlocked) UserView.ActionIndicator.Menu else UserView.ActionIndicator.None,
            groupAdmin = groupAdmin)

        if (unlocked) {
            viewHolder.view.setOnClickListener { this.memberClickListener?.invoke(member) }
        } else {
            viewHolder.view.setOnClickListener(null)
        }
    }

    class ViewHolder(val view: UserView) : RecyclerView.ViewHolder(view)
}