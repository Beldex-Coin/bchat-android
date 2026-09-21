package io.beldex.bchat.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import io.beldex.bchat.R;

public final class ToastMessageUtil {

    private ToastMessageUtil() {
    }

    public static void showLong(@NonNull Context context, @StringRes int messageResId) {
        show(context, context.getString(messageResId), Toast.LENGTH_LONG);
    }

    public static void show(@NonNull Context context, @NonNull String message, int duration) {
        TextView view = (TextView) LayoutInflater.from(context)
                .inflate(R.layout.view_toast_message, null, false);
        view.setText(message);

        Toast toast = Toast.makeText(context.getApplicationContext(), "", duration);
        toast.setView(view);
        toast.show();
    }
}