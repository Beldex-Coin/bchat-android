package com.thoughtcrimes.securesms.webrtc

import com.beldex.libbchat.utilities.recipients.Recipient
import java.util.*

data class PreOffer(val callId: UUID, val recipient: Recipient)