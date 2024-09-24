package io.beldex.bchat.components;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.annimon.stream.Stream;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.mms.DecryptableStreamUriLoader;
import io.beldex.bchat.mms.GlideRequests;
import io.beldex.bchat.mms.Slide;
import io.beldex.bchat.mms.SlideDeck;
import io.beldex.bchat.util.UiModeUtilities;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.beldex.libbchat.messaging.contacts.Contact;
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import com.beldex.libbchat.utilities.ThemeUtil;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libbchat.utilities.recipients.RecipientModifiedListener;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.util.UiModeUtilities;
import io.beldex.bchat.database.BchatContactDatabase;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.mms.DecryptableStreamUriLoader;
import io.beldex.bchat.mms.GlideRequests;
import io.beldex.bchat.mms.Slide;
import io.beldex.bchat.mms.SlideDeck;
import io.beldex.bchat.util.UiModeUtilities;

import java.util.List;

import io.beldex.bchat.R;

public class QuoteView extends FrameLayout implements RecipientModifiedListener {

  private static final String TAG = QuoteView.class.getSimpleName();

  private static final int MESSAGE_TYPE_PREVIEW  = 0;
  private static final int MESSAGE_TYPE_OUTGOING = 1;
  private static final int MESSAGE_TYPE_INCOMING = 2;

  private ViewGroup mainView;
  private ViewGroup footerView;
  private TextView  authorView;
  private TextView  bodyView;
  private ImageView quoteBarView;
  private ImageView thumbnailView;
  private View      attachmentVideoOverlayView;
  private ViewGroup attachmentContainerView;
  private TextView  attachmentNameView;
  private ImageView dismissView;

  private long       id;
  private Recipient  author;
  private String     body;
  private Recipient  conversationRecipient;
  private TextView   mediaDescriptionText;
  private TextView   missingLinkText;
  private SlideDeck attachments;
  private int        messageType;
  private int        largeCornerRadius;
  private int        smallCornerRadius;
  private CornerMask cornerMask;


  public QuoteView(Context context) {
    super(context);
    initialize(null);
  }

  public QuoteView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize(attrs);
  }

  public QuoteView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(attrs);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public QuoteView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initialize(attrs);
  }

  private void initialize(@Nullable AttributeSet attrs) {
    inflate(getContext(), R.layout.quote_view, this);

    this.mainView                     = findViewById(R.id.quote_main);
    this.footerView                   = findViewById(R.id.quote_missing_footer);
    this.authorView                   = findViewById(R.id.quote_author);
    this.bodyView                     = findViewById(R.id.quote_text);
    this.quoteBarView                 = findViewById(R.id.quote_bar);
    this.thumbnailView                = findViewById(R.id.quote_thumbnail);
    this.attachmentVideoOverlayView   = findViewById(R.id.quote_video_overlay);
    this.attachmentContainerView      = findViewById(R.id.quote_attachment_container);
    this.attachmentNameView           = findViewById(R.id.quote_attachment_name);
    this.dismissView                  = findViewById(R.id.quote_dismiss);
    this.mediaDescriptionText         = findViewById(R.id.media_type);
    this.missingLinkText              = findViewById(R.id.quote_missing_text);
    this.largeCornerRadius            = getResources().getDimensionPixelSize(R.dimen.quote_corner_radius_bottom);
    this.smallCornerRadius            = getResources().getDimensionPixelSize(R.dimen.quote_corner_radius_bottom);

    cornerMask = new CornerMask(this);
    cornerMask.setRadii(largeCornerRadius, largeCornerRadius, smallCornerRadius, smallCornerRadius);

    if (attrs != null) {
      TypedArray typedArray     = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.QuoteView, 0, 0);
      int        primaryColor   = typedArray.getColor(R.styleable.QuoteView_quote_colorPrimary, Color.BLACK);
      int        secondaryColor = typedArray.getColor(R.styleable.QuoteView_quote_colorSecondary, Color.BLACK);
      messageType = typedArray.getInt(R.styleable.QuoteView_message_type, 0);
      typedArray.recycle();

      dismissView.setVisibility(messageType == MESSAGE_TYPE_PREVIEW ? VISIBLE : GONE);

      authorView.setTextColor(primaryColor);
      bodyView.setTextColor(primaryColor);
      attachmentNameView.setTextColor(primaryColor);
      mediaDescriptionText.setTextColor(secondaryColor);
      missingLinkText.setTextColor(primaryColor);

      if (messageType == MESSAGE_TYPE_PREVIEW) {
        int radius = getResources().getDimensionPixelOffset(R.dimen.quote_corner_radius_preview);
        cornerMask.setTopLeftRadius(radius);
        cornerMask.setTopRightRadius(radius);
      }
    }

    dismissView.setOnClickListener(view -> setVisibility(GONE));
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    cornerMask.mask(canvas);
  }

  public void setQuote(GlideRequests glideRequests,
                       long id,
                       @NonNull Recipient author,
                       @Nullable String body,
                       boolean originalMissing,
                       @NonNull SlideDeck attachments,
                       @NonNull Recipient conversationRecipient)
  {
    if (this.author != null) this.author.removeListener(this);

    this.id          = id;
    this.author      = author;
    this.body        = body;
    this.attachments = attachments;
    this.conversationRecipient = conversationRecipient;

    author.addListener(this);
    setQuoteAuthor(author);
    setQuoteText(body, attachments);
    setQuoteAttachment(glideRequests, attachments);
    setQuoteMissingFooter(originalMissing);
  }

  public void setTopCornerSizes(boolean topLeftLarge, boolean topRightLarge) {
    cornerMask.setTopLeftRadius(topLeftLarge ? largeCornerRadius : smallCornerRadius);
    cornerMask.setTopRightRadius(topRightLarge ? largeCornerRadius : smallCornerRadius);
  }

  public void dismiss() {
    if (this.author != null) this.author.removeListener(this);

    this.id     = 0;
    this.author = null;
    this.body   = null;

    setVisibility(GONE);
  }

  @Override
  public void onModified(Recipient recipient) {
    Util.runOnMain(() -> {
      if (recipient == author) {
        setQuoteAuthor(recipient);
      }
    });
  }

  private void setQuoteAuthor(@NonNull Recipient author) {
    boolean outgoing    = messageType != MESSAGE_TYPE_INCOMING;
    boolean isOwnNumber = Util.isOwnNumber(getContext(), author.getAddress().serialize());

    String quoteeDisplayName;

    String senderHexEncodedPublicKey = author.getAddress().serialize();
    if (senderHexEncodedPublicKey.equalsIgnoreCase(TextSecurePreferences.getLocalNumber(getContext()))) {
      quoteeDisplayName = TextSecurePreferences.getProfileName(getContext());
    } else {
      BchatContactDatabase contactDB = DatabaseComponent.get(getContext()).bchatContactDatabase();
      Contact contact = contactDB.getContactWithBchatID(senderHexEncodedPublicKey);
      if (contact != null) {
        Contact.ContactContext context = (this.conversationRecipient.isOpenGroupRecipient()) ? Contact.ContactContext.OPEN_GROUP : Contact.ContactContext.REGULAR;
        quoteeDisplayName = contact.displayName(context);
      } else {
        quoteeDisplayName = senderHexEncodedPublicKey;
      }
    }

    authorView.setText(isOwnNumber ? getContext().getString(R.string.QuoteView_you) : quoteeDisplayName);

    // We use the raw color resource because Android 4.x was struggling with tints here
    int colorID = UiModeUtilities.isDayUiMode(getContext()) ? R.color.black : R.color.accent;
    quoteBarView.setImageResource(colorID);
    mainView.setBackgroundColor(ThemeUtil.getThemedColor(getContext(),
            outgoing ? R.attr.message_received_background_color : R.attr.message_sent_background_color));
  }

  private void setQuoteText(@Nullable String body, @NonNull SlideDeck attachments) {
    if (!TextUtils.isEmpty(body) || !attachments.containsMediaSlide()) {
      bodyView.setVisibility(VISIBLE);
      bodyView.setText(body == null ? "" : body);
      mediaDescriptionText.setVisibility(GONE);
      return;
    }

    bodyView.setVisibility(GONE);
    mediaDescriptionText.setVisibility(VISIBLE);

    List<Slide> audioSlides    = Stream.of(attachments.getSlides()).filter(Slide::hasAudio).limit(1).toList();
    List<Slide> documentSlides = Stream.of(attachments.getSlides()).filter(Slide::hasDocument).limit(1).toList();
    List<Slide> imageSlides    = Stream.of(attachments.getSlides()).filter(Slide::hasImage).limit(1).toList();
    List<Slide> videoSlides    = Stream.of(attachments.getSlides()).filter(Slide::hasVideo).limit(1).toList();

    // Given that most types have images, we specifically check images last
    if (!audioSlides.isEmpty()) {
      mediaDescriptionText.setText(R.string.QuoteView_audio);
    } else if (!documentSlides.isEmpty()) {
      mediaDescriptionText.setVisibility(GONE);
    } else if (!videoSlides.isEmpty()) {
      mediaDescriptionText.setText(R.string.QuoteView_video);
    } else if (!imageSlides.isEmpty()) {
      mediaDescriptionText.setText(R.string.QuoteView_photo);
    }
  }

  private void setQuoteAttachment(@NonNull GlideRequests glideRequests, @NonNull SlideDeck slideDeck) {
    List<Slide> imageVideoSlides = Stream.of(slideDeck.getSlides()).filter(s -> s.hasImage() || s.hasVideo()).limit(1).toList();
    List<Slide> documentSlides   = Stream.of(attachments.getSlides()).filter(Slide::hasDocument).limit(1).toList();

    attachmentVideoOverlayView.setVisibility(GONE);

    if (!imageVideoSlides.isEmpty() && imageVideoSlides.get(0).getThumbnailUri() != null) {
      thumbnailView.setVisibility(VISIBLE);
      attachmentContainerView.setVisibility(GONE);
      dismissView.setBackgroundResource(R.drawable.dismiss_background);
      if (imageVideoSlides.get(0).hasVideo()) {
        attachmentVideoOverlayView.setVisibility(VISIBLE);
      }
      glideRequests.load(new DecryptableStreamUriLoader.DecryptableUri(imageVideoSlides.get(0).getThumbnailUri()))
                   .centerCrop()
                   .diskCacheStrategy(DiskCacheStrategy.NONE)
                   .into(thumbnailView);
    } else if (!documentSlides.isEmpty()){
      thumbnailView.setVisibility(GONE);
      attachmentContainerView.setVisibility(VISIBLE);
      attachmentNameView.setText(documentSlides.get(0).getFileName().or(""));
    } else {
      thumbnailView.setVisibility(GONE);
      attachmentContainerView.setVisibility(GONE);
      dismissView.setBackgroundDrawable(null);
    }

    if (ThemeUtil.isDarkTheme(getContext())) {
      dismissView.setBackgroundResource(R.drawable.circle_alpha);
    }
  }

  private void setQuoteMissingFooter(boolean missing) {
    footerView.setVisibility(missing ? VISIBLE : GONE);
    footerView.setBackgroundColor(getResources().getColor(R.color.quote_not_found_background));
  }

  public long getQuoteId() {
    return id;
  }

  public Recipient getAuthor() {
    return author;
  }

  public String getBody() {
    return body;
  }

  public List<Attachment> getAttachments() {
    return attachments.asAttachments();
  }
}
