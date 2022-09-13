package com.thoughtcrimes.securesms.wallet.utils.keyboardview.interfaces;

import android.app.Activity;

public interface LifeCycleInterface {

    public void onActivityResumed(Activity activity);

    public void onActivityPaused(Activity activity);
}
