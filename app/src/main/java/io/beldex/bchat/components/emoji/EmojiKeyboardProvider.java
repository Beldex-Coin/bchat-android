package io.beldex.bchat.components.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import io.beldex.bchat.util.ResUtil;
import io.beldex.bchat.components.emoji.EmojiPageViewGridAdapter.VariationSelectorListener;
import io.beldex.bchat.mms.GlideRequests;

import com.beldex.libbchat.utilities.ThemeUtil;

import java.util.LinkedList;
import java.util.List;

import io.beldex.bchat.R;

/**
 * A provider to select emoji in the {@link MediaKeyboard}.
 */
public class EmojiKeyboardProvider implements MediaKeyboardProvider,
                                              MediaKeyboardProvider.TabIconProvider,
                                              MediaKeyboardProvider.BackspaceObserver,
        EmojiPageViewGridAdapter.VariationSelectorListener
{
  private static final KeyEvent DELETE_KEY_EVENT = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);

  private final Context              context;
  private final List<EmojiPageModel> models;
  private final RecentEmojiPageModel recentModel;
  private final EmojiPagerAdapter    emojiPagerAdapter;
  private final EmojiEventListener   emojiEventListener;

  private Controller controller;

  public EmojiKeyboardProvider(@NonNull Context context, @Nullable EmojiEventListener emojiEventListener) {
    this.context            = context;
    this.emojiEventListener = emojiEventListener;
    this.models             = new LinkedList<>();
    this.recentModel        = new RecentEmojiPageModel(context);
    this.emojiPagerAdapter  = new EmojiPagerAdapter(context, models, new EmojiEventListener() {
      @Override
      public void onEmojiSelected(String emoji) {
        recentModel.onCodePointSelected(emoji);

        if (emojiEventListener != null) {
          emojiEventListener.onEmojiSelected(emoji);
        }
      }

      @Override
      public void onKeyEvent(KeyEvent keyEvent) {
        if (emojiEventListener != null) {
          emojiEventListener.onKeyEvent(keyEvent);
        }
      }
    }, this);

    models.add(recentModel);
    models.addAll(EmojiPages.DISPLAY_PAGES);
  }

  @Override
  public void requestPresentation(@NonNull Presenter presenter, boolean isSoloProvider) {
    presenter.present(this, emojiPagerAdapter, this, this, null, null, recentModel.getEmoji().size() > 0 ? 0 : 1);
  }

  @Override
  public void setController(@Nullable Controller controller) {
    this.controller = controller;
  }

  @Override
  public int getProviderIconView(boolean selected) {
    if (selected) {
      return ThemeUtil.isDarkTheme(context) ? R.layout.emoji_keyboard_icon_dark_selected : R.layout.emoji_keyboard_icon_light_selected;
    } else {
      return ThemeUtil.isDarkTheme(context) ? R.layout.emoji_keyboard_icon_dark : R.layout.emoji_keyboard_icon_light;
    }
  }

  @Override
  public void loadCategoryTabIcon(@NonNull GlideRequests glideRequests, @NonNull ImageView imageView, int index) {
    Drawable drawable = ResUtil.getDrawable(context, models.get(index).getIconAttr());
    imageView.setImageDrawable(drawable);
  }

  @Override
  public void onBackspaceClicked() {
    if (emojiEventListener != null) {
      emojiEventListener.onKeyEvent(DELETE_KEY_EVENT);
    }
  }

  @Override
  public void onVariationSelectorStateChanged(boolean open) {
    if (controller != null) {
      controller.setViewPagerEnabled(!open);
    }
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof EmojiKeyboardProvider;
  }

  private static class EmojiPagerAdapter extends PagerAdapter {
    private Context                   context;
    private List<EmojiPageModel>      pages;
    private EmojiEventListener emojiSelectionListener;
    private EmojiPageViewGridAdapter.VariationSelectorListener variationSelectorListener;

    public EmojiPagerAdapter(@NonNull Context context,
                             @NonNull List<EmojiPageModel> pages,
                             @NonNull EmojiEventListener emojiSelectionListener,
                             @NonNull EmojiPageViewGridAdapter.VariationSelectorListener variationSelectorListener)
    {
      super();
      this.context                   = context;
      this.pages                     = pages;
      this.emojiSelectionListener    = emojiSelectionListener;
      this.variationSelectorListener = variationSelectorListener;
    }

    @Override
    public int getCount() {
      return pages.size();
    }

    @Override
    public @NonNull Object instantiateItem(@NonNull ViewGroup container, int position) {
      EmojiPageView page = new EmojiPageView(context, emojiSelectionListener, variationSelectorListener, false);
      container.addView(page);
      return page;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
      container.removeView((View)object);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
      EmojiPageView current = (EmojiPageView) object;
      current.onSelected();
      super.setPrimaryItem(container, position, object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
      return view == object;
    }
  }
}
