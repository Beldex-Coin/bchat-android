package io.beldex.bchat.wallet.utils.pincodeview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.beldex.libbchat.utilities.TextSecurePreferences;
import io.beldex.bchat.util.UiMode;
import io.beldex.bchat.util.UiModeUtilities;
import io.beldex.bchat.wallet.utils.pincodeview.managers.AppLockActivity;

import java.util.Objects;

import io.beldex.bchat.R;


public class CustomPinActivity extends AppLockActivity {

    @Override
    public void showForgotDialog() {
    }

    @Override
    public void onPinFailure(int attempts) {
        Log.d(TAG,"onPinFailure");
    }

    @Override
    public void onPinSuccess(int attempts, int pinLockStatus, AppLockActivity appLockActivity) {
        if(pinLockStatus==3) {
            setUpPinPopUp(appLockActivity,true,this);
        }
        else if(pinLockStatus== 7){
            setUpPinPopUp(appLockActivity,false, this);
        }
        else if(pinLockStatus == 4){
            //getWalletValuesFromSharedPreferences(this); //-
            showWalletFragment();
        }
        else if(pinLockStatus==6){
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK,returnIntent);
        }

        Log.d(TAG,"onPinSuccess "+attempts);
    }

    private void setUpPinPopUp(AppLockActivity appLockActivity, boolean status, CustomPinActivity customPinActivity){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.setup_pin_success, null);

        dialog.setView(dialogView);
        boolean isDarkTheme = UiModeUtilities.getUserSelectedUiMode(getApplicationContext()) == UiMode.NIGHT;
        Button okButton = dialogView.findViewById(R.id.okButton);
        TextView title = dialogView.findViewById(R.id.setUpPinSuccessTitle);
        LottieAnimationView animationView = dialogView.findViewById(R.id.success_animation);
        if(isDarkTheme){
            animationView.setAnimation(R.raw.sent);
        }else {
            animationView.setAnimation(R.raw.sent_light);
        }

        if(status){
            title.setText(R.string.your_pin_has_been_set_up_successfully);
        }else{
            title.setText(R.string.your_pin_has_been_changed_successfully);
        }

        AlertDialog alert = dialog.create();
        Objects.requireNonNull(alert.getWindow()).setBackgroundDrawableResource(R.color.transparent);
        alert.setCanceledOnTouchOutside(false);
        alert.show();

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
                appLockActivity.finish();
                if(status){
                    showWalletFragment();
                }
            }
        });

    }

    private void showWalletFragment(){
        TextSecurePreferences.callFiatCurrencyApi(this,true);
        TextSecurePreferences.setIncomingTransactionStatus(this, true);
        TextSecurePreferences.setOutgoingTransactionStatus(this, true);
        TextSecurePreferences.setTransactionsByDateStatus(this,false);
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK,returnIntent);
    }

    @Override
    public int getPinLength() {
        return super.getPinLength();//you can override this method to change the pin length from the default 4
    }
}
