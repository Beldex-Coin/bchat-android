package io.beldex.bchat.giph.ui;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.loader.content.Loader;

import io.beldex.bchat.giph.model.GiphyImage;
import io.beldex.bchat.giph.net.GiphyGifLoader;
import io.beldex.bchat.giph.model.GiphyImage;
import io.beldex.bchat.giph.net.GiphyGifLoader;
import io.beldex.bchat.giph.model.GiphyImage;
import io.beldex.bchat.giph.net.GiphyGifLoader;

import java.util.List;

public class GiphyGifFragment extends GiphyFragment {

  @Override
  public @NonNull Loader<List<GiphyImage>> onCreateLoader(int id, Bundle args) {
    return new GiphyGifLoader(getActivity(), searchString);
  }

}
