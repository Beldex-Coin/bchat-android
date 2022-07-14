package com.beldex.libbchat.messaging.messages.signal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beldex.libbchat.utilities.DistributionTypes;
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import com.beldex.libbchat.utilities.Contact;
import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview;
import com.beldex.libbchat.messaging.sending_receiving.quotes.QuoteModel;
import com.beldex.libbchat.utilities.recipients.Recipient;

import java.util.LinkedList;
import java.util.List;

public class OutgoingGroupMediaMessage extends OutgoingSecureMediaMessage {

  private final String groupID;
  private final boolean isUpdateMessage;

  public OutgoingGroupMediaMessage(@NonNull Recipient recipient,
                                   @NonNull String body,
                                   @Nullable String groupId,
                                   @Nullable final Attachment avatar,
                                   long sentTime,
                                   long expireIn,
                                   boolean updateMessage,
                                   @Nullable QuoteModel quote,
                                   @NonNull List<Contact> contacts,
                                   @NonNull List<LinkPreview> previews)
  {
    super(recipient, body,
          new LinkedList<Attachment>() {{if (avatar != null) add(avatar);}},
          sentTime,
          DistributionTypes.CONVERSATION, expireIn, quote, contacts, previews);

    this.groupID = groupId;
    this.isUpdateMessage = updateMessage;
  }

  @Override
  public boolean isGroup() {
    return true;
  }

  public String getGroupId() {
    return groupID;
  }

  public boolean isUpdateMessage() {
    return isUpdateMessage;
  }
}
