package io.beldex.bchat;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.beldex.libbchat.utilities.ExpirationUtil;
import io.beldex.bchat.R;

public class ExpirationDialog extends AlertDialog {

  protected ExpirationDialog(Context context) {
    super(context);
  }

  protected ExpirationDialog(Context context, int theme) {
    super(context, theme);
  }

  protected ExpirationDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
    super(context, cancelable, cancelListener);
  }

  @SuppressLint("ResourceAsColor")
  public static void show(final Context context,
                          final int currentExpiration,
                          final @NonNull OnClickListener listener)
  {
    final View view = createNumberPickerView(context, currentExpiration);

    AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BChatAlertDialog);
    builder.setTitle(context.getString(R.string.ExpirationDialog_disappearing_messages));
    builder.setView(Gravity.CENTER);
    builder.setView(view);
    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
      int selected = ((android.widget.NumberPicker)view.findViewById(R.id.expiration_number_picker)).getValue();
      listener.onClick(context.getResources().getIntArray(R.array.expiration_times)[selected]);
    });
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.show();
  }

  private static View createNumberPickerView(final Context context, final int currentExpiration) {
    final LayoutInflater   inflater                = LayoutInflater.from(context);
    final View             view                    = inflater.inflate(R.layout.expiration_dialog, null);
    final NumberPicker     numberPickerView        = view.findViewById(R.id.expiration_number_picker);
    final TextView         textView                = view.findViewById(R.id.expiration_details);
    final int[]            expirationTimes         = context.getResources().getIntArray(R.array.expiration_times);
    final String[]         expirationDisplayValues = new String[expirationTimes.length];

    int selectedIndex = expirationTimes.length - 1;

    for (int i=0;i<expirationTimes.length;i++) {
      expirationDisplayValues[i] = ExpirationUtil.getExpirationDisplayValue(context, expirationTimes[i]);

      if ((currentExpiration >= expirationTimes[i]) &&
          (i == expirationTimes.length -1 || currentExpiration < expirationTimes[i+1])) {
        selectedIndex = i;
      }
    }

    numberPickerView.setDisplayedValues(expirationDisplayValues);
    numberPickerView.setMinValue(0);
    numberPickerView.setMaxValue(expirationTimes.length-1);

    NumberPicker.OnValueChangeListener listener = (picker, oldVal, newVal) -> {
      if (newVal == 0) {
        textView.setText(R.string.ExpirationDialog_your_messages_will_not_expire);
      } else {
        textView.setText(context.getString(R.string.ExpirationDialog_your_messages_will_disappear_s_after_they_have_been_seen, picker.getDisplayedValues()[newVal]));
      }
    };

    //New Line
    Typeface face = Typeface.createFromAsset(context.getAssets(),"fonts/open_sans_medium.ttf");
    textView.setTypeface(face);

    numberPickerView.setOnValueChangedListener(listener);
    numberPickerView.setValue(selectedIndex);
    listener.onValueChange(numberPickerView, selectedIndex, selectedIndex);

    return view;
  }

  public interface OnClickListener {
    void onClick(int expirationTime);
  }

}
