package com.beldex.libbchat.avatars;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.annimon.stream.Stream;
import com.beldex.libbchat.R;
import com.beldex.libbchat.utilities.Address;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class AvatarHelper {

  private static final String AVATAR_DIRECTORY = "avatars";

  public static InputStream getInputStreamFor(@NonNull Context context, @NonNull Address address, Boolean isNotification) {
    File avatarFile = getAvatarFile(context, address);
    try {
      if (!avatarFile.exists() && isNotification) {
        return getDrawableInputStream(context, R.drawable.defualt_profile_pic);
      }
      return new FileInputStream(avatarFile);
    } catch (IOException e) {
      return new ByteArrayInputStream(new byte[0]); // Return an empty stream
    }
  }

  private static InputStream getDrawableInputStream(Context context, int drawableRes) {
    Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
    if (drawable == null) return null;

    Bitmap bitmap;

    if (drawable instanceof BitmapDrawable) {
      bitmap = ((BitmapDrawable) drawable).getBitmap();
    } else {
      // Convert VectorDrawable to Bitmap
      bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
      drawable.draw(canvas);
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  public static List<File> getAvatarFiles(@NonNull Context context) {
    File   avatarDirectory = new File(context.getFilesDir(), AVATAR_DIRECTORY);
    File[] results         = avatarDirectory.listFiles();

    if (results == null) return new LinkedList<>();
    else                 return Stream.of(results).toList();
  }

  public static void delete(@NonNull Context context, @NonNull Address address) {
    getAvatarFile(context, address).delete();
  }

  public static @NonNull File getAvatarFile(@NonNull Context context, @NonNull Address address) {
    File avatarDirectory = new File(context.getFilesDir(), AVATAR_DIRECTORY);
    avatarDirectory.mkdirs();

    return new File(avatarDirectory, new File(address.serialize()).getName());
  }

  public static boolean avatarFileExists(@NonNull Context context , @NonNull Address address) {
    File avatarFile = getAvatarFile(context, address);
    return avatarFile.exists();
  }

  public static void setAvatar(@NonNull Context context, @NonNull Address address, @Nullable byte[] data)
    throws IOException
  {
    if (data == null)  {
      delete(context, address);
    } else {
      try (FileOutputStream out = new FileOutputStream(getAvatarFile(context, address))) {
        out.write(data);
      }
    }
  }

}
