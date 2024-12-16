package com.beldex.libbchat.messaging.sending_receiving.notifications
enum class Server(val url: String, val publicKey: String) {
    LATEST("http://pushnotificationv2.rpcnode.stream", "d6caced35a04f65022468b2854fbe61aa86f415c4832876353dc774a2848b30c"),
    LEGACY("http://pushnotificationv1.rpcnode.stream", "589f8d0d376933e6a48266423235f323dfa4eb4179903314cf5dfb30d6cf794a")
}