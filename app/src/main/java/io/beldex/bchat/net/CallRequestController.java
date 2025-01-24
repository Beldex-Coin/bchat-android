package io.beldex.bchat.net;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.beldex.libbchat.utilities.Util;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libsignal.utilities.guava.Optional;

import java.io.InputStream;

import okhttp3.Call;

public class CallRequestController implements RequestController {

  private final Call call;

  private InputStream  stream;
  private boolean      canceled;

  public CallRequestController(@NonNull Call call) {
    this.call = call;
  }

  @Override
  public void cancel() {
    if (canceled) return;
    try {
      call.cancel();
      if (stream != null) {
        Util.close(stream);
      }
    } catch (Exception e) {
      Log.e("Stream", e.getClass().getName() + " : " + e.getLocalizedMessage());
    } finally {
      canceled = true;
    }
  }

  public synchronized void setStream(@NonNull InputStream stream) {
    if (canceled) {
      Util.close(stream);
    } else {
      this.stream = stream;
    }
    notifyAll();
  }

  /**
   * Blocks until the stream is available or until the request is canceled.
   */
  @WorkerThread
  public synchronized Optional<InputStream> getStream() {
    while(stream == null && !canceled) {
      Util.wait(this, 0);
    }

    return Optional.fromNullable(this.stream);
  }
}
