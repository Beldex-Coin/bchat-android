package io.beldex.bchat.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.app.RemoteInput;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import io.beldex.bchat.database.BchatContactDatabase;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.mms.DecryptableStreamUriLoader;
import io.beldex.bchat.mms.Slide;
import io.beldex.bchat.mms.SlideDeck;
import io.beldex.bchat.util.AvatarPlaceholderGenerator;
import io.beldex.bchat.util.BitmapUtil;

import com.beldex.libbchat.avatars.ContactColors;
import com.beldex.libbchat.avatars.ContactPhoto;
import com.beldex.libbchat.avatars.ResourceContactPhoto;
import com.beldex.libbchat.messaging.contacts.Contact;
import com.beldex.libbchat.utilities.NotificationPrivacyPreference;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.database.BchatContactDatabase;
import io.beldex.bchat.dependencies.DatabaseComponent;

import io.beldex.bchat.mms.GlideApp;

import io.beldex.bchat.util.AvatarPlaceholderGenerator;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.beldex.bchat.R;

public class SingleRecipientNotificationBuilder extends AbstractNotificationBuilder {

  private static final String TAG = SingleRecipientNotificationBuilder.class.getSimpleName();

  private final List<CharSequence> messageBodies = new LinkedList<>();

  private SlideDeck slideDeck;
  private CharSequence contentTitle;
  private CharSequence contentText;

  private static final Integer ICON_SIZE = 128;

  public SingleRecipientNotificationBuilder(@NonNull Context context, @NonNull NotificationPrivacyPreference privacy)
  {
    super(context, privacy);


    setSmallIcon(R.drawable.ic_notification_);
    setColor(context.getResources().getColor(R.color.textsecure_primary));
    setCategory(NotificationCompat.CATEGORY_MESSAGE);

    if (!NotificationChannels.supported()) {
      setPriority(TextSecurePreferences.getNotificationPriority(context));
    }
  }

  public void setThread(@NonNull Recipient recipient) {
    String channelId = recipient.getNotificationChannel();
    setChannelId(channelId != null ? channelId : NotificationChannels.getMessagesChannel(context));

    if (privacy.isDisplayContact()) {
      setContentTitle(recipient.toShortString());

      if (recipient.getContactUri() != null) {
        addPerson(recipient.getContactUri().toString());
      }

      ContactPhoto contactPhoto = recipient.getContactPhoto();
      if (contactPhoto != null) {
        try {
          // AC: For some reason, if not use ".asBitmap()" method, the returned BitmapDrawable
          // wraps a recycled bitmap and leads to a crash.
          Bitmap iconBitmap = GlideApp.with(context.getApplicationContext())
                  .asBitmap()
                  .load(contactPhoto)
                  .diskCacheStrategy(DiskCacheStrategy.NONE)
                  .circleCrop()
                  .submit(context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                          context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height))
                  .get();
          setLargeIcon(iconBitmap);
        } catch (InterruptedException | ExecutionException e) {
          Log.w(TAG, "get iconBitmap in getThread failed",e);
          setLargeIcon(getPlaceholderDrawable(context, recipient));
        }
      } else {
        setLargeIcon(getPlaceholderDrawable(context, recipient));
      }

    } else {
      setContentTitle(context.getString(R.string.SingleRecipientNotificationBuilder_signal));
      setLargeIcon(AvatarPlaceholderGenerator.generate(context, ICON_SIZE, "", "Unknown"));
    }
  }

  public void setMessageCount(int messageCount) {
    setContentInfo(String.valueOf(messageCount));
    setNumber(messageCount);
  }

  public void setPrimaryMessageBody(@NonNull  Recipient threadRecipients,
                                    @NonNull  Recipient individualRecipient,
                                    @NonNull  CharSequence message,
                                    @Nullable SlideDeck slideDeck)
  {
    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

    if (privacy.isDisplayContact() && threadRecipients.isOpenGroupRecipient()) {
      String displayName = getOpenGroupDisplayName(individualRecipient);
      stringBuilder.append(Util.getBoldedString(displayName + ": "));
    }

    if (privacy.isDisplayMessage()) {
      setContentText(stringBuilder.append(message));
      this.slideDeck = slideDeck;
    } else {
      setContentText(stringBuilder.append(context.getString(R.string.SingleRecipientNotificationBuilder_new_message)));
    }
  }

  public void addAndroidAutoAction(@NonNull PendingIntent androidAutoReplyIntent,
                                   @NonNull PendingIntent androidAutoHeardIntent, long timestamp)
  {

    if (contentTitle == null || contentText == null)
      return;

    RemoteInput remoteInput = new RemoteInput.Builder(AndroidAutoReplyReceiver.VOICE_REPLY_KEY)
                                  .setLabel(context.getString(R.string.MessageNotifier_reply))
                                  .build();

    NotificationCompat.CarExtender.UnreadConversation.Builder unreadConversationBuilder =
            new NotificationCompat.CarExtender.UnreadConversation.Builder(contentTitle.toString())
                .addMessage(contentText.toString())
                .setLatestTimestamp(timestamp)
                .setReadPendingIntent(androidAutoHeardIntent)
                .setReplyAction(androidAutoReplyIntent, remoteInput);

    extend(new NotificationCompat.CarExtender().setUnreadConversation(unreadConversationBuilder.build()));
  }

  public void addActions(@NonNull PendingIntent markReadIntent,
                         @Nullable PendingIntent quickReplyIntent,
                         @Nullable PendingIntent wearableReplyIntent,
                         @NonNull ReplyMethod replyMethod)
  {
    Action markAsReadAction = new Action(R.drawable.check,
                                         context.getString(R.string.MessageNotifier_mark_read),
                                         markReadIntent);

    addAction(markAsReadAction);

    NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender().addAction(markAsReadAction);

    if (quickReplyIntent != null) {
      String actionName = context.getString(R.string.MessageNotifier_reply);
      String label = context.getString(replyMethodLongDescription(replyMethod));

      Action replyAction = new Action(R.drawable.ic_reply_white_36dp, actionName, quickReplyIntent);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        replyAction = new Action.Builder(R.drawable.ic_reply_white_36dp,
                actionName,
                wearableReplyIntent)
                .addRemoteInput(new RemoteInput.Builder(DefaultMessageNotifier.EXTRA_REMOTE_REPLY).setLabel(label).build())
                .build();
      }

      Action wearableReplyAction = new Action.Builder(R.drawable.ic_reply,
              actionName,
              wearableReplyIntent)
              .addRemoteInput(new RemoteInput.Builder(DefaultMessageNotifier.EXTRA_REMOTE_REPLY).setLabel(label).build())
              .build();


      addAction(replyAction);
      wearableExtender.addAction(wearableReplyAction);
    }

    extend(wearableExtender);
  }

  @StringRes
  private static int replyMethodLongDescription(@NonNull ReplyMethod replyMethod) {
    return R.string.MessageNotifier_reply;
  }

  public void putStringExtra(String key, String value) {
    extras.putString(key,value);
  }

  public void addMessageBody(@NonNull Recipient threadRecipient,
                             @NonNull Recipient individualRecipient,
                             @Nullable CharSequence messageBody)
  {
    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

    if (privacy.isDisplayContact() && threadRecipient.isOpenGroupRecipient()) {
      String displayName = getOpenGroupDisplayName(individualRecipient);
      stringBuilder.append(Util.getBoldedString(displayName + ": "));
    }

    if (privacy.isDisplayMessage()) {
      messageBodies.add(stringBuilder.append(messageBody == null ? "" : messageBody));
    } else {
      messageBodies.add(stringBuilder.append(context.getString(R.string.SingleRecipientNotificationBuilder_new_message)));
    }
  }

  @Override
  public Notification build() {
    if (privacy.isDisplayMessage()) {
      if (messageBodies.size() == 1 && hasBigPictureSlide(slideDeck)) {
        setStyle(new NotificationCompat.BigPictureStyle()
                     .bigPicture(getBigPicture(slideDeck))
                     .setSummaryText(getBigText(messageBodies)));
      } else {
        setStyle(new NotificationCompat.BigTextStyle().bigText(getBigText(messageBodies)));
      }
    }

    return super.build();
  }

  private void setLargeIcon(@Nullable Drawable drawable) {
    if (drawable != null) {
      int    largeIconTargetSize  = context.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);
      Bitmap recipientPhotoBitmap = BitmapUtil.createFromDrawable(drawable, largeIconTargetSize, largeIconTargetSize);

      if (recipientPhotoBitmap != null) {
        setLargeIcon(getCircularBitmap(recipientPhotoBitmap));
      }
    }
  }

  private Bitmap getCircularBitmap(Bitmap bitmap) {
    final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(output);
    final int color = Color.RED;
    final Paint paint = new Paint();
    final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
    final RectF rectF = new RectF(rect);

    paint.setAntiAlias(true);
    canvas.drawARGB(0, 0, 0, 0);
    paint.setColor(color);
    canvas.drawOval(rectF, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(bitmap, rect, rect, paint);

    bitmap.recycle();

    return output;
  }

  private boolean hasBigPictureSlide(@Nullable SlideDeck slideDeck) {
    if (slideDeck == null) {
      return false;
    }

    Slide thumbnailSlide = slideDeck.getThumbnailSlide();

    return thumbnailSlide != null         &&
           thumbnailSlide.hasImage()      &&
           !thumbnailSlide.isInProgress() &&
           thumbnailSlide.getThumbnailUri() != null;
  }

  private Bitmap getBigPicture(@NonNull SlideDeck slideDeck)
  {
    try {
      @SuppressWarnings("ConstantConditions")
      Uri uri = slideDeck.getThumbnailSlide().getThumbnailUri();

      return GlideApp.with(context.getApplicationContext())
                     .asBitmap()
                     .load(new DecryptableStreamUriLoader.DecryptableUri(uri))
                     .diskCacheStrategy(DiskCacheStrategy.NONE)
                     .submit(64, 64)
                     .get();
    } catch (InterruptedException | ExecutionException e) {
      Log.w(TAG, "getBigPicture failed",e);
      return Bitmap.createBitmap(64, 64, Bitmap.Config.RGB_565);
    }
  }

  @Override
  public NotificationCompat.Builder setContentTitle(CharSequence contentTitle) {
    this.contentTitle = contentTitle;
    return super.setContentTitle(contentTitle);
  }

  public NotificationCompat.Builder setContentText(CharSequence contentText) {
    this.contentText = trimToDisplayLength(contentText);
    return super.setContentText(this.contentText);
  }

  private CharSequence getBigText(List<CharSequence> messageBodies) {
    SpannableStringBuilder content = new SpannableStringBuilder();

    for (int i = 0; i < messageBodies.size(); i++) {
      content.append(trimToDisplayLength(messageBodies.get(i)));
      if (i < messageBodies.size() - 1) {
        content.append('\n');
      }
    }

    return content;
  }

  private static Drawable getPlaceholderDrawable(Context context, Recipient recipient) {
    String publicKey = recipient.getAddress().serialize();
    String displayName = recipient.getName();
    return AvatarPlaceholderGenerator.generate(context, ICON_SIZE, publicKey, displayName);
  }

  /**
   * @param recipient the * individual * recipient for which to get the social group display name.
   */
  private String getOpenGroupDisplayName(Recipient recipient) {
    BchatContactDatabase contactDB = DatabaseComponent.get(context).bchatContactDatabase();
    String bchatID = recipient.getAddress().serialize();
    Contact contact = contactDB.getContactWithBchatID(bchatID);
    if (contact == null) { return bchatID; }
    String displayName = contact.displayName(Contact.ContactContext.OPEN_GROUP);
    if (displayName == null) { return bchatID; }
    return displayName;
  }
}
