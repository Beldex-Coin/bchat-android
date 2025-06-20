package io.beldex.bchat;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import io.beldex.bchat.model.NetworkType;
import io.beldex.bchat.util.LocalHelper;
import timber.log.Timber;

public class BchatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        //Important
        //NightmodeHelper.setPreferredNightmode(this);
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(LocalHelper.setPreferredLocale(context));
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        LocalHelper.updateSystemDefaultLocale(configuration.getLocales().get(0));
        LocalHelper.setPreferredLocale(this);
    }

    static public NetworkType getNetworkType() {
        switch (BuildConfig.NETWORK_TYPE) {
            case "mainnet":
                return NetworkType.NetworkType_Mainnet;
            case "stagenet":
                return NetworkType.NetworkType_Stagenet;
            case "devnet": // flavors cannot start with "test"
                return NetworkType.NetworkType_Testnet;
            default:
                throw new IllegalStateException("unknown net flavor " + BuildConfig.FLAVOR);
        }
    }
}