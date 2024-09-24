package io.beldex.bchat.mediasend;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import io.beldex.bchat.mms.DecryptableStreamUriLoader;
import io.beldex.bchat.mms.GlideApp;
import io.beldex.bchat.mms.DecryptableStreamUriLoader;

import io.beldex.bchat.R;
import io.beldex.bchat.mms.DecryptableStreamUriLoader;
import io.beldex.bchat.mms.GlideApp;

public class MediaSendGifFragment extends Fragment implements MediaSendPageFragment {

  private static final String KEY_URI = "uri";

  private Uri uri;

  public static MediaSendGifFragment newInstance(@NonNull Uri uri) {
    Bundle args = new Bundle();
    args.putParcelable(KEY_URI, uri);

    MediaSendGifFragment fragment = new MediaSendGifFragment();
    fragment.setArguments(args);
    fragment.setUri(uri);
    return fragment;
  }

  @Override
  public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.mediasend_image_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    uri = getArguments().getParcelable(KEY_URI);
    GlideApp.with(this).load(new DecryptableStreamUriLoader.DecryptableUri(uri)).into((ImageView) view);
  }

  @Override
  public void setUri(@NonNull Uri uri) {
    this.uri = uri;
  }

  @Override
  public @NonNull Uri getUri() {
    return uri;
  }

  @Override
  public @Nullable View getPlaybackControls() {
    return null;
  }

  @Override
  public @Nullable Object saveState() {
    return null;
  }

  @Override
  public void restoreState(@NonNull Object state) { }
}
