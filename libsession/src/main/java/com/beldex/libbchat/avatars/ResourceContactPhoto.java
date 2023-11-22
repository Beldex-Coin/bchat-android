package com.beldex.libbchat.avatars;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.amulyakhare.textdrawable.TextDrawable;
import com.makeramen.roundedimageview.RoundedDrawable;


import com.beldex.libbchat.R;
import com.beldex.libbchat.utilities.ThemeUtil;

public class ResourceContactPhoto implements FallbackContactPhoto {

  private final int resourceId;

  public ResourceContactPhoto(@DrawableRes int resourceId) {
    this.resourceId = resourceId;
  }

  @Override
  public Drawable asDrawable(Context context, int color) {
    return asDrawable(context, color, false);
  }

  @Override
  public Drawable asDrawable(Context context, int color, boolean inverted) {
    Drawable        background = TextDrawable.builder().buildRound(" ", inverted ? Color.WHITE :color);
    RoundedDrawable foreground = (RoundedDrawable) RoundedDrawable.fromDrawable(ContextCompat.getDrawable(context,resourceId));

    assert foreground != null;
    foreground.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

    if (inverted) {
      foreground.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    Drawable gradient = ContextCompat.getDrawable(context,ThemeUtil.isDarkTheme(context) ? R.drawable.avatar_gradient_dark
            : R.drawable.avatar_gradient_light);

    return new ExpandingLayerDrawable(new Drawable[] {background, foreground, gradient});
  }

  private static class ExpandingLayerDrawable extends LayerDrawable {
    public ExpandingLayerDrawable(Drawable[] layers) {
      super(layers);
    }

    @Override
    public int getIntrinsicWidth() {
      return -1;
    }

    @Override
    public int getIntrinsicHeight() {
      return -1;
    }
  }

}
