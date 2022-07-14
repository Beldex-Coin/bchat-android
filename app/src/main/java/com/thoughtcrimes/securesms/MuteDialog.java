package com.thoughtcrimes.securesms;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.w3c.dom.Text;

import java.util.concurrent.TimeUnit;

import io.beldex.bchat.R;

public class MuteDialog extends AlertDialog {


  protected MuteDialog(Context context) {
    super(context);
  }

  protected MuteDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
    super(context, cancelable, cancelListener);
  }

  protected MuteDialog(Context context, int theme) {
    super(context, theme);
  }

  public static void show(final Context context, final @NonNull MuteSelectionListener listener) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BChatAlertDialog_centre);

//    String title   = SpannableString("Alert dialog title");
//
//    // alert dialog title align center
//    title.setSpan(
//            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
//            0,
//            title.length,
//            0
//    )
//
//    // dialog title
//    builder.setTitle(title)




    builder.setTitle(R.string.MuteDialog_mute_notifications);
    /*final AlertDialog notificationDialog = builder.create();
    notificationDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialog) {

        TextView titleText = (TextView) notificationDialog.findViewById(R.id.alertTitle);
        if(titleText != null) {
          titleText.setGravity(Gravity.CENTER);
        }

     *//*   TextView messageText = (TextView) notificationDialog.findViewById(android.R.id.message);
        if(messageText != null) {
          messageText.setGravity(Gravity.CENTER);
        }*//*
      }
    });*/

    builder.setItems(R.array.mute_durations, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, final int which) {
        final long muteUntil;

        switch (which) {
          case 1:  muteUntil = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2);  break;
          case 2:  muteUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);   break;
          case 3:  muteUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7);   break;
          case 4:  muteUntil = Long.MAX_VALUE; break;
          default: muteUntil = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);  break;
        }

        listener.onMuted(muteUntil);
      }
    });

    builder.show();

  }

  public interface MuteSelectionListener {
    public void onMuted(long until);
  }

}
