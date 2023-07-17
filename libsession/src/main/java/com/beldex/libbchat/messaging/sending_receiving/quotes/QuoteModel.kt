package com.beldex.libbchat.messaging.sending_receiving.quotes

import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment
import com.beldex.libbchat.utilities.Address

class QuoteModel(val id: Long,
                 val author: Address,
                 val text: String?,
                 val missing: Boolean,
                 val attachments: List<Attachment>?)
