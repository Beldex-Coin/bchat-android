package com.thoughtcrimes.securesms.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.TaskStackBuilder;

import com.beldex.libbchat.utilities.recipients.Recipient;
import com.thoughtcrimes.securesms.conversation.v2.ConversationActivityV2;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;

import io.beldex.bchat.R;

public class CommunicationActions {

  public static void startConversation(@NonNull  Context          context,
                                       @NonNull  Recipient        recipient,
                                       @Nullable String           text,
                                       @Nullable TaskStackBuilder backStack)
  {
    new AsyncTask<Void, Void, Long>() {
      @Override
      protected Long doInBackground(Void... voids) {
        return DatabaseComponent.get(context).threadDatabase().getOrCreateThreadIdFor(recipient);
      }

      @Override
      protected void onPostExecute(Long threadId) {
        Intent intent = new Intent(context, ConversationActivityV2.class);
        intent.putExtra(ConversationActivityV2.ADDRESS, recipient.getAddress());
        intent.putExtra(ConversationActivityV2.THREAD_ID, threadId);

        if (backStack != null) {
          backStack.addNextIntent(intent);
          backStack.startActivities();
        } else {
          context.startActivity(intent);
        }
      }
    }.execute();
  }

  public static void openBrowserLink(@NonNull Context context, @NonNull String link) {
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
      context.startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(context, R.string.CommunicationActions_no_browser_found, Toast.LENGTH_SHORT).show();
    }
  }
}
