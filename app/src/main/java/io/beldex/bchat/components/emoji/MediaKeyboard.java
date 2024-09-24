package io.beldex.bchat.components.emoji;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;


import io.beldex.bchat.components.InputAwareLayout;
import io.beldex.bchat.components.RepeatableImageKey;
import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.mms.GlideApp;

import java.util.Arrays;

import io.beldex.bchat.R;

public class MediaKeyboard extends FrameLayout implements InputAwareLayout.InputView,
                                                          MediaKeyboardProvider.Presenter,
                                                          MediaKeyboardProvider.Controller,
                                                          MediaKeyboardBottomTabAdapter.EventListener
{

  private static final String TAG = Log.tag(MediaKeyboard.class);

  private RecyclerView            categoryTabs;
  private ViewPager               categoryPager;
  private ViewGroup               providerTabs;
  private RepeatableImageKey backspaceButton;
  private RepeatableImageKey      backspaceButtonBackup;
  private View                    searchButton;
  private View                    addButton;
  private MediaKeyboardListener keyboardListener;
  private MediaKeyboardProvider[] providers;
  private int                     providerIndex;

  private MediaKeyboardBottomTabAdapter categoryTabAdapter;

  public MediaKeyboard(Context context) {
    this(context, null);
  }

  public MediaKeyboard(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setProviders(int startIndex, MediaKeyboardProvider... providers) {
    if (!Arrays.equals(this.providers, providers)) {
      this.providers     = providers;
      this.providerIndex = startIndex;

      requestPresent(providers, providerIndex);
    }
  }

  public void setKeyboardListener(MediaKeyboardListener listener) {
    this.keyboardListener = listener;
  }

  @Override
  public boolean isShowing() {
    return getVisibility() == VISIBLE;
  }

  @Override
  public void show(int height, boolean immediate) {
    if (this.categoryPager == null) initView();

    ViewGroup.LayoutParams params = getLayoutParams();
    params.height = height;
    Log.i(TAG, "showing emoji drawer with height " + params.height);
    setLayoutParams(params);
    setVisibility(VISIBLE);

    if (keyboardListener != null) keyboardListener.onShown();

    requestPresent(providers, providerIndex);
  }

  @Override
  public void hide(boolean immediate) {
    setVisibility(GONE);
    if (keyboardListener != null) keyboardListener.onHidden();
    Log.i(TAG, "hide()");
  }

  @Override
  public void present(@NonNull MediaKeyboardProvider provider,
                      @NonNull PagerAdapter pagerAdapter,
                      @NonNull MediaKeyboardProvider.TabIconProvider tabIconProvider,
                      @Nullable MediaKeyboardProvider.BackspaceObserver backspaceObserver,
                      @Nullable MediaKeyboardProvider.AddObserver addObserver,
                      @Nullable MediaKeyboardProvider.SearchObserver searchObserver,
                      int startingIndex)
  {
    if (categoryPager == null) return;
    if (!provider.equals(providers[providerIndex])) return;
    if (keyboardListener != null) keyboardListener.onKeyboardProviderChanged(provider);

    boolean isSolo = providers.length == 1;

    presentProviderStrip(isSolo);
    presentCategoryPager(pagerAdapter, tabIconProvider, startingIndex);
    presentProviderTabs(providers, providerIndex);
    presentSearchButton(searchObserver);
    presentBackspaceButton(backspaceObserver, isSolo);
    presentAddButton(addObserver);
  }

  @Override
  public int getCurrentPosition() {
    return categoryPager != null ? categoryPager.getCurrentItem() : 0;
  }

  @Override
  public void requestDismissal() {
    hide(true);
    providerIndex = 0;
    keyboardListener.onKeyboardProviderChanged(providers[providerIndex]);
  }

  @Override
  public boolean isVisible() {
    return getVisibility() == View.VISIBLE;
  }

  @Override
  public void onTabSelected(int index) {
    if (categoryPager != null) {
      categoryPager.setCurrentItem(index);
      categoryTabs.smoothScrollToPosition(index);
    }
  }

  @Override
  public void setViewPagerEnabled(boolean enabled) {
    if (categoryPager != null) {
      categoryPager.setEnabled(enabled);
    }
  }

  private void initView() {
    final View view = LayoutInflater.from(getContext()).inflate(R.layout.media_keyboard, this, true);

    this.categoryTabs          = view.findViewById(R.id.media_keyboard_tabs);
    this.categoryPager         = view.findViewById(R.id.media_keyboard_pager);
    this.providerTabs          = view.findViewById(R.id.media_keyboard_provider_tabs);
    this.backspaceButton       = view.findViewById(R.id.media_keyboard_backspace);
    this.backspaceButtonBackup = view.findViewById(R.id.media_keyboard_backspace_backup);
    this.searchButton          = view.findViewById(R.id.media_keyboard_search);
    this.addButton             = view.findViewById(R.id.media_keyboard_add);

    this.categoryTabAdapter = new MediaKeyboardBottomTabAdapter(GlideApp.with(this), this);

    categoryTabs.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
    categoryTabs.setAdapter(categoryTabAdapter);
  }

  private void requestPresent(@NonNull MediaKeyboardProvider[] providers, int newIndex) {
    providers[providerIndex].setController(null);
    providerIndex = newIndex;

    providers[providerIndex].setController(this);
    providers[providerIndex].requestPresentation(this, providers.length == 1);
  }


  private void presentCategoryPager(@NonNull PagerAdapter pagerAdapter,
                                    @NonNull MediaKeyboardProvider.TabIconProvider iconProvider,
                                    int startingIndex) {
    if (categoryPager.getAdapter() != pagerAdapter) {
      categoryPager.setAdapter(pagerAdapter);
    }

    categoryPager.setCurrentItem(startingIndex);

    categoryPager.clearOnPageChangeListeners();
    categoryPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int i, float v, int i1) {
      }

      @Override
      public void onPageSelected(int i) {
        categoryTabAdapter.setActivePosition(i);
        categoryTabs.smoothScrollToPosition(i);
      }

      @Override
      public void onPageScrollStateChanged(int i) {
      }
    });

    categoryTabAdapter.setTabIconProvider(iconProvider, pagerAdapter.getCount());
    categoryTabAdapter.setActivePosition(startingIndex);
  }

  private void presentProviderTabs(@NonNull MediaKeyboardProvider[] providers, int selected) {
    providerTabs.removeAllViews();

    LayoutInflater inflater = LayoutInflater.from(getContext());

    for (int i = 0; i < providers.length; i++) {
      MediaKeyboardProvider provider = providers[i];
      View                  view     = inflater.inflate(provider.getProviderIconView(i == selected), providerTabs, false);

      view.setTag(provider);

      final int index = i;
      view.setOnClickListener(v -> {
        requestPresent(providers, index);
      });

      providerTabs.addView(view);
    }
  }

  private void presentBackspaceButton(@Nullable MediaKeyboardProvider.BackspaceObserver backspaceObserver,
                                      boolean useBackupPosition)
  {
    if (backspaceObserver != null) {
      if (useBackupPosition) {
        backspaceButton.setVisibility(INVISIBLE);
        backspaceButton.setOnKeyEventListener(null);
        backspaceButtonBackup.setVisibility(VISIBLE);
        backspaceButtonBackup.setOnKeyEventListener(backspaceObserver::onBackspaceClicked);
      } else {
        backspaceButton.setVisibility(VISIBLE);
        backspaceButton.setOnKeyEventListener(backspaceObserver::onBackspaceClicked);
        backspaceButtonBackup.setVisibility(GONE);
        backspaceButtonBackup.setOnKeyEventListener(null);
      }
    } else {
      backspaceButton.setVisibility(INVISIBLE);
      backspaceButton.setOnKeyEventListener(null);
      backspaceButtonBackup.setVisibility(GONE);
      backspaceButton.setOnKeyEventListener(null);
    }
  }

  private void presentAddButton(@Nullable MediaKeyboardProvider.AddObserver addObserver) {
    if (addObserver != null) {
      addButton.setVisibility(VISIBLE);
      addButton.setOnClickListener(v -> addObserver.onAddClicked());
    } else {
      addButton.setVisibility(GONE);
      addButton.setOnClickListener(null);
    }
  }

  private void presentSearchButton(@Nullable MediaKeyboardProvider.SearchObserver searchObserver) {
    searchButton.setVisibility(searchObserver != null ? VISIBLE : INVISIBLE);
  }

  private void presentProviderStrip(boolean isSolo) {
    int visibility = isSolo ? View.GONE : View.VISIBLE;

    searchButton.setVisibility(visibility);
    backspaceButton.setVisibility(visibility);
    providerTabs.setVisibility(visibility);
  }

  public interface MediaKeyboardListener {
    void onShown();
    void onHidden();
    void onKeyboardProviderChanged(@NonNull MediaKeyboardProvider provider);
  }
}
