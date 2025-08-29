package io.beldex.bchat.reactions.any;

import android.content.Context;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.components.emoji.RecentEmojiPageModel;
import io.beldex.bchat.emoji.EmojiCategory;
import io.beldex.bchat.emoji.EmojiSource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.beldex.bchat.R;

final class ReactWithAnyEmojiRepository {

  private static final String TAG = Log.tag(ReactWithAnyEmojiRepository.class);

  private final Context                     context;
  private final RecentEmojiPageModel        recentEmojiPageModel;
  private final List<ReactWithAnyEmojiPage> emojiPages;

  ReactWithAnyEmojiRepository(@NonNull Context context) {
    this.context              = context;
    this.recentEmojiPageModel = new RecentEmojiPageModel(context);
    this.emojiPages           = new LinkedList<>();

    emojiPages.addAll(Stream.of(EmojiSource.getLatest().getDisplayPages())
                            .map(page -> new ReactWithAnyEmojiPage(Collections.singletonList(new ReactWithAnyEmojiPageBlock(EmojiCategory.getCategoryLabel(page.getIconAttr()), page))))
                            .toList());
  }

  List<ReactWithAnyEmojiPage> getEmojiPageModels() {
    List<ReactWithAnyEmojiPage> pages       = new LinkedList<>();

    pages.add(new ReactWithAnyEmojiPage(Collections.singletonList(new ReactWithAnyEmojiPageBlock(R.string.emojiCategoryRecentlyUsed, recentEmojiPageModel))));
    pages.addAll(emojiPages);

    return pages;
  }

  void addEmojiToMessage(@NonNull String emoji) {
    recentEmojiPageModel.onCodePointSelected(emoji);
  }
}
