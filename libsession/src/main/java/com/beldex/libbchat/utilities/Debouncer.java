package com.beldex.libbchat.utilities;

import android.os.Handler;

/**
 * A class that will throttle the number of runnables executed to be at most once every specified
 * interval. However, it could be longer if events are published consistently.
 *
 * Useful for performing actions in response to rapid user input, such as inputting text, where you
 * don't necessarily want to perform an action after <em>every</em> input.
 *
 * See http://rxmarbles.com/#debounce
 */
public class Debouncer {
  private final Handler handler;
  private final long    threshold;

  /**
   * @param threshold Only one runnable will be executed via {@link #publish(Runnable)} every
   *                  {@code threshold} milliseconds.
   */
  public Debouncer(long threshold) {
    this.handler   = new Handler();
    this.threshold = threshold;
  }

  public Debouncer(Handler handler, long threshold) {
    this.handler   = handler;
    this.threshold = threshold;
  }

  public void publish(Runnable runnable) {
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(runnable, threshold);
  }

  public void clear() {
    handler.removeCallbacksAndMessages(null);
  }
}
