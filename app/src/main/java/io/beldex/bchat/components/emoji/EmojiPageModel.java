package io.beldex.bchat.components.emoji;

import android.net.Uri;

import androidx.annotation.Nullable;

import java.util.List;

public interface EmojiPageModel {
  String getKey();

  int getIconAttr();
  List<String> getEmoji();
  List<Emoji> getDisplayEmoji();
  boolean hasSpriteMap();
  @Nullable Uri getSpriteUri();
  boolean isDynamic();
}
