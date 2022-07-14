package com.thoughtcrimes.securesms.notifications;

import android.content.Context;
import androidx.annotation.NonNull;

import com.beldex.libbchat.utilities.recipients.Recipient;

public enum ReplyMethod {

  GroupMessage,
  SecureMessage;

  public static @NonNull ReplyMethod forRecipient(Context context, Recipient recipient) {
    if (recipient.isGroupRecipient()) {
      return ReplyMethod.GroupMessage;
    }
    return ReplyMethod.SecureMessage;
  }
}
