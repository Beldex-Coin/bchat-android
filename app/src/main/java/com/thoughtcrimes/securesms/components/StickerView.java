package com.thoughtcrimes.securesms.components;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.thoughtcrimes.securesms.mms.Slide;
import com.thoughtcrimes.securesms.mms.SlideClickListener;
import com.thoughtcrimes.securesms.mms.SlidesClickedListener;
import com.thoughtcrimes.securesms.conversation.v2.utilities.ThumbnailView;
import com.thoughtcrimes.securesms.mms.GlideRequests;

import com.thoughtcrimes.securesms.mms.Slide;
import com.thoughtcrimes.securesms.mms.SlideClickListener;
import com.thoughtcrimes.securesms.mms.SlidesClickedListener;

import io.beldex.bchat.R;

public class StickerView extends FrameLayout {

  private ThumbnailView image;
  private View          missingShade;

  public StickerView(@NonNull Context context) {
    super(context);
    init();
  }

  public StickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.sticker_view, this);

    this.image        = findViewById(R.id.sticker_thumbnail);
    this.missingShade = findViewById(R.id.sticker_missing_shade);
  }

  @Override
  public void setFocusable(boolean focusable) {
    image.setFocusable(focusable);
  }

  @Override
  public void setClickable(boolean clickable) {
    image.setClickable(clickable);
  }

  @Override
  public void setOnLongClickListener(@Nullable OnLongClickListener l) {
    image.setOnLongClickListener(l);
  }

  public void setSticker(@NonNull GlideRequests glideRequests, @NonNull Slide stickerSlide) {
    boolean showControls = stickerSlide.asAttachment().getDataUri() == null;

    image.setImageResource(glideRequests, stickerSlide, showControls, false);
    missingShade.setVisibility(showControls ? View.VISIBLE : View.GONE);
  }

  public void setThumbnailClickListener(@NonNull SlideClickListener listener) {
    image.setThumbnailClickListener(listener);
  }

  public void setDownloadClickListener(@NonNull SlidesClickedListener listener) {
    image.setDownloadClickListener(listener);
  }
}
