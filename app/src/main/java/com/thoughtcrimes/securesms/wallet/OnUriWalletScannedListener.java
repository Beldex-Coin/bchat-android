package com.thoughtcrimes.securesms.wallet;

import com.thoughtcrimes.securesms.data.BarcodeData;

public interface OnUriWalletScannedListener {
    boolean onUriWalletScanned(BarcodeData barcodeData);
}
