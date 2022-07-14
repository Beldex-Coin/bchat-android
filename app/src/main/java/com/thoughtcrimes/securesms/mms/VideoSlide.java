/** 
 * Copyright (C) 2011 Whisper Systems
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
package com.thoughtcrimes.securesms.mms;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.net.Uri;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.beldex.bchat.R;
import com.beldex.libbchat.messaging.sending_receiving.attachments.Attachment;
import com.beldex.libbchat.utilities.MediaTypes;
import com.thoughtcrimes.securesms.util.MediaUtil;
import com.thoughtcrimes.securesms.util.ResUtil;

public class VideoSlide extends Slide {

  public VideoSlide(Context context, Uri uri, long dataSize) {
    this(context, uri, dataSize, null);
  }

  public VideoSlide(Context context, Uri uri, long dataSize, @Nullable String caption) {
    super(context, constructAttachmentFromUri(context, uri, MediaTypes.VIDEO_UNSPECIFIED, dataSize, 0, 0, MediaUtil.hasVideoThumbnail(uri), null, caption, false, false));
  }

  public VideoSlide(Context context, Attachment attachment) {
    super(context, attachment);
  }

  @Override
  public boolean hasPlaceholder() {
    return true;
  }

  @Override
  public boolean hasPlayOverlay() {
    return true;
  }

  @Override
  public @DrawableRes int getPlaceholderRes(Theme theme) {
    return ResUtil.getDrawableRes(theme, R.attr.conversation_icon_attach_video);
  }

  @Override
  public boolean hasImage() {
    return true;
  }

  @Override
  public boolean hasVideo() {
    return true;
  }

  @NonNull @Override
  public String getContentDescription() {
    return context.getString(R.string.Slide_video);
  }
}
