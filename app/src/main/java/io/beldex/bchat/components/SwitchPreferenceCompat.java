package io.beldex.bchat.components;

import android.content.Context;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;

import android.util.AttributeSet;

import io.beldex.bchat.R;

public class SwitchPreferenceCompat extends CheckBoxPreference {

    private Preference.OnPreferenceClickListener listener;

    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutRes();
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutRes();
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutRes();
    }

    public SwitchPreferenceCompat(Context context) {
        super(context);
        setLayoutRes();
    }

    private void setLayoutRes() {
        setWidgetLayoutResource(R.layout.switch_compat_preference);
    }

    @Override
    public void setOnPreferenceClickListener(Preference.OnPreferenceClickListener listener) {
        this.listener = listener;
    }
    @Override
    protected void onClick() {
        if (listener == null || !listener.onPreferenceClick(this)) {
            super.onClick();
        }
    }
}
