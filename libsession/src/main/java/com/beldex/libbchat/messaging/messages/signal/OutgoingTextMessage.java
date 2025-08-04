package com.beldex.libbchat.messaging.messages.signal;

import com.beldex.libbchat.messaging.messages.visible.OpenGroupInvitation;
import com.beldex.libbchat.messaging.messages.visible.Payment;
import com.beldex.libbchat.messaging.messages.visible.SharedContact;
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
  //Payment Tag
  private boolean         isPayment = false;

  private boolean         isContact = false;

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

  //Payment Tag
  public static OutgoingTextMessage fromPayment(Payment payment, Recipient recipient, Long sentTimestamp) {
    String amount = payment.getAmount();
    String txnId = payment.getTxnId();
    if (amount == null || txnId == null) { return null; }
    // FIXME: Doing toJSON() to get the body here is weird
    String body = UpdateMessageData.Companion.buildPayment(amount, txnId).toJSON();
    OutgoingTextMessage outgoingTextMessage = new OutgoingTextMessage(recipient, body, recipient.getExpireMessages() * 1000, -1, sentTimestamp);
    outgoingTextMessage.isPayment = true;
    return outgoingTextMessage;
  }

  public static OutgoingTextMessage fromSharedContact(SharedContact contact, Recipient recipient, Long sentTimestamp) {
    String threadId = contact.getThreadId();
    String address = contact.getAddress();
    String name = contact.getName();
    if (threadId == null || address == null || name == null) { return null; }
    // FIXME: Doing toJSON() to get the body here is weird
    String body = UpdateMessageData.Companion.buildSharedContact(threadId, address, name).toJSON();
    OutgoingTextMessage outgoingTextMessage = new OutgoingTextMessage(recipient, body, recipient.getExpireMessages() * 1000, -1, sentTimestamp);
    outgoingTextMessage.isContact = true;
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

  public boolean isPayment() { return isPayment; }

  public boolean isContact() { return isContact; }
}
