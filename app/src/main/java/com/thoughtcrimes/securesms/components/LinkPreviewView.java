package com.thoughtcrimes.securesms.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.thoughtcrimes.securesms.mms.ImageSlide;
import com.thoughtcrimes.securesms.mms.SlidesClickedListener;
import com.thoughtcrimes.securesms.mms.GlideRequests;

import com.beldex.libbchat.messaging.sending_receiving.link_preview.LinkPreview;

import io.beldex.bchat.R;
import okhttp3.HttpUrl;

public class LinkPreviewView extends FrameLayout {

  private static final int TYPE_CONVERSATION = 0;
  private static final int TYPE_COMPOSE      = 1;

  private ViewGroup             container;
  private OutlinedThumbnailView thumbnail;
  private TextView              title;
  private TextView              site;
  private View                  divider;
  private View                  closeButton;
  private View                  spinner;

  private int                  type;
  private int                  defaultRadius;
  private CornerMask           cornerMask;
  private Outliner             outliner;
  private CloseClickedListener closeClickedListener;

  public LinkPreviewView(Context context) {
    super(context);
    init(null);
  }

  public LinkPreviewView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  private void init(@Nullable AttributeSet attrs) {
    inflate(getContext(), R.layout.link_preview, this);

    container     = findViewById(R.id.linkpreview_container);
    thumbnail     = findViewById(R.id.linkpreview_thumbnail);
    title         = findViewById(R.id.linkpreview_title);
    site          = findViewById(R.id.linkpreview_site);
    divider       = findViewById(R.id.linkpreview_divider);
    spinner       = findViewById(R.id.linkpreview_progress_wheel);
    closeButton   = findViewById(R.id.linkpreview_close);
    defaultRadius = getResources().getDimensionPixelSize(R.dimen.thumbnail_default_radius);
    cornerMask    = new CornerMask(this);
    outliner      = new Outliner();

    outliner.setColor(getResources().getColor(R.color.transparent));

    if (attrs != null) {
      TypedArray typedArray   = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.LinkPreviewView, 0, 0);
      type = typedArray.getInt(R.styleable.LinkPreviewView_linkpreview_type, 0);
      typedArray.recycle();
    }

    if (type == TYPE_COMPOSE) {
      container.setBackgroundColor(Color.TRANSPARENT);
      container.setPadding(0, 0, 0, 0);
      divider.setVisibility(VISIBLE);

      closeButton.setOnClickListener(v -> {
        if (closeClickedListener != null) {
          closeClickedListener.onCloseClicked();
        }
      });
    }

    setWillNotDraw(false);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    if (type == TYPE_COMPOSE) return;

    cornerMask.mask(canvas);
    outliner.draw(canvas);
  }

  public void setLoading() {
    title.setVisibility(GONE);
    site.setVisibility(GONE);
    thumbnail.setVisibility(GONE);
    spinner.setVisibility(VISIBLE);
    closeButton.setVisibility(GONE);
  }

  public void setLinkPreview(@NonNull GlideRequests glideRequests, @NonNull LinkPreview linkPreview, boolean showThumbnail, boolean showCloseButton) {
    setLinkPreview(glideRequests, linkPreview, showThumbnail);
    if (showCloseButton) {
      closeButton.setVisibility(VISIBLE);
    } else {
      closeButton.setVisibility(GONE);
    }
  }

  public void setLinkPreview(@NonNull GlideRequests glideRequests, @NonNull LinkPreview linkPreview, boolean showThumbnail) {
    title.setVisibility(VISIBLE);
    site.setVisibility(VISIBLE);
    thumbnail.setVisibility(VISIBLE);
    spinner.setVisibility(GONE);
    closeButton.setVisibility(VISIBLE);

    title.setText(linkPreview.getTitle());

    HttpUrl url = HttpUrl.parse(linkPreview.getUrl());
    if (url != null) {
      site.setText(url.topPrivateDomain());
    }

    if (showThumbnail && linkPreview.getThumbnail().isPresent()) {
      thumbnail.setVisibility(VISIBLE);
      thumbnail.setImageResource(glideRequests, new ImageSlide(getContext(), linkPreview.getThumbnail().get()), type == TYPE_CONVERSATION, false);
      thumbnail.showDownloadText(false);
    } else {
      thumbnail.setVisibility(GONE);
    }
  }

  public void setCorners(int topLeft, int topRight) {
    cornerMask.setRadii(topLeft, topRight, 0, 0);
    outliner.setRadii(topLeft, topRight, 0, 0);
    thumbnail.setCorners(topLeft, defaultRadius, defaultRadius, defaultRadius);
    postInvalidate();
  }

  public void setCloseClickedListener(@Nullable CloseClickedListener closeClickedListener) {
    this.closeClickedListener = closeClickedListener;
  }

  public void setDownloadClickedListener(SlidesClickedListener listener) {
    thumbnail.setDownloadClickListener(listener);
  }

  public interface CloseClickedListener {
    void onCloseClicked();
  }
}
