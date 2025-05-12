package io.beldex.bchat.components;


import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import io.beldex.bchat.util.AnimationCompleteListener;
import io.beldex.bchat.util.AnimationCompleteListener;

import io.beldex.bchat.R;

public class SearchToolbar extends LinearLayout {

  private float x, y;
  private MenuItem searchItem;
  private SearchListener listener;

  public SearchToolbar(Context context) {
    super(context);
    initialize();
  }

  public SearchToolbar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public SearchToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  @SuppressLint("UseCompatLoadingForDrawables")
  private void initialize() {
    inflate(getContext(), R.layout.search_toolbar, this);
    setOrientation(VERTICAL);

    Toolbar toolbar = findViewById(R.id.toolbar);

    //supportActionBar?.setHomeAsUpIndicator(R.drawable.homeNavigationIcon);

    toolbar.setNavigationIcon(ContextCompat.getDrawable(getContext(),R.drawable.ic_baseline_clear_24));
    toolbar.inflateMenu(R.menu.conversation_list_search);

    this.searchItem = toolbar.getMenu().findItem(R.id.action_filter_search);
    SearchView searchView = (SearchView) searchItem.getActionView();
    EditText   searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);

    searchView.setSubmitButtonEnabled(false);
    Log.d("Beldex","this is toolbar class");

    if (searchText != null) searchText.setHint(R.string.SearchToolbar_search);
    else                    searchView.setQueryHint(getResources().getString(R.string.SearchToolbar_search));

    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        if (listener != null) listener.onSearchTextChange(query);
        Log.d("Beldex","this is toolbar class search action");
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return onQueryTextSubmit(newText);
      }
    });

    searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
      @Override
      public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
      }

      @Override
      public boolean onMenuItemActionCollapse(MenuItem item) {
        hide();
        return true;
      }
    });

    toolbar.setNavigationOnClickListener(v -> hide());
  }

  @MainThread
  public void display(float x, float y) {
    if (getVisibility() != View.VISIBLE) {
      this.x = x;
      this.y = y;

      searchItem.expandActionView();

      if (Build.VERSION.SDK_INT >= 21) {
        Animator animator = ViewAnimationUtils.createCircularReveal(this, (int)x, (int)y, 0, getWidth());
        animator.setDuration(400);

        setVisibility(View.VISIBLE);
        animator.start();
      } else {
        setVisibility(View.VISIBLE);
      }
    }
  }

  public void collapse() {
    searchItem.collapseActionView();
  }

  @MainThread
  private void hide() {
    if (getVisibility() == View.VISIBLE) {

      if (listener != null) listener.onSearchClosed();

      if (Build.VERSION.SDK_INT >= 21) {
        Animator animator = ViewAnimationUtils.createCircularReveal(this, (int)x, (int)y, getWidth(), 0);
        animator.setDuration(400);
        animator.addListener(new AnimationCompleteListener() {
          @Override
          public void onAnimationEnd(Animator animation) {
            setVisibility(View.INVISIBLE);
          }
        });
        animator.start();
      } else {
        setVisibility(View.INVISIBLE);
      }
    }
  }

  public boolean isVisible() {
    return getVisibility() == View.VISIBLE;
  }

  @MainThread
  public void setListener(SearchListener listener) {
    this.listener = listener;
  }

  public interface SearchListener {
    void onSearchTextChange(String text);
    void onSearchClosed();
  }

}
