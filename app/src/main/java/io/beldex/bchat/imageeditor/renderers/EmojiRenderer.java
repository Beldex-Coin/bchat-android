package io.beldex.bchat.imageeditor.renderers;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import androidx.annotation.NonNull;

import io.beldex.bchat.imageeditor.Bounds;
import io.beldex.bchat.imageeditor.RendererContext;


public final class EmojiRenderer extends InvalidateableRenderer {

  private static final float TEXT_SIZE = 100;

  @NonNull
  private final String emoji;

  private final Paint  paint                   = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF  emojiBounds             = new RectF();
  private final Matrix projectionMatrix        = new Matrix();
  private final Matrix inverseProjectionMatrix = new Matrix();

  public EmojiRenderer(@NonNull String emoji) {
    this.emoji = emoji;
    paint.setTextSize(TEXT_SIZE);
    paint.setColor(0xFF000000);

    Rect bounds = new Rect();
    paint.getTextBounds(emoji, 0, emoji.length(), bounds);
    if (bounds.isEmpty()) {
      bounds.set(0, (int) -TEXT_SIZE, (int) TEXT_SIZE, 0);
    }
    emojiBounds.set(bounds);

    projectionMatrix.setRectToRect(emojiBounds, Bounds.FULL_BOUNDS, Matrix.ScaleToFit.CENTER);
    projectionMatrix.invert(inverseProjectionMatrix);
  }

  private EmojiRenderer(Parcel in) {
    this(in.readString());
  }

  public static final Creator<EmojiRenderer> CREATOR = new Creator<EmojiRenderer>() {
    @Override
    public EmojiRenderer createFromParcel(Parcel in) {
      return new EmojiRenderer(in);
    }

    @Override
    public EmojiRenderer[] newArray(int size) {
      return new EmojiRenderer[size];
    }
  };

  @Override
  public void render(@NonNull RendererContext rendererContext) {
    super.render(rendererContext);
    rendererContext.save();
    rendererContext.canvasMatrix.concat(projectionMatrix);

    int alpha = paint.getAlpha();
    paint.setAlpha(rendererContext.getAlpha(alpha));
    rendererContext.canvas.drawText(emoji, 0, 0, paint);
    paint.setAlpha(alpha);

    rendererContext.restore();
  }

  @Override
  public boolean hitTest(float x, float y) {
    float[] dst = new float[2];
    inverseProjectionMatrix.mapPoints(dst, new float[]{ x, y });
    return emojiBounds.contains(dst[0], dst[1]);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(emoji);
  }
}
