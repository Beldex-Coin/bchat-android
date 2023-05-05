/*
 * Copyright (C) 2014-2017 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.thoughtcrimes.securesms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.provider.OpenableColumns;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.DistributionTypes;
import com.beldex.libbchat.utilities.ViewUtil;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libsignal.utilities.Log;
import com.thoughtcrimes.securesms.components.SearchToolbar;
import com.thoughtcrimes.securesms.contacts.ContactSelectionListFragment;
import com.thoughtcrimes.securesms.contacts.ContactSelectionListLoader.DisplayMode;
import com.thoughtcrimes.securesms.conversation.v2.ConversationActivityV2;
import com.thoughtcrimes.securesms.conversation.v2.ConversationFragmentV2;
import com.thoughtcrimes.securesms.dependencies.DatabaseComponent;
import com.thoughtcrimes.securesms.home.HomeActivity;
import com.thoughtcrimes.securesms.mms.PartAuthority;
import com.thoughtcrimes.securesms.providers.BlobProvider;
import com.thoughtcrimes.securesms.util.MediaUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.beldex.bchat.R;

/**
 * An activity to quickly share content with contacts
 *
 * @author Jake McGinty
 */
public class ShareActivity extends PassphraseRequiredActionBarActivity
        implements ContactSelectionListFragment.OnContactSelectedListener
{
  private static final String TAG = ShareActivity.class.getSimpleName();

  public static final String EXTRA_THREAD_ID          = "thread_id";
  public static final String EXTRA_ADDRESS_MARSHALLED = "address_marshalled";
  public static final String EXTRA_DISTRIBUTION_TYPE  = "distribution_type";
  //SetDataAndType
  public static final String MEDIA_PREVIEW_PAGE ="media_preview_page";


  private ContactSelectionListFragment contactsFragment;
  private SearchToolbar                searchToolbar;
  private ImageView                    searchAction;
  private View                         progressWheel;
  private Uri                          resolvedExtra;
  private CharSequence                 resolvedPlaintext;
  private String                       mimeType;
  private boolean                      isPassingAlongMedia;

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    if (!getIntent().hasExtra(ContactSelectionListFragment.DISPLAY_MODE)) {
      getIntent().putExtra(ContactSelectionListFragment.DISPLAY_MODE, DisplayMode.FLAG_ALL);
    }

    getIntent().putExtra(ContactSelectionListFragment.REFRESHABLE, false);

    setContentView(R.layout.share_activity);

    initializeToolbar();
    initializeResources();
    initializeSearch();
    initializeMedia();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    Log.i(TAG, "onNewIntent()");
    super.onNewIntent(intent);
    setIntent(intent);
    initializeMedia();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (!isPassingAlongMedia && resolvedExtra != null) {
      BlobProvider.getInstance().delete(this, resolvedExtra);

      if (!isFinishing()) {
        finish();
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {
    if (searchToolbar.isVisible()) searchToolbar.collapse();
    else                           super.onBackPressed();
  }

  private void initializeToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
//    actionBar.setHomeButtonEnabled(false);
    actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
  }

  private void initializeResources() {
    progressWheel    = findViewById(R.id.progress_wheel);
    searchToolbar    = findViewById(R.id.search_toolbar);
    searchAction     = findViewById(R.id.search_action);
    contactsFragment = (ContactSelectionListFragment) getSupportFragmentManager().findFragmentById(R.id.contact_selection_list_fragment);
    contactsFragment.setOnContactSelectedListener(this);
  }

  private void initializeSearch() {
    searchAction.setOnClickListener(v -> searchToolbar.display(searchAction.getX() + (searchAction.getWidth() / 2),
                                                               searchAction.getY() + (searchAction.getHeight() / 2)));

    searchToolbar.setListener(new SearchToolbar.SearchListener() {
      @Override
      public void onSearchTextChange(String text) {
        if (contactsFragment != null) {
          contactsFragment.setQueryFilter(text);
        }
      }

      @Override
      public void onSearchClosed() {
        if (contactsFragment != null) {
          contactsFragment.resetQueryFilter();
        }
      }
    });
  }

  private void initializeMedia() {
    final Context context = this;
    isPassingAlongMedia = false;

    Uri streamExtra = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
    CharSequence charSequenceExtra = getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT);
    mimeType = getMimeType(streamExtra);
    /*Hales63*/
    resolvedExtra       = streamExtra;

    if (streamExtra != null && PartAuthority.isLocalUri(streamExtra)) {
      isPassingAlongMedia = true;
      resolvedExtra       = streamExtra;
      handleResolvedMedia(getIntent(), false);
    } else if (charSequenceExtra != null && mimeType != null && mimeType.startsWith("text/")) {
      resolvedPlaintext = charSequenceExtra;
      handleResolvedMedia(getIntent(), false);
    } else {
      contactsFragment.getView().setVisibility(View.GONE);
      progressWheel.setVisibility(View.VISIBLE);
      new ResolveMediaTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, streamExtra);
    }
  }

  private void handleResolvedMedia(Intent intent, boolean animate) {
    long      threadId         = intent.getLongExtra(EXTRA_THREAD_ID, -1);
    int       distributionType = intent.getIntExtra(EXTRA_DISTRIBUTION_TYPE, -1);
    Address   address          = null;

    if (intent.hasExtra(EXTRA_ADDRESS_MARSHALLED)) {
      Parcel parcel = Parcel.obtain();
      byte[] marshalled = intent.getByteArrayExtra(EXTRA_ADDRESS_MARSHALLED);
      parcel.unmarshall(marshalled, 0, marshalled.length);
      parcel.setDataPosition(0);
      address = parcel.readParcelable(getClassLoader());
      parcel.recycle();
    }

    boolean hasResolvedDestination = threadId != -1 && address != null && distributionType != -1;

    if (!hasResolvedDestination && animate) {
      ViewUtil.fadeIn(contactsFragment.getView(), 300);
      ViewUtil.fadeOut(progressWheel, 300);
    } else if (!hasResolvedDestination) {
      contactsFragment.getView().setVisibility(View.VISIBLE);
      progressWheel.setVisibility(View.GONE);
    } else {
      Log.d("mediaPreviewPage->handleResolvedMedia ","true");
      createConversation(threadId, address, distributionType,true);
    }
  }

  private void createConversation(long threadId, Address address, int distributionType, boolean mediaPreviewPage) {
    final Intent intent = getBaseShareIntent(HomeActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    intent.putExtra(ConversationFragmentV2.ADDRESS, address);
    intent.putExtra(ConversationFragmentV2.THREAD_ID, threadId);

    isPassingAlongMedia = true;
    Log.d("share->address ",""+address);
    Log.d("share->threadId ",""+threadId);
    if(!mediaPreviewPage){
      Log.d("mediaPreviewPage-> ","true");
      intent.putExtra(HomeActivity.SHORTCUT_LAUNCHER,true);
      startActivity(intent);
    }else {
      Log.d("mediaPreviewPage-> ","false");
      setResult(RESULT_OK, intent);
      finish();
    }
  }

  private Intent getBaseShareIntent(final @NonNull Class<?> target) {
    final Intent           intent       = new Intent(this, target);

    if (resolvedExtra != null) {
      intent.putExtra(ConversationFragmentV2.URI,resolvedExtra);
      intent.putExtra(ConversationFragmentV2.TYPE,mimeType);
    } else if (resolvedPlaintext != null) {
      intent.putExtra(Intent.EXTRA_TEXT, resolvedPlaintext);
      intent.putExtra(ConversationFragmentV2.TYPE,"text/plain");
    }

    return intent;
  }

  private String getMimeType(@Nullable Uri uri) {
    if (uri != null) {
      final String mimeType = MediaUtil.getMimeType(getApplicationContext(), uri);
      if (mimeType != null) return mimeType;
    }
    return MediaUtil.getCorrectedMimeType(getIntent().getType());
  }

  @Override
  public void onContactSelected(String number) {
    Recipient recipient = Recipient.from(this, Address.fromExternal(this, number), true);
    long existingThread = DatabaseComponent.get(this).threadDatabase().getThreadIdIfExistsFor(recipient);
    //SetDataAndType
    boolean   mediaPreviewPage = getIntent().getBooleanExtra(MEDIA_PREVIEW_PAGE,false);
    Log.d("mediaPreviewPage->onContactSelected ",""+mediaPreviewPage);
    createConversation(existingThread, recipient.getAddress(), DistributionTypes.DEFAULT, mediaPreviewPage);
  }

  @Override
  public void onContactDeselected(String number) {
  }

  @SuppressLint("StaticFieldLeak")
  private class ResolveMediaTask extends AsyncTask<Uri, Void, Uri> {
    private final Context context;

    ResolveMediaTask(Context context) {
      this.context = context;
    }

    @Override
    protected Uri doInBackground(Uri... uris) {
      try {
        if (uris.length != 1 || uris[0] == null) {
          return null;
        }

        InputStream inputStream;

        if ("file".equals(uris[0].getScheme())) {
          inputStream = new FileInputStream(uris[0].getPath());
        } else {
          inputStream = context.getContentResolver().openInputStream(uris[0]);
        }

        if (inputStream == null) {
          return null;
        }

        Cursor cursor   = getContentResolver().query(uris[0], new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
        String fileName = null;
        Long   fileSize = null;

        try {
          if (cursor != null && cursor.moveToFirst()) {
            try {
              fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
              fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            } catch (IllegalArgumentException e) {
              Log.w(TAG, e);
            }
          }
        } finally {
          if (cursor != null) cursor.close();
        }

        return BlobProvider.getInstance()
                           .forData(inputStream, fileSize == null ? 0 : fileSize)
                           .withMimeType(mimeType)
                           .withFileName(fileName)
                           .createForMultipleBchatsOnDisk(context, e -> Log.w(TAG, "Failed to write to disk.", e));
      } catch (IOException ioe) {
        Log.w(TAG, ioe);
        return null;
      }
    }

    @Override
    protected void onPostExecute(Uri uri) {
      resolvedExtra = uri;
      handleResolvedMedia(getIntent(), true);
    }
  }
}