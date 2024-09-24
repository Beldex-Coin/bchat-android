package io.beldex.bchat.wallet.addressbook

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libbchat.utilities.recipients.Recipient
import io.beldex.bchat.database.RecipientDatabase
import io.beldex.bchat.mms.GlideRequests
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

     val list = mutableListOf<AddressBookAdapter>()

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
        Log.d("Beldex","beldexaddress $beldexAddress")

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
        val sendButtonDisable = TextSecurePreferences.getSendAddress(context)
        Log.d("Beldex","value of sendbuttondisable value $sendButtonDisable")

        if(sendButtonDisable)
        {
            viewHolder.view.sendAction().visibility = View.GONE
            viewHolder.view.copyAction().visibility = View.VISIBLE
        }
        else {
            viewHolder.view.copyAction().visibility = View.GONE
            viewHolder.view.sendAction().visibility = View.VISIBLE
            viewHolder.view.sendAction().setOnClickListener {

                if (beldexAddress != null) {
                    listener.onAddressBookClick(position, beldexAddress)
                }
            }
        }
    }


    fun updateList(list: List<String>) {
        members = list
        notifyDataSetChanged()
    }


}


interface AddressBookClickListener {
    fun onAddressBookClick(position: Int, address: String)
}
// endregion