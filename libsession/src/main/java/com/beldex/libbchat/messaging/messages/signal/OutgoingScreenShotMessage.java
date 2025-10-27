package com.beldex.libbchat.messaging.messages.signal;

import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import com.beldex.libbchat.utilities.DistributionTypes;
import com.beldex.libbchat.utilities.recipients.Recipient;

import java.util.Collections;
import java.util.LinkedList;

public class OutgoingScreenShotMessage extends OutgoingSecureMediaMessage {

    private final String screenShot;

    public OutgoingScreenShotMessage(Recipient recipient, long sentTimeMillis) {
        super(recipient, "", new LinkedList<Attachment>(), sentTimeMillis, DistributionTypes.CONVERSATION, 0, null, Collections.emptyList(), Collections.emptyList());
        this.screenShot = "enabled";
    }

    @Override
    public boolean isScreenShot() {
        return screenShot != null;
    }
}