package io.beldex.bchat.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import io.beldex.bchat.R;

public class QuoteBodyTextViewLayout extends RelativeLayout {
    private TextView quoteViewPartMessage;
    private View quoteViewPartTime;

    private TypedArray a;

    private RelativeLayout.LayoutParams quoteViewPartMessageLayoutParams;
    private int quoteViewPartMessageWidth;
    private int quoteViewPartMessageHeight;

    private RelativeLayout.LayoutParams quoteViewPartTimeLayoutParams;
    private int quoteViewPartTimeWidth;
    private int quoteViewPartTimeHeight;

    public QuoteBodyTextViewLayout(Context context) {
        super(context);
    }

    public QuoteBodyTextViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        a = context.obtainStyledAttributes(attrs, R.styleable.QuoteBodyTextViewLayout, 0, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        try {
            quoteViewPartMessage = (TextView) this.findViewById(a.getResourceId(R.styleable.QuoteBodyTextViewLayout_quoteViewPartMessage, -1));
            quoteViewPartTime = this.findViewById(a.getResourceId(R.styleable.QuoteBodyTextViewLayout_quoteViewPartTime, -1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (quoteViewPartMessage == null || quoteViewPartTime == null || widthSize <= 0) {
            return;
        }

        int availableWidth = widthSize - getPaddingLeft() - getPaddingRight();
        int availableHeight = heightSize - getPaddingTop() - getPaddingBottom();

        quoteViewPartMessageLayoutParams = (LayoutParams) quoteViewPartMessage.getLayoutParams();
        quoteViewPartMessageWidth = quoteViewPartMessage.getMeasuredWidth() + quoteViewPartMessageLayoutParams.leftMargin + quoteViewPartMessageLayoutParams.rightMargin;
        quoteViewPartMessageHeight = quoteViewPartMessage.getMeasuredHeight() + quoteViewPartMessageLayoutParams.topMargin + quoteViewPartMessageLayoutParams.bottomMargin;

        quoteViewPartTimeLayoutParams = (LayoutParams) quoteViewPartTime.getLayoutParams();
        quoteViewPartTimeWidth = quoteViewPartTime.getMeasuredWidth() + quoteViewPartTimeLayoutParams.leftMargin + quoteViewPartTimeLayoutParams.rightMargin;
        quoteViewPartTimeHeight = quoteViewPartTime.getMeasuredHeight() + quoteViewPartTimeLayoutParams.topMargin + quoteViewPartTimeLayoutParams.bottomMargin;

        int quoteViewPartMessageLineCount = quoteViewPartMessage.getLineCount();
        float quoteViewPartMessageLastLineWidth = quoteViewPartMessageLineCount > 0 ? quoteViewPartMessage.getLayout().getLineWidth(quoteViewPartMessageLineCount - 1) : 0;

        widthSize = getPaddingLeft() + getPaddingRight();
        heightSize = getPaddingTop() + getPaddingBottom();

        if (quoteViewPartMessageLineCount > 1 && !(quoteViewPartMessageLastLineWidth + quoteViewPartTimeWidth >= quoteViewPartMessage.getMeasuredWidth())) {
            widthSize += availableWidth;
            heightSize += quoteViewPartMessageHeight;
        } else if (quoteViewPartMessageLineCount > 1 && (quoteViewPartMessageLastLineWidth + quoteViewPartTimeWidth >= availableWidth)) {
            widthSize += availableWidth;
            heightSize += quoteViewPartMessageHeight + quoteViewPartTimeHeight;
        } else if (quoteViewPartMessageLineCount == 1 && (quoteViewPartMessageWidth + quoteViewPartTimeWidth >= availableWidth)) {
            widthSize += availableWidth;
            heightSize += quoteViewPartMessageHeight + quoteViewPartTimeHeight;
        } else {
            widthSize += availableWidth;
            heightSize += quoteViewPartMessageHeight;
        }

        this.setMeasuredDimension(widthSize, heightSize);
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (quoteViewPartMessage == null || quoteViewPartTime == null) {
            return;
        }

        quoteViewPartMessage.layout(
                getPaddingLeft(),
                getPaddingTop(),
                quoteViewPartMessage.getWidth() + getPaddingLeft(),
                quoteViewPartMessage.getHeight() + getPaddingTop());

        quoteViewPartTime.layout(
                right - left - quoteViewPartTimeWidth - getPaddingRight(),
                bottom - top - getPaddingBottom() - quoteViewPartTimeHeight,
                right - left - getPaddingRight(),
                bottom - top - getPaddingBottom());
    }
}
