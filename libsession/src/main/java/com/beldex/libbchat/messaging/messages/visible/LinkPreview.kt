package com.beldex.libbchat.messaging.messages.visible

import com.beldex.libbchat.messaging.MessagingModuleConfiguration
import com.beldex.libsignal.protos.SignalServiceProtos
import com.beldex.libsignal.utilities.Log
import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview as SignalLinkPreiview

class LinkPreview() {
    var title: String? = null
    var url: String? = null
    var attachmentID: Long? = 0

    fun isValid(): Boolean {
        return (title != null && url != null && attachmentID != null)
    }

    companion object {
        const val TAG = "LinkPreview"

        fun fromProto(proto: SignalServiceProtos.DataMessage.Preview): LinkPreview? {
            val title = proto.title
            val url = proto.url
            return LinkPreview(title, url, null)
        }

        fun from(signalLinkPreview: SignalLinkPreiview?): LinkPreview? {
            if (signalLinkPreview == null) { return null }
            return LinkPreview(signalLinkPreview.title, signalLinkPreview.url, signalLinkPreview.attachmentId?.rowId)
        }
    }

    internal constructor(title: String?, url: String, attachmentID: Long?) : this() {
        this.title = title
        this.url = url
        this.attachmentID = attachmentID
    }

    fun toProto(): SignalServiceProtos.DataMessage.Preview? {
        val url = url
        if (url == null) {
            Log.w(TAG, "Couldn't construct link preview proto from: $this")
            return null
        }
        val linkPreviewProto = SignalServiceProtos.DataMessage.Preview.newBuilder()
        linkPreviewProto.url = url
        title?.let { linkPreviewProto.title = it }
        val database = MessagingModuleConfiguration.shared.messageDataProvider
        attachmentID?.let {
            database.getSignalAttachmentPointer(it)?.let {
                val attachmentProto = Attachment.createAttachmentPointer(it)
                linkPreviewProto.image = attachmentProto
            }
        }
        // Build
        try {
            return linkPreviewProto.build()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't construct link preview proto from: $this")
            return null
        }
    }
}