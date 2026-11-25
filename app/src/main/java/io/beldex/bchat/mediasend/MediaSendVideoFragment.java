package io.beldex.bchat.mediasend;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.LegacyPlayerControlView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.beldex.bchat.R;
import io.beldex.bchat.mms.VideoSlide;
import io.beldex.bchat.video.VideoPlayer;
import com.beldex.libsignal.utilities.Log;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@OptIn(markerClass = UnstableApi.class)
public class MediaSendVideoFragment extends Fragment implements MediaSendPageFragment {

  private static final String TAG = MediaSendVideoFragment.class.getSimpleName();

  private static final String KEY_URI = "uri";

  private Uri uri;

  @Nullable private VideoPlayer videoPlayer;
  @Nullable private View        metaView;

  public static MediaSendVideoFragment newInstance(@NonNull Uri uri) {
    Bundle args = new Bundle();
    args.putParcelable(KEY_URI, uri);

    MediaSendVideoFragment fragment = new MediaSendVideoFragment();
    fragment.setArguments(args);
    fragment.setUri(uri);
    return fragment;
  }

  @Override
  public @Nullable View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.mediasend_video_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    uri         = getArguments().getParcelable(KEY_URI);
    videoPlayer = view.findViewById(R.id.mediasend_video_player);
    metaView    = LayoutInflater.from(view.getContext()).inflate(R.layout.mediasend_video_meta, (ViewGroup) view, false);

    VideoSlide slide = new VideoSlide(requireContext(), uri, 0);
    try {
      videoPlayer.setWindow(requireActivity().getWindow());
      videoPlayer.setVideoSource(slide, false);
    } catch (IOException e) {
      Log.w(TAG, "Failed to play video.", e);
    }

    Player player = videoPlayer.getPlayer();
    if (player == null) return;

    LegacyPlayerControlView controls = view.findViewById(R.id.mediasend_video_controls);
    TextView                meta     = metaView.findViewById(R.id.mediasend_video_meta);

    controls.setPlayer(player);
    controls.show();

    player.addListener(new Player.Listener() {
      @Override
      public void onPlaybackStateChanged(int playbackState) {
        if (playbackState == Player.STATE_READY && player.getDuration() != C.TIME_UNSET) {
          meta.setText(formatMeta(player.getDuration()));
          meta.setVisibility(View.VISIBLE);
        }
      }
    });
  }

  private @NonNull String formatMeta(long durationMs) {
    long   minutes  = TimeUnit.MILLISECONDS.toMinutes(durationMs);
    long   seconds  = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60;
    String duration = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    long   size     = videoSize();
    return size > 0 ? duration + " · " + Formatter.formatShortFileSize(requireContext(), size) : duration;
  }

  private long videoSize() {
    if (uri == null) return -1;
    if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
      return new File(uri.getPath()).length();
    }
    try (Cursor cursor = requireContext().getContentResolver().query(uri, new String[] { OpenableColumns.SIZE }, null, null, null)) {
      if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
        return cursor.getLong(0);
      }
    } catch (Exception e) {
      Log.w(TAG, "Failed to read video size.", e);
    }
    return -1;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (videoPlayer != null) {
      videoPlayer.cleanup();
      videoPlayer = null;
    }
    metaView = null;
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
    return metaView;
  }

  @Override
  public @Nullable Object saveState() {
    return null;
  }

  @Override
  public void restoreState(@NonNull Object state) { }
}
