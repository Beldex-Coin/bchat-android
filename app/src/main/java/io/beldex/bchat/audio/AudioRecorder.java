package io.beldex.bchat.audio;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;

import com.beldex.libbchat.utilities.MediaTypes;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libsignal.utilities.Log;
import android.util.Pair;

import io.beldex.bchat.util.MediaUtil;
import io.beldex.bchat.providers.BlobProvider;

import com.beldex.libsignal.utilities.ThreadUtils;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libsignal.utilities.ListenableFuture;
import com.beldex.libsignal.utilities.SettableFuture;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AudioRecorder {

  private static final String TAG = AudioRecorder.class.getSimpleName();

  private static final ExecutorService executor = ThreadUtils.newDynamicSingleThreadedExecutor();

  private final Context context;

  private AudioCodec audioCodec;
  private Uri        captureUri;

  public AudioRecorder(@NonNull Context context) {
    this.context = context;
  }

  public void startRecording() {
    Log.i(TAG, "startRecording()");

    executor.execute(() -> {
      Log.i(TAG, "Running startRecording() + " + Thread.currentThread().getId());
      try {
        if (audioCodec != null) {
          throw new AssertionError("We can only record once at a time.");
        }
        TextSecurePreferences.setRecordingStatus(context,true);

        ParcelFileDescriptor fds[] = ParcelFileDescriptor.createPipe();

        captureUri = BlobProvider.getInstance()
                                 .forData(new ParcelFileDescriptor.AutoCloseInputStream(fds[0]), 0)
                                 .withMimeType(MediaTypes.AUDIO_AAC)
                                 .createForSingleBchatOnDisk(context, e -> Log.w(TAG, "Error during recording", e));
        audioCodec = new AudioCodec();

        audioCodec.start(new ParcelFileDescriptor.AutoCloseOutputStream(fds[1]));
      } catch (IOException e) {
        Log.w(TAG, e);
      }
    });
  }

  public @NonNull ListenableFuture<Pair<Uri, Long>> stopRecording() {
    Log.i(TAG, "stopRecording()");

    final SettableFuture<Pair<Uri, Long>> future = new SettableFuture<>();
    TextSecurePreferences.setRecordingStatus(context,false);
    executor.execute(() -> {
      if (audioCodec == null) {
        sendToFuture(future, new IOException("MediaRecorder was never initialized successfully!"));
        return;
      }

      audioCodec.stop();

      try {
        long size = MediaUtil.getMediaSize(context, captureUri);
        sendToFuture(future, new Pair<>(captureUri, size));
      } catch (IOException ioe) {
        Log.w(TAG, ioe);
        sendToFuture(future, ioe);
      }

      audioCodec = null;
      captureUri = null;
    });

    return future;
  }

  private <T> void sendToFuture(final SettableFuture<T> future, final Exception exception) {
    Util.runOnMain(() -> future.setException(exception));
  }

  private <T> void sendToFuture(final SettableFuture<T> future, final T result) {
    Util.runOnMain(() -> future.set(result));
  }

  /*public float getAmplitude() {
    if (audioCodec != null)
      return audioCodec.getAmplitude();
    else return -1f;
  }*/
}
