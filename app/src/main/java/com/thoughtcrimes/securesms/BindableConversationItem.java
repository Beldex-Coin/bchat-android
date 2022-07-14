package com.thoughtcrimes.securesms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.thoughtcrimes.securesms.database.model.MessageRecord;
import com.thoughtcrimes.securesms.database.model.MmsMessageRecord;
import com.thoughtcrimes.securesms.mms.GlideRequests;
import com.beldex.libsignal.utilities.guava.Optional;

import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.recipients.Recipient;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationItem extends Unbindable {
  void bind(@NonNull MessageRecord           messageRecord,
            @NonNull Optional<MessageRecord> previousMessageRecord,
            @NonNull Optional<MessageRecord> nextMessageRecord,
            @NonNull GlideRequests           glideRequests,
            @NonNull Locale                  locale,
            @NonNull Set<MessageRecord>      batchSelected,
            @NonNull Recipient               recipients,
            @Nullable String                 searchQuery,
                     boolean                 pulseHighlight);

  MessageRecord getMessageRecord();

  void setEventListener(@Nullable EventListener listener);

  interface EventListener {
    void onQuoteClicked(MmsMessageRecord messageRecord);
    void onLinkPreviewClicked(@NonNull LinkPreview linkPreview);
    void onMoreTextClicked(@NonNull Address conversationAddress, long messageId, boolean isMms);
  }
}
