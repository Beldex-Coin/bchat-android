package io.beldex.bchat.giph.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import io.beldex.bchat.PassphraseRequiredActionBarActivity;
import com.beldex.libbchat.utilities.MediaTypes;
import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.providers.BlobProvider;
import com.beldex.libbchat.utilities.ViewUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.beldex.bchat.R;

public class GiphyActivity extends PassphraseRequiredActionBarActivity
    implements GiphyActivityToolbar.OnLayoutChangedListener,
               GiphyActivityToolbar.OnFilterChangedListener,
               GiphyAdapter.OnItemClickListener
{

  private static final String TAG = GiphyActivity.class.getSimpleName();

  public static final String EXTRA_IS_MMS = "extra_is_mms";
  public static final String EXTRA_WIDTH  = "extra_width";
  public static final String EXTRA_HEIGHT = "extra_height";

  private GiphyGifFragment     gifFragment;
  private GiphyStickerFragment stickerFragment;
  private boolean              forMms;

  private GiphyAdapter.GiphyViewHolder finishingImage;

  @Override
  public void onCreate(Bundle bundle, boolean ready) {
    setContentView(R.layout.giphy_activity);

    initializeToolbar();
    initializeResources();
  }

  private void initializeToolbar() {
    GiphyActivityToolbar toolbar = ViewUtil.findById(this, R.id.giphy_toolbar);
    toolbar.setOnFilterChangedListener(this);
    toolbar.setOnLayoutChangedListener(this);
    toolbar.setPersistence(GiphyActivityToolbarTextSecurePreferencesPersistence.fromContext(this));

    setSupportActionBar(toolbar);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
  }

  private void initializeResources() {

    ViewPager viewPager = ViewUtil.findById(this, R.id.giphy_pager);
    TabLayout tabLayout = ViewUtil.findById(this, R.id.tab_layout);

    GiphyFragmentPagerAdapter adapter =
            new GiphyFragmentPagerAdapter(getSupportFragmentManager());

    viewPager.setAdapter(adapter);
    tabLayout.setupWithViewPager(viewPager);
  }

  private List<GiphyFragment> getFragments() {
    List<GiphyFragment> fragments = new ArrayList<>();
    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
      if (fragment instanceof GiphyFragment) {
        fragments.add((GiphyFragment) fragment);
      }
    }
    return fragments;
  }


  @Override
  public void onFilterChanged(String filter) {
    for (GiphyFragment fragment : getFragments()) {
      fragment.setSearchString(filter);
    }
  }

  @Override
  public void onLayoutChanged(boolean gridLayout) {
    for (GiphyFragment fragment : getFragments()) {
      fragment.setLayoutManager(gridLayout);
    }
  }


  @SuppressLint("StaticFieldLeak")
  @Override
  public void onClick(final GiphyAdapter.GiphyViewHolder viewHolder) {

    viewHolder.gifProgress.setVisibility(View.VISIBLE);

    new AsyncTask<Void, Void, Uri>() {

      @Override
      protected Uri doInBackground(Void... params) {
        try {
          byte[] data = viewHolder.getData(forMms);

          return BlobProvider.getInstance()
                  .forData(data)
                  .withMimeType(MediaTypes.IMAGE_GIF)
                  .createForSingleBchatOnDisk(GiphyActivity.this,
                          e -> Log.w(TAG, "Failed to write to disk.", e)
                  );

        } catch (InterruptedException | ExecutionException | IOException e) {
          Log.w(TAG, e);
          return null;
        }
      }

      @Override
      protected void onPostExecute(@Nullable Uri uri) {

        // Activity might be destroyed after theme change
        if (isFinishing() || isDestroyed()) return;

        viewHolder.gifProgress.setVisibility(View.GONE);

        if (uri == null) {
          Toast.makeText(
                  GiphyActivity.this,
                  R.string.GiphyActivity_error_while_retrieving_full_resolution_gif,
                  Toast.LENGTH_LONG
          ).show();
          return;
        }

        Intent intent = new Intent();
        intent.setData(uri);
        intent.putExtra(EXTRA_WIDTH, viewHolder.image.getGifWidth());
        intent.putExtra(EXTRA_HEIGHT, viewHolder.image.getGifHeight());

        setResult(RESULT_OK, intent);
        finish();
      }

    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }
  private class GiphyFragmentPagerAdapter extends FragmentPagerAdapter {

    public GiphyFragmentPagerAdapter(FragmentManager fm) {
      super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @Override
    public Fragment getItem(int position) {
      if (position == 0) return new GiphyGifFragment();
      else return new GiphyStickerFragment();
    }

    @Override
    public int getCount() {
      return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      if (position == 0)
        return getString(R.string.GiphyFragmentPagerAdapter_gifs);
      else
        return getString(R.string.GiphyFragmentPagerAdapter_stickers);
    }
  }

}
