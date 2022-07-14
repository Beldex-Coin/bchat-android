package com.thoughtcrimes.securesms.jobmanager;

import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import com.beldex.libsignal.utilities.Log;

import java.util.List;

/**
 * Schedules future runs on an in-app handler. Intended to be used in combination with a persistent
 * {@link Scheduler} to improve responsiveness when the app is open.
 *
 * This should only schedule runs when all constraints are met. Because this only works when the
 * app is foregrounded, jobs that don't have their constraints met will be run when the relevant
 * {@link ConstraintObserver} is triggered.
 *
 * Similarly, this does not need to schedule retries with no delay, as this doesn't provide any
 * persistence, and other mechanisms will take care of that.
 */
class InAppScheduler implements Scheduler {

  private static final String TAG = InAppScheduler.class.getSimpleName();

  private final JobManager jobManager;
  private final Handler     handler;

  InAppScheduler(@NonNull JobManager jobManager) {
    HandlerThread handlerThread = new HandlerThread("InAppScheduler");
    handlerThread.start();

    this.jobManager = jobManager;
    this.handler    = new Handler(handlerThread.getLooper());
  }

  @Override
  public void schedule(long delay, @NonNull List<Constraint> constraints) {
    if (delay > 0 && Stream.of(constraints).allMatch(Constraint::isMet)) {
      Log.i(TAG, "Scheduling a retry in " + delay + " ms.");
      handler.postDelayed(() -> {
        Log.i(TAG, "Triggering a job retry.");
        jobManager.wakeUp();
      }, delay);
    }
  }
}
