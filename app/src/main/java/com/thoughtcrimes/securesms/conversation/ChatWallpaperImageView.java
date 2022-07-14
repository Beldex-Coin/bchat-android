package com.thoughtcrimes.securesms.conversation;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

public class ChatWallpaperImageView extends AppCompatImageView {

    public ChatWallpaperImageView(Context context) {
        super(context);
        setScaleType(ImageView.ScaleType.MATRIX);
    }

    public ChatWallpaperImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ImageView.ScaleType.MATRIX);
    }

    public ChatWallpaperImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setScaleType(ImageView.ScaleType.MATRIX);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        computeMatrix();
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        computeMatrix();
        return super.setFrame(l, t, r, b);
    }


    private void computeMatrix() {
        if (getDrawable() == null) return;
        Matrix matrix = getImageMatrix();
        float scaleFactor = getWidth() / (float) getDrawable().getIntrinsicWidth();
        matrix.setScale(scaleFactor, scaleFactor, 0, 0);
        setImageMatrix(matrix);
    }
}
