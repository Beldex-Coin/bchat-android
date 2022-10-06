package com.thoughtcrimes.securesms.wallet.listener;

import com.thoughtcrimes.securesms.model.Wallet;

public interface OnBlockUpdateListener {
    void onBlockUpdate(final Wallet wallet);
}
