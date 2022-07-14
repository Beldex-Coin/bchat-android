package com.thoughtcrimes.securesms.mediapreview;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.thoughtcrimes.securesms.database.MediaDatabase;
import com.thoughtcrimes.securesms.mediasend.Media;
import com.beldex.libsignal.utilities.guava.Optional;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MediaPreviewViewModel extends ViewModel {

  private final MutableLiveData<PreviewData> previewData = new MutableLiveData<>();

  private boolean leftIsRecent;

  private @Nullable Cursor cursor;

  public void setCursor(@NonNull Context context, @Nullable Cursor cursor, boolean leftIsRecent) {
    boolean firstLoad = (this.cursor == null) && (cursor != null);
    if (this.cursor != null && !this.cursor.equals(cursor)) {
      this.cursor.close();
    }
    this.cursor       = cursor;
    this.leftIsRecent = leftIsRecent;

    if (firstLoad) {
      setActiveAlbumRailItem(context, 0);
    }
  }

  public void setActiveAlbumRailItem(@NonNull Context context, int activePosition) {
    if (cursor == null) {
      previewData.postValue(new PreviewData(Collections.emptyList(), null, 0));
      return;
    }

    activePosition = getCursorPosition(activePosition);

    cursor.moveToPosition(activePosition);

    MediaDatabase.MediaRecord activeRecord = MediaDatabase.MediaRecord.from(context, cursor);
    LinkedList<Media> rail         = new LinkedList<>();

    Media activeMedia = toMedia(activeRecord);
    if (activeMedia != null) rail.add(activeMedia);

    while (cursor.moveToPrevious()) {
      MediaDatabase.MediaRecord record = MediaDatabase.MediaRecord.from(context, cursor);
      if (record.getAttachment().getMmsId() == activeRecord.getAttachment().getMmsId()) {
        Media media = toMedia(record);
        if (media != null) rail.addFirst(media);
      } else {
        break;
      }
    }

    cursor.moveToPosition(activePosition);

    while (cursor.moveToNext()) {
      MediaDatabase.MediaRecord record = MediaDatabase.MediaRecord.from(context, cursor);
      if (record.getAttachment().getMmsId() == activeRecord.getAttachment().getMmsId()) {
        Media media = toMedia(record);
        if (media != null) rail.addLast(media);
      } else {
        break;
      }
    }

    if (!leftIsRecent) {
      Collections.reverse(rail);
    }

    previewData.postValue(new PreviewData(rail.size() > 1 ? rail : Collections.emptyList(),
                                          activeRecord.getAttachment().getCaption(),
                                          rail.indexOf(activeMedia)));
  }

  private int getCursorPosition(int position) {
    if (cursor == null) {
      return 0;
    }

    if (leftIsRecent) return position;
    else              return cursor.getCount() - 1 - position;
  }

  private @Nullable Media toMedia(@NonNull MediaDatabase.MediaRecord mediaRecord) {
    Uri uri = mediaRecord.getAttachment().getThumbnailUri() != null ? mediaRecord.getAttachment().getThumbnailUri()
                                                                    : mediaRecord.getAttachment().getDataUri();

    if (uri == null) {
      return null;
    }

    return new Media(uri,
                     mediaRecord.getContentType(),
                     mediaRecord.getDate(),
                     mediaRecord.getAttachment().getWidth(),
                     mediaRecord.getAttachment().getHeight(),
                     mediaRecord.getAttachment().getSize(),
                     Optional.absent(),
                     Optional.fromNullable(mediaRecord.getAttachment().getCaption()));
  }

  public LiveData<PreviewData> getPreviewData() {
    return previewData;
  }

  public static class PreviewData {
    private final List<Media> albumThumbnails;
    private final String      caption;
    private final int         activePosition;

    public PreviewData(@NonNull List<Media> albumThumbnails, @Nullable String caption, int activePosition) {
      this.albumThumbnails = albumThumbnails;
      this.caption         = caption;
      this.activePosition  = activePosition;
    }

    public @NonNull List<Media> getAlbumThumbnails() {
      return albumThumbnails;
    }

    public @Nullable String getCaption() {
      return caption;
    }

    public int getActivePosition() {
      return activePosition;
    }
  }
}
