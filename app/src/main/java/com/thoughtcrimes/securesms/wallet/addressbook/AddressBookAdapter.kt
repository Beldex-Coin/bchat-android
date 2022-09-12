package com.thoughtcrimes.securesms.wallet.addressbook

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.recipients.Recipient
import com.thoughtcrimes.securesms.database.RecipientDatabase
import com.thoughtcrimes.securesms.mms.GlideRequests
import com.thoughtcrimes.securesms.preferences.SettingsActivity
import io.beldex.bchat.R
import javax.inject.Inject

class AddressBookAdapter(
    private val context: Context,
    private val glide: GlideRequests,
    val listener: AddressBookActivity
) : RecyclerView.Adapter<AddressBookAdapter.ViewHolder>() {
    var members = listOf<String>()
        set(value) {
            field = value; this.notifyDataSetChanged()
        }

    lateinit var button: Button

    class ViewHolder(val view: AddressBookView) : RecyclerView.ViewHolder(view)

    @Inject
    lateinit var recipientDatabase: RecipientDatabase

    override fun getItemCount(): Int {
        return members.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AddressBookView(context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val member = members[position]
        val beldexAddress = viewHolder.view.getBeldexAddress(member)

        viewHolder.view.bind(
            Recipient.from(
                context,
                Address.fromSerialized(member), false
            ),
            glide
        )


        viewHolder.view.copyAction().setOnClickListener {

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Beldex Address", beldexAddress)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }

        viewHolder.view.sendAction().setOnClickListener {

            Toast.makeText(context, "Send Action", Toast.LENGTH_SHORT).show()
            /*val intent = Intent(viewHolder.view.context, SendActivity::class.java)
                intent.putExtra(SendActivity.SEND_ADDRESS,beldexAddress)
                viewHolder.view.context.startActivity(intent)*/
        }
    }
}


interface AddressBookClickListener {
    fun onAddressBookClick(position: Int)
}
// endregion