package io.beldex.bchat.wallet.utils.pincodeview;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * An ImageView that shrinks its larger dimension to become square.
 */
public class SquareImageView extends ImageView {
    public SquareImageView(Context context) {
        super(context);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int measuredWidth = this.getMeasuredWidth();
        int measuredHeight = this.getMeasuredHeight();

        if (measuredHeight > measuredWidth) {
            this.setMeasuredDimension(measuredWidth, measuredWidth);
        } else {
            this.setMeasuredDimension(measuredHeight, measuredHeight);
        }
    }
}
