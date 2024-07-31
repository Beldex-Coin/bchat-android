package io.beldex.bchat.wallet;

import io.beldex.bchat.data.BarcodeData;

public interface OnUriWalletScannedListener {
    boolean onUriWalletScanned(BarcodeData barcodeData);
}
