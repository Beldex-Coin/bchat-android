package io.beldex.bchat.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import android.util.AttributeSet;
import android.widget.TextView;

import io.beldex.bchat.R;

public class SwitchPreferenceCompat extends CheckBoxPreference {

    private Preference.OnPreferenceClickListener listener;

    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutRes();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
