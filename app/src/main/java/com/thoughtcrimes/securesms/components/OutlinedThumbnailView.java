package com.thoughtcrimes.securesms.components;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.beldex.libbchat.utilities.ThemeUtil;
import com.thoughtcrimes.securesms.conversation.v2.utilities.ThumbnailView;

import io.beldex.bchat.R;

public class OutlinedThumbnailView extends ThumbnailView {

  private CornerMask cornerMask;
  private Outliner   outliner;

  public OutlinedThumbnailView(Context context) {
    super(context);
    init();
  }

  public OutlinedThumbnailView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    cornerMask = new CornerMask(this);
    outliner   = new Outliner();

    outliner.setColor(ThemeUtil.getThemedColor(getContext(), R.attr.conversation_item_image_outline_color));
    setWillNotDraw(false);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);

    cornerMask.mask(canvas);
    outliner.draw(canvas);
  }

  public void setCorners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
    cornerMask.setRadii(topLeft, topRight, bottomRight, bottomLeft);
    outliner.setRadii(topLeft, topRight, bottomRight, bottomLeft);
    postInvalidate();
  }
}
