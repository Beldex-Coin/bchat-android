package io.beldex.bchat.sskenvironment

import android.content.Context
import com.beldex.libbchat.utilities.Address
import com.beldex.libbchat.utilities.SSKEnvironment
import com.beldex.libbchat.utilities.TextSecurePreferences
import com.beldex.libsignal.utilities.Log
import io.beldex.bchat.database.MessagingDatabase.SyncMessageId
import io.beldex.bchat.dependencies.DatabaseComponent

class ReadReceiptManager: SSKEnvironment.ReadReceiptManagerProtocol {

    override fun processReadReceipts(context: Context, fromRecipientId: String, sentTimestamps: List<Long>, readTimestamp: Long) {
        if (TextSecurePreferences.isReadReceiptsEnabled(context)) {

            // Redirect message to master device conversation
            val address = Address.fromSerialized(fromRecipientId)
            for (timestamp in sentTimestamps) {
                Log.i("Beldex", "Received encrypted read receipt: (XXXXX, $timestamp)")
                DatabaseComponent.get(context).mmsSmsDatabase().incrementReadReceiptCount(SyncMessageId(address, timestamp), readTimestamp)
            }
        }
    }
}