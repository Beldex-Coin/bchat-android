package com.beldex.libbchat.avatars;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Key;

import java.io.IOException;
import java.io.InputStream;

public interface ContactPhoto extends Key {

  InputStream openInputStream(Context context, Boolean isNotification) throws IOException;

  @Nullable Uri getUri(@NonNull Context context);

  boolean isProfilePhoto();
}
