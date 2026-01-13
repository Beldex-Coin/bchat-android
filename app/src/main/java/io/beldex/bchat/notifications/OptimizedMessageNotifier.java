package io.beldex.bchat.notifications;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import io.beldex.bchat.ApplicationContext;

import com.beldex.libbchat.messaging.sending_receiving.notifications.MessageNotifier;
import com.beldex.libbchat.messaging.sending_receiving.pollers.Poller;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libbchat.utilities.Debouncer;
import com.beldex.libsignal.utilities.ThreadUtils;
import io.beldex.bchat.groups.OpenGroupManager;

import java.util.concurrent.TimeUnit;

public class OptimizedMessageNotifier implements MessageNotifier {
  private final MessageNotifier         wrapped;
  private final Debouncer               debouncer;

  @MainThread
  public OptimizedMessageNotifier(@NonNull MessageNotifier wrapped) {
    this.wrapped   = wrapped;
    this.debouncer = new Debouncer(TimeUnit.SECONDS.toMillis(2));
  }

  @Override
  public void setVisibleThread(long threadId) { wrapped.setVisibleThread(threadId); }

  @Override
  public void setHomeScreenVisible(boolean isVisible) {
    wrapped.setHomeScreenVisible(isVisible);
  }

  @Override
  public void setLastDesktopActivityTimestamp(long timestamp) { wrapped.setLastDesktopActivityTimestamp(timestamp);}

  @Override
  public void notifyMessageDeliveryFailed(Context context, Recipient recipient, long threadId) {
    wrapped.notifyMessageDeliveryFailed(context, recipient, threadId);
  }

  @Override
  public void cancelDelayedNotifications() { wrapped.cancelDelayedNotifications(); }

  @Override
  public void updateNotification(@NonNull Context context) {
    Poller poller = ApplicationContext.getInstance(context).poller;
    boolean isCaughtUp = true;
    if (poller != null) {
      isCaughtUp = isCaughtUp && poller.isCaughtUp();
    }

    isCaughtUp = isCaughtUp && OpenGroupManager.INSTANCE.isAllCaughtUp();

    if (isCaughtUp) {
      performOnBackgroundThreadIfNeeded(() -> wrapped.updateNotification(context));
    } else {
      debouncer.publish(() -> performOnBackgroundThreadIfNeeded(() -> wrapped.updateNotification(context)));
    }
  }

  @Override
  public void updateNotification(@NonNull Context context, long threadId) {
    Poller beldexPoller = ApplicationContext.getInstance(context).poller;
    boolean isCaughtUp = true;
    if (beldexPoller != null) {
      isCaughtUp = isCaughtUp && beldexPoller.isCaughtUp();
    }

    isCaughtUp = isCaughtUp && OpenGroupManager.INSTANCE.isAllCaughtUp();
    
    if (isCaughtUp) {
      performOnBackgroundThreadIfNeeded(() -> wrapped.updateNotification(context, threadId));
    } else {
      debouncer.publish(() -> performOnBackgroundThreadIfNeeded(() -> wrapped.updateNotification(context, threadId)));
    }
  }

  @Override
  public void updateNotification(@NonNull Context context, long threadId, boolean signal) {
    Poller beldexPoller = ApplicationContext.getInstance(context).poller;
    boolean isCaughtUp = true;
    if (beldexPoller != null) {
      isCaughtUp = isCaughtUp && beldexPoller.isCaughtUp();
    }

    isCaughtUp = isCaughtUp && OpenGroupManager.INSTANCE.isAllCaughtUp();

    if (isCaughtUp) {
      performOnBackgroundThreadIfNeeded(() -> wrapped.updateNotification(context, threadId, signal));
    } else {
      debouncer.publish(() -> performOnBackgroundThreadIfNeeded(() -> wrapped.updateNotification(context, threadId, signal)));
    }
  }

  @Override
  public void updateNotification(@androidx.annotation.NonNull Context context, boolean signal, int reminderCount) {
    Poller beldexPoller = ApplicationContext.getInstance(context).poller;
    boolean isCaughtUp = true;
    if (beldexPoller != null) {
      isCaughtUp = isCaughtUp && beldexPoller.isCaughtUp();
    }

    isCaughtUp = isCaughtUp && OpenGroupManager.INSTANCE.isAllCaughtUp();

    if (isCaughtUp) {
      performOnBackgroundThreadIfNeeded(() -> wrapped.updateNotification(context, signal, reminderCount));
    } else {
      debouncer.publish(() -> performOnBackgroundThreadIfNeeded(() -> wrapped.updateNotification(context, signal, reminderCount)));
    }
  }

  @Override
  public void clearReminder(@NonNull Context context) { wrapped.clearReminder(context); }

  private void performOnBackgroundThreadIfNeeded(Runnable r) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      ThreadUtils.queue(r);
    } else {
      r.run();
    }
  }
}
