package com.thoughtcrimes.securesms.wallet.listener;

import com.thoughtcrimes.securesms.data.BarcodeData;

public interface OnUriScannedListener {
    boolean onUriScanned(BarcodeData barcodeData);
}
