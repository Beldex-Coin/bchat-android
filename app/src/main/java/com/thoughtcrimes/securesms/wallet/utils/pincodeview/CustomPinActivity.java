package com.thoughtcrimes.securesms.wallet.utils.pincodeview;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.widget.Toast;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.thoughtcrimes.securesms.wallet.WalletActivity;
import com.thoughtcrimes.securesms.wallet.utils.keyboardview.enums.KeyboardButtonEnum;
import com.thoughtcrimes.securesms.wallet.utils.pincodeview.managers.AppLockActivity;

import lombok.val;


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
    public void onPinSuccess(int attempts,int pinLockStatus) {
        if(pinLockStatus==3 || pinLockStatus==4) {
            String walletName = TextSecurePreferences.getWalletName(this);
            String walletPassword = TextSecurePreferences.getWalletPassword(this);
            if (walletName != null && walletPassword !=null) {
                startWallet(walletName, walletPassword,  false,  false);
            }
        }
        else if(pinLockStatus==6){
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK,returnIntent);
        }

        Log.d(TAG,"onPinSuccess "+attempts);
    }

    private void startWallet(
            String walletName, String walletPassword,
            Boolean fingerprintUsed, Boolean streetmode) {
        String REQUEST_ID = "id";
        String REQUEST_PW = "pw";
        String REQUEST_FINGERPRINT_USED = "fingerprint";
        String REQUEST_STREETMODE = "streetmode";
        String REQUEST_URI = "uri";

        Log.d("startWallet()","");
        TextSecurePreferences.setIncomingTransactionStatus(this, true);
        TextSecurePreferences.setOutgoingTransactionStatus(this, true);
        TextSecurePreferences.setTransactionsByDateStatus(this,false);
        Intent intent =new Intent(this, WalletActivity.class);
        intent.putExtra(REQUEST_ID, walletName);
        intent.putExtra(REQUEST_PW, walletPassword);
        intent.putExtra(REQUEST_FINGERPRINT_USED, fingerprintUsed);
        intent.putExtra(REQUEST_STREETMODE, streetmode);
        //Important
        /*if (uri != null) {
            intent.putExtra(REQUEST_URI, uri)
            uri = null // use only once
        }*/
        startActivity(intent);
    }

    @Override
    public int getPinLength() {
        return super.getPinLength();//you can override this method to change the pin length from the default 4
    }
}
