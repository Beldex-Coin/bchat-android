package com.thoughtcrimes.securesms.util;


import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MemoryFileUtil {

  public static ParcelFileDescriptor getParcelFileDescriptor(MemoryFile file) throws IOException {
    try {
      Method         method         = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
      FileDescriptor fileDescriptor = (FileDescriptor) method.invoke(file);

      Field  field  = fileDescriptor.getClass().getDeclaredField("descriptor");
      field.setAccessible(true);

      int fd = field.getInt(fileDescriptor);

      return ParcelFileDescriptor.adoptFd(fd);
    } catch (IllegalAccessException e) {
      throw new IOException(e);
    } catch (InvocationTargetException e) {
      throw new IOException(e);
    } catch (NoSuchMethodException e) {
      throw new IOException(e);
    } catch (NoSuchFieldException e) {
      throw new IOException(e);
    }
  }
}
