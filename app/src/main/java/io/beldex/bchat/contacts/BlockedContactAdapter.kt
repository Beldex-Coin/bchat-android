package io.beldex.bchat.contacts

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import com.bumptech.glide.RequestManager
import io.beldex.bchat.dependencies.DatabaseComponent
import javax.inject.Inject

class BlockedContactAdapter(
        private val context: Context,
        private val glide: RequestManager,
        val listener: BlockedContactActivity
) : RecyclerView.Adapter<BlockedContactAdapter.ViewHolder>() {
    var members = listOf<String>()
        set(value) {
            field = value; this.notifyDataSetChanged()
        }

    lateinit var button: Button

    class ViewHolder(val view: BlockedView) : RecyclerView.ViewHolder(view)

    @Inject
    lateinit var recipientDatabase: io.beldex.bchat.database.RecipientDatabase

    override fun getItemCount(): Int {
        return members.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = BlockedView(context)
        button = view.unblockButton()
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val member = members[position]

        viewHolder.view.unblockButton().setOnClickListener {
            DatabaseComponent.get(context).recipientDatabase().setBlocked(
                Recipient.from(
                    context,
                    Address.fromSerialized(member), false
                ), false
            )
            TextSecurePreferences.setUnBlockStatus(context, true)
            Toast.makeText(context, "UnBlocked this contact", Toast.LENGTH_SHORT).show()
            listener.onBlockedContactClick(position)
        }
        viewHolder.view.bind(
            Recipient.from(
                context,
                Address.fromSerialized(member), false
            ),
            glide
        )

    }

}

interface BlockedContactClickListener {
    fun onBlockedContactClick(position: Int)
}
// endregion