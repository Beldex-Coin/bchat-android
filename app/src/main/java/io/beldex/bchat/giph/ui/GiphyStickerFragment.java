package io.beldex.bchat.giph.ui;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.loader.content.Loader;

import io.beldex.bchat.giph.model.GiphyImage;
import io.beldex.bchat.giph.net.GiphyStickerLoader;
import io.beldex.bchat.giph.model.GiphyImage;
import io.beldex.bchat.giph.net.GiphyStickerLoader;
import io.beldex.bchat.giph.model.GiphyImage;
import io.beldex.bchat.giph.net.GiphyStickerLoader;

import java.util.List;

public class GiphyStickerFragment extends GiphyFragment {
  @Override
  public @NonNull Loader<List<GiphyImage>> onCreateLoader(int id, Bundle args) {
    return new GiphyStickerLoader(getActivity(), searchString);
  }
}
