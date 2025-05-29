package io.beldex.bchat.components;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class InputPanel extends LinearLayout {

    public InputPanel(Context context) {
        super(context);
    }

    public InputPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public InputPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public interface MediaListener {
        void onMediaSelected(@NonNull Uri uri, String contentType);
    }
}
