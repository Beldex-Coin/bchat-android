package com.beldex.libbchat.utilities.task;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

public abstract class ProgressDialogAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

  private final WeakReference<Context> contextReference;
  private       Dialog                 progress;

  /** Creates the loading dialog; the app overrides this with its Revamp_2026 loader (libsession has no Compose UI). */
  public static DialogFactory dialogFactory = null;

  public interface DialogFactory {
    Dialog create(Context context, String title, String message);
  }
  private final String                 title;
  private final String                 message;

  public ProgressDialogAsyncTask(@NonNull Context context, @NonNull String title, @NonNull String message) {
    super();
    this.contextReference = new WeakReference<>(context);
    this.title            = title;
    this.message          = message;
  }

  public ProgressDialogAsyncTask(@NonNull Context context, int title, int message) {
    this(context, context.getString(title), context.getString(message));
  }

  @Override
  protected void onPreExecute() {
    final Context context = contextReference.get();
    if (context != null) {
      if (dialogFactory != null) {
        progress = dialogFactory.create(context, title, message);
        progress.show();
      } else {
        progress = android.app.ProgressDialog.show(context, title, message, true);
      }
    }
  }

  @Override
  protected void onPostExecute(Result result) {
    if (progress != null) progress.dismiss();
  }

  protected @NonNull Context getContext() {
    return contextReference.get();
  }
}

