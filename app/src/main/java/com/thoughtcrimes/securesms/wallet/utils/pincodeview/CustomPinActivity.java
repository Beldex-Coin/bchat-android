package com.thoughtcrimes.securesms.wallet.utils.pincodeview;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.widget.Toast;

import com.thoughtcrimes.securesms.wallet.WalletActivity;
import com.thoughtcrimes.securesms.wallet.utils.keyboardview.enums.KeyboardButtonEnum;
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLockActivity;


public class CustomPinActivity extends AppLockActivity {

    @Override
    public void showForgotDialog() {
       /* Resources res = getResources();
        // Create the builder with required paramaters - Context, Title, Positive Text
        CustomDialog.Builder builder = new CustomDialog.Builder(this,
                res.getString(R.string.activity_dialog_title),
                res.getString(R.string.activity_dialog_accept));
        builder.content(res.getString(R.string.activity_dialog_content));
        builder.negativeText(res.getString(R.string.activity_dialog_decline));

        //Set theme
        builder.darkTheme(false);
        builder.typeface(Typeface.SANS_SERIF);
        builder.positiveColor(res.getColor(R.color.light_blue_500)); // int res, or int colorRes parameter versions available as well.
        builder.negativeColor(res.getColor(R.color.light_blue_500));
        builder.rightToLeft(false); // Enables right to left positioning for languages that may require so.
        builder.titleAlignment(BaseDialog.Alignment.CENTER);
        builder.buttonAlignment(BaseDialog.Alignment.CENTER);
        builder.setButtonStacking(false);

        //Set text sizes
        builder.titleTextSize((int) res.getDimension(R.dimen.activity_dialog_title_size));
        builder.contentTextSize((int) res.getDimension(R.dimen.activity_dialog_content_size));
        builder.positiveButtonTextSize((int) res.getDimension(R.dimen.activity_dialog_positive_button_size));
        builder.negativeButtonTextSize((int) res.getDimension(R.dimen.activity_dialog_negative_button_size));

        //Build the dialog.
        CustomDialog customDialog = builder.build();
        customDialog.setCanceledOnTouchOutside(false);
        customDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        customDialog.setClickListener(new CustomDialog.ClickListener() {
            @Override
            public void onConfirmClick() {
                Toast.makeText(getApplicationContext(), "Yes", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelClick() {
                Toast.makeText(getApplicationContext(), "Cancel", Toast.LENGTH_SHORT).show();
            }
        });

        // Show the dialog.
        customDialog.show();*/
    }

    @Override
    public void onPinFailure(int attempts) {
        Log.d(TAG,"onPinFailure");
    }

    @Override
    public void onPinSuccess(int attempts) {
        Intent i = new Intent(getApplicationContext(), WalletActivity.class);
        startActivity(i);
        Log.d(TAG,"onPinSuccess "+attempts);
    }

    @Override
    public int getPinLength() {
        return super.getPinLength();//you can override this method to change the pin length from the default 4
    }
}
