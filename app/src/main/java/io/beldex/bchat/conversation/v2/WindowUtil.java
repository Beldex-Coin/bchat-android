package io.beldex.bchat.conversation.v2;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.beldex.libbchat.utilities.ThemeUtil;

public final class WindowUtil {

  private WindowUtil() {
  }

  public static void setLightNavigationBarFromTheme(@NonNull Activity activity) {
    if (Build.VERSION.SDK_INT < 27) return;

    final boolean isLightNavigationBar = ThemeUtil.getThemedBoolean(activity, android.R.attr.windowLightNavigationBar);

    if (isLightNavigationBar) setLightNavigationBar(activity.getWindow());
    else                      clearLightNavigationBar(activity.getWindow());
  }

  public static void clearLightNavigationBar(@NonNull Window window) {
    if (Build.VERSION.SDK_INT < 27) return;

    clearSystemUiFlags(window, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }

  public static void setLightNavigationBar(@NonNull Window window) {
    if (Build.VERSION.SDK_INT < 27) return;

    setSystemUiFlags(window, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }

  public static void setNavigationBarColor(@NonNull Window window, @ColorInt int color) {
    window.setNavigationBarColor(color);
  }

  public static void setLightStatusBarFromTheme(@NonNull Activity activity) {
    final boolean isLightStatusBar = ThemeUtil.getThemedBoolean(activity, android.R.attr.windowLightStatusBar);

    if (isLightStatusBar) setLightStatusBar(activity.getWindow());
    else                  clearLightStatusBar(activity.getWindow());
  }

  public static void clearLightStatusBar(@NonNull Window window) {
    clearSystemUiFlags(window, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  }

  public static void setLightStatusBar(@NonNull Window window) {
    setSystemUiFlags(window, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  }

  public static void setStatusBarColor(@NonNull Window window, @ColorInt int color) {
    window.setStatusBarColor(color);
  }

  /**
   * A sort of roundabout way of determining if the status bar is present by seeing if there's a
   * vertical window offset.
   */
  public static boolean isStatusBarPresent(@NonNull Window window) {
    Rect rectangle = new Rect();
    window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
    return rectangle.top > 0;
  }

  private static void clearSystemUiFlags(@NonNull Window window, int flags) {
    View view    = window.getDecorView();
    int  uiFlags = view.getSystemUiVisibility();

    uiFlags &= ~flags;
    view.setSystemUiVisibility(uiFlags);
  }

  private static void setSystemUiFlags(@NonNull Window window, int flags) {
    View view    = window.getDecorView();
    int  uiFlags = view.getSystemUiVisibility();

    uiFlags |= flags;
    view.setSystemUiVisibility(uiFlags);
  }
}
