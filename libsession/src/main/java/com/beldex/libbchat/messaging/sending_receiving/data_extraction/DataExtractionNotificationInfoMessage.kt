package com.beldex.libbchat.messaging.sending_receiving.data_extraction

class DataExtractionNotificationInfoMessage {

    enum class Kind {
        SCREENSHOT,
        MEDIA_SAVED
    }

    var kind: Kind? = null

    constructor(kind: Kind?) {
        this.kind = kind
    }

}
