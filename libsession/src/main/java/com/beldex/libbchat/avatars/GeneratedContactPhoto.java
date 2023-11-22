package com.beldex.libbchat.avatars;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.amulyakhare.textdrawable.TextDrawable;

import com.beldex.libbchat.R;
import com.beldex.libbchat.utilities.ThemeUtil;
import com.beldex.libbchat.utilities.ViewUtil;

import java.util.regex.Pattern;

public class GeneratedContactPhoto implements FallbackContactPhoto {

  private static final Pattern  PATTERN  = Pattern.compile("[^\\p{L}\\p{Nd}\\p{S}]+");
  private static final Typeface TYPEFACE = Typeface.create("sans-serif-medium", Typeface.NORMAL);

  private final String name;
  private final int    fallbackResId;

  public GeneratedContactPhoto(@NonNull String name, @DrawableRes int fallbackResId) {
    this.name          = name;
    this.fallbackResId = fallbackResId;
  }

  @Override
  public Drawable asDrawable(Context context, int color) {
    return asDrawable(context, color,false);
  }

  @Override
  public Drawable asDrawable(Context context, int color, boolean inverted) {
    int targetSize = context.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);
    String character = getAbbreviation(name);

    if (!TextUtils.isEmpty(character)) {
      Drawable base = TextDrawable.builder()
                                  .beginConfig()
                                  .width(targetSize)
                                  .height(targetSize)
                                  .useFont(TYPEFACE)
                                  .fontSize(ViewUtil.dpToPx(context, 24))
                                  .textColor(inverted ? color : Color.WHITE)
                                  .endConfig()
                                  .buildRound(character, inverted ? Color.WHITE : color);

      Drawable gradient = ContextCompat.getDrawable(context,ThemeUtil.isDarkTheme(context) ? R.drawable.avatar_gradient_dark
                                                                                            : R.drawable.avatar_gradient_light);
      return new LayerDrawable(new Drawable[] { base, gradient });
    }

    return new ResourceContactPhoto(fallbackResId).asDrawable(context, color, inverted);
  }

  private @Nullable String getAbbreviation(String name) {
    String[]      parts   = name.split(" ");
    StringBuilder builder = new StringBuilder();
    int           count   = 0;

    for (int i = 0; i < parts.length && count < 2; i++) {
      String cleaned = PATTERN.matcher(parts[i]).replaceFirst("");
      if (!TextUtils.isEmpty(cleaned)) {
        builder.appendCodePoint(cleaned.codePointAt(0));
        count++;
      }
    }

    if (builder.length() == 0) {
      return null;
    } else {
      return builder.toString();
    }
  }
}
