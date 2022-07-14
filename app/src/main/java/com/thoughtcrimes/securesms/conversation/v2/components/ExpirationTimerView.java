package com.thoughtcrimes.securesms.conversation.v2.components;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beldex.libbchat.utilities.Util;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.beldex.bchat.R;

public class ExpirationTimerView extends androidx.appcompat.widget.AppCompatImageView {

  private long startedAt;
  private long expiresIn;

  private boolean visible = false;
  private boolean stopped = true;

  private final int[] frames = new int[]{ R.drawable.timer00,
                                          R.drawable.timer05,
                                          R.drawable.timer10,
                                          R.drawable.timer15,
                                          R.drawable.timer20,
                                          R.drawable.timer25,
                                          R.drawable.timer30,
                                          R.drawable.timer35,
                                          R.drawable.timer40,
                                          R.drawable.timer45,
                                          R.drawable.timer50,
                                          R.drawable.timer55,
                                          R.drawable.timer60 };

  public ExpirationTimerView(Context context) {
    super(context);
  }

  public ExpirationTimerView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public ExpirationTimerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setExpirationTime(long startedAt, long expiresIn) {
    this.startedAt = startedAt;
    this.expiresIn = expiresIn;
    setPercentComplete(calculateProgress(this.startedAt, this.expiresIn));
  }

  public void setPercentComplete(float percentage) {
    float percentFull = 1 - percentage;
    int frame = (int) Math.ceil(percentFull * (frames.length - 1));

    frame = Math.max(0, Math.min(frame, frames.length - 1));
    setImageResource(frames[frame]);
  }

  public void startAnimation() {
    synchronized (this) {
      visible = true;
      if (!stopped) return;
      else          stopped = false;
    }

    Util.runOnMainDelayed(new AnimationUpdateRunnable(this), calculateAnimationDelay(this.startedAt, this.expiresIn));
  }

  public void stopAnimation() {
    synchronized (this) {
      visible = false;
    }
  }

  private float calculateProgress(long startedAt, long expiresIn) {
    long  progressed      = System.currentTimeMillis() - startedAt;
    float percentComplete = (float)progressed / (float)expiresIn;

    return Math.max(0, Math.min(percentComplete, 1));
  }

  private long calculateAnimationDelay(long startedAt, long expiresIn) {
    long progressed = System.currentTimeMillis() - startedAt;
    long remaining  = expiresIn - progressed;

    if (remaining <= 0) {
      return 0;
    } else if (remaining < TimeUnit.SECONDS.toMillis(30)) {
      return 1000;
    } else {
      return 5000;
    }
  }

  private static class AnimationUpdateRunnable implements Runnable {

    private final WeakReference<ExpirationTimerView> expirationTimerViewReference;

    private AnimationUpdateRunnable(@NonNull ExpirationTimerView expirationTimerView) {
      this.expirationTimerViewReference = new WeakReference<>(expirationTimerView);
    }

    @Override
    public void run() {
      ExpirationTimerView timerView = expirationTimerViewReference.get();
      if (timerView == null) return;

      long nextUpdate = timerView.calculateAnimationDelay(timerView.startedAt, timerView.expiresIn);
      synchronized (timerView) {
        if (timerView.visible) {
          timerView.setExpirationTime(timerView.startedAt, timerView.expiresIn);
        } else {
          timerView.stopped = true;
          return;
        }
        if (nextUpdate <= 0) {
          timerView.stopped = true;
          return;
        }
      }
      Util.runOnMainDelayed(this, nextUpdate);
    }
  }
}
