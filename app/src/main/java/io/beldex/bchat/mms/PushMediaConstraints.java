package io.beldex.bchat.mms;

import android.content.Context;

import io.beldex.bchat.util.Util;
import io.beldex.bchat.util.Util;

import com.beldex.libbchat.messaging.file_server.FileServerAPIV2;

public class PushMediaConstraints extends MediaConstraints {

  private static final int MAX_IMAGE_DIMEN_LOWMEM = 768;
  private static final int MAX_IMAGE_DIMEN        = 4096;

  @Override
  public int getImageMaxWidth(Context context) {
    return Util.isLowMemory(context) ? MAX_IMAGE_DIMEN_LOWMEM : MAX_IMAGE_DIMEN;
  }

  @Override
  public int getImageMaxHeight(Context context) {
    return getImageMaxWidth(context);
  }

  @Override
  public int getImageMaxSize(Context context) {
    return FileServerAPIV2.maxFileSize;
  }

  @Override
  public int getGifMaxSize(Context context) {
    return FileServerAPIV2.maxFileSize;
  }

  @Override
  public int getVideoMaxSize(Context context) {
    return FileServerAPIV2.maxFileSize;
  }

  @Override
  public int getAudioMaxSize(Context context) {
    return FileServerAPIV2.maxFileSize;
  }

  @Override
  public int getDocumentMaxSize(Context context) {
    return FileServerAPIV2.maxFileSize;
  }
}
