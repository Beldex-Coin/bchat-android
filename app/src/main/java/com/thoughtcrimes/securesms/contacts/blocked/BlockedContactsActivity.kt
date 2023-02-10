package com.thoughtcrimes.securesms.contacts.blocked

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import dagger.hilt.android.AndroidEntryPoint
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityBlockedContactsBinding

@AndroidEntryPoint
class BlockedContactsActivity: PassphraseRequiredActionBarActivity(), View.OnClickListener {

    lateinit var binding: ActivityBlockedContactsBinding

    val viewModel: BlockedContactsViewModel by viewModels()

    val adapter = BlockedContactsAdapter(this)

    override fun onClick(v: View?) {
        if (v === binding.unblockButton && adapter.getSelectedItems().isNotEmpty()) {
            val contactsToUnblock = adapter.getSelectedItems()
            // show dialog
            val title = if (contactsToUnblock.size == 1) {
                getString(R.string.Unblock_dialog__title_single, contactsToUnblock.first().name)
            } else {
                getString(R.string.Unblock_dialog__title_multiple)
            }

            val message = if (contactsToUnblock.size == 1) {
                getString(R.string.Unblock_dialog__message, contactsToUnblock.first().name)
            } else {
                val stringBuilder = StringBuilder()
                val iterator = contactsToUnblock.iterator()
                var numberAdded = 0
                while (iterator.hasNext() && numberAdded < 3) {
                    val nextRecipient = iterator.next()
                    if (numberAdded > 0) stringBuilder.append(", ")

                    stringBuilder.append(nextRecipient.name)
                    numberAdded++
                }
                val overflow = contactsToUnblock.size - numberAdded
                if (overflow > 0) {
                    stringBuilder.append(" ")
                    val string = resources.getQuantityString(R.plurals.Unblock_dialog__message_multiple_overflow, overflow)
                    stringBuilder.append(string.format(overflow))
                }
                getString(R.string.Unblock_dialog__message, stringBuilder.toString())
            }

           val dialog = AlertDialog.Builder(this,R.style.BChatAlertDialog_Clear_All)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.continue_2) { d, _ ->
                    TextSecurePreferences.setUnBlockStatus(this, true)
                    viewModel.unblock(contactsToUnblock)
                    d.dismiss()
                }
                .setNegativeButton(R.string.cancel) { d, _ ->
                    d.dismiss()
                }
                .show()
            //New Line
            val textView: TextView? = dialog.findViewById(android.R.id.message)
            val face: Typeface = Typeface.createFromAsset(this.assets,"fonts/open_sans_medium.ttf")
            textView!!.typeface = face
        }
    }

    override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
        super.onCreate(savedInstanceState, ready)
        binding = ActivityBlockedContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.adapter = adapter

        viewModel.subscribe(this)
            .observe(this) { newState ->
                adapter.submitList(newState.blockedContacts)
                val isEmpty = newState.blockedContacts.isEmpty()
                binding.emptyStateMessageTextView.isVisible = isEmpty
                binding.nonEmptyStateGroup.isVisible = !isEmpty
            }

        binding.unblockButton.setOnClickListener(this)

    }

}