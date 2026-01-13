/*
 * Copyright (C) 2015 Open Whisper Systems
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

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.beldex.bchat.database.loaders.BucketedThreadMediaLoader;
import com.bumptech.glide.RequestManager;
import io.beldex.bchat.mms.Slide;
import io.beldex.bchat.util.MediaUtil;
import com.codewaves.stickyheadergrid.StickyHeaderGridAdapter;


import io.beldex.bchat.conversation.v2.utilities.ThumbnailView;
import io.beldex.bchat.database.MediaDatabase.MediaRecord;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import io.beldex.bchat.R;

class MediaGalleryAdapter extends StickyHeaderGridAdapter {

  @SuppressWarnings("unused")
  private static final String TAG = MediaGalleryAdapter.class.getSimpleName();

  private final Context             context;
  private final RequestManager glideRequests;
  private final Locale              locale;
  private final ItemClickListener   itemClickListener;
  private final Set<MediaRecord>    selected;

  private BucketedThreadMediaLoader.BucketedThreadMedia media;

  private static class ViewHolder extends StickyHeaderGridAdapter.ItemViewHolder {
    final ThumbnailView imageView;
    final View          selectedIndicator;

    ViewHolder(View v) {
      super(v);
      imageView         = v.findViewById(R.id.image);
      selectedIndicator = v.findViewById(R.id.selected_indicator);
    }
  }

  private static class HeaderHolder extends StickyHeaderGridAdapter.HeaderViewHolder {
    final TextView textView;

    HeaderHolder(View itemView) {
      super(itemView);
      textView = itemView.findViewById(R.id.text);
    }
  }

  MediaGalleryAdapter(@NonNull Context context,
                      @NonNull RequestManager glideRequests,
                      BucketedThreadMediaLoader.BucketedThreadMedia media,
                      Locale locale,
                      ItemClickListener clickListener)
  {
    this.context           = context;
    this.glideRequests     = glideRequests;
    this.locale            = locale;
    this.media             = media;
    this.itemClickListener = clickListener;
    this.selected          = new HashSet<>();
  }

  public void setMedia(BucketedThreadMediaLoader.BucketedThreadMedia media) {
    this.media = media;
  }

  @Override
  public StickyHeaderGridAdapter.HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent, int headerType) {
    return new HeaderHolder(LayoutInflater.from(context).inflate(R.layout.media_overview_gallery_item_header, parent, false));
  }

  @Override
  public ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
    return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.media_overview_gallery_item, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(StickyHeaderGridAdapter.HeaderViewHolder viewHolder, int section) {
    ((HeaderHolder)viewHolder).textView.setText(media.getName(section, locale));
  }

  @Override
  public void onBindItemViewHolder(ItemViewHolder viewHolder, int section, int offset) {
    MediaRecord   mediaRecord       = media.get(section, offset);
    ThumbnailView thumbnailView     = ((ViewHolder)viewHolder).imageView;
    View          selectedIndicator = ((ViewHolder)viewHolder).selectedIndicator;
    Slide slide             = MediaUtil.getSlideForAttachment(context, mediaRecord.getAttachment());

    if (slide != null) {
      thumbnailView.setImageResource(glideRequests, slide, false, null);
    }

    thumbnailView.setOnClickListener(view -> itemClickListener.onMediaClicked(mediaRecord));
    thumbnailView.setOnLongClickListener(view -> {
      itemClickListener.onMediaLongClicked(mediaRecord);
      return true;
    });

    selectedIndicator.setVisibility(selected.contains(mediaRecord) ? View.VISIBLE : View.GONE);
  }

  @Override
  public int getSectionCount() {
    return media.getSectionCount();
  }

  @Override
  public int getSectionItemCount(int section) {
    return media.getSectionItemCount(section);
  }

  public void toggleSelection(@NonNull MediaRecord mediaRecord) {
    if (!selected.remove(mediaRecord)) {
      selected.add(mediaRecord);
    }
    notifyDataSetChanged();
  }

  public int getSelectedMediaCount() {
    return selected.size();
  }

  @NonNull
  public Collection<MediaRecord> getSelectedMedia() {
    return new HashSet<>(selected);
  }

  public void clearSelection() {
    selected.clear();
    notifyDataSetChanged();
  }

  void selectAllMedia() {
    for (int section = 0; section < media.getSectionCount(); section++) {
      for (int item = 0; item < media.getSectionItemCount(section); item++) {
        selected.add(media.get(section, item));
      }
    }
    this.notifyDataSetChanged();
  }

  interface ItemClickListener {
    void onMediaClicked(@NonNull MediaRecord mediaRecord);
    void onMediaLongClicked(MediaRecord mediaRecord);
  }
}
