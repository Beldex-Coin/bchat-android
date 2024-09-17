package com.beldex.libbchat.messaging.messages.signal;

import com.beldex.libbchat.messaging.messages.visible.OpenGroupInvitation;
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libbchat.messaging.utilities.UpdateMessageData;

public class OutgoingTextMessage {
  private final Recipient recipient;
  private final String    message;
  private final int       subscriptionId;
  private final long      expiresIn;
  private final long      sentTimestampMillis;
  private boolean         isOpenGroupInvitation = false;

  public OutgoingTextMessage(Recipient recipient, String message, long expiresIn, int subscriptionId, long sentTimestampMillis) {
    this.recipient      = recipient;
    this.message        = message;
    this.expiresIn      = expiresIn;
    this.subscriptionId = subscriptionId;
    this.sentTimestampMillis = sentTimestampMillis;
  }

  public static OutgoingTextMessage from(VisibleMessage message, Recipient recipient) {
    return new OutgoingTextMessage(recipient, message.getText(), recipient.getExpireMessages() * 1000, -1, message.getSentTimestamp());
  }

  public static OutgoingTextMessage fromOpenGroupInvitation(OpenGroupInvitation openGroupInvitation, Recipient recipient, Long sentTimestamp) {
    String url = openGroupInvitation.getUrl();
    String name = openGroupInvitation.getName();
    if (url == null || name == null) { return null; }
    // FIXME: Doing toJSON() to get the body here is weird
    String body = UpdateMessageData.Companion.buildOpenGroupInvitation(url, name).toJSON();
    OutgoingTextMessage outgoingTextMessage = new OutgoingTextMessage(recipient, body, recipient.getExpireMessages() * 1000, -1, sentTimestamp);
    outgoingTextMessage.isOpenGroupInvitation = true;
    return outgoingTextMessage;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public int getSubscriptionId() {
    return subscriptionId;
  }

  public String getMessageBody() {
    return message;
  }

  public Recipient getRecipient() {
    return recipient;
  }

  public long getSentTimestampMillis() {
    return sentTimestampMillis;
  }

  public boolean isSecureMessage() {
    return true;
  }

  public boolean isOpenGroupInvitation() { return isOpenGroupInvitation; }
}
