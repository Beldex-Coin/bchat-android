package io.beldex.bchat.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import io.beldex.bchat.R;

public class BodyTextViewLayout extends RelativeLayout {
    private TextView viewPartMessage;
    private View viewPartTime;

    private TypedArray a;

    private RelativeLayout.LayoutParams viewPartMessageLayoutParams;
    private int viewPartMessageWidth;
    private int viewPartMessageHeight;

    private RelativeLayout.LayoutParams viewPartTimeLayoutParams;
    private int viewPartTimeWidth;
    private int viewPartTimeHeight;

    public BodyTextViewLayout(Context context) {
        super(context);
    }

    public BodyTextViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        a = context.obtainStyledAttributes(attrs, R.styleable.BodyTextViewLayout, 0, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        try {
            viewPartMessage = (TextView) this.findViewById(a.getResourceId(R.styleable.BodyTextViewLayout_viewPartMessage, -1));
            viewPartTime = this.findViewById(a.getResourceId(R.styleable.BodyTextViewLayout_viewPartTime, -1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (viewPartMessage == null || viewPartTime == null || widthSize <= 0) {
            return;
        }

        int availableWidth = widthSize - getPaddingStart() - getPaddingEnd();
        int availableHeight = heightSize - getPaddingTop() - getPaddingBottom();

        viewPartMessageLayoutParams = (LayoutParams) viewPartMessage.getLayoutParams();
        viewPartMessageWidth = viewPartMessage.getMeasuredWidth() + viewPartMessageLayoutParams.leftMargin + viewPartMessageLayoutParams.rightMargin;
        viewPartMessageHeight = viewPartMessage.getMeasuredHeight() + viewPartMessageLayoutParams.topMargin + viewPartMessageLayoutParams.bottomMargin;

        viewPartTimeLayoutParams = (LayoutParams) viewPartTime.getLayoutParams();
        viewPartTimeWidth = viewPartTime.getMeasuredWidth() + viewPartTimeLayoutParams.leftMargin + viewPartTimeLayoutParams.rightMargin;
        viewPartTimeHeight = viewPartTime.getMeasuredHeight() + viewPartTimeLayoutParams.topMargin + viewPartTimeLayoutParams.bottomMargin;

        int viewPartMessageLineCount = viewPartMessage.getLineCount();
        float viewPartMessageLastLineWidth = viewPartMessageLineCount > 0 ? viewPartMessage.getLayout().getLineWidth(viewPartMessageLineCount - 1) : 0;

        boolean timeFitsBesideLastLine = isTimeFitsBesideLastLine(viewPartMessageLineCount);

        widthSize = getPaddingStart() + getPaddingEnd();
        heightSize = getPaddingTop() + getPaddingBottom();

        if (viewPartMessageLineCount > 1 && timeFitsBesideLastLine) {
            widthSize += viewPartMessageWidth;
            heightSize += viewPartMessageHeight;
        } else if (viewPartMessageLineCount > 1 && (viewPartMessageLastLineWidth + viewPartTimeWidth >= availableWidth)) {
            widthSize += viewPartMessageWidth;
            heightSize += viewPartMessageHeight + viewPartTimeHeight;
        } else if (viewPartMessageLineCount == 1 && (viewPartMessageWidth + viewPartTimeWidth >= availableWidth)) {
            widthSize += viewPartMessage.getMeasuredWidth();
            heightSize += viewPartMessageHeight + viewPartTimeHeight;
        } else {
            widthSize += viewPartMessageWidth + viewPartTimeWidth;
            heightSize += viewPartMessageHeight;
        }

        this.setMeasuredDimension(widthSize, heightSize);
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
    }

    private boolean isTimeFitsBesideLastLine(int viewPartMessageLineCount) {
        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        boolean timeFitsBesideLastLine = false;

        if (viewPartMessageLineCount > 1) {
            Layout messageLayout = viewPartMessage.getLayout();
            int lastLineIndex = viewPartMessageLineCount - 1;
            float lastLineLeft = messageLayout.getLineLeft(lastLineIndex);
            float lastLineRight = messageLayout.getLineRight(lastLineIndex);
            int messageContentWidth = viewPartMessage.getMeasuredWidth() - viewPartMessage.getPaddingStart() - viewPartMessage.getPaddingEnd();

            // The timestamp sits against the bubble's start edge (left in an RTL layout, right in
            // LTR), so it may only share the last line's row when that line's rendered text stays
            // clear of the timestamp's zone. Depending on the paragraph direction the last line can
            // be drawn from either edge, so the line's actual left/right extent is checked instead
            // of assuming it always starts at the same edge as the message.
            if (isRtl) {
                timeFitsBesideLastLine = lastLineLeft + viewPartMessage.getPaddingStart() >= viewPartTimeWidth;
            } else {
                timeFitsBesideLastLine = lastLineRight <= messageContentWidth - viewPartTimeWidth + viewPartMessage.getPaddingEnd();
            }
        }
        return timeFitsBesideLastLine;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (viewPartMessage == null || viewPartTime == null) {
            return;
        }

        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        int width = right - left;
        int height = bottom - top;

        if (isRtl) {

            // Message aligned to end (right)
            viewPartMessage.layout(
                    width - getPaddingEnd() - viewPartMessage.getMeasuredWidth(),
                    getPaddingTop(),
                    width - getPaddingEnd(),
                    getPaddingTop() + viewPartMessage.getMeasuredHeight()
            );

            // Time aligned to start (left)
            viewPartTime.layout(
                    getPaddingStart(),
                    height - getPaddingBottom() - viewPartTimeHeight,
                    getPaddingStart() + viewPartTimeWidth,
                    height - getPaddingBottom()
            );

        } else {

            // Message aligned to start (left)
            viewPartMessage.layout(
                    getPaddingStart(),
                    getPaddingTop(),
                    getPaddingStart() + viewPartMessage.getMeasuredWidth(),
                    getPaddingTop() + viewPartMessage.getMeasuredHeight()
            );

            // Time aligned to end (right)
            viewPartTime.layout(
                    width - viewPartTimeWidth - getPaddingEnd(),
                    height - getPaddingBottom() - viewPartTimeHeight,
                    width - getPaddingEnd(),
                    height - getPaddingBottom()
            );
        }
    }
}
