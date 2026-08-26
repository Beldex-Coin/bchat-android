package io.beldex.bchat.jobmanager.impl;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import io.beldex.bchat.jobmanager.ConstraintObserver;

public class NetworkConstraintObserver implements ConstraintObserver {

    private static final String REASON = NetworkConstraintObserver.class.getSimpleName();

    private final Application application;

    public NetworkConstraintObserver(Application application) {
        this.application = application;
    }

    @Override
    public void register(@NonNull Notifier notifier) {
        ContextCompat.registerReceiver(application, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkConstraint constraint = new NetworkConstraint.Factory(application).create();

                if (constraint.isMet()) {
                    notifier.onConstraintMet(REASON);
                }
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED);
    }
}
