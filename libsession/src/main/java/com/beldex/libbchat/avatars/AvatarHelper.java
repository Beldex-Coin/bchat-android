package com.beldex.libbchat.avatars;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;

import com.beldex.libbchat.utilities.Address;

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

  public static InputStream getInputStreamFor(@NonNull Context context, @NonNull Address address)
      throws IOException
  {
    File avatarFile = getAvatarFile(context, address);
    if (!avatarFile.exists()) {
      throw new FileNotFoundException("Avatar file not found: " + avatarFile.getAbsolutePath());
    }
    return new FileInputStream(avatarFile);
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
