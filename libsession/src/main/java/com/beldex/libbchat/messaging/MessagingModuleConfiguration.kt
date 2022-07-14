package com.beldex.libbchat.messaging

import android.content.Context
import com.goterl.lazysodium.utils.KeyPair
import com.beldex.libbchat.database.MessageDataProvider
import com.beldex.libbchat.database.StorageProtocol

class MessagingModuleConfiguration(
    val context: Context,
    val storage: StorageProtocol,
    val messageDataProvider: MessageDataProvider,
    val getUserED25519KeyPair: ()-> KeyPair?
) {

    companion object {
        lateinit var shared: MessagingModuleConfiguration

        fun configure(context: Context, storage: StorageProtocol,
            messageDataProvider: MessageDataProvider, keyPairProvider: () -> KeyPair?
        ) {
            if (Companion::shared.isInitialized) { return }
            shared = MessagingModuleConfiguration(context, storage, messageDataProvider, keyPairProvider)
        }
    }
}