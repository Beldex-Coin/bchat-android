package io.beldex.bchat.components;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import io.beldex.bchat.permissions.Permissions;
import com.beldex.libbchat.utilities.ViewUtil;

import io.beldex.bchat.R;

public class AttachmentTypeSelector extends PopupWindow {

  public static final int ADD_GALLERY       = 1;
  public static final int ADD_DOCUMENT      = 2;
  public static final int ADD_SOUND         = 3;
  public static final int ADD_CONTACT_INFO  = 4;
  public static final int TAKE_PHOTO        = 5;
  public static final int ADD_LOCATION      = 6;
  public static final int ADD_GIF           = 7;

  private static final int ANIMATION_DURATION = 300;

  @SuppressWarnings("unused")
  private static final String TAG = AttachmentTypeSelector.class.getSimpleName();

  private final @NonNull Context             context;
  public                 int                 keyboardHeight;
  private final @NonNull LoaderManager       loaderManager;
  private final @NonNull RecentPhotoViewRail recentRail;
  private final @NonNull ImageView           imageButton;
  private final @NonNull ImageView           audioButton;
  private final @NonNull ImageView           documentButton;
  private final @NonNull ImageView           contactButton;
  private final @NonNull ImageView           cameraButton;
  private final @NonNull ImageView           locationButton;
  private final @NonNull ImageView           gifButton;
  private final @NonNull ImageView           closeButton;

  private @Nullable View                      currentAnchor;
  private @Nullable AttachmentClickedListener listener;

  public AttachmentTypeSelector(@NonNull Context context, @NonNull LoaderManager loaderManager, @Nullable AttachmentClickedListener listener, int keyboardHeight) {
    super(context);

    this.context = context;
    this.keyboardHeight = keyboardHeight;

    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    LinearLayout   layout   = (LinearLayout) inflater.inflate(R.layout.attachment_type_selector, null, true);

    this.listener       = listener;
    this.loaderManager  = loaderManager;
    this.recentRail     = ViewUtil.findById(layout, R.id.recent_photos);
    this.imageButton    = ViewUtil.findById(layout, R.id.gallery_button);
    this.audioButton    = ViewUtil.findById(layout, R.id.audio_button);
    this.documentButton = ViewUtil.findById(layout, R.id.document_button);
    this.contactButton  = ViewUtil.findById(layout, R.id.contact_button);
    this.cameraButton   = ViewUtil.findById(layout, R.id.camera_button);
    this.locationButton = ViewUtil.findById(layout, R.id.location_button);
    this.gifButton      = ViewUtil.findById(layout, R.id.giphy_button);
    this.closeButton    = ViewUtil.findById(layout, R.id.close_button);

    this.imageButton.setOnClickListener(new PropagatingClickListener(ADD_GALLERY));
    this.audioButton.setOnClickListener(new PropagatingClickListener(ADD_SOUND));
    this.documentButton.setOnClickListener(new PropagatingClickListener(ADD_DOCUMENT));
    this.contactButton.setOnClickListener(new PropagatingClickListener(ADD_CONTACT_INFO));
    this.cameraButton.setOnClickListener(new PropagatingClickListener(TAKE_PHOTO));
    this.locationButton.setOnClickListener(new PropagatingClickListener(ADD_LOCATION));
    this.gifButton.setOnClickListener(new PropagatingClickListener(ADD_GIF));
    this.closeButton.setOnClickListener(new CloseClickListener());
    this.recentRail.setListener(new RecentPhotoSelectedListener());

    setContentView(layout);
    setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
    setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
    setBackgroundDrawable(new BitmapDrawable());
    setAnimationStyle(0);
    setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
    setFocusable(true);
    setTouchable(true);

    updateHeight();

    loaderManager.initLoader(1, null, recentRail);
  }

  public void show(@NonNull Activity activity, final @NonNull View anchor) {
    updateHeight();

    if (Permissions.hasAll(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
      recentRail.setVisibility(View.VISIBLE);
      loaderManager.restartLoader(1, null, recentRail);
    } else {
      recentRail.setVisibility(View.GONE);
    }

    this.currentAnchor = anchor;

    showAtLocation(anchor, Gravity.BOTTOM, 0, 0);

    getContentView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        getContentView().getViewTreeObserver().removeGlobalOnLayoutListener(this);

        animateWindowInCircular(anchor, getContentView());
      }
    });

    animateButtonIn(imageButton, ANIMATION_DURATION / 2);
    animateButtonIn(cameraButton, ANIMATION_DURATION / 2);

    animateButtonIn(audioButton, ANIMATION_DURATION / 3);
    animateButtonIn(locationButton, ANIMATION_DURATION / 3);
    animateButtonIn(documentButton, ANIMATION_DURATION / 4);
    animateButtonIn(gifButton, ANIMATION_DURATION / 4);
    animateButtonIn(contactButton, 0);
    animateButtonIn(closeButton, 0);
  }

  private void updateHeight() {
    int thresholdInDP = 120;
    float scale = context.getResources().getDisplayMetrics().density;
    int thresholdInPX = (int)(thresholdInDP * scale);
    View contentView = ViewUtil.findById(getContentView(), R.id.contentView);
    LinearLayout.LayoutParams contentViewLayoutParams = (LinearLayout.LayoutParams)contentView.getLayoutParams();
    contentViewLayoutParams.height = keyboardHeight > thresholdInPX ? keyboardHeight : LinearLayout.LayoutParams.WRAP_CONTENT;
    contentView.setLayoutParams(contentViewLayoutParams);
  }

  @Override
  public void dismiss() {
    animateWindowOutCircular(currentAnchor, getContentView());
  }

  public void setListener(@Nullable AttachmentClickedListener listener) {
    this.listener = listener;
  }

  private void animateButtonIn(View button, int delay) {
    AnimationSet animation = new AnimationSet(true);
    Animation scale = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
                                         Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.0f);

    animation.addAnimation(scale);
    animation.setInterpolator(new OvershootInterpolator(1));
    animation.setDuration(ANIMATION_DURATION);
    animation.setStartOffset(delay);
    button.startAnimation(animation);
  }


  private void animateWindowInCircular(@Nullable View anchor, @NonNull View contentView) {
    Pair<Integer, Integer> coordinates = getClickOrigin(anchor, contentView);
    Animator animator = ViewAnimationUtils.createCircularReveal(contentView,
                                                                coordinates.first,
                                                                coordinates.second,
                                                                0,
                                                                Math.max(contentView.getWidth(), contentView.getHeight()));
    animator.setDuration(ANIMATION_DURATION);
    animator.start();
  }

  private void animateWindowInTranslate(@NonNull View contentView) {
    Animation animation = new TranslateAnimation(0, 0, contentView.getHeight(), 0);
    animation.setDuration(ANIMATION_DURATION);

    getContentView().startAnimation(animation);
  }


  private void animateWindowOutCircular(@Nullable View anchor, @NonNull View contentView) {
    Pair<Integer, Integer> coordinates = getClickOrigin(anchor, contentView);
    Animator               animator    = ViewAnimationUtils.createCircularReveal(getContentView(),
                                                                                 coordinates.first,
                                                                                 coordinates.second,
                                                                                 Math.max(getContentView().getWidth(), getContentView().getHeight()),
                                                                                 0);

    animator.setDuration(ANIMATION_DURATION);
    animator.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {
      }

      @Override
      public void onAnimationEnd(Animator animation) {
        AttachmentTypeSelector.super.dismiss();
      }

      @Override
      public void onAnimationCancel(Animator animation) {
      }

      @Override
      public void onAnimationRepeat(Animator animation) {
      }
    });

    animator.start();
  }

  private void animateWindowOutTranslate(@NonNull View contentView) {
    Animation animation = new TranslateAnimation(0, 0, 0, contentView.getTop() + contentView.getHeight());
    animation.setDuration(ANIMATION_DURATION);
    animation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        AttachmentTypeSelector.super.dismiss();
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }
    });

    getContentView().startAnimation(animation);
  }

  private Pair<Integer, Integer> getClickOrigin(@Nullable View anchor, @NonNull View contentView) {
    if (anchor == null) return new Pair<>(0, 0);

    final int[] anchorCoordinates = new int[2];
    anchor.getLocationOnScreen(anchorCoordinates);
    anchorCoordinates[0] += anchor.getWidth() / 2;
    anchorCoordinates[1] += anchor.getHeight() / 2;

    final int[] contentCoordinates = new int[2];
    contentView.getLocationOnScreen(contentCoordinates);

    int x = anchorCoordinates[0] - contentCoordinates[0];
    int y = anchorCoordinates[1] - contentCoordinates[1];

    return new Pair<>(x, y);
  }

  private class RecentPhotoSelectedListener implements RecentPhotoViewRail.OnItemClickedListener {
    @Override
    public void onItemClicked(Uri uri, String mimeType, String bucketId, long dateTaken, int width, int height, long size) {
      animateWindowOutTranslate(getContentView());

      if (listener != null) listener.onQuickAttachment(uri, mimeType, bucketId, dateTaken, width, height, size);
    }
  }

  private class PropagatingClickListener implements View.OnClickListener {

    private final int type;

    private PropagatingClickListener(int type) {
      this.type = type;
    }

    @Override
    public void onClick(View v) {
      animateWindowOutTranslate(getContentView());

      if (listener != null) listener.onClick(type);
    }

  }

  private class CloseClickListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      dismiss();
    }
  }

  public interface AttachmentClickedListener {
    void onClick(int type);
    void onQuickAttachment(Uri uri, String mimeType, String bucketId, long dateTaken, int width, int height, long size);
  }

}
