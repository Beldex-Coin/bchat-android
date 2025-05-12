/*
 * Copyright (C) 2014 Open Whisper Systems
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
package io.beldex.bchat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import io.beldex.bchat.database.loaders.PagingMediaLoader;
import io.beldex.bchat.database.model.MmsMessageRecord;
import io.beldex.bchat.mms.Slide;
import io.beldex.bchat.util.AttachmentUtil;
import io.beldex.bchat.util.DateUtils;
import com.beldex.libbchat.messaging.messages.control.DataExtractionNotification;
import com.beldex.libbchat.messaging.sending_receiving.MessageSender;
import com.beldex.libbchat.messaging.sending_receiving.attachments.DatabaseAttachment;
import com.beldex.libbchat.mnode.MnodeAPI;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libbchat.utilities.Util;
import com.beldex.libbchat.utilities.recipients.Recipient;
import com.beldex.libbchat.utilities.recipients.RecipientModifiedListener;
import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.components.MediaView;
import io.beldex.bchat.conversation.v2.ConversationFragmentV2;
import io.beldex.bchat.database.MediaDatabase.MediaRecord;
import io.beldex.bchat.mediapreview.MediaPreviewViewModel;
import io.beldex.bchat.mediapreview.MediaRailAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import io.beldex.bchat.permissions.Permissions;
import io.beldex.bchat.util.SaveAttachmentTask;
import io.beldex.bchat.util.SaveAttachmentTask.Attachment;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.WeakHashMap;

import kotlin.Unit;

/**
 * Activity for displaying media attachments in-app
 */
public class MediaPreviewActivity extends PassphraseRequiredActionBarActivity implements RecipientModifiedListener,
                                                                                         LoaderManager.LoaderCallbacks<Pair<Cursor, Integer>>,
                                                                                         MediaRailAdapter.RailItemListener
{

  private final static String TAG = MediaPreviewActivity.class.getSimpleName();

  public static final String ADDRESS_EXTRA        = "address";
  public static final String DATE_EXTRA           = "date";
  public static final String SIZE_EXTRA           = "size";
  public static final String CAPTION_EXTRA        = "caption";
  public static final String OUTGOING_EXTRA       = "outgoing";
  public static final String LEFT_IS_RECENT_EXTRA = "left_is_recent";
  //
  public static final String ALBUM_THUMBNAIL_VIEW = "album_thumbnail_view";


  private ViewPager             mediaPager;
  private View                  detailsContainer;
  private TextView              caption;
  private View                  captionContainer;
  private RecyclerView          albumRail;
  private MediaRailAdapter      albumRailAdapter;
  private ViewGroup             playbackControlsContainer;
  private Uri                   initialMediaUri;
  private String                initialMediaType;
  private long                  initialMediaSize;
  private String                initialCaption;
  private Recipient             conversationRecipient;
  private boolean               leftIsRecent;
  private GestureDetector       clickDetector;
  private MediaPreviewViewModel viewModel;
  private ViewPagerListener     viewPagerListener;

  private int restartItem = -1;

  private MediaItemAdapter adapter;

  public static Intent getPreviewIntent(Context context, Slide slide, MmsMessageRecord mms, Recipient threadRecipient) {
    Intent previewIntent = null;
    if (isContentTypeSupported(slide.getContentType()) && slide.getUri() != null) {
      previewIntent = new Intent(context, MediaPreviewActivity.class);
      previewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              .setDataAndType(slide.getUri(), slide.getContentType())
              .putExtra(ADDRESS_EXTRA, threadRecipient.getAddress())
              .putExtra(OUTGOING_EXTRA, mms.isOutgoing())
              .putExtra(DATE_EXTRA, mms.getTimestamp())
              .putExtra(SIZE_EXTRA, slide.asAttachment().getSize())
              .putExtra(CAPTION_EXTRA, slide.getCaption().orNull())
              .putExtra(LEFT_IS_RECENT_EXTRA, false)
              .putExtra(ALBUM_THUMBNAIL_VIEW,true);
    }
    return previewIntent;
  }


  @SuppressWarnings("ConstantConditions")
  @Override
  protected void onCreate(Bundle bundle, boolean ready) {
    viewModel = new ViewModelProvider(this).get(MediaPreviewViewModel.class);

    setContentView(R.layout.media_preview_activity);

    initializeViews();
    initializeResources();
    initializeObservers();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    clickDetector.onTouchEvent(ev);
    //SteveJosephh21
    try {
      return super.dispatchTouchEvent(ev);
    }catch(IllegalArgumentException e){
      return false;
    }
  }

  @SuppressLint("MissingSuperCall")
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  public void onModified(Recipient recipient) {
    Util.runOnMain(this::updateActionBar);
  }

  @Override
  public void onRailItemClicked(int distanceFromActive) {
    mediaPager.setCurrentItem(mediaPager.getCurrentItem() + distanceFromActive);
  }

  @Override
  public void onRailItemDeleteClicked(int distanceFromActive) {
    throw new UnsupportedOperationException("Callback unsupported.");
  }

  @SuppressWarnings("ConstantConditions")
  private void updateActionBar() {
    MediaItem mediaItem = getCurrentMediaItem();

    if (mediaItem != null) {
      CharSequence relativeTimeSpan;

      if (mediaItem.date > 0) {
        relativeTimeSpan = DateUtils.getDisplayFormattedTimeSpanString(this, Locale.getDefault(), mediaItem.date);
      } else {
        relativeTimeSpan = getString(R.string.MediaPreviewActivity_draft);
      }

      if      (mediaItem.outgoing)          getSupportActionBar().setTitle(getString(R.string.MediaPreviewActivity_you));
      else if (mediaItem.recipient != null) getSupportActionBar().setTitle(mediaItem.recipient.toShortString());
      else                                  getSupportActionBar().setTitle("");

      getSupportActionBar().setSubtitle(relativeTimeSpan);
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    initializeMedia();
  }

  @Override
  public void onPause() {
    super.onPause();
    restartItem = cleanupMedia();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    initializeResources();
  }

  private void initializeViews() {
    mediaPager = findViewById(R.id.media_pager);
    mediaPager.setOffscreenPageLimit(1);

    albumRail        = findViewById(R.id.media_preview_album_rail);
    albumRailAdapter = new MediaRailAdapter(Glide.with(this), this, false);

    albumRail.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    albumRail.setAdapter(albumRailAdapter);

    detailsContainer          = findViewById(R.id.media_preview_details_container);
    caption                   = findViewById(R.id.media_preview_caption);
    captionContainer          = findViewById(R.id.media_preview_caption_container);
    playbackControlsContainer = findViewById(R.id.media_preview_playback_controls_container);

    setSupportActionBar(findViewById(R.id.toolbar));
    ActionBar actionBar = getSupportActionBar();
    actionBar.setHomeAsUpIndicator(R.drawable.ic_back);
    actionBar.setDisplayHomeAsUpEnabled(true);
  }

  private void initializeResources() {
    Address address = getIntent().getParcelableExtra(ADDRESS_EXTRA);

    initialMediaUri  = getIntent().getData();
    initialMediaType = getIntent().getType();
    initialMediaSize = getIntent().getLongExtra(SIZE_EXTRA, 0);
    initialCaption   = getIntent().getStringExtra(CAPTION_EXTRA);
    leftIsRecent     = getIntent().getBooleanExtra(LEFT_IS_RECENT_EXTRA, false);
    restartItem      = -1;

    if (address != null) {
      conversationRecipient = Recipient.from(this, address, true);
    } else {
      conversationRecipient = null;
    }
  }

  private void initializeObservers() {
    viewModel.getPreviewData().observe(this, previewData -> {
      if (previewData == null || mediaPager == null || mediaPager.getAdapter() == null) {
        return;
      }

      View playbackControls = ((MediaItemAdapter) mediaPager.getAdapter()).getPlaybackControls(mediaPager.getCurrentItem());

      if (previewData.getAlbumThumbnails().isEmpty() && previewData.getCaption() == null && playbackControls == null) {
        detailsContainer.setVisibility(View.GONE);
      } else {
        detailsContainer.setVisibility(View.VISIBLE);
      }

      albumRail.setVisibility(previewData.getAlbumThumbnails().isEmpty() ? View.GONE : View.VISIBLE);
      albumRailAdapter.setMedia(previewData.getAlbumThumbnails(), previewData.getActivePosition());
      albumRail.smoothScrollToPosition(previewData.getActivePosition());

      captionContainer.setVisibility(previewData.getCaption() == null ? View.GONE : View.VISIBLE);
      caption.setText(previewData.getCaption());

      if (playbackControls != null) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        playbackControls.setLayoutParams(params);

        playbackControlsContainer.removeAllViews();
        playbackControlsContainer.addView(playbackControls);
      } else {
        playbackControlsContainer.removeAllViews();
      }
    });

    clickDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onSingleTapUp(MotionEvent e) {
        if (e.getY() < detailsContainer.getTop()) {
          detailsContainer.setVisibility(detailsContainer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
        return super.onSingleTapUp(e);
      }
    });
  }

  private void initializeMedia() {
    if (!isContentTypeSupported(initialMediaType)) {
      Log.w(TAG, "Unsupported media type sent to MediaPreviewActivity, finishing.");
      Toast.makeText(getApplicationContext(), R.string.MediaPreviewActivity_unssuported_media_type, Toast.LENGTH_LONG).show();
      finish();
    }

    Log.i(TAG, "Loading Part URI: " + initialMediaUri);

    if (conversationRecipient != null) {
      getSupportLoaderManager().restartLoader(0, null, this);
    } else {
      adapter = new SingleItemPagerAdapter(this, Glide.with(this), getWindow(), initialMediaUri, initialMediaType, initialMediaSize);
      mediaPager.setAdapter(adapter);

      if (initialCaption != null) {
        detailsContainer.setVisibility(View.VISIBLE);
        captionContainer.setVisibility(View.VISIBLE);
        caption.setText(initialCaption);
      }
    }
  }

  private int cleanupMedia() {
    int restartItem = mediaPager.getCurrentItem();

    mediaPager.removeAllViews();
    mediaPager.setAdapter(null);

    return restartItem;
  }

  private void showOverview() {
    Intent intent = new Intent(this, MediaOverviewActivity.class);
    intent.putExtra(MediaOverviewActivity.ADDRESS_EXTRA, conversationRecipient.getAddress());
    startActivity(intent);
  }

  //SetDataAndType
  ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result->{
    if (result.getResultCode() == Activity.RESULT_OK) {
      Bundle extras = new Bundle();
      assert result.getData() != null;
      extras.putParcelable(ConversationFragmentV2.ADDRESS,result.getData().getParcelableExtra(ConversationFragmentV2.ADDRESS));
      extras.putLong(ConversationFragmentV2.THREAD_ID, result.getData().getLongExtra(ConversationFragmentV2.THREAD_ID,-1));
      Log.d("MediaPreviewActivity->uri",""+result.getData().getParcelableExtra(ConversationFragmentV2.URI));
      Log.d("MediaPreviewActivity->type",""+result.getData().getStringExtra(ConversationFragmentV2.TYPE));
      extras.putParcelable(ConversationFragmentV2.URI,result.getData().getParcelableExtra(ConversationFragmentV2.URI));
      Intent intent = new Intent();
      intent.putExtra(Intent.EXTRA_TEXT,result.getData().getCharSequenceExtra(Intent.EXTRA_TEXT));
      intent.putExtra(ConversationFragmentV2.TYPE,result.getData().getStringExtra(ConversationFragmentV2.TYPE));
      intent.putExtras(extras);
      setResult(RESULT_OK,intent);
      finish();
    }
  });

  private void forward() {
    MediaItem mediaItem = getCurrentMediaItem();

    if (mediaItem != null) {
      Intent composeIntent = new Intent(this, ShareActivity.class);
      composeIntent.putExtra(Intent.EXTRA_STREAM, mediaItem.uri);
      composeIntent.setType(mediaItem.type);
      composeIntent.putExtra(ShareActivity.IN_CHAT_SHARE,true);
      resultLauncher.launch(composeIntent);
    }
  }

  @SuppressWarnings("CodeBlock2Expr")
  @SuppressLint("InlinedApi")
  private void saveToDisk() {
    MediaItem mediaItem = getCurrentMediaItem();
    if (mediaItem == null) return;

    SaveAttachmentTask.showWarningDialog(this, 1, () -> {
      Permissions.with(this)
              .request(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
              .maxSdkVersion(Build.VERSION_CODES.P)
              .withPermanentDenialDialog(getString(R.string.MediaPreviewActivity_signal_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied))
              .onAnyDenied(() -> Toast.makeText(this, R.string.MediaPreviewActivity_unable_to_write_to_external_storage_without_permission, Toast.LENGTH_LONG).show())
              .onAllGranted(() -> {
                SaveAttachmentTask saveTask = new SaveAttachmentTask(MediaPreviewActivity.this);
                long saveDate = (mediaItem.date > 0) ? mediaItem.date : MnodeAPI.getNowWithOffset();
                saveTask.executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR,
                        new Attachment(mediaItem.uri, mediaItem.type, saveDate, null));
                if (!mediaItem.outgoing) {
                  sendMediaSavedNotificationIfNeeded();
                }
              })
              .execute();
      return Unit.INSTANCE;
    });
  }

  private void sendMediaSavedNotificationIfNeeded() {
    if (conversationRecipient.isGroupRecipient()) return;
    DataExtractionNotification message = new DataExtractionNotification(new DataExtractionNotification.Kind.MediaSaved(MnodeAPI.getNowWithOffset()));
    MessageSender.send(message, conversationRecipient.getAddress());
  }

  @SuppressLint("StaticFieldLeak")
  private void deleteMedia() {
    MediaItem mediaItem = getCurrentMediaItem();
    if (mediaItem == null || mediaItem.attachment == null) {
      return;
    }

    LayoutInflater factory = LayoutInflater.from(this);
    View deleteMediaPermissionDialogView = factory.inflate(R.layout.delete_media_dialog_box, null);
    AlertDialog deleteMediaPermissionDialog = new AlertDialog.Builder(this).create();
    deleteMediaPermissionDialog.setView(deleteMediaPermissionDialogView);
    deleteMediaPermissionDialogView.<Button>findViewById(R.id.deleteMediaDialogBoxButton).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... voids) {
            if (mediaItem.attachment == null) {
              return null;
            }
            AttachmentUtil.deleteAttachment(MediaPreviewActivity.this.getApplicationContext(),
                    mediaItem.attachment);
            return null;
          }
        }.execute();
        deleteMediaPermissionDialog.dismiss();
        finish();
      }
    });
    deleteMediaPermissionDialogView.<Button>findViewById(R.id.cancelMediaDialogBoxButton).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        deleteMediaPermissionDialog.dismiss();
      }
    });
    Objects.requireNonNull(deleteMediaPermissionDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
    deleteMediaPermissionDialog.show();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);

    menu.clear();
    MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.media_preview, menu);

    if (!isMediaInDb()) {
      menu.findItem(R.id.media_preview__overview).setVisible(false);
      menu.findItem(R.id.delete).setVisible(false);
    }

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case R.id.media_preview__overview: showOverview(); return true;
      case R.id.media_preview__forward:  forward();      return true;
      case R.id.save:                    saveToDisk();   return true;
      case R.id.delete:                  deleteMedia();  return true;
      case android.R.id.home:            finish();       return true;
    }

    return false;
  }

  private boolean isMediaInDb() {
    return conversationRecipient != null;
  }

  private @Nullable MediaItem getCurrentMediaItem() {
    if (adapter == null) return null;
    return adapter.getMediaItemFor(mediaPager.getCurrentItem());
  }

  public static boolean isContentTypeSupported(final String contentType) {
    return contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/"));
  }

  @Override
  public @NonNull Loader<Pair<Cursor, Integer>> onCreateLoader(int id, Bundle args) {
    return new PagingMediaLoader(this, conversationRecipient, initialMediaUri, leftIsRecent);
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Pair<Cursor, Integer>> loader, @Nullable Pair<Cursor, Integer> data) {
    if (data == null) return;

    mediaPager.removeOnPageChangeListener(viewPagerListener);


    adapter = new CursorPagerAdapter(this, Glide.with(this), getWindow(), data.first, data.second, leftIsRecent);
    mediaPager.setAdapter(adapter);

    viewModel.setCursor(this, data.first, leftIsRecent);

    int item = restartItem >= 0  && restartItem < adapter.getCount() ? restartItem : Math.max(Math.min(data.second, adapter.getCount() - 1), 0);

    viewPagerListener = new ViewPagerListener();
    mediaPager.addOnPageChangeListener(viewPagerListener);

    try {
      mediaPager.setCurrentItem(item);
    } catch (CursorIndexOutOfBoundsException e) {
      throw new RuntimeException("restartItem = " + restartItem + ", data.second = " + data.second + " leftIsRecent = " + leftIsRecent, e);
    }

    if (item == 0) {
      viewPagerListener.onPageSelected(0);
    }

  }

  @Override
  public void onLoaderReset(@NonNull Loader<Pair<Cursor, Integer>> loader) {

  }

  private class ViewPagerListener implements ViewPager.OnPageChangeListener {

    private int currentPage = -1;

    @Override
    public void onPageSelected(int position) {
      if (currentPage != -1 && currentPage != position) onPageUnselected(currentPage);
      currentPage = position;

      if (adapter == null) return;

      MediaItem item = adapter.getMediaItemFor(position);
      if (item.recipient != null) item.recipient.addListener(MediaPreviewActivity.this);
      viewModel.setActiveAlbumRailItem(MediaPreviewActivity.this, position);
      updateActionBar();
    }


    public void onPageUnselected(int position) {
      try {
        MediaItem item = adapter.getMediaItemFor(position);
        if (item.recipient != null) item.recipient.removeListener(MediaPreviewActivity.this);
      } catch (CursorIndexOutOfBoundsException e) {
        throw new RuntimeException("position = " + position + " leftIsRecent = " + leftIsRecent, e);
      }

      adapter.pause(position);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
  }

  private static class SingleItemPagerAdapter extends MediaItemAdapter {

    private final RequestManager glideRequests;
    private final Window        window;
    private final Uri           uri;
    private final String        mediaType;
    private final long          size;

    private final LayoutInflater inflater;

    SingleItemPagerAdapter(@NonNull Context context, @NonNull RequestManager glideRequests,
                           @NonNull Window window, @NonNull Uri uri, @NonNull String mediaType,
                           long size)
    {
      this.glideRequests = glideRequests;
      this.window        = window;
      this.uri           = uri;
      this.mediaType     = mediaType;
      this.size          = size;
      this.inflater      = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
      return 1;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
      return view == object;
    }

    @Override
    public @NonNull Object instantiateItem(@NonNull ViewGroup container, int position) {
      View      itemView  = inflater.inflate(R.layout.media_view_page, container, false);
      MediaView mediaView = itemView.findViewById(R.id.media_view);

      try {
        mediaView.set(glideRequests, window, uri, mediaType, size, true);
      } catch (IOException e) {
        Log.w(TAG, e);
      }

      container.addView(itemView);

      return itemView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
      MediaView mediaView = ((FrameLayout)object).findViewById(R.id.media_view);
      mediaView.cleanup();

      container.removeView((FrameLayout)object);
    }

    @Override
    public MediaItem getMediaItemFor(int position) {
      return new MediaItem(null, null, uri, mediaType, -1, true);
    }

    @Override
    public void pause(int position) {

    }

    @Override
    public @Nullable View getPlaybackControls(int position) {
      return null;
    }
  }

  private static class CursorPagerAdapter extends MediaItemAdapter {

    private final WeakHashMap<Integer, MediaView> mediaViews = new WeakHashMap<>();

    private final Context       context;
    private final RequestManager glideRequests;
    private final Window        window;
    private final Cursor        cursor;
    private final boolean       leftIsRecent;

    private int     autoPlayPosition;

    CursorPagerAdapter(@NonNull Context context, @NonNull RequestManager glideRequests,
                       @NonNull Window window, @NonNull Cursor cursor, int autoPlayPosition,
                       boolean leftIsRecent)
    {
      this.context          = context.getApplicationContext();
      this.glideRequests    = glideRequests;
      this.window           = window;
      this.cursor           = cursor;
      this.autoPlayPosition = autoPlayPosition;
      this.leftIsRecent     = leftIsRecent;
    }

    @Override
    public int getCount() {
      return cursor.getCount();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
      return view == object;
    }

    @Override
    public @NonNull Object instantiateItem(@NonNull ViewGroup container, int position) {
      View      itemView       = LayoutInflater.from(context).inflate(R.layout.media_view_page, container, false);
      MediaView mediaView      = itemView.findViewById(R.id.media_view);
      boolean   autoplay       = position == autoPlayPosition;
      int       cursorPosition = getCursorPosition(position);

      autoPlayPosition = -1;

      cursor.moveToPosition(cursorPosition);

      MediaRecord mediaRecord = MediaRecord.from(context, cursor);

      try {
        //noinspection ConstantConditions
        mediaView.set(glideRequests, window, mediaRecord.getAttachment().getDataUri(),
                      mediaRecord.getAttachment().getContentType(), mediaRecord.getAttachment().getSize(), autoplay);
      } catch (IOException e) {
        Log.w(TAG, e);
      }

      mediaViews.put(position, mediaView);
      container.addView(itemView);

      return itemView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
      MediaView mediaView = ((FrameLayout)object).findViewById(R.id.media_view);
      mediaView.cleanup();

      mediaViews.remove(position);
      container.removeView((FrameLayout)object);
    }

    public MediaItem getMediaItemFor(int position) {
      cursor.moveToPosition(getCursorPosition(position));
      MediaRecord mediaRecord = MediaRecord.from(context, cursor);
      Address     address     = mediaRecord.getAddress();

      if (mediaRecord.getAttachment().getDataUri() == null) throw new AssertionError();

      return new MediaItem(address != null ? Recipient.from(context, address,true) : null,
                           mediaRecord.getAttachment(),
                           mediaRecord.getAttachment().getDataUri(),
                           mediaRecord.getContentType(),
                           mediaRecord.getDate(),
                           mediaRecord.isOutgoing());
    }

    @Override
    public void pause(int position) {
      MediaView mediaView = mediaViews.get(position);
      if (mediaView != null) mediaView.pause();
    }

    @Override
    public @Nullable View getPlaybackControls(int position) {
      MediaView mediaView = mediaViews.get(position);
      if (mediaView != null) return mediaView.getPlaybackControls();
      return null;
    }

    private int getCursorPosition(int position) {
      int unClamped = leftIsRecent ? position : cursor.getCount() - 1 - position;
      return Math.max(Math.min(unClamped, cursor.getCount() - 1), 0);
    }
  }

  private static class MediaItem {
    private final @Nullable Recipient          recipient;
    private final @Nullable DatabaseAttachment attachment;
    private final @NonNull  Uri                uri;
    private final @NonNull  String             type;
    private final           long               date;
    private final           boolean            outgoing;

    private MediaItem(@Nullable Recipient recipient,
                      @Nullable DatabaseAttachment attachment,
                      @NonNull Uri uri,
                      @NonNull String type,
                      long date,
                      boolean outgoing)
    {
      this.recipient  = recipient;
      this.attachment = attachment;
      this.uri        = uri;
      this.type       = type;
      this.date       = date;
      this.outgoing   = outgoing;
    }
  }

  abstract static class MediaItemAdapter extends PagerAdapter {
    abstract MediaItem getMediaItemFor(int position);
    abstract void pause(int position);
    @Nullable abstract View getPlaybackControls(int position);
  }
}
