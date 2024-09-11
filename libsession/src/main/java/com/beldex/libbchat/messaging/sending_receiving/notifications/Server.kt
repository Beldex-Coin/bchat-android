package com.beldex.libbchat.messaging.sending_receiving.notifications

enum class Server(val url: String, val publicKey: String) {
    LATEST("http://194.233.68.227:2900", "2c32e3ae8b976b210126d74a47264d883f90d80055a636cbccac2f32d82a5924"),
    LEGACY("http://notification.rpcnode.stream", "54e8ce6a688f6decd414350408cae373ab6070d91d4512e17454d2470c7cf911")
}