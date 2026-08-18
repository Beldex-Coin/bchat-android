package io.beldex.bchat.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Layout;
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

        boolean timeFitsBesideLastLine = isTimeFitsBesideLastLine(quoteViewPartMessageLineCount);

        widthSize = getPaddingStart() + getPaddingEnd();
        heightSize = getPaddingTop() + getPaddingBottom();

        if (quoteViewPartMessageLineCount > 1 && timeFitsBesideLastLine) {
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

    private boolean isTimeFitsBesideLastLine(int quoteViewPartMessageLineCount) {
        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        boolean timeFitsBesideLastLine = false;

        if (quoteViewPartMessageLineCount > 1) {
            Layout messageLayout = quoteViewPartMessage.getLayout();
            int lastLineIndex = quoteViewPartMessageLineCount - 1;
            float lastLineLeft = messageLayout.getLineLeft(lastLineIndex);
            float lastLineRight = messageLayout.getLineRight(lastLineIndex);
            int messageContentWidth = quoteViewPartMessage.getMeasuredWidth() - quoteViewPartMessage.getPaddingStart() - quoteViewPartMessage.getPaddingEnd();

            // The timestamp sits against the bubble's start edge (left in an RTL layout, right in
            // LTR), so it may only share the last line's row when that line's rendered text stays
            // clear of the timestamp's zone. Depending on the paragraph direction the last line can
            // be drawn from either edge, so the line's actual left/right extent is checked instead
            // of assuming it always starts at the same edge as the message.
            if (isRtl) {
                timeFitsBesideLastLine = lastLineLeft + quoteViewPartMessage.getPaddingStart() >= quoteViewPartTimeWidth;
            } else {
                timeFitsBesideLastLine = lastLineRight <= messageContentWidth - quoteViewPartTimeWidth + quoteViewPartMessage.getPaddingEnd();
            }
        }
        return timeFitsBesideLastLine;
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
