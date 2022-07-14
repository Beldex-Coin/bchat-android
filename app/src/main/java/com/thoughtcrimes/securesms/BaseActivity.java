package com.thoughtcrimes.securesms;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.fragment.app.FragmentActivity;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageActivityHelper;
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper;

import io.beldex.bchat.R;

public abstract class BaseActivity extends FragmentActivity {
  @Override
  protected void onResume() {
    super.onResume();
    DynamicLanguageActivityHelper.recreateIfNotInCorrectLanguage(this, TextSecurePreferences.getLanguage(this));
    String name = getResources().getString(R.string.app_name);
    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground);
    int color = getResources().getColor(R.color.app_icon_background);
    setTaskDescription(new ActivityManager.TaskDescription(name, icon, color));
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(DynamicLanguageContextWrapper.updateContext(newBase, TextSecurePreferences.getLanguage(newBase)));
  }
}
