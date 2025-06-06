package com.beldex.libbchat.messaging.messages.signal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beldex.libbchat.messaging.messages.visible.SharedContact;
import com.beldex.libbchat.messaging.messages.visible.VisibleMessage;
import com.beldex.libbchat.messaging.utilities.UpdateMessageData;
import com.beldex.libbchat.utilities.DistributionTypes;
import com.beldex.libbchat.utilities.IdentityKeyMismatch;
import com.beldex.libbchat.utilities.NetworkFailure;
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import com.beldex.libbchat.utilities.Contact;
import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview;
import com.beldex.libbchat.messaging.sending_receiving.quotes.QuoteModel;
import com.beldex.libbchat.utilities.recipients.Recipient;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class OutgoingMediaMessage {

  private   final Recipient                 recipient;
  protected final String                    body;
  protected final List<Attachment>          attachments;
  private   final long                      sentTimeMillis;
  private   final int                       distributionType;
  private   final int                       subscriptionId;
  private   final long                      expiresIn;
  private   final QuoteModel                outgoingQuote;

  private   final List<NetworkFailure>      networkFailures       = new LinkedList<>();
  private   final List<IdentityKeyMismatch> identityKeyMismatches = new LinkedList<>();
  private   final List<Contact>             contacts              = new LinkedList<>();
  private   final List<LinkPreview>         linkPreviews          = new LinkedList<>();

  public OutgoingMediaMessage(Recipient recipient, String message,
                              List<Attachment> attachments, long sentTimeMillis,
                              int subscriptionId, long expiresIn,
                              int distributionType,
                              @Nullable QuoteModel outgoingQuote,
                              @NonNull List<Contact> contacts,
                              @NonNull List<LinkPreview> linkPreviews,
                              @NonNull List<NetworkFailure> networkFailures,
                              @NonNull List<IdentityKeyMismatch> identityKeyMismatches)
  {
    this.recipient             = recipient;
    this.body                  = message;
    this.sentTimeMillis        = sentTimeMillis;
    this.distributionType      = distributionType;
    this.attachments           = attachments;
    this.subscriptionId        = subscriptionId;
    this.expiresIn             = expiresIn;
    this.outgoingQuote         = outgoingQuote;

    this.contacts.addAll(contacts);
    this.linkPreviews.addAll(linkPreviews);
    this.networkFailures.addAll(networkFailures);
    this.identityKeyMismatches.addAll(identityKeyMismatches);
  }

  public OutgoingMediaMessage(OutgoingMediaMessage that) {
    this.recipient           = that.getRecipient();
    this.body                = that.body;
    this.distributionType    = that.distributionType;
    this.attachments         = that.attachments;
    this.sentTimeMillis      = that.sentTimeMillis;
    this.subscriptionId      = that.subscriptionId;
    this.expiresIn           = that.expiresIn;
    this.outgoingQuote       = that.outgoingQuote;

    this.identityKeyMismatches.addAll(that.identityKeyMismatches);
    this.networkFailures.addAll(that.networkFailures);
    this.contacts.addAll(that.contacts);
    this.linkPreviews.addAll(that.linkPreviews);
  }

  public static OutgoingMediaMessage from(VisibleMessage message,
                                          Recipient recipient,
                                          List<Attachment> attachments,
                                          @Nullable QuoteModel outgoingQuote,
                                          @Nullable LinkPreview linkPreview)
  {
    List<LinkPreview> previews = Collections.emptyList();
    if (linkPreview != null) {
      previews = Collections.singletonList(linkPreview);
    }
    return new OutgoingMediaMessage(recipient, message.getText(), attachments, message.getSentTimestamp(), -1,
            recipient.getExpireMessages() * 1000, DistributionTypes.DEFAULT, outgoingQuote, Collections.emptyList(),
            previews, Collections.emptyList(), Collections.emptyList());
  }

  public static OutgoingMediaMessage fromSharedContact(VisibleMessage message,
                                          Recipient recipient,
                                          List<Attachment> attachments,
                                          @Nullable QuoteModel outgoingQuote,
                                          @Nullable LinkPreview linkPreview)
  {
    List<LinkPreview> previews = Collections.emptyList();
    if (linkPreview != null) {
      previews = Collections.singletonList(linkPreview);
    }
    SharedContact contact = message.getSharedContact();
    String body = UpdateMessageData.Companion.buildSharedContact(contact.getThreadId(), contact.getAddress(), contact.getName()).toJSON();

    return new OutgoingMediaMessage(recipient, body, attachments, message.getSentTimestamp(), -1,
            recipient.getExpireMessages() * 1000, DistributionTypes.DEFAULT, outgoingQuote, Collections.emptyList(),
            previews, Collections.emptyList(), Collections.emptyList());
  }

  public Recipient getRecipient() {
    return recipient;
  }

  public String getBody() {
    return body;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public boolean isSecure() {
    return true;
  }

  public boolean isGroup() {
    return false;
  }

  public boolean isExpirationUpdate() { return false; }

  public long getSentTimeMillis() {
    return sentTimeMillis;
  }

  public int getSubscriptionId() {
    return subscriptionId;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public @Nullable QuoteModel getOutgoingQuote() {
    return outgoingQuote;
  }

  public @NonNull List<Contact> getSharedContacts() {
    return contacts;
  }

  public @NonNull List<LinkPreview> getLinkPreviews() {
    return linkPreviews;
  }

}
