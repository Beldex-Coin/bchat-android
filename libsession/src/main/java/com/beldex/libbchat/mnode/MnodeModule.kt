package com.beldex.libbchat.mnode

import com.beldex.libsignal.database.BeldexAPIDatabaseProtocol
import com.beldex.libsignal.utilities.Broadcaster

class MnodeModule(val storage: BeldexAPIDatabaseProtocol, val broadcaster: Broadcaster) {

    companion object {
        lateinit var shared: MnodeModule

        val isInitialized: Boolean get() = Companion::shared.isInitialized

        fun configure(storage: BeldexAPIDatabaseProtocol, broadcaster: Broadcaster) {
            if (isInitialized) { return }
            shared = MnodeModule(storage, broadcaster)
        }
    }
}