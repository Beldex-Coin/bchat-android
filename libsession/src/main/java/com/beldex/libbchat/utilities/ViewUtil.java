/**
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.beldex.libbchat.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.beldex.libsignal.utilities.ListenableFuture;
import com.beldex.libsignal.utilities.SettableFuture;

public class ViewUtil {
  @SuppressWarnings("deprecation")
  public static void setBackground(final @NonNull View v, final @Nullable Drawable drawable) {
    v.setBackground(drawable);
  }

  @SuppressWarnings("unchecked")
  public static <T extends View> T findById(@NonNull View parent, @IdRes int resId) {
    return (T) parent.findViewById(resId);
  }

  @SuppressWarnings("unchecked")
  public static <T extends View> T findById(@NonNull Activity parent, @IdRes int resId) {
    return (T) parent.findViewById(resId);
  }

  private static Animation getAlphaAnimation(float from, float to, int duration) {
    final Animation anim = new AlphaAnimation(from, to);
    anim.setInterpolator(new FastOutSlowInInterpolator());
    anim.setDuration(duration);
    return anim;
  }

  public static void fadeIn(final @NonNull View view, final int duration) {
    animateIn(view, getAlphaAnimation(0f, 1f, duration));
  }

  public static ListenableFuture<Boolean> fadeOut(final @NonNull View view, final int duration) {
    return fadeOut(view, duration, View.GONE);
  }

  public static ListenableFuture<Boolean> fadeOut(@NonNull View view, int duration, int visibility) {
    return animateOut(view, getAlphaAnimation(1f, 0f, duration), visibility);
  }

  public static ListenableFuture<Boolean> animateOut(final @NonNull View view, final @NonNull Animation animation, final int visibility) {
    final SettableFuture future = new SettableFuture();
    if (view.getVisibility() == visibility) {
      future.set(true);
    } else {
      view.clearAnimation();
      animation.reset();
      animation.setStartTime(0);
      animation.setAnimationListener(new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}

        @Override
        public void onAnimationEnd(Animation animation) {
          view.setVisibility(visibility);
          future.set(true);
        }
      });
      view.startAnimation(animation);
    }
    return future;
  }

  public static void animateIn(final @NonNull View view, final @NonNull Animation animation) {
    if (view.getVisibility() == View.VISIBLE) return;

    view.clearAnimation();
    animation.reset();
    animation.setStartTime(0);
    view.setVisibility(View.VISIBLE);
    view.startAnimation(animation);
  }

  @SuppressWarnings("unchecked")
  public static <T extends View> T inflate(@NonNull   LayoutInflater inflater,
                                           @NonNull   ViewGroup      parent,
                                           @LayoutRes int            layoutResId)
  {
    return (T)(inflater.inflate(layoutResId, parent, false));
  }

  public static int dpToPx(Context context, int dp) {
    return (int)((dp * context.getResources().getDisplayMetrics().density) + 0.5);
  }
  public static int dpToPx(int dp) {
    return Math.round(dp * Resources.getSystem().getDisplayMetrics().density);
  }


  public static void updateLayoutParams(@NonNull View view, int width, int height) {
    view.getLayoutParams().width  = width;
    view.getLayoutParams().height = height;
    view.requestLayout();
  }

  public static int getLeftMargin(@NonNull View view) {
    if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
      return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).leftMargin;
    }
    return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).rightMargin;
  }

  public static int getRightMargin(@NonNull View view) {
    if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
      return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).rightMargin;
    }
    return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).leftMargin;
  }

  public static void setLeftMargin(@NonNull View view, int margin) {
    if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_LTR) {
      ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).leftMargin = margin;
    } else {
      ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).rightMargin = margin;
    }
    view.forceLayout();
    view.requestLayout();
  }

  public static void setTopMargin(@NonNull View view, int margin) {
    ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).topMargin = margin;
    view.requestLayout();
  }

  public static void setPaddingTop(@NonNull View view, int padding) {
    view.setPadding(view.getPaddingLeft(), padding, view.getPaddingRight(), view.getPaddingBottom());
  }

  public static void setPaddingBottom(@NonNull View view, int padding) {
    view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), padding);
  }

  public static boolean isPointInsideView(@NonNull View view, float x, float y) {
    int[] location = new int[2];

    view.getLocationOnScreen(location);

    int viewX = location[0];
    int viewY = location[1];

    return x > viewX && x < viewX + view.getWidth() &&
           y > viewY && y < viewY + view.getHeight();
  }
}
