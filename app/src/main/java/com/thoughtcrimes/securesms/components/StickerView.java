package com.thoughtcrimes.securesms.components;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.thoughtcrimes.securesms.conversation.v2.utilities.ThumbnailView;

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
}
