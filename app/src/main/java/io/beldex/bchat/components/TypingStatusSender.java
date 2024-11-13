package io.beldex.bchat.components;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import com.beldex.libbchat.messaging.messages.control.TypingIndicator;
import com.beldex.libbchat.messaging.sending_receiving.MessageSender;
import com.beldex.libbchat.utilities.Util;
import io.beldex.bchat.database.ThreadDatabase;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.util.BchatMetaProtocol;

import com.beldex.libbchat.utilities.recipients.Recipient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressLint("UseSparseArrays")
public class TypingStatusSender {

  private static final long REFRESH_TYPING_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
  private static final long PAUSE_TYPING_TIMEOUT   = TimeUnit.SECONDS.toMillis(3);

  private final Context              context;
  private final Map<Long, TimerPair> selfTypingTimers;

  public TypingStatusSender(@NonNull Context context) {
    this.context          = context;
    this.selfTypingTimers = new HashMap<>();
  }

  public synchronized void onTypingStarted(long threadId) {
    TimerPair pair = Util.getOrDefault(selfTypingTimers, threadId, new TimerPair());
    selfTypingTimers.put(threadId, pair);

    if (pair.getStart() == null) {
      sendTyping(threadId, true);

      Runnable start = new StartRunnable(threadId);
      Util.runOnMainDelayed(start, REFRESH_TYPING_TIMEOUT);
      pair.setStart(start);
    }

    if (pair.getStop() != null) {
      Util.cancelRunnableOnMain(pair.getStop());
    }

    Runnable stop = () -> onTypingStopped(threadId, true);
    Util.runOnMainDelayed(stop, PAUSE_TYPING_TIMEOUT);
    pair.setStop(stop);
  }

  public synchronized void onTypingStopped(long threadId) {
    onTypingStopped(threadId, false);
  }

  private synchronized void onTypingStopped(long threadId, boolean notify) {
    TimerPair pair = Util.getOrDefault(selfTypingTimers, threadId, new TimerPair());
    selfTypingTimers.put(threadId, pair);

    if (pair.getStart() != null) {
      Util.cancelRunnableOnMain(pair.getStart());

      if (notify) {
        sendTyping(threadId, false);
      }
    }

    if (pair.getStop() != null) {
      Util.cancelRunnableOnMain(pair.getStop());
    }

    pair.setStart(null);
    pair.setStop(null);
  }

  private void sendTyping(long threadId, boolean typingStarted) {
    ThreadDatabase threadDatabase = DatabaseComponent.get(context).threadDatabase();
    Recipient recipient = threadDatabase.getRecipientForThreadId(threadId);
    if (recipient == null) { return; }
    if (!BchatMetaProtocol.shouldSendTypingIndicator(recipient)) { return; }
    TypingIndicator typingIndicator;
    if (typingStarted) {
      typingIndicator = new TypingIndicator(TypingIndicator.Kind.STARTED);
    } else {
      typingIndicator = new TypingIndicator(TypingIndicator.Kind.STOPPED);
    }
    MessageSender.send(typingIndicator, recipient.getAddress());
  }

  private class StartRunnable implements Runnable {

    private final long threadId;

    private StartRunnable(long threadId) {
      this.threadId = threadId;
    }

    @Override
    public void run() {
      sendTyping(threadId, true);
      Util.runOnMainDelayed(this, REFRESH_TYPING_TIMEOUT);
    }
  }

  private static class TimerPair {
    private Runnable start;
    private Runnable stop;

    public Runnable getStart() {
      return start;
    }

    public void setStart(Runnable start) {
      this.start = start;
    }

    public Runnable getStop() {
      return stop;
    }

    public void setStop(Runnable stop) {
      this.stop = stop;
    }
  }
}
