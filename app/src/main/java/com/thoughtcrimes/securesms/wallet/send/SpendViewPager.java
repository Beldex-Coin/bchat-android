

package com.thoughtcrimes.securesms.wallet.send;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;


public class SpendViewPager extends ViewPager {

    public interface OnValidateFieldsListener {
        boolean onValidateFields();
    }

    public SpendViewPager(Context context) {
        super(context);
    }

    public SpendViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void next() {
        int pos = getCurrentItem();
        if (validateFields(pos)) {
            setCurrentItem(pos + 1);
        }
    }

    public void previous() {
        setCurrentItem(getCurrentItem() - 1);
    }

    private boolean allowSwipe = true;

    public void allowSwipe(boolean allow) {
        allowSwipe = allow;
    }

    public boolean validateFields(int position) {
        OnValidateFieldsListener c = (OnValidateFieldsListener) ((SendFragmentMain.SpendPagerAdapter) getAdapter()).getFragment(position);
        return c.onValidateFields();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (allowSwipe) return super.onInterceptTouchEvent(event);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (allowSwipe) return super.onTouchEvent(event);
        return false;
    }
}