package io.beldex.bchat.wallet.utils.keyboardview.interfaces;

import android.app.Activity;

public interface LifeCycleInterface {

    void onActivityResumed(Activity activity);

    void onActivityPaused(Activity activity);
}
