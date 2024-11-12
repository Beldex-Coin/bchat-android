package com.beldex.libbchat.messaging.sending_receiving.notifications

enum class Server(val url: String, val publicKey: String) {
    LATEST("http://194.233.68.227:5000", "d6caced35a04f65022468b2854fbe61aa86f415c4832876353dc774a2848b30c"),
    LEGACY("http://notification.rpcnode.stream", "54e8ce6a688f6decd414350408cae373ab6070d91d4512e17454d2470c7cf911")
}