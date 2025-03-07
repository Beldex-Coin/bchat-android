package io.beldex.bchat.permissions;


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import io.beldex.bchat.R;
import com.beldex.libbchat.utilities.ViewUtil;

public class RationaleDialog {

  public static AlertDialog.Builder createFor(@NonNull Context context, @NonNull String message, @DrawableRes int... drawables) {
    View      view   = LayoutInflater.from(context).inflate(R.layout.permissions_rationale_dialog, null);
    ViewGroup header = view.findViewById(R.id.header_container);
    TextView  text   = view.findViewById(R.id.message);

    for (int i=0;i<drawables.length;i++) {
      ImageView imageView = new ImageView(context);
      imageView.setImageDrawable(ContextCompat.getDrawable(context,drawables[i]));
      imageView.setColorFilter(ContextCompat.getColor(context,R.color.download_icon));
      imageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

      header.addView(imageView);

      if (i != drawables.length - 1) {
        TextView plus = new TextView(context);
        plus.setText("+");
        plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        plus.setTextColor(Color.WHITE);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(ViewUtil.dpToPx(context, 20), 0, ViewUtil.dpToPx(context, 20), 0);

        plus.setLayoutParams(layoutParams);
        header.addView(plus);
      }
    }

    text.setText(message);

    return new AlertDialog.Builder(context, R.style.Theme_TextSecure_Dialog_Rationale).setView(view);
  }

}
