package io.beldex.bchat.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import io.beldex.bchat.R;

public class SquareFrameLayout extends FrameLayout {

  private final boolean squareHeight;

  @SuppressWarnings("unused")
  public SquareFrameLayout(Context context) {
    this(context, null);
  }

  @SuppressWarnings("unused")
  public SquareFrameLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  @TargetApi(VERSION_CODES.HONEYCOMB) @SuppressWarnings("unused")
  public SquareFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    if (attrs != null) {
      TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SquareFrameLayout, 0, 0);
      this.squareHeight = typedArray.getBoolean(R.styleable.SquareFrameLayout_square_height, false);
      typedArray.recycle();
    } else {
      this.squareHeight = false;
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    //noinspection SuspiciousNameCombination
    if (squareHeight) super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    else              super.onMeasure(widthMeasureSpec, widthMeasureSpec);
  }
}
