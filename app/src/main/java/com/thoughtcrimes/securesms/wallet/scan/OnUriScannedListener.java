
package com.thoughtcrimes.securesms.wallet.scan;


import com.thoughtcrimes.securesms.data.BarcodeData;

public interface OnUriScannedListener {
    boolean onUriScanned(BarcodeData barcodeData);
}
