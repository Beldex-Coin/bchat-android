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

        int availableWidth = widthSize - getPaddingStart() - getPaddingEnd();
        int availableHeight = heightSize - getPaddingTop() - getPaddingBottom();

        quoteViewPartMessageLayoutParams = (LayoutParams) quoteViewPartMessage.getLayoutParams();
        quoteViewPartMessageWidth = quoteViewPartMessage.getMeasuredWidth() + quoteViewPartMessageLayoutParams.leftMargin + quoteViewPartMessageLayoutParams.rightMargin;
        quoteViewPartMessageHeight = quoteViewPartMessage.getMeasuredHeight() + quoteViewPartMessageLayoutParams.topMargin + quoteViewPartMessageLayoutParams.bottomMargin;

        quoteViewPartTimeLayoutParams = (LayoutParams) quoteViewPartTime.getLayoutParams();
        quoteViewPartTimeWidth = quoteViewPartTime.getMeasuredWidth() + quoteViewPartTimeLayoutParams.leftMargin + quoteViewPartTimeLayoutParams.rightMargin;
        quoteViewPartTimeHeight = quoteViewPartTime.getMeasuredHeight() + quoteViewPartTimeLayoutParams.topMargin + quoteViewPartTimeLayoutParams.bottomMargin;

        int quoteViewPartMessageLineCount = quoteViewPartMessage.getLineCount();
        float quoteViewPartMessageLastLineWidth = quoteViewPartMessageLineCount > 0 ? quoteViewPartMessage.getLayout().getLineWidth(quoteViewPartMessageLineCount - 1) : 0;

        // The time view sits on the side opposite to the message start. It may only share the last
        // line's row if the last line is short enough to accommodate both the text and time,
        // otherwise the last line (whose left/right edge can carry Latin words in an RTL layout)
        // would overlap the time.
        boolean timeFitsBeside;
        if (quoteViewPartMessageLineCount > 1) {
            timeFitsBeside = quoteViewPartMessageLastLineWidth + quoteViewPartTimeWidth <= availableWidth;
        } else {
            timeFitsBeside = quoteViewPartMessageWidth + quoteViewPartTimeWidth <= availableWidth;
        }

        widthSize = getPaddingStart() + getPaddingEnd();
        heightSize = getPaddingTop() + getPaddingBottom();

        widthSize += availableWidth;
        if (timeFitsBeside) {
            heightSize += quoteViewPartMessageHeight;
        } else {
            heightSize += quoteViewPartMessageHeight + quoteViewPartTimeHeight;
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

        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        int width = right - left;
        int height = bottom - top;

        if (isRtl) {

            // Message aligned to end (right)
            quoteViewPartMessage.layout(
                    width - getPaddingEnd() - quoteViewPartMessage.getMeasuredWidth(),
                    getPaddingTop(),
                    width - getPaddingEnd(),
                    getPaddingTop() + quoteViewPartMessage.getMeasuredHeight()
            );

            // Time aligned to start (left)
            quoteViewPartTime.layout(
                    getPaddingStart(),
                    height - getPaddingBottom() - quoteViewPartTimeHeight,
                    getPaddingStart() + quoteViewPartTimeWidth,
                    height - getPaddingBottom()
            );

        } else {

            // Message aligned to start (left)
            quoteViewPartMessage.layout(
                    getPaddingStart(),
                    getPaddingTop(),
                    getPaddingStart() + quoteViewPartMessage.getMeasuredWidth(),
                    getPaddingTop() + quoteViewPartMessage.getMeasuredHeight()
            );

            // Time aligned to end (right)
            quoteViewPartTime.layout(
                    width - quoteViewPartTimeWidth - getPaddingEnd(),
                    height - getPaddingBottom() - quoteViewPartTimeHeight,
                    width - getPaddingEnd(),
                    height - getPaddingBottom()
            );
        }
    }
}
