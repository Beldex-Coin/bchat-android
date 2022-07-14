package com.beldex.libbchat.messaging.messages.signal;

import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import com.beldex.libbchat.utilities.DistributionTypes;
import com.beldex.libbchat.utilities.recipients.Recipient;

import java.util.Collections;
import java.util.LinkedList;

public class OutgoingExpirationUpdateMessage extends OutgoingSecureMediaMessage {

  private final String groupId;

  public OutgoingExpirationUpdateMessage(Recipient recipient,  long sentTimeMillis, long expiresIn, String groupId) {
    super(recipient, "", new LinkedList<Attachment>(), sentTimeMillis,
          DistributionTypes.CONVERSATION, expiresIn, null, Collections.emptyList(),
          Collections.emptyList());
    this.groupId = groupId;
  }

  @Override
  public boolean isExpirationUpdate() {
    return true;
  }

  @Override
  public boolean isGroup() {
    return groupId != null;
  }

  public String getGroupId() {
    return groupId;
  }

}
