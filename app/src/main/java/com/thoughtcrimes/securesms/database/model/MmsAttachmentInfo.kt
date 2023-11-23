package com.thoughtcrimes.securesms.database.model

import com.thoughtcrimes.securesms.util.MediaUtil

data class MmsAttachmentInfo(val dataFile: String?, val thumbnailFile: String?, val contentType: String?) {
    companion object {
        @JvmStatic
        fun List<MmsAttachmentInfo>.anyImages() = any {
            MediaUtil.isImageType(it.contentType)
        }

        @JvmStatic
        fun List<MmsAttachmentInfo>.anyThumbnailNonNull() = any {
            it.thumbnailFile?.isNotEmpty() == true
        }
    }
}
